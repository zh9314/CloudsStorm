package provisioning.engine.SEngine;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import commonTool.ClassDB;
import commonTool.CommonTool;
import provisioning.engine.VEngine.VEngine;
import provisioning.engine.VEngine.VEngineOpMethod;
import provisioning.engine.VEngine.adapter.VEngineAdapter;
import provisioning.engine.VEngine.adapter.VEngine_connect;
import provisioning.engine.VEngine.adapter.VEngine_delete;
import provisioning.engine.VEngine.adapter.VEngine_deploy;
import provisioning.engine.VEngine.adapter.VEngine_detach;
import provisioning.engine.VEngine.adapter.VEngine_provision;
import provisioning.engine.VEngine.adapter.VEngine_start;
import provisioning.engine.VEngine.adapter.VEngine_stop;
import provisioning.credential.Credential;
import provisioning.credential.SSHKeyPair;
import provisioning.database.DCMetaInfo;
import provisioning.database.Database;
import provisioning.database.VMMetaInfo;
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
		//create a key pair for this sub-topology, if there is none.
        if (xSubTopology.accessKeyPair == null) {
            String keyPairId = UUID.randomUUID().toString();
            String currentDir = CommonTool.getPathDir(xSubTopology.loadingPath);
            String sshKeyDir = currentDir + keyPairId + File.separator;
            File keyDir = new File(sshKeyDir);
            if (!keyDir.exists()) {
                logger.info("There is no ssh key pair for sub-topology '" + xSubTopology.topologyName + "'! Generating!");
                if (!CommonTool.rsaKeyGenerate(sshKeyDir)) {
                    return false;
                }
            } else 
                logger.info("The ssh key pair for sub-topology '" + xSubTopology.topologyName + "' has already exist!");

            String privateKeyPath = sshKeyDir + "id_rsa";
            File privateKeyFile = new File(privateKeyPath);
            String publicKeyPath = sshKeyDir + "id_rsa.pub";
            File publicKeyFile = new File(publicKeyPath);
            String privateKeyString, publicKeyString;
            try {
                privateKeyString = FileUtils.readFileToString(privateKeyFile, "UTF-8");
                publicKeyString = FileUtils.readFileToString(publicKeyFile, "UTF-8");
            } catch (IOException e) {
                e.printStackTrace();
                logger.error(e.getMessage());
                return false;
            }
            subTopologyInfo.sshKeyPairId = keyPairId;
            xSubTopology.accessKeyPair = new SSHKeyPair();
            xSubTopology.accessKeyPair.publicKeyString = publicKeyString;
            xSubTopology.accessKeyPair.privateKeyString = privateKeyString;
            xSubTopology.accessKeyPair.SSHKeyPairId = keyPairId;
            xSubTopology.accessKeyPair.publicKeyId = keyPairId;
        } else {
        		xSubTopology.accessKeyPair.publicKeyId = xSubTopology.accessKeyPair.SSHKeyPairId;
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
			return false;
		}
		
		if(!checkVEnginesResults(vEAs))
			return false;
        
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
		
		if(!subTopologyInfo.status.trim().equalsIgnoreCase("running")){
			String msg = "Only 'running' sub-topology can be marked as 'failed' for "
					+subTopologyInfo.topology;
			logger.warn(msg);
			subTopologyInfo.logsInfo.put("WARN", msg);
			return false;
		}
		ArrayList<VM> xVMs = xSubTopology.getVMsinSubClass();
		for(int vi = 0 ; vi < xVMs.size() ; vi++){
			VM curVM = xVMs.get(vi);
			subTopologyInfo.status = "failed";
			long failureTime = System.currentTimeMillis();
			subTopologyInfo.logsInfo.put("failed", String.valueOf(failureTime));
			curVM.publicAddress = null;
			if(curVM.vmConnectors != null){
				for(int vapi = 0 ; vapi < curVM.vmConnectors.size() ; vapi++)
					curVM.vmConnectors.get(vapi).ethName = null;
			}
		}
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

		for(int vi = 0 ; vi<xVMs.size() ; vi++)
			xVMs.get(vi).publicAddress = null;
		return true;
	}

	@Override
	public boolean start(SubTopologyInfo subTopologyInfo,
			Credential credential, Database database) {
		SubTopology xSubTopology = subTopologyInfo.subTopology;
		
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
		
		for(int vi = 0 ; vi<xVMs.size() ; vi++)
			xVMs.get(vi).publicAddress = null;
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
	

	
}
