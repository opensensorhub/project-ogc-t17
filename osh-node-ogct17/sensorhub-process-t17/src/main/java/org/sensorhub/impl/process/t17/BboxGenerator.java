/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2021 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.process.t17;

import java.time.Instant;
import org.sensorhub.api.processing.OSHProcessInfo;
import org.sensorhub.impl.process.opencv.CVHelper;
import org.vast.process.ExecutableProcessImpl;
import org.vast.process.ProcessException;
import net.opengis.swe.v20.Count;
import net.opengis.swe.v20.DataArray;
import net.opengis.swe.v20.Time;


/**
 * <p>
 * Used to generate detection bbox at certain points in time on the video
 * time scale
 * </p>
 *
 * @author Alex Robin
 * @date Jun 18, 2021
 */
public class BboxGenerator extends ExecutableProcessImpl
{
    public static final OSHProcessInfo INFO = new OSHProcessInfo("t17:BboxGenerator", "Bbox Generator", "Generate a BBOX at specific points in time", BboxGenerator.class);
    
    protected Time timeIn;
    protected Count numOutputBboxes;
    protected DataArray bboxesOut;
    protected boolean restart = true;
    
    
    public BboxGenerator()
    {
        super(INFO);
        var swe = new CVHelper();
        
        // inputs
        inputData.add("time", timeIn = swe.createTime()
            .asSamplingTimeIsoUTC()
            .build());
        
        // outputs
        outputData.add("objectRois", swe.createRecord()
            .label("Object Bboxes")
            .description("Rectangular image areas containing objects to be tracked")
            .addField("numRois", numOutputBboxes = swe.createCount()
                .id("NUM_ROIS")
                .build())
            .addField("bboxList", bboxesOut = swe.createBboxList(numOutputBboxes)
                .build())
            .build());
        
        restart = true;
    }
    
    
    @Override
    public void execute() throws ProcessException
    {
        var timeStamp = timeIn.getData().getDoubleValue();
        
        var lockTime1 = Instant.parse("2012-06-29T14:33:15.85Z").toEpochMilli() / 1000.;
        var lockTime2 = Instant.parse("2012-06-29T14:33:59.199Z").toEpochMilli() / 1000.;
        var lockTime3 = Instant.parse("2012-06-29T14:37:20Z").toEpochMilli() / 1000.;
        
        if (restart)
        {
            // clear box on startup
            numOutputBboxes.getData().setIntValue(1);
            bboxesOut.updateSize();
            
            bboxesOut.getData().setDoubleValue(0, 0);
            bboxesOut.getData().setDoubleValue(1, 0);
            bboxesOut.getData().setDoubleValue(2, 0);
            bboxesOut.getData().setDoubleValue(3, 0);
        }
        
        // otherwise lookup bbox by lock time
        if (timeStamp >= lockTime1 && timeStamp < lockTime1+0.033)
        {
            numOutputBboxes.getData().setIntValue(1);
            bboxesOut.updateSize();
            
            bboxesOut.getData().setDoubleValue(0, 305);
            bboxesOut.getData().setDoubleValue(1, 200);
            bboxesOut.getData().setDoubleValue(2, 40);
            bboxesOut.getData().setDoubleValue(3, 40);
        }
        else if (timeStamp >= lockTime2 && timeStamp < lockTime2+0.033)
        {
            numOutputBboxes.getData().setIntValue(1);
            bboxesOut.updateSize();
            
            bboxesOut.getData().setDoubleValue(0, 320);
            bboxesOut.getData().setDoubleValue(1, 250);
            bboxesOut.getData().setDoubleValue(2, 40);
            bboxesOut.getData().setDoubleValue(3, 40);
        }
        else if (timeStamp >= lockTime3 && timeStamp < lockTime3+0.033)
        {
            numOutputBboxes.getData().setIntValue(1);
            bboxesOut.updateSize();
            
            bboxesOut.getData().setDoubleValue(0, 0);
            bboxesOut.getData().setDoubleValue(1, 0);
            bboxesOut.getData().setDoubleValue(2, 0);
            bboxesOut.getData().setDoubleValue(3, 0);
        }
        else
        {
            numOutputBboxes.getData().setIntValue(0);
            bboxesOut.updateSize();
        }
        
        
    }
}