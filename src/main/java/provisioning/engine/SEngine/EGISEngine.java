/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * Copyright © Huan Zhou (SNE, University of Amsterdam) and contributors
 */
package provisioning.engine.SEngine;


import org.apache.log4j.Logger;

import provisioning.database.BasicVMMetaInfo;
import provisioning.database.Database;
import provisioning.database.EGI.EGIDatabase;
import topology.dataStructure.EGI.EGISubTopology;
import topology.dataStructure.EGI.EGIVM;
import topology.description.actual.SubTopologyInfo;

public class EGISEngine extends SEngine {

    private static final Logger logger = Logger.getLogger(EGISEngine.class);


    @Override
    public boolean runtimeCheckandUpdate(SubTopologyInfo subTopologyInfo,
            Database database) {
    	
    		///general update
		if(!super.runtimeCheckandUpdate(subTopologyInfo, database))
			return false;
    			
        ///Update the endpoint information
        EGIDatabase egiDatabase = (EGIDatabase) database;
        EGISubTopology egiSubTopology = (EGISubTopology) subTopologyInfo.subTopology;
        String domain = subTopologyInfo.domain.trim().toLowerCase();
        

        ////update the information of occi ids (os and resource type).
        for (int vi = 0; vi < egiSubTopology.VMs.size(); vi++) {
            EGIVM curVM = egiSubTopology.VMs.get(vi);
            String vmType = curVM.nodeType.toLowerCase().trim();
            String OS = curVM.OStype.toLowerCase().trim();
            BasicVMMetaInfo egiVMMetaInfo = null;
            if((egiVMMetaInfo = ((BasicVMMetaInfo)egiDatabase.getVMMetaInfo(domain, OS, vmType))) == null){
            	 	logger.error("The EGI VM meta information for 'OStype' '" 
            	 			+ curVM.OStype + "' and 'nodeType' '" + curVM.nodeType
            	 			+ "' in domain '" + domain + "' is not known!");
                 return false;
            }
            if(egiVMMetaInfo.extraInfo != null){
	        		curVM.OS_occi_ID = egiVMMetaInfo.extraInfo.get("OS_OCCI_ID");
	        		curVM.Res_occi_ID = egiVMMetaInfo.extraInfo.get("RES_OCCI_ID");
	        }
            if(curVM.OS_occi_ID == null){
	        		logger.error("There must be 'OS_OCCI_ID' information in EC2Database!");
	        		return false;
	        }
            if(curVM.Res_occi_ID == null){
	        		logger.error("There must be 'RES_OCCI_ID' information in EC2Database!");
	        		return false;
	        }
        }

        return true;
    }
   
   
}
