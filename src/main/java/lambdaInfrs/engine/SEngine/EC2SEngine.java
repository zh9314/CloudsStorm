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
package lambdaInfrs.engine.SEngine;

import java.io.File;
import java.util.ArrayList;
import java.util.UUID;

import lambdaInfrs.credential.Credential;
import lambdaInfrs.credential.SSHKeyPair;
import lambdaInfrs.database.BasicDCMetaInfo;
import lambdaInfrs.database.BasicVMMetaInfo;
import lambdaInfrs.database.Database;
import lambdaInfrs.database.EC2.EC2Database;
import lambdaInfrs.engine.VEngine.EC2.EC2VEngine;

import org.apache.log4j.Logger;

import commonTool.CommonTool;
import topology.dataStructure.EC2.EC2SubTopology;
import topology.dataStructure.EC2.EC2VM;
import topology.description.actual.SubTopology;
import topology.description.actual.SubTopologyInfo;
import topology.description.actual.VM;

public class EC2SEngine extends SEngine {
	
	private static final Logger logger = Logger.getLogger(EC2SEngine.class);
	
	
	/**
	 * 1. Update the AMI information.
	 * 2. Update the endpoint information.
	 * 3. To be completed, check the validity of nodeType.
	 */
	@Override
	public boolean runtimeCheckandUpdate(SubTopologyInfo subTopologyInfo,
			Database database) {
		
		///general update
		if(!super.runtimeCheckandUpdate(subTopologyInfo, database))
			return false;
		
		///Update the endpoint information
		EC2Database ec2Database = (EC2Database)database;
		EC2SubTopology ec2SubTopology = (EC2SubTopology)subTopologyInfo.subTopology;
		String domain = subTopologyInfo.domain.trim().toLowerCase();
		
		for(int vi = 0 ; vi < ec2SubTopology.VMs.size() ; vi++){
			EC2VM curVM = ec2SubTopology.VMs.get(vi);
			String vmType = curVM.nodeType.toLowerCase().trim();
            String OS = curVM.OStype.toLowerCase().trim();
            BasicVMMetaInfo ec2VMMetaInfo = null;
            if((ec2VMMetaInfo = ((BasicVMMetaInfo)ec2Database.getVMMetaInfo(domain, OS, vmType))) == null){
            	 	logger.error("The EC2 VM meta information for 'OStype' '" 
            	 			+ curVM.OStype + "' and 'nodeType' '" 
            	 			+ curVM.nodeType + "' in domain '" + domain 
            	 			+ "' is not known!");
                 return false;
            }
            if(ec2VMMetaInfo.extraInfo != null)
	        		curVM.AMI = ec2VMMetaInfo.extraInfo.get("AMI");
	        if(curVM.AMI == null){
	        		logger.error("There must be 'AMI' information in EC2Database!");
	        		return false;
	        }
	        
	        BasicDCMetaInfo ec2DCMetaInfo = null;
            if((ec2DCMetaInfo = ((BasicDCMetaInfo)ec2Database.getDCMetaInfo(domain))) == null){
            	 	logger.error("The EC2 datacenter meta information for domain '" + domain 
            	 			+ "' is not known!");
                 return false;
            }
            if(ec2DCMetaInfo.extraInfo != null){
	        		curVM.vpcId = ec2DCMetaInfo.extraInfo.get("vpcId");
	        		curVM.subnetId = ec2DCMetaInfo.extraInfo.get("subnetId");
	        		curVM.securityGroupId = ec2DCMetaInfo.extraInfo.get("securityGroupId");
	        		curVM.routeTableId = ec2DCMetaInfo.extraInfo.get("routeTableId");
	        		curVM.internetGatewayId = ec2DCMetaInfo.extraInfo.get("internetGatewayId");
            }
	        
		}
		
		return true;
	}
	
	@Override
	public boolean createAccessSSHKey(SubTopologyInfo subTopologyInfo, 
			Credential credential, Database database) {
		SubTopology ec2SubTopology = subTopologyInfo.subTopology;
		String cp = subTopologyInfo.cloudProvider.trim().toLowerCase();
		String dc = subTopologyInfo.domain.trim().toLowerCase();
		//create a key pair for this sub-topology, if there is none.
		if(ec2SubTopology.accessKeyPair == null){
			String keyPairId = cp+"-"+dc;
			subTopologyInfo.sshKeyPairId = keyPairId;
			String currentDir = CommonTool.getPathDir(ec2SubTopology.loadingPath);
            String sshKeyDir = currentDir + keyPairId + File.separator;
            File keyDir = new File(sshKeyDir);
            if (!keyDir.exists()) {
                logger.info("There is no ssh key pair for sub-topology '" + ec2SubTopology.topologyName + "'! Generating!");
                
                String publicKeyId = "publicKey-"+UUID.randomUUID().toString();
	    			String privateKeyString = 
	    					EC2VEngine.createSSHKeyPair(subTopologyInfo, 
	    							credential, publicKeyId);
	    			if(privateKeyString == null){
	    				logger.error("Unexpected error for creating ssh key pair for sub-topology '"+ec2SubTopology.topologyName+"'!");
	    				return false;
	    			}
	    			ec2SubTopology.accessKeyPair = new SSHKeyPair();
	    			ec2SubTopology.accessKeyPair.publicKeyId = publicKeyId;
	    			ec2SubTopology.accessKeyPair.privateKeyString = privateKeyString;
	    			ec2SubTopology.accessKeyPair.SSHKeyPairId = keyPairId;
                
            } else {
                logger.info("The ssh key pair for sub-topology '" + ec2SubTopology.topologyName + "' has already exist!");
                ec2SubTopology.accessKeyPair = new SSHKeyPair();
                if(!ec2SubTopology.accessKeyPair.loadSSHKeyPair(keyPairId, sshKeyDir)){
                		logger.error("Error when loading SSH key Pair from "+sshKeyDir);
                		return false;
                }
            }

			
		}
		return true;
	}
	
	@Override
	public boolean provision(SubTopologyInfo subTopologyInfo,
			Credential credential, Database database) {
		/////Create a common Subnet if there is a VM do not specify the VPC id etc.
		ArrayList<VM> vms = subTopologyInfo.subTopology.getVMsinSubClass();
		boolean needCommonSubnet = false;
		if(vms != null){
			for(int vi = 0 ; vi < vms.size() ; vi++){
				EC2VM curVM = (EC2VM)vms.get(vi);
				if(curVM.vpcId == null
					|| curVM.subnetId == null 
					|| curVM.securityGroupId == null
					|| curVM.routeTableId == null
					|| curVM.internetGatewayId == null){
					needCommonSubnet = true;
					break;
				}
			}
		}
		if(needCommonSubnet){
			if(!EC2VEngine.createCommonSubnet(subTopologyInfo, credential)){
				logger.error("Cannot create common Subnet!");
				subTopologyInfo.logsInfo.put("ERROR", "Cannot create common Subnet!");
				return false;
			}
		}
		if(super.provision(subTopologyInfo, credential, database))
			return true;
		else
			return false;
	}
	

	@Override
	public boolean supportStop(SubTopologyInfo subTopologyInfo) {
		return true;
	}


	@Override
	public boolean delete(SubTopologyInfo subTopologyInfo,
			Credential credential, Database database) {
		////leverage VEngine to delete all the VMs
		if(!super.delete(subTopologyInfo, credential, database))
			return false;
		
		if(subTopologyInfo.topology.startsWith("_tmp_"))
			return true;
		
		if( ((EC2SubTopology)(subTopologyInfo.subTopology)).WhetherCreateVPC.equalsIgnoreCase("true") ){
			if(!EC2VEngine.deleteVPC(subTopologyInfo, credential))
				return false;
		}
		
		return true;
	}
	
	



}
