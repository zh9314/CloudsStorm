package provisioning.engine.SEngine;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import commonTool.CommonTool;
import provisioning.credential.Credential;
import provisioning.credential.ExoGENICredential;
import provisioning.credential.SSHKeyPair;
import provisioning.credential.UserCredential;
import provisioning.database.Database;
import provisioning.database.ExoGENI.ExoGENIDatabase;
import provisioning.engine.VEngine.ExoGENI.ExoGENIAgent;
import provisioning.engine.VEngine.ExoGENI.ExoGENIVEngine;
import topologyAnalysis.dataStructure.Eth;
import topologyAnalysis.dataStructure.SubConnection;
import topologyAnalysis.dataStructure.SubConnectionPoint;
import topologyAnalysis.dataStructure.SubTopologyInfo;
import topologyAnalysis.dataStructure.Subnet;
import topologyAnalysis.dataStructure.TopConnectionPoint;
import topologyAnalysis.dataStructure.VM;
import topologyAnalysis.dataStructure.ExoGENI.ExoGENISubTopology;
import topologyAnalysis.dataStructure.ExoGENI.ExoGENIVM;

public class ExoGENISEngine extends SEngine implements SEngineCoreMethod{
	
	private static final Logger logger = Logger.getLogger(ExoGENISEngine.class);

	private ExoGENIAgent exoGENIAgent;

	
	/**
	 * 1. Update the endpoint
	 * 2. Update the OSurl and OSguid for each VM
	 * 3. Every VM get the endpoint.
	 * 4. Generate the sliceName.
	 * 5. To be completed, check the validity of nodeType.
	 */
	@Override
	public boolean runtimeCheckandUpdate(SubTopologyInfo subTopologyInfo,
			Database database) {
		///Update the endpoint information
		ExoGENIDatabase exoGENIDatabase = (ExoGENIDatabase)database;
		ExoGENISubTopology exoGENISubTopology = (ExoGENISubTopology)subTopologyInfo.subTopology;
		String domain = subTopologyInfo.domain;
		if(exoGENIDatabase.domainMap.containsKey(domain)){
			if((subTopologyInfo.endpoint = exoGENIDatabase.domainMap.get(domain)) == null){
				logger.error("Domain '"+domain+"' of sub-topology '"+subTopologyInfo.topology+"' cannot be mapped into some ExoGENI endpoint!");
				return false;
			}
		}else{
			logger.error("Domain '"+domain+"' of sub-topology '"+subTopologyInfo.topology+"' is not a valid ExoGENI domain!");
			return false;
		}
		
		
		for(int vi = 0 ; vi < exoGENISubTopology.components.size() ; vi++){
			ExoGENIVM curVM = exoGENISubTopology.components.get(vi);
			String OS = curVM.OStype;
			curVM.endpoint = subTopologyInfo.endpoint;
			if(exoGENIDatabase.OSMap.containsKey(OS)){
				if((curVM.OSguid = exoGENIDatabase.OSMap.get(OS).OSguid) == null){
					logger.error("The ExoGENI 'OSguid' of VM '"+curVM.name+"' is not known!");
					return false;
				}
				if((curVM.OSurl = exoGENIDatabase.OSMap.get(OS).OSurl) == null){
					logger.error("The ExoGENI 'OSurl' of VM '"+curVM.name+"' is not known!");
					return false;
				}
			}else{
				logger.error("The ExoGENI 'OStype' '"+OS+"' in domain '"+domain+"' is not known!");
				return false;
			}
			
			////for exogeni, the default account for all the VMs are 'root'
			curVM.defaultSSHAccount = "root";
		}
		
		////only when there is no slice name, we need to generate one.
		if(exoGENISubTopology.sliceName == null)	
			exoGENISubTopology.sliceName = exoGENISubTopology.topologyName+"_"+UUID.randomUUID().toString();
		
		//create a key pair for this sub-topology, if there is none.
		if(exoGENISubTopology.accessKeyPair == null){
			String keyPairId = UUID.randomUUID().toString();
			String currentDir = CommonTool.getPathDir(exoGENISubTopology.loadingPath);
			String sshKeyDir = currentDir+keyPairId+File.separator;
			File keyDir = new File(sshKeyDir);
			if(!keyDir.exists()){
				logger.info("There is no ssh key pair for sub-topology '"+exoGENISubTopology.topologyName+"'! Generating!");
				if(!CommonTool.rsaKeyGenerate(sshKeyDir))
					return false;
			}else
				logger.info("The ssh key pair for sub-topology '"+exoGENISubTopology.topologyName+"' has already exist!");
			
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
			exoGENISubTopology.accessKeyPair = new SSHKeyPair();
			exoGENISubTopology.accessKeyPair.publicKeyString = publicKeyString;
			exoGENISubTopology.accessKeyPair.privateKeyString = privateKeyString;
			exoGENISubTopology.accessKeyPair.SSHKeyPairId = keyPairId;
			exoGENISubTopology.accessKeyPair.publicKeyId = keyPairId;
		}else
			exoGENISubTopology.accessKeyPair.publicKeyId = exoGENISubTopology.accessKeyPair.SSHKeyPairId;
	
		return true;
	}
	
	private boolean createSubTopology(SubTopologyInfo subTopologyInfo, Credential credential, Database database){
		ExoGENISubTopology exoGENISubTopology = (ExoGENISubTopology)subTopologyInfo.subTopology;
		ExoGENICredential exoGENICredential = (ExoGENICredential)credential;
		ExoGENIDatabase exoGENIDatabase = (ExoGENIDatabase)database;
		
		if(exoGENIAgent == null){
			exoGENIAgent = new ExoGENIAgent(exoGENIDatabase, exoGENICredential);
		}
		logger.debug("Endpoint for '"+subTopologyInfo.topology+"' is "+subTopologyInfo.endpoint);
		
		long provisioningStart = System.currentTimeMillis();
		boolean result = exoGENIAgent.createSlice(exoGENISubTopology);
		long provisioningEnd = System.currentTimeMillis();
		
		////Configure for ssh account and set ssh max client
		int vmPoolSize = exoGENISubTopology.components.size();
		ExecutorService executor4conf = Executors.newFixedThreadPool(vmPoolSize);
		for(int vi = 0 ; vi < exoGENISubTopology.components.size() ; vi++){
			ExoGENIVM curVM = exoGENISubTopology.components.get(vi);
			String vEngineNameOS = "provisioning.engine.VEngine.ExoGENI.ExoGENIVEngine_";
			if(curVM.OStype.toLowerCase().contains("ubuntu"))
				vEngineNameOS += "ubuntu";
			else{
				logger.warn("The OS type of "+curVM.name+" in sub-topology "+exoGENISubTopology.topologyName+" is not supported yet!");
				continue;
			}
			try {
				Object vEngine = Class.forName(vEngineNameOS).newInstance();
				((ExoGENIVEngine)vEngine).cmd = "all";
				((ExoGENIVEngine)vEngine).curVM = curVM;
				((ExoGENIVEngine)vEngine).privateKeyString = exoGENISubTopology.accessKeyPair.privateKeyString;
				((ExoGENIVEngine)vEngine).publicKeyString = subTopologyInfo.publicKeyString;
				((ExoGENIVEngine)vEngine).userName = subTopologyInfo.userName;
				((ExoGENIVEngine)vEngine).subConnections = exoGENISubTopology.connections;
				((ExoGENIVEngine)vEngine).currentDir = CommonTool.getPathDir(exoGENISubTopology.loadingPath);
				
				executor4conf.execute(((Runnable)vEngine));
			} catch (InstantiationException | IllegalAccessException
					| ClassNotFoundException e) {
				e.printStackTrace();
			}
		}
		executor4conf.shutdown();
		try {
			int count = 0;
			while (!executor4conf.awaitTermination(2, TimeUnit.SECONDS)){
				count++;
				if(count > 200*vmPoolSize){
					logger.error("Unknown error! Some VM cannot be configured!");
					return false;
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
			logger.error("Unexpected error!");
			return false;
		}
			
		long configureEnd = System.currentTimeMillis();
		long configureOverhead = configureEnd - provisioningEnd;
		
		if(result){
			subTopologyInfo.status = "running";
			subTopologyInfo.statusInfo = "provisioning overhead: "+(provisioningEnd-provisioningStart);
			subTopologyInfo.statusInfo += "; configuration overhead: " + configureOverhead;
			if(!exoGENISubTopology.overwirteControlOutput()){
				logger.error("Control information of '"+exoGENISubTopology.topologyName+"' has not been overwritten to the origin file!");
				return false;
			}
			logger.info("The control information of "+exoGENISubTopology.topologyName+" has been written back!");
			
		}
		
		return result;
	}

	@Override
	public boolean provision(SubTopologyInfo subTopologyInfo,
			Credential credential, Database database) {
		if(!subTopologyInfo.status.trim().toLowerCase().equals("fresh")
				&& !subTopologyInfo.status.trim().toLowerCase().equals("deleted")){
			logger.warn("The sub-topology '"+subTopologyInfo.topology+"' is not in the status of 'fresh' or 'deleted'!");
			return false;
		}
		if(createSubTopology(subTopologyInfo, credential, database))
			return true;
		else 
			return false;
	}

	@Override
	public boolean confTopConnection(SubTopologyInfo subTopologyInfo,
			Credential credential, Database database) {
		ExoGENISubTopology exoGENISubTopology = (ExoGENISubTopology)subTopologyInfo.subTopology;
		ExoGENICredential exoGENICredential = (ExoGENICredential)credential;
		ExoGENIDatabase exoGENIDatabase = (ExoGENIDatabase)database;
		String domain = subTopologyInfo.domain.trim().toLowerCase();
		if(subTopologyInfo.endpoint == null){
			if((subTopologyInfo.endpoint = exoGENIDatabase.domainMap.get(domain)) == null){
				logger.error("Domain '"+domain+"' of sub-topology '"+subTopologyInfo.topology+"' cannot be mapped into some ExoGENI endpoint!");
				return false;
			}
		}
		
		if(exoGENIAgent == null){
			exoGENIAgent = new ExoGENIAgent(exoGENIDatabase, exoGENICredential);
		}
		logger.debug("Endpoint for '"+subTopologyInfo.topology+"' is "+subTopologyInfo.endpoint);
		
		////Configure all the inter connections
		ExecutorService executor4conf = Executors.newFixedThreadPool(exoGENISubTopology.components.size());
		for(int vi = 0 ; vi < exoGENISubTopology.components.size() ; vi++){
			ExoGENIVM curVM = exoGENISubTopology.components.get(vi);
			String vEngineNameOS = "provisioning.engine.VEngine.ExoGENI.ExoGENIVEngine_";
			if(curVM.OStype.toLowerCase().contains("ubuntu"))
				vEngineNameOS += "ubuntu";
			else{
				logger.warn("The OS type of "+curVM.name+" in sub-topology "+exoGENISubTopology.topologyName+" is not supported yet!");
				continue;
			}
			try {
				Object vEngine = Class.forName(vEngineNameOS).newInstance();
				((ExoGENIVEngine)vEngine).cmd = "connection";
				((ExoGENIVEngine)vEngine).curVM = curVM;
				((ExoGENIVEngine)vEngine).privateKeyString = exoGENISubTopology.accessKeyPair.privateKeyString;
				((ExoGENIVEngine)vEngine).topConnectors = subTopologyInfo.connectors;
				
				executor4conf.execute(((Runnable)vEngine));
			} catch (InstantiationException | IllegalAccessException
					| ClassNotFoundException e) {
				e.printStackTrace();
			}
		}
		executor4conf.shutdown();
		try {
			int count = 0;
			while (!executor4conf.awaitTermination(2, TimeUnit.SECONDS)){
				count++;
				if(count > 200*exoGENISubTopology.components.size()){
					logger.error("Unknown error! Some VM cannot be configured!");
					return false;
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
			logger.error("Unexpected error!");
			return false;
		}
		
		return true;
	}

	@Override
	public boolean recover(SubTopologyInfo subTopologyInfo,
			Credential credential, Database database) {
		long recoverStart = System.currentTimeMillis();
		if(!subTopologyInfo.status.trim().toLowerCase().equals("failed")){
			logger.warn("The sub-topology '"+subTopologyInfo.topology+"' is not in the status of 'failed'!");
			return false;
		}
		if(createSubTopology(subTopologyInfo, credential, database)){
			long recoverEnd = System.currentTimeMillis();
			subTopologyInfo.statusInfo += "; recovery overhead: " 
					+ (recoverEnd - recoverStart);
			return true;
		}
		else 
			return false;
	}
	
	///detach all the failed or stopped sub-topologies which are originally connected with this sub-topology.
	private boolean detachSubTopology(SubTopologyInfo subTopologyInfo,
			Credential credential, Database database){
		ExoGENISubTopology exoGENISubTopology = (ExoGENISubTopology)subTopologyInfo.subTopology;
		if(subTopologyInfo.connectors == null || subTopologyInfo.connectors.size() == 0)
			return true;
		ExecutorService executor4del = Executors.newFixedThreadPool(subTopologyInfo.connectors.size());
		for(int vi = 0 ; vi < subTopologyInfo.connectors.size() ; vi++){
			TopConnectionPoint curTCP = subTopologyInfo.connectors.get(vi);
			if(curTCP.peerTCP.ethName != null)   ////This means the peer sub-topology is not failed
				continue;
			
			ArrayList<TopConnectionPoint> curConnector = new ArrayList<TopConnectionPoint>();
			curConnector.add(curTCP);
			ExoGENIVM curVM = ((ExoGENIVM)curTCP.belongingVM);
			String vEngineNameOS = "provisioning.engine.VEngine.ExoGENI.ExoGENIVEngine_";
			if(curVM.OStype.toLowerCase().contains("ubuntu"))
				vEngineNameOS += "ubuntu";
			else{
				logger.warn("The OS type of "+curVM.name+" in sub-topology "+exoGENISubTopology.topologyName+" is not supported yet!");
				continue;
			}
			try {
				Object sEngine = Class.forName(vEngineNameOS).newInstance();
				((ExoGENIVEngine)sEngine).cmd = "remove";
				((ExoGENIVEngine)sEngine).curVM = curVM;
				((ExoGENIVEngine)sEngine).privateKeyString = exoGENISubTopology.accessKeyPair.privateKeyString;
				((ExoGENIVEngine)sEngine).topConnectors = curConnector;   ///only one element in this arraylist.
				
				executor4del.execute(((Runnable)sEngine));
			} catch (InstantiationException | IllegalAccessException
					| ClassNotFoundException e) {
				e.printStackTrace();
			}
		}
		executor4del.shutdown();
		try {
			int count = 0;
			while (!executor4del.awaitTermination(2, TimeUnit.SECONDS)){
				count++;
				if(count > 200*subTopologyInfo.connectors.size()){
					logger.error("Unknown error! Some VM cannot be configured!");
					return false;
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
			logger.error("Unexpected error!");
			return false;
		}
		return true;
	}

	@Override
	public boolean markFailure(SubTopologyInfo subTopologyInfo,
			Credential credential, Database database) {
		if(!detachSubTopology(subTopologyInfo, credential, database))
			return false;
		else
			return true;
	}

	@Override
	public boolean supportStop() {
		return false;
	}

	@Override
	public boolean stop(SubTopologyInfo subTopologyInfo, Credential credential,
			Database database) {
		return false;
	}

	@Override
	public boolean start(SubTopologyInfo subTopologyInfo,
			Credential credential, Database database) {
		return false;
	}

	@Override
	public boolean delete(SubTopologyInfo subTopologyInfo,
			Credential credential, Database database) {
		ExoGENISubTopology exoGENISubTopology = (ExoGENISubTopology)subTopologyInfo.subTopology;
		ExoGENICredential exoGENICredential = (ExoGENICredential)credential;
		ExoGENIDatabase exoGENIDatabase = (ExoGENIDatabase)database;
		
		if(exoGENIAgent == null){
			exoGENIAgent = new ExoGENIAgent(exoGENIDatabase, exoGENICredential);
		}
		logger.debug("Endpoint for '"+subTopologyInfo.topology+"' is "+subTopologyInfo.endpoint);
		
		boolean result = exoGENIAgent.deleteSlice(exoGENISubTopology);
		if(result){
			////clear all the information
			for(int vi = 0 ; vi < exoGENISubTopology.components.size() ; vi++){
				ExoGENIVM curVM = exoGENISubTopology.components.get(vi);
				curVM.publicAddress = null;
			}
		}
		exoGENISubTopology.sliceName = null;
		
		return result;
	}

	@Override
	public SubTopologyInfo generateScalingCopy(String domain,
			SubTopologyInfo scalingTemplate, UserCredential userCredential) {
		SubTopologyInfo generatedSTI = new SubTopologyInfo();
		generatedSTI.cloudProvider = scalingTemplate.cloudProvider;
		generatedSTI.copyOf = scalingTemplate.topology;
		generatedSTI.domain = domain;
		generatedSTI.fatherTopology = scalingTemplate;
		generatedSTI.publicKeyString = scalingTemplate.publicKeyString;
		generatedSTI.userName = scalingTemplate.userName;
		generatedSTI.status = "fresh";
		generatedSTI.tag = "scaled";
		generatedSTI.topology = scalingTemplate.topology + "_" + UUID.randomUUID().toString();
		
		ExoGENISubTopology exoGENIsubTopology = new ExoGENISubTopology();
		ExoGENISubTopology tempSubTopology = (ExoGENISubTopology)scalingTemplate.subTopology;
		SSHKeyPair kp = null;
		if((kp = userCredential.sshAccess.get(domain.trim().toLowerCase())) == null){
			generatedSTI.sshKeyPairId = null;
			exoGENIsubTopology.accessKeyPair = null;
		}else{
			generatedSTI.sshKeyPairId = kp.SSHKeyPairId;
			exoGENIsubTopology.accessKeyPair = kp;
		}
		
		String currentDir = CommonTool.getPathDir(tempSubTopology.loadingPath);
		exoGENIsubTopology.loadingPath = currentDir + generatedSTI.topology + ".yml";
		exoGENIsubTopology.topologyName = generatedSTI.topology;
		exoGENIsubTopology.topologyType = "ExoGENI";
		exoGENIsubTopology.duration = ((ExoGENISubTopology)scalingTemplate.subTopology).duration;
		exoGENIsubTopology.components = new ArrayList<ExoGENIVM>();
		for(int vi = 0 ; vi < tempSubTopology.components.size() ; vi++){
			ExoGENIVM curVM = (ExoGENIVM)tempSubTopology.components.get(vi);
			ExoGENIVM newVM = new ExoGENIVM();
			newVM.dockers = curVM.dockers;
			newVM.name = curVM.name;
			newVM.nodeType = curVM.nodeType;
			newVM.OStype = curVM.OStype;
			newVM.role = curVM.role;
			newVM.script = curVM.script;
			newVM.type = curVM.type;
			newVM.v_scriptString = curVM.v_scriptString;
			newVM.ethernetPort = new ArrayList<Eth>();
			if(curVM.ethernetPort != null){
				for(int ei = 0 ; ei < curVM.ethernetPort.size() ; ei++){
					Eth newEth = new Eth();
					newEth.address = curVM.ethernetPort.get(ei).address;
					newEth.connectionName = curVM.ethernetPort.get(ei).connectionName;
					newEth.name = curVM.ethernetPort.get(ei).name;
					newEth.subnetName = curVM.ethernetPort.get(ei).subnetName;
					newVM.ethernetPort.add(newEth);
				}
			}
			
			exoGENIsubTopology.components.add(newVM);
		}
		
		
		if(tempSubTopology.subnets != null){
			exoGENIsubTopology.subnets = new ArrayList<Subnet>();
			for(int si = 0 ; si < tempSubTopology.subnets.size() ; si++){
				Subnet newSubnet = new Subnet();
				Subnet curSubnet = tempSubTopology.subnets.get(si);
				newSubnet.name = curSubnet.name;
				newSubnet.netmask = curSubnet.netmask;
				newSubnet.subnet = curSubnet.subnet;
				exoGENIsubTopology.subnets.add(newSubnet);
			}
		}
		
		if(tempSubTopology.connections != null){
			exoGENIsubTopology.connections = new ArrayList<SubConnection>();
			for(int ci = 0 ; ci < tempSubTopology.connections.size() ; ci++){
				SubConnection newConnection = new SubConnection();
				SubConnection curConnection = tempSubTopology.connections.get(ci);
				newConnection.bandwidth = curConnection.bandwidth;
				newConnection.latency = curConnection.latency;
				newConnection.name = curConnection.name;
				
				newConnection.source = new SubConnectionPoint();
				newConnection.source.address = curConnection.source.address;
				newConnection.source.componentName = curConnection.source.componentName;
				newConnection.source.netmask = curConnection.source.netmask;
				newConnection.source.portName = curConnection.source.portName;
				
				newConnection.target = new SubConnectionPoint();
				newConnection.target.address = curConnection.target.address;
				newConnection.target.componentName = curConnection.target.componentName;
				newConnection.target.netmask = curConnection.target.netmask;
				newConnection.target.portName = curConnection.target.portName;
				
				exoGENIsubTopology.connections.add(newConnection);
			}
		}
		generatedSTI.subTopology = exoGENIsubTopology;
		
		////Calculate the private IP addresses for the connectors.
		////And update the connectors.
		if(scalingTemplate.scalingAddressPool == null){
			logger.error("The address pool for scaling sub-topology cannot be null");
			return null;
		}
		if(scalingTemplate.connectors == null)
			return generatedSTI;
		
		generatedSTI.connectors = new ArrayList<TopConnectionPoint>();
		Map<String, Boolean> scalingAddressPool = scalingTemplate.scalingAddressPool;
		for(int tci = 0 ; tci < scalingTemplate.connectors.size() ; tci++){
			String availableIP = null;
			TopConnectionPoint curTCP = scalingTemplate.connectors.get(tci);
			for (Map.Entry<String, Boolean> entry : scalingAddressPool.entrySet()){
				if(entry.getValue()){   ////this denotes that the IP address is free
					availableIP = entry.getKey();
					///Since the scalingAddressPool contains all the ip addresses,
					///it's very important to test whether this IP address is in the same subnet with its peer connection point.
					String availableSubnet = CommonTool.getSubnet(availableIP, Integer.valueOf(curTCP.peerTCP.netmask));
					String peerSubnet = CommonTool.getSubnet(curTCP.peerTCP.address, Integer.valueOf(curTCP.peerTCP.netmask));
					if(availableSubnet.equals(peerSubnet)){
						entry.setValue(Boolean.FALSE);
						break;
					}
				}
			}
			if(availableIP == null){
				logger.error("No free IP address available in address pool!");
				return null;
			}
			TopConnectionPoint newTCP = new TopConnectionPoint();
			String [] t_VM = curTCP.componentName.split("\\.");
			String VMName = t_VM[1]; 
			newTCP.componentName = generatedSTI.topology + "." + VMName;
			newTCP.address = availableIP;
			newTCP.netmask = curTCP.netmask;
			newTCP.portName = curTCP.portName;
			
			//Create a new peer top connection point
			TopConnectionPoint newPeerTCP = new TopConnectionPoint();
			newPeerTCP.address = curTCP.peerTCP.address;
			newPeerTCP.belongingVM = curTCP.peerTCP.belongingVM;
			newPeerTCP.componentName = curTCP.peerTCP.componentName;
			newPeerTCP.netmask = curTCP.peerTCP.netmask;
			newPeerTCP.portName = curTCP.peerTCP.portName;
			newPeerTCP.peerTCP = newTCP;
			
			newTCP.peerTCP = newPeerTCP;
			
			//Get the VM in the sub-topology
			VM vmInfo = exoGENIsubTopology.getVMinSubClassbyName(VMName);
			if(vmInfo == null){
				logger.error("There is no VM called "+VMName+" in "+exoGENIsubTopology.topologyName);
				return null;
			}
			newTCP.belongingVM = vmInfo;
			
			generatedSTI.connectors.add(newTCP);
		}
		return generatedSTI;
	}

	@Override
	public boolean scaleUp(SubTopologyInfo subTopologyInfo,
			Credential credential, Database database) {
		if(!subTopologyInfo.status.equals("fresh") && !subTopologyInfo.tag.equals("scaled")){
			logger.warn("The sub-topology '"+subTopologyInfo.topology+"' is not a 'scaled' part!");
			return false;
		}
		if(createSubTopology(subTopologyInfo, credential, database))
			return true;
		else 
			return false;
	}

}
