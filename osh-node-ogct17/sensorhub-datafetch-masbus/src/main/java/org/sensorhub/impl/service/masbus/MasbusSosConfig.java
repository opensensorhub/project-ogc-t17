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

import java.util.ArrayList;
import java.util.List;
import org.sensorhub.api.client.ClientConfig;
import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.api.config.DisplayInfo.Required;


public class MasbusSosConfig extends ClientConfig
{
    
    @Required
    @DisplayInfo(desc="URL of MASBUS SOS KVP endpoint")
    public String endpoint = "http://masbus.riversideresearch.org:8484/cxf/sos/2.0/kvp";
    
    
    @DisplayInfo(desc="List of procedures to fetch data from")
    public List<String> procedures = new ArrayList<>();
    
    
    @DisplayInfo(desc="New observation polling period in seconds")
    public int pollingPeriod = 60;
    
    
    @Required
    @DisplayInfo(desc="ID of database to insert observations into")
    public String databaseID;
}
