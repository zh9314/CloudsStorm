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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import lambdaInfrs.credential.Credential;
import lambdaInfrs.credential.SSHKeyPair;
import lambdaInfrs.database.DCMetaInfo;
import lambdaInfrs.database.Database;
import lambdaInfrs.database.VMMetaInfo;
import lambdaInfrs.engine.VEngine.VEngine;
import lambdaInfrs.engine.VEngine.VEngineOpMethod;
import lambdaInfrs.engine.VEngine.adapter.VEngineAdapter;
import lambdaInfrs.engine.VEngine.adapter.VEngine_connect;
import lambdaInfrs.engine.VEngine.adapter.VEngine_delete;
import lambdaInfrs.engine.VEngine.adapter.VEngine_deploy;
import lambdaInfrs.engine.VEngine.adapter.VEngine_detach;
import lambdaInfrs.engine.VEngine.adapter.VEngine_provision;
import lambdaInfrs.engine.VEngine.adapter.VEngine_start;
import lambdaInfrs.engine.VEngine.adapter.VEngine_stop;

import org.apache.log4j.Logger;

import commonTool.ClassDB;
import commonTool.CommonTool;
import commonTool.Values;
import topology.description.actual.SubTopology;
import topology.description.actual.SubTopologyInfo;
import topology.description.actual.VM;


public class SEngine implements SEngineKeyMethod {

	private final Logger logger = Logger.getLogger(this.getClass());
	
	/**
	 * All the needed database information for VM is in extraInfo of VM.
	 * All the needed database information for DC is in extraInfo of SubTopology.
	 * After this database is usually not needed. But in case.
	 */
	@Override
	public boolean runtimeCheckandUpdate(SubTopologyInfo subTopologyInfo,
			Database database) {
		///Update the endpoint information
		SubTopology xSubTopology = subTopologyInfo.subTopology;
		String domain = subTopologyInfo.domain.trim().toLowerCase();
		if((subTopologyInfo.endpoint = database.getEndpoint(domain)) == null){
			String msg = "The endpoint of Domain '"+domain+"' for sub-topology '"
					+subTopologyInfo.topology+"' cannot be found!";
			logger.error(msg);
			subTopologyInfo.logsInfo.put(subTopologyInfo.topology+"#ERROR", msg);
			return false;
		}
		
		///Update the datacenter extra information to the sub-topology extra information 
		DCMetaInfo dcMetaInfo = database.getDCMetaInfo(domain);
		if(dcMetaInfo == null){
			String msg = "The Domain information of '"+domain+"' for sub-topology '"
					+subTopologyInfo.topology+"' cannot be found!";
			logger.error(msg);
			subTopologyInfo.logsInfo.put(subTopologyInfo.topology+"#ERROR", msg);
			return false;
		}
		
		if(dcMetaInfo.extraInfo != null){
			for(Map.Entry<String, String> entry: dcMetaInfo.extraInfo.entrySet()){
				String entryKey = entry.getKey();
	    			if(entryKey != null){
	    				if(xSubTopology.extraInfo == null)
	    					xSubTopology.extraInfo = new HashMap<String, String>();
	    				////In the sub-topology extra information definition, there is this field but without value.
	    				////Then update the value of this field according to the database information.
	    				////Otherwise, leave it. 
	    				////Or there is no such definition of this field in sub-topology extra information, update it directly.
	    				if(xSubTopology.extraInfo.containsKey(entryKey) 
	    						&& xSubTopology.extraInfo.get(entryKey) == null)
	    					xSubTopology.extraInfo.put(entryKey, entry.getValue());
	    				if(!xSubTopology.extraInfo.containsKey(entryKey))
	    					xSubTopology.extraInfo.put(entryKey, entry.getValue());
	    			}
			}
		}
		
		///update the VM related information
		ArrayList<VM> vms = xSubTopology.getVMsinSubClass();
		if(vms == null)
			return false;
		for(int vi = 0 ; vi < vms.size() ; vi++){
			VM curVM = vms.get(vi);
			String vmType = curVM.nodeType.toLowerCase().trim();
            String OS = curVM.OStype.toLowerCase().trim();
            VMMetaInfo vmMetaInfo = null;
            if((vmMetaInfo = ((VMMetaInfo)database.getVMMetaInfo(domain, OS, vmType))) == null){
            	 	String msg = "The '"+subTopologyInfo.cloudProvider+"' VM meta information for 'OStype' '" 
            	 			+ curVM.OStype + "' and 'nodeType' '" 
            	 			+ curVM.nodeType + "' in domain '" + domain 
            	 			+ "' is not known!";
        			logger.error(msg);
        			subTopologyInfo.logsInfo.put(subTopologyInfo.topology+"#ERROR", msg);
                 return false;
            }
            curVM.CPU = vmMetaInfo.CPU;
            curVM.Mem = vmMetaInfo.MEM;
            curVM.defaultSSHAccount = vmMetaInfo.DefaultSSHAccount;
            
            ///update extra information for this VM, if there is any. 
        		if(vmMetaInfo.extraInfo != null){
	        		for(Map.Entry<String, String> entry: vmMetaInfo.extraInfo.entrySet()){
	        			String entryKey = entry.getKey();
	        			if(entryKey != null){
	        				if(curVM.extraInfo == null)
	            				curVM.extraInfo = new HashMap<String, String>();
	        				////In the VM extra information definition, there is this field but without value.
	        				////Then update the value of this field according to the database information.
	        				////Otherwise, leave it. 
	        				////Or there is no such definition of this field in VM extra information, update it directly.
	        				if(curVM.extraInfo.containsKey(entryKey) 
	        						&& curVM.extraInfo.get(entryKey) == null)
	        					curVM.extraInfo.put(entryKey, entry.getValue());
	        				if(!curVM.extraInfo.containsKey(entryKey))
	        					curVM.extraInfo.put(entryKey, entry.getValue());
	        			}
	        		}
        		}
		}
		return true;
	}
	
	@Override
	public boolean createAccessSSHKey(SubTopologyInfo subTopologyInfo,
			Credential credential, Database database) {
		SubTopology xSubTopology = subTopologyInfo.subTopology;
		String cp = subTopologyInfo.cloudProvider.trim().toLowerCase();
		String dc = subTopologyInfo.domain.trim().toLowerCase();
		//create a key pair for this sub-topology, if there is none.
        if (xSubTopology.accessKeyPair == null) {
            String keyPairId = cp+"-"+dc;
            String currentDir = CommonTool.getPathDir(xSubTopology.loadingPath);
            String sshKeyDir = currentDir + keyPairId + File.separator;
            File keyDir = new File(sshKeyDir);
            if (!keyDir.exists()) {
                logger.info("There is no ssh key pair for sub-topology '" + xSubTopology.topologyName + "'! Generating!");
                if (!CommonTool.rsaKeyGenerate(sshKeyDir)) {
                    ///it may be caused by there is some illegal character in 'cp' or 'dc' to be directory name 
                		keyPairId = UUID.randomUUID().toString();
                		sshKeyDir = currentDir + keyPairId + File.separator;
                		logger.info("Try another key pair dir "+sshKeyDir);
                		if (!CommonTool.rsaKeyGenerate(sshKeyDir)) {
                			logger.error("FATAL! cannot generate ssh key pair dir!");
                			return false;
                		}
                }
            } else 
                logger.info("The ssh key pair for sub-topology '" + xSubTopology.topologyName + "' has already exist!");

            xSubTopology.accessKeyPair = new SSHKeyPair();
            if(!xSubTopology.accessKeyPair.loadSSHKeyPair(keyPairId, sshKeyDir)){
            		logger.error("Error when loading SSH key Pair from "+sshKeyDir);
            		return false;
            }
            subTopologyInfo.sshKeyPairId = keyPairId;
           
        } else {
            if(xSubTopology.accessKeyPair.publicKeyString == null
            		|| xSubTopology.accessKeyPair.privateKeyString == null){
            		String msg = "Sub-topology "+ subTopologyInfo.topology 
        					+ "cannot be provisioned! Because of missing "
        					+ "the SSH public key string for accessing!";
        			logger.error(msg);
        			subTopologyInfo.logsInfo.put(subTopologyInfo.topology+"#ERROR", msg);
                return false;
            }
        }
		return true;
	}
	


	@Override
	public boolean provision(SubTopologyInfo subTopologyInfo,
			Credential credential, Database database) {
		SubTopology xSubTopology = subTopologyInfo.subTopology;
		
		ArrayList<VEngineAdapter> vEAs = new ArrayList<VEngineAdapter>();
		ArrayList<VM> xVMs = xSubTopology.getVMsinSubClass();
		ExecutorService executor4vm = Executors.newFixedThreadPool(xVMs.size());
		for(int vi = 0 ; vi<xVMs.size() ; vi++){
			VM curVM = xVMs.get(vi);
			VEngine_provision v_provision = new VEngine_provision(
					curVM, credential, database);
			vEAs.add(v_provision);
			executor4vm.execute(v_provision);
		}
		
		executor4vm.shutdown();
		try {
			int count = 0;
			while (!executor4vm.awaitTermination(2, TimeUnit.SECONDS)){
				count++;
				if(count > 100*xVMs.size()){
					logger.error("Some VM cannot be provisioned!");
					subTopologyInfo.status = Values.STStatus.unknown;
					return false;
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
			logger.error("Unexpected error!");
			return false;
		}

		if(!xSubTopology.overwirteControlOutput()){
			String msg = "Control information of '"+xSubTopology.topologyName
					+"' has not been overwritten to the origin file!";
			logger.error(msg);
			subTopologyInfo.logsInfo.put(subTopologyInfo.topology+"#ERROR", msg);
			subTopologyInfo.status = Values.STStatus.running;
			return false;
		}
		
		if(!checkVEnginesResults(vEAs)){
			subTopologyInfo.status = Values.STStatus.unknown;
			return false;
		}
        
		subTopologyInfo.status = Values.STStatus.running;
		return true;
	}

	@Override
	public boolean networkConf(SubTopologyInfo subTopologyInfo,
			Credential credential, Database database) {
		SubTopology xSubTopology = subTopologyInfo.subTopology;
		
		ArrayList<VEngineAdapter> vEAs = new ArrayList<VEngineAdapter>();
		ArrayList<VM> xVMs = xSubTopology.getVMsinSubClass();
		ExecutorService executor4vm = Executors.newFixedThreadPool(xVMs.size());
		for(int vi = 0 ; vi<xVMs.size() ; vi++){
			VM curVM = xVMs.get(vi);
			if(curVM.fake != null && curVM.fake.trim().equalsIgnoreCase("true")){
				continue;
			}
			curVM.fake = null;
			VEngine_connect v_connect = new VEngine_connect(
					curVM, credential, database);
			vEAs.add(v_connect);
			executor4vm.execute(v_connect);
		}
		
		executor4vm.shutdown();
		try {
			int count = 0;
			while (!executor4vm.awaitTermination(2, TimeUnit.SECONDS)){
				count++;
				if(count > 100*xVMs.size()){
					logger.error("Some VM cannot be provisioned!");
					return false;
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
			logger.error("Unexpected error!");
			return false;
		}

		if(!checkVEnginesResults(vEAs))
			return false;
		return true;
	}
	
	@Override
	public boolean envConf(SubTopologyInfo subTopologyInfo,
			Credential credential, Database database) {
		SubTopology xSubTopology = subTopologyInfo.subTopology;
		
		
		ArrayList<VEngineAdapter> vEAs = new ArrayList<VEngineAdapter>();
		ArrayList<VM> xVMs = xSubTopology.getVMsinSubClass();
		ExecutorService executor4vm = Executors.newFixedThreadPool(xVMs.size());
		for(int vi = 0 ; vi<xVMs.size() ; vi++){
			VM curVM = xVMs.get(vi);
			if(curVM.fake != null && curVM.fake.trim().equalsIgnoreCase("true")){
				continue;
			}
			curVM.fake = null;
			VEngine_deploy v_deploy = new VEngine_deploy(
					curVM, credential, database);
			vEAs.add(v_deploy);
			executor4vm.execute(v_deploy);
		}
			
		executor4vm.shutdown();
		try {
			int count = 0;
			while (!executor4vm.awaitTermination(2, TimeUnit.SECONDS)){
				count++;
				if(count > 100*xVMs.size()){
					logger.error("Some VM cannot be provisioned!");
					return false;
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
			logger.error("Unexpected error!");
			return false;
		}
        
		if(!checkVEnginesResults(vEAs))
			return false;
		return true;
	}


	@Override
	public boolean markFailure(SubTopologyInfo subTopologyInfo,
			Credential credential, Database database) {
		SubTopology xSubTopology = subTopologyInfo.subTopology;
		////mark the deleted sub-topology as failed is to make the sub-topology can be recovered from another datacenter
		if(!subTopologyInfo.status.trim().equalsIgnoreCase(Values.STStatus.running)
			&& !subTopologyInfo.status.trim().equalsIgnoreCase(Values.STStatus.deleted)){
			String msg = "Only 'running' and 'deleted' sub-topology can be marked as 'failed' for "
					+subTopologyInfo.topology;
			logger.warn(msg);
			subTopologyInfo.logsInfo.put("WARN", msg);
			return false;
		}
		ArrayList<VM> xVMs = xSubTopology.getVMsinSubClass();
		for(int vi = 0 ; vi < xVMs.size() ; vi++){
			VM curVM = xVMs.get(vi);
			curVM.publicAddress = null;
			if(curVM.vmConnectors != null){
				for(int vapi = 0 ; vapi < curVM.vmConnectors.size() ; vapi++)
					curVM.vmConnectors.get(vapi).ethName = null;
			}
			if(curVM.selfEthAddresses != null){
				for(Map.Entry<String, String> entry : curVM.selfEthAddresses.entrySet())
					curVM.selfEthAddresses.put(entry.getKey(), null);
			}
		}
		long failureTime = System.currentTimeMillis();
		subTopologyInfo.logsInfo.put(subTopologyInfo.topology+"#Failed", String.valueOf(failureTime));
		subTopologyInfo.status = Values.STStatus.failed;
		if(!xSubTopology.overwirteControlOutput()){
			String msg = "Control information of '"+xSubTopology.topologyName
					+"' has not been overwritten to the origin file!";
			logger.error(msg);
			subTopologyInfo.logsInfo.put(subTopologyInfo.topology+"#ERROR", msg);
			return false;
		}
		return true;
	}

	/**
	 * Check whether all the VEngines of these VMs support the stop operation. 
	 * As long as one VM does not support, the whole sub-topology cannot be stopped.
	 * @param subTopologyInfo
	 * @return
	 */
	@Override
	public boolean supportStop(SubTopologyInfo subTopologyInfo) {
		SubTopology xSubTopology = subTopologyInfo.subTopology;
		ArrayList<VM> xVMs = xSubTopology.getVMsinSubClass();
		for(int vi = 0 ; vi < xVMs.size() ; vi++){
			VM curVM = xVMs.get(vi);
			Class<?> CurVEngine = ClassDB.getVEngine(subTopologyInfo.cloudProvider, 
					curVM.VEngineClass, curVM.OStype);
			if(CurVEngine == null){
				logger.error("VEngine cannot be loaded for '"+curVM.name+"'!");
				subTopologyInfo.logsInfo.put(curVM.name, "VEngine not found!");
				return false;
			}
			try {
				Object vEngine = (VEngine)CurVEngine.newInstance();
				if( !((VEngineOpMethod)vEngine).supportStop() ){
					logger.info("VM '"+curVM.name+"' does not support stop operation!");
					return false;
				}
			} catch (InstantiationException | IllegalAccessException e) {
				e.printStackTrace();
				logger.error(e.getMessage());
				subTopologyInfo.logsInfo.put(curVM.name, 
						CurVEngine.getName()+" is not valid!");
				return false;
			}
			
		}
		return true;
	}

	@Override
	public boolean stop(SubTopologyInfo subTopologyInfo, Credential credential,
			Database database) {
		SubTopology xSubTopology = subTopologyInfo.subTopology;
		
		if(!subTopologyInfo.status.trim().equalsIgnoreCase(Values.STStatus.running)){
			String msg = "Only 'running' sub-topology can be stopped for "
					+subTopologyInfo.topology;
			logger.warn(msg);
			subTopologyInfo.logsInfo.put("WARN", msg);
			return false;
		}
		
		ArrayList<VEngineAdapter> vEAs = new ArrayList<VEngineAdapter>();
		ArrayList<VM> xVMs = xSubTopology.getVMsinSubClass();
		ExecutorService executor4vm = Executors.newFixedThreadPool(xVMs.size());
		for(int vi = 0 ; vi<xVMs.size() ; vi++){
			VM curVM = xVMs.get(vi);
			VEngine_stop v_stop = new VEngine_stop(
					curVM, credential, database);
			vEAs.add(v_stop);
			executor4vm.execute(v_stop);
		}
		
		executor4vm.shutdown();
		try {
			int count = 0;
			while (!executor4vm.awaitTermination(2, TimeUnit.SECONDS)){
				count++;
				if(count > 100*xVMs.size()){
					logger.error("Some VM cannot be provisioned!");
					subTopologyInfo.status = Values.STStatus.unknown;
					return false;
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
			logger.error("Unexpected error!");
			return false;
		}

		if(!checkVEnginesResults(vEAs)){
			subTopologyInfo.status = Values.STStatus.unknown;
			return false;
		}

		for(int vi = 0 ; vi<xVMs.size() ; vi++)
			xVMs.get(vi).publicAddress = null;
		
		subTopologyInfo.status = Values.STStatus.stopped;
		return true;
	}

	@Override
	public boolean start(SubTopologyInfo subTopologyInfo,
			Credential credential, Database database) {
		SubTopology xSubTopology = subTopologyInfo.subTopology;
		
		if(!subTopologyInfo.status.trim().equalsIgnoreCase(Values.STStatus.stopped)){
			String msg = "Only 'stopped' sub-topology can be started for "
					+subTopologyInfo.topology;
			logger.warn(msg);
			subTopologyInfo.logsInfo.put("WARN", msg);
			return false;
		}
		
		ArrayList<VEngineAdapter> vEAs = new ArrayList<VEngineAdapter>();
		ArrayList<VM> xVMs = xSubTopology.getVMsinSubClass();
		ExecutorService executor4vm = Executors.newFixedThreadPool(xVMs.size());
		for(int vi = 0 ; vi<xVMs.size() ; vi++){
			VM curVM = xVMs.get(vi);
			VEngine_start v_start = new VEngine_start(
					curVM, credential, database);
			vEAs.add(v_start);
			executor4vm.execute(v_start);
		}
		
		executor4vm.shutdown();
		try {
			int count = 0;
			while (!executor4vm.awaitTermination(2, TimeUnit.SECONDS)){
				count++;
				if(count > 100*xVMs.size()){
					logger.error("Some VM cannot be provisioned!");
					subTopologyInfo.status = Values.STStatus.unknown;
					return false;
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
			logger.error("Unexpected error!");
			return false;
		}

		
		if(!checkVEnginesResults(vEAs)){
			subTopologyInfo.status = Values.STStatus.unknown;
			return false;
		}
		
		subTopologyInfo.status = Values.STStatus.running;
		return true;
	}

	@Override
	public boolean delete(SubTopologyInfo subTopologyInfo,
			Credential credential, Database database) {
		SubTopology xSubTopology = subTopologyInfo.subTopology;
		
		ArrayList<VEngineAdapter> vEAs = new ArrayList<VEngineAdapter>();
		ArrayList<VM> xVMs = xSubTopology.getVMsinSubClass();
		ExecutorService executor4vm = Executors.newFixedThreadPool(xVMs.size());
		for(int vi = 0 ; vi<xVMs.size() ; vi++){
			VM curVM = xVMs.get(vi);
			VEngine_delete v_delete = new VEngine_delete(
					curVM, credential, database);
			vEAs.add(v_delete);
			executor4vm.execute(v_delete);
		}
		
		executor4vm.shutdown();
		try {
			int count = 0;
			while (!executor4vm.awaitTermination(2, TimeUnit.SECONDS)){
				count++;
				if(count > 100*xVMs.size()){
					logger.error("Some VM cannot be provisioned!");
					subTopologyInfo.status = Values.STStatus.unknown;
					return false;
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
			logger.error("Unexpected error!");
			return false;
		}
		if(!checkVEnginesResults(vEAs)){
			subTopologyInfo.status = Values.STStatus.unknown;
			return false;
		}
		
		for(int vi = 0 ; vi<xVMs.size() ; vi++){
			VM curVM = xVMs.get(vi);
			curVM.publicAddress = null;
			if(curVM.vmConnectors != null){
				for(int vapi = 0 ; vapi < curVM.vmConnectors.size() ; vapi++)
					curVM.vmConnectors.get(vapi).ethName = null;
			}
			if(curVM.selfEthAddresses != null){
				for(Map.Entry<String, String> entry : curVM.selfEthAddresses.entrySet())
					curVM.selfEthAddresses.put(entry.getKey(), null);
			}
			curVM.fake = null;
		}
		subTopologyInfo.status = Values.STStatus.deleted;
		return true;
	}


	@Override
	public boolean detach(SubTopologyInfo subTopologyInfo,
			Credential credential, Database database) {
		SubTopology xSubTopology = subTopologyInfo.subTopology;
		
		ArrayList<VEngineAdapter> vEAs = new ArrayList<VEngineAdapter>();
		ArrayList<VM> xVMs = xSubTopology.getVMsinSubClass();
		ExecutorService executor4vm = Executors.newFixedThreadPool(xVMs.size());
		for(int vi = 0 ; vi<xVMs.size() ; vi++){
			VM curVM = xVMs.get(vi);
			if(curVM.fake != null && curVM.fake.trim().equalsIgnoreCase("true")){
				continue;
			}
			curVM.fake = null;
			VEngine_detach v_detach = new VEngine_detach(
					curVM, credential, database);
			vEAs.add(v_detach);
			executor4vm.execute(v_detach);
		}
		
		executor4vm.shutdown();
		try {
			int count = 0;
			while (!executor4vm.awaitTermination(2, TimeUnit.SECONDS)){
				count++;
				if(count > 100*xVMs.size()){
					logger.error("Some VM cannot be provisioned!");
					return false;
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
			logger.error("Unexpected error!");
			return false;
		}

		if(!checkVEnginesResults(vEAs))
			return false;
		return true;
	}

	public boolean checkVEnginesResults(ArrayList<VEngineAdapter> vEngineAdapters){
		for(int vi = 0 ; vi<vEngineAdapters.size() ; vi++)
			if(!vEngineAdapters.get(vi).opResult)
				return false;

		return true;
	}

	@Override
	public boolean supportSeparate() {
		return true;
	}
	

	
}
