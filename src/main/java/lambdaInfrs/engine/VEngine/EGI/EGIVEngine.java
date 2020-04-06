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
package lambdaInfrs.engine.VEngine.EGI;

import java.net.URI;

import lambdaInfrs.credential.Credential;
import lambdaInfrs.credential.EGICredential;
import lambdaInfrs.database.Database;
import lambdaInfrs.engine.VEngine.VEngine;

import org.apache.log4j.Logger;

import topology.dataStructure.EGI.EGISubTopology;
import topology.dataStructure.EGI.EGIVM;
import topology.description.actual.SubTopologyInfo;
import topology.description.actual.VM;

public class EGIVEngine extends VEngine{
	
	private static final Logger logger = Logger.getLogger(EGIVEngine.class);
	
	public static boolean provision(VM subjectVM,
			Credential credential, Database database){
		SubTopologyInfo subTopologyInfo = subjectVM.ponintBack2STI;
		EGISubTopology egiSubTopology = (EGISubTopology)subTopologyInfo.subTopology;
        EGICredential egiCredential = (EGICredential) credential;
        EGIVM curVM = (EGIVM)subjectVM;
		
		EGIAgent egiAgent = new EGIAgent(egiCredential.proxyFilePath, egiCredential.trustedCertPath);
	        
		int rtnum = 0;
        while(!egiAgent.initClient(subTopologyInfo.endpoint)){
        		rtnum++;
        		if(rtnum > egiAgent.retryTimes)
        			break;
        		logger.info("Retry to initial EGI client!");
        }
        logger.debug("Set endpoint for '" + subTopologyInfo.topology 
        					+ "' as " + subTopologyInfo.endpoint);

	     
        if(egiSubTopology.accessKeyPair == null 
        		|| egiSubTopology.accessKeyPair.publicKeyString == null
        		|| egiSubTopology.accessKeyPair.publicKeyId == null){
        		logger.error("Invaild SSH public Key!");
        		return false;
        }
        
        
        URI createURI = egiAgent.createComputeVM(curVM.name, egiSubTopology.accessKeyPair.publicKeyId, 
        				egiSubTopology.accessKeyPair.publicKeyId,
	                curVM.OS_occi_ID, curVM.Res_occi_ID);
        if (createURI == null) {
            logger.error("VM '" + curVM.name + "' cannot be created!");
            return false;
        }
        curVM.VMResourceID = createURI.toString();

        int count = 0;
        while (count < 100) {
            try {
                Thread.sleep(100);
                String vmStatus = egiAgent.getVMStatus(curVM.VMResourceID);
                if (vmStatus.equals("active")) {
                    logger.info("VM " + curVM.VMResourceID + " is active!");
                    break;
                }
                logger.debug("VM: " + curVM.VMResourceID + " status: " + vmStatus);
                count++;
            } catch (InterruptedException ex) {
                logger.error("Unexpected! " + ex);
            }
        }

        boolean addressGot = false;
        String publicAddress = egiAgent.getPubAddress(curVM.VMResourceID);
        if (publicAddress == null) {
            logger.info("VM '" + curVM.name + "' cannot be assigned a public address automatically!");
            ////attach a public address to this VM
            URI pubNetwork;
            if ((pubNetwork = egiAgent.getPublicNetworkURI()) == null) {
                logger.warn("VM '" + curVM.name 
                				+ "' cannot be assigned a public address!");
                subTopologyInfo.logsInfo.put(curVM.name, "No publicIP");
    			
                return false;
            }
            egiAgent.attachPublicNetwork(curVM.VMResourceID, pubNetwork.toString());
        } else if (!publicAddress.equals("")) {
            addressGot = true;
        } else {
        }

        if (!addressGot) {
            //Wait for 2min (120s) for maximum.
            long getAddressStartTime = System.currentTimeMillis();
            long getAddressEndTime = System.currentTimeMillis();
            while ((getAddressEndTime - getAddressStartTime) < 120000) {
                publicAddress = egiAgent.getPubAddress(curVM.VMResourceID);
                if (publicAddress == null) {
                    continue;
                }
                if (!publicAddress.equals("")) {
                    break;
                }
            }
            getAddressEndTime = System.currentTimeMillis();
        }
        if (publicAddress == null) {
            logger.error("Public address of VM '" + curVM.name + "' cannot be assigned!");
            subTopologyInfo.logsInfo.put(curVM.name, "Public address failed");
            return false;
        }

        //Wait for 5min (300s) for maximum.
        count = 0;
        while (count < 40) {
            count++;
            try {
                if (VEngine.isAlive(publicAddress, 22, 
                		egiSubTopology.accessKeyPair.privateKeyString, curVM.defaultSSHAccount)) {
                    logger.info(curVM.name + " (" + publicAddress + ") is activated!");
                    	
                    curVM.publicAddress = publicAddress;

                    
                    return true;
                }
                Thread.sleep(100);
            } catch (InterruptedException ex) {
            		logger.error("Unexpected! " + ex);
            }
        }
        logger.info(curVM.name + " (" + publicAddress + ") is not activated!");
        subTopologyInfo.logsInfo.put(curVM.name, "Provision timeout!");
        return false;
	}
	
	
	public static boolean start(VM subjectVM, Credential credential, Database database) {
		SubTopologyInfo subTopologyInfo = subjectVM.ponintBack2STI;
		EGISubTopology egiSubTopology = (EGISubTopology)subTopologyInfo.subTopology;
        EGICredential egiCredential = (EGICredential) credential;
        EGIVM curVM = (EGIVM)subjectVM;
		EGIAgent egiAgent = new EGIAgent(egiCredential.proxyFilePath, 
								egiCredential.trustedCertPath);
		int rtnum = 0;
        while(!egiAgent.initClient(subTopologyInfo.endpoint)){
        		rtnum++;
        		if(rtnum > egiAgent.retryTimes)
        			break;
        		logger.info("Retry to initial EGI client!");
        }
        logger.debug("Set endpoint for '" + subTopologyInfo.topology 
        						+ "' as " + subTopologyInfo.endpoint);

		if(curVM.VMResourceID == null){
			logger.error("VM '"+curVM.name
						+"' doesn't have valid reource location!");
			return false;
		}
		if(curVM.publicAddress == null){
			logger.error("There is no public address for "+curVM.name);
			return false;
		}
		rtnum = 0;
		boolean success = false;
        while(!success){
        		success = egiAgent.startVM(curVM.VMResourceID);
        		logger.info("Start the VM!");
        		if(rtnum > egiAgent.retryTimes)
        			break;
        		rtnum++;
        }
		if(!success){
			logger.error("VM '"+curVM.name+"' cannot be started!");
			subTopologyInfo.logsInfo.put(curVM.name, "Start failed");
			
			return false;
		}
		
		logger.debug("VM '"+curVM.name
						+"' with address ("+curVM.publicAddress+") is started!");
		
		//Wait for 5min (300s) for maximum.
		long sshStartTime = System.currentTimeMillis();
		long sshEndTime = System.currentTimeMillis();
		while((sshEndTime - sshStartTime) < 300000){
			if(VEngine.isAlive(curVM.publicAddress, 22, egiSubTopology.accessKeyPair.privateKeyString,
								curVM.defaultSSHAccount)){
				logger.info(curVM.name+" ("+curVM.publicAddress+") can be accessed!");
				return true ;
			}
		}
		
		logger.error("VM '"+curVM.name+"' cannot be accessed!");
		subTopologyInfo.logsInfo.put(curVM.name, "Start timeout!");
		return false;
	}
	
	public static boolean stop(VM subjectVM, Credential credential, 
			Database database) {
		SubTopologyInfo subTopologyInfo = subjectVM.ponintBack2STI;
        EGICredential egiCredential = (EGICredential) credential;
        EGIVM curVM = (EGIVM)subjectVM;
		EGIAgent egiAgent = new EGIAgent(egiCredential.proxyFilePath, 
								egiCredential.trustedCertPath);
		int rtnum = 0;
        while(!egiAgent.initClient(subTopologyInfo.endpoint)){
        		rtnum++;
        		if(rtnum > egiAgent.retryTimes)
        			break;
        		logger.info("Retry to initial EGI client!");
        }
        logger.debug("Set endpoint for '" + subTopologyInfo.topology 
        						+ "' as " + subTopologyInfo.endpoint);

		if (curVM.VMResourceID == null) {
            logger.error("The resource location of " + curVM.name 
            					+ " is unknown!");
            return false;
        } else{
            	rtnum = 0;
        		boolean success = false;
            while(!success){
            		success = egiAgent.stopVM(curVM.VMResourceID);
            		logger.info("Stop a VM!");
            		if(rtnum > egiAgent.retryTimes)
            			break;
            		rtnum++;
            }
        		if(!success){
        			logger.error("VM '" + curVM.name + "' cannot be stopped!");
        			subTopologyInfo.logsInfo.put(curVM.name, "Stop failed");
        			
        			return false;
        		}
            return true;
        }
	}
	
	
	public static boolean delete(VM subjectVM, Credential credential, 
							Database database) {
		SubTopologyInfo subTopologyInfo = subjectVM.ponintBack2STI;
        EGICredential egiCredential = (EGICredential) credential;
        EGIVM curVM = (EGIVM)subjectVM;
		EGIAgent egiAgent = new EGIAgent(egiCredential.proxyFilePath, 
								egiCredential.trustedCertPath);
		int rtnum = 0;
        while(!egiAgent.initClient(subTopologyInfo.endpoint)){
        		rtnum++;
        		if(rtnum > egiAgent.retryTimes)
        			break;
        		logger.info("Retry to initial EGI client!");
        }
        logger.debug("Set endpoint for '" + subTopologyInfo.topology 
        						+ "' as " + subTopologyInfo.endpoint);

		if (curVM.VMResourceID == null) {
            logger.error("The resource location of " 
            						+ curVM.name + " is unknown!");
            return false;
        } else {
            rtnum = 0;
        		boolean success = false;
            while(!success){
            		success = egiAgent.deleteVM(curVM.VMResourceID);
            		logger.info("Delete a VM!");
            		if(rtnum > egiAgent.retryTimes)
            			break;
            		rtnum++;
            }
        		if(!success){
        			logger.error("VM '" + curVM.name + "' cannot be deleted");
        			subTopologyInfo.logsInfo.put(curVM.name, "Delete failed");
        			return false;
        		}
        		curVM.publicAddress = null;
            curVM.VMResourceID = null;
            return true;
        }
	}
	
}
