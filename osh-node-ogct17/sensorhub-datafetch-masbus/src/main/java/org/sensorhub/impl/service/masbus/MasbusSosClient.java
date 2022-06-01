/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2021 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.masbus;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.database.IObsSystemDatabase;
import org.sensorhub.impl.client.sos.SOSClient;
import org.sensorhub.impl.module.AbstractModule;
import org.sensorhub.impl.system.DataStreamTransactionHandler;
import org.sensorhub.impl.system.SystemDatabaseTransactionHandler;
import org.sensorhub.impl.system.SystemTransactionHandler;
import org.sensorhub.impl.system.wrapper.SmlFeatureWrapper;
import org.sensorhub.utils.DataComponentChecks;
import org.sensorhub.utils.Lambdas;
import org.vast.data.DataRecordImpl;
import org.vast.data.ScalarIterator;
import org.vast.data.TextEncodingImpl;
import org.vast.ows.sos.GetResultRequest;
import org.vast.ows.swe.DescribeSensorRequest;
import org.vast.swe.SWEConstants;
import org.vast.swe.SWEHelper;
import org.vast.util.TimeExtent;
import com.google.common.hash.HashCode;
import net.opengis.gml.v32.AbstractFeature;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataRecord;
import net.opengis.swe.v20.HasUom;


/**
 * <p>
 * Implementation of batch SOS client to retrieve metadata and observations
 * from MASBUS server
 * </p>
 *
 * @author Alex Robin
 * @since Aug 25, 2021
 */
public class MasbusSosClient extends AbstractModule<MasbusSosConfig>
{
    SOSClient sosClient;
    SystemDatabaseTransactionHandler txnHandler;
    Set<String> foiIds;
    

    @Override
    protected void doInit() throws SensorHubException
    {
        super.doInit();
        
        // get handle to write database
        // use the configured database
        IObsSystemDatabase writeDatabase;
        if (config.databaseID != null)
        {
            writeDatabase = (IObsSystemDatabase)getParentHub().getModuleRegistry()
                .getModuleById(config.databaseID);
        }
        
        // or default to the procedure state DB
        else
        {
            writeDatabase = getParentHub().getSystemDriverRegistry().getSystemStateDatabase();
        }
        
        // prepare transaction handler
        this.txnHandler = new SystemDatabaseTransactionHandler(
            getParentHub().getEventBus(),
            writeDatabase);
        
        // prepare SOS client
        var grRequest = new GetResultRequest();
        grRequest.setGetServer(config.endpoint);
        grRequest.setVersion("2.0.0");
        this.sosClient = new SOSClient(grRequest, false);
        
        this.foiIds = new HashSet<>();
    }


    @Override
    protected void doStart() throws SensorHubException
    {
     // fetch observation and publish to event bus
        CompletableFuture.runAsync(Lambdas.checked(() -> {
            for (var sysUID: config.procedures)
            {
                getLogger().info("Fetching data for procedure {}", sysUID);
                var procHandler = registerSensor(sysUID);
                fetchObservations(procHandler, sysUID);
            }
        }))
        .exceptionally(e -> {
            getLogger().error("Error connecting to MASBUS SOS", e);
            return null;
        });
    }
    
    
    protected SystemTransactionHandler registerSensor(String sysUID) throws SensorHubException
    {
        // request SensorML description
        var smlProc = sosClient.getSensorDescription(sysUID, DescribeSensorRequest.FORMAT_SML_V1_01);
        
        // patch UID and hide I/O signals
        smlProc.setUniqueIdentifier(sysUID);
        var procWrapper = new SmlFeatureWrapper(smlProc)
            .hideOutputs()
            .hideTaskableParams()
            .defaultToValidTime(TimeExtent.endNow(Instant.parse("2019-03-01T00:00:00Z")));
        
        var procHandler = txnHandler.addOrUpdateSystem(procWrapper);
        getLogger().info("Registered new procedure {}", sysUID);

        return procHandler;
    }
    
    
    protected void fetchObservations(SystemTransactionHandler procHandler, String sysUID) throws SensorHubException
    {
        var obsList = sosClient.getObservations(sysUID, TimeExtent.ALL_TIMES);
        Map<HashCode, DataStreamTransactionHandler> dsHandlers = new HashMap<>();
        Set<String> outputNames = new HashSet<>();
                
        for (var obs: obsList)
        {
            var obsProp = obs.getObservedProperty().getHref();
            var result = obs.getResult();
            DataComponent newResult;
            
            // cleanup structure
            var it = new ScalarIterator(result);
            while (it.hasNext())
            {
                var c = it.next();
                if (c instanceof HasUom)
                {
                    var uom = ((HasUom) c).getUom();
                    if (uom.getCode() != null && uom.getCode().equals("sec"))
                        ((HasUom) c).getUom().setCode("s");
                }
            }
            
            // register FOI if needed
            var foi = obs.getFeatureOfInterest();
            if (!foiIds.contains(foi.getId()))
            {
                // patch foi UID
                ((AbstractFeature)foi).setUniqueIdentifier("urn:masbus:foi:" + foi.getId());
                ((AbstractFeature)foi).setName("MASBUS FOI " + foi.getId());
                
                // patch geom CRS
                String crs = ((AbstractFeature)foi).getGeometry().getSrsName();
                if (crs.startsWith("EPSG:"))
                    ((AbstractFeature)foi).getGeometry().setSrsName(crs.replace("EPSG:", SWEConstants.EPSG_URI_PREFIX));
                
                procHandler.addOrUpdateFoi(foi);
                foiIds.add(foi.getId());
            }
            
            // add timestamp to result record
            var swe = new SWEHelper();
            var ts = swe.createTime()
                .name("time")
                .asPhenomenonTimeIsoUTC()
                .value(obs.getPhenomenonTime().begin())
                .build();
            
            if (result instanceof DataRecord)
            {
                ((DataRecord) result).getFieldList().add(0, ts);
                newResult = result;
            }
            else
            {
                newResult = swe.createRecord()
                    .addField("time", ts)
                    .addField("result", result)
                    .build();
            }
            
            var structHC = DataComponentChecks.getStructCompatibilityHashCode(result);
            var dsHandler = dsHandlers.computeIfAbsent(structHC, Lambdas.checked(k -> {
                // find a good output name
                int lastSepIdx = obsProp.lastIndexOf(':');
                if (lastSepIdx < 0)
                    lastSepIdx = obsProp.lastIndexOf('/');
                String obsPropName = lastSepIdx > 0 ? obsProp.substring(lastSepIdx+1) : "output";
                int outputIdx = 1;
                String outputName = obsPropName;
                while (outputNames.contains(outputName))
                    outputName = obsPropName + (outputIdx++);
                
                // get or create datastream handler
                var dataStruct = newResult.copy();
                return procHandler.addOrUpdateDataStream(outputName, dataStruct, new TextEncodingImpl());
            }));
            
            if (result instanceof DataRecordImpl)
                ((DataRecordImpl) result).combineDataBlocks();
            dsHandler.addObs(result.getData());
        }
    }
}
