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
import provisioning.credential.EGICredential;
import provisioning.credential.SSHKeyPair;
import provisioning.credential.UserCredential;
import provisioning.database.Database;
import provisioning.database.EGI.DomainInfo;
import provisioning.database.EGI.EGIDatabase;
import provisioning.engine.VEngine.EGI.EGIAgent;
import provisioning.engine.VEngine.EGI.EGIVEngine;
import provisioning.engine.VEngine.EGI.EGIVEngine_createVM;
import provisioning.engine.VEngine.EGI.EGIVEngine_startVM;
import topologyAnalysis.dataStructure.Eth;
import topologyAnalysis.dataStructure.SubConnection;
import topologyAnalysis.dataStructure.SubConnectionPoint;
import topologyAnalysis.dataStructure.SubTopologyInfo;
import topologyAnalysis.dataStructure.TopConnectionPoint;
import topologyAnalysis.dataStructure.VM;
import topologyAnalysis.dataStructure.EGI.EGISubTopology;
import topologyAnalysis.dataStructure.EGI.EGIVM;

public class EGISEngine extends SEngine implements SEngineCoreMethod{

	private static final Logger logger = Logger.getLogger(EGISEngine.class);

	private EGIAgent egiAgent;
	
	@Override
	public boolean runtimeCheckandUpdate(SubTopologyInfo subTopologyInfo,
			Database database) {
		///Update the endpoint information
		EGIDatabase egiDatabase = (EGIDatabase)database;
		EGISubTopology egiSubTopology = (EGISubTopology)subTopologyInfo.subTopology;
		String domain = subTopologyInfo.domain.trim().toLowerCase();
		if(!egiDatabase.domainInfos.containsKey(domain)){
			logger.error("Domain '"+domain+"' of sub-topology '"+subTopologyInfo.topology+"' cannot be mapped into some EGI endpoint!");
			return false;
		}
		DomainInfo curDomainInfo = egiDatabase.domainInfos.get(domain);
		subTopologyInfo.endpoint = curDomainInfo.endpoint;
		
		////update the information of occi ids (os and resource type).
		for(int vi = 0 ; vi < egiSubTopology.components.size() ; vi++){
			EGIVM curVM = egiSubTopology.components.get(vi);
			String nodeType = curVM.nodeType.toLowerCase().trim();
			String osType = curVM.OStype.toLowerCase().trim();
			if(!curDomainInfo.resTpls.containsKey(nodeType+"##"+osType)){
				logger.error("The EGI occi ID information of 'OStype' '"+curVM.OStype+"' and 'nodeType' '"+osType+"' in domain '"+domain+"' is not known!");
				return false;
			}
			curVM.OS_occi_ID = curDomainInfo.resTpls.get(nodeType+"##"+osType).OS_occi_ID;
			curVM.Res_occi_ID = curDomainInfo.resTpls.get(nodeType+"##"+osType).res_occi_ID;
			curVM.defaultSSHAccount = curDomainInfo.resTpls.get(nodeType+"##"+osType).defaultSSHAccount;
		}
		
		return true;
	}
	
	private boolean createSubTopology(SubTopologyInfo subTopologyInfo, Credential credential, Database database){
		EGISubTopology egiSubTopology = (EGISubTopology)subTopologyInfo.subTopology;
		EGICredential egiCredential = (EGICredential)credential;
		
		if(egiAgent == null)
			egiAgent = new EGIAgent(egiCredential.proxyFilePath, egiCredential.trustedCertPath);
			
		egiAgent.initClient(subTopologyInfo.endpoint);
		logger.debug("Set endpoint for '"+subTopologyInfo.topology+"' as "+subTopologyInfo.endpoint);
		
		long provisioningStart = System.currentTimeMillis();
		
		////Provisioning VMs based on the occi ids etc.
		//First, create a key pair for this sub-topology, if there is none.
		if(egiSubTopology.accessKeyPair == null){
			String keyPairId = UUID.randomUUID().toString();
			String currentDir = CommonTool.getPathDir(egiSubTopology.loadingPath);
			String sshKeyDir = currentDir+keyPairId+File.separator;
			File keyDir = new File(sshKeyDir);
			if(!keyDir.exists()){
				logger.info("There is no ssh key pair for sub-topology '"+egiSubTopology.topologyName+"'! Generating!");
				if(!CommonTool.rsaKeyGenerate(sshKeyDir))
					return false;
			}else
				logger.info("The ssh key pair for sub-topology '"+egiSubTopology.topologyName+"' has already exist!");
			
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
			egiSubTopology.accessKeyPair = new SSHKeyPair();
			egiSubTopology.accessKeyPair.publicKeyString = publicKeyString;
			egiSubTopology.accessKeyPair.privateKeyString = privateKeyString;
			egiSubTopology.accessKeyPair.SSHKeyPairId = keyPairId;
			egiSubTopology.accessKeyPair.publicKeyId = keyPairId;
		}else
			egiSubTopology.accessKeyPair.publicKeyId = egiSubTopology.accessKeyPair.SSHKeyPairId;
		int vmPoolSize = egiSubTopology.components.size();
		ExecutorService executor4vm = Executors.newFixedThreadPool(vmPoolSize);
		for(int vi = 0 ; vi<egiSubTopology.components.size() ; vi++){
			EGIVM curVM = egiSubTopology.components.get(vi);
			
			EGIVEngine_createVM egiCreateVM = new EGIVEngine_createVM(
					egiAgent, curVM, 
					egiSubTopology.accessKeyPair.publicKeyId,
					egiSubTopology.accessKeyPair.publicKeyString,
					egiSubTopology.accessKeyPair.privateKeyString);
			executor4vm.execute(egiCreateVM);
		}
		
		executor4vm.shutdown();
		try {
			int count = 0;
			while (!executor4vm.awaitTermination(2, TimeUnit.SECONDS)){
				count++;
				if(count > 100*vmPoolSize){
					logger.error("Unknown error! Some subnet cannot be set up!");
					return false;
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
			logger.error("Unexpected error!");
			return false;
		}
		long provisioningEnd = System.currentTimeMillis();
		
		boolean allSuccess = true;
		for(int vi = 0 ; vi<egiSubTopology.components.size() ; vi++){
			EGIVM curVM = egiSubTopology.components.get(vi);
			if(curVM.VMResourceID == null || curVM.publicAddress == null){
				allSuccess = false;
				logger.error(curVM.name+" of sub-topology '"+egiSubTopology.topologyName+"' is not provisioned!");
			}
		}
		if(!allSuccess){   ///Errors happen during provisioning
			return false;
		}else{
			long overhead = provisioningEnd - provisioningStart;
			logger.debug("All the vms have been created! Provisioning overhead: "+overhead);
			subTopologyInfo.status = "running";
			subTopologyInfo.statusInfo = "provisioning overhead: "+overhead;
		}
		
		////Configure all the inner connections
		ExecutorService executor4conf = Executors.newFixedThreadPool(vmPoolSize);
		for(int vi = 0 ; vi < egiSubTopology.components.size() ; vi++){
			EGIVM curVM = egiSubTopology.components.get(vi);
			String vEngineNameOS = "provisioning.engine.VEngine.EGI.EGIVEngine_";
			if(curVM.OStype.toLowerCase().contains("ubuntu"))
				vEngineNameOS += "ubuntu";
			else{
				logger.warn("The OS type of "+curVM.name+" in sub-topology "+egiSubTopology.topologyName+" is not supported yet!");
				continue;
			}
			try {
				Object vEngine = Class.forName(vEngineNameOS).newInstance();
				((EGIVEngine)vEngine).cmd = "all";
				((EGIVEngine)vEngine).curVM = curVM;
				((EGIVEngine)vEngine).egiAgent = this.egiAgent;
				((EGIVEngine)vEngine).privateKeyString = egiSubTopology.accessKeyPair.privateKeyString;
				((EGIVEngine)vEngine).publicKeyString = subTopologyInfo.publicKeyString;
				((EGIVEngine)vEngine).userName = subTopologyInfo.userName;
				((EGIVEngine)vEngine).subConnections = egiSubTopology.connections;
				((EGIVEngine)vEngine).currentDir = CommonTool.getPathDir(egiSubTopology.loadingPath);
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
		subTopologyInfo.statusInfo += "; configuration overhead: " + configureOverhead;
		
		if(!egiSubTopology.overwirteControlOutput()){
			logger.error("Control information of '"+egiSubTopology.topologyName+"' has not been overwritten to the origin file!");
			return false;
		}
		logger.info("The control information of "+egiSubTopology.topologyName+" has been written back!");
		return true;
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
		EGISubTopology egiSubTopology = (EGISubTopology)subTopologyInfo.subTopology;
		EGICredential egiCredential = (EGICredential)credential;
		EGIDatabase egiDatabase = (EGIDatabase)database;
		String domain = subTopologyInfo.domain.trim().toLowerCase();
		if(!egiDatabase.domainInfos.containsKey(domain)){
			logger.error("Domain '"+domain+"' of sub-topology '"+subTopologyInfo.topology+"' cannot be mapped into some EGI endpoint!");
			return false;
		}
		DomainInfo curDomainInfo = egiDatabase.domainInfos.get(domain);
		subTopologyInfo.endpoint = curDomainInfo.endpoint;
		
		if(egiAgent == null)
			egiAgent = new EGIAgent(egiCredential.proxyFilePath, egiCredential.trustedCertPath);
			
		egiAgent.initClient(subTopologyInfo.endpoint);
		logger.debug("Set endpoint for '"+subTopologyInfo.topology+"' as "+subTopologyInfo.endpoint);
		
		////Configure all the inter connections
		ExecutorService executor4conf = Executors.newFixedThreadPool(egiSubTopology.components.size());
		for(int vi = 0 ; vi < egiSubTopology.components.size() ; vi++){
			EGIVM curVM = egiSubTopology.components.get(vi);
			String vEngineNameOS = "provisioning.engine.VEngine.EGI.EGIVEngine_";
			if(curVM.OStype.toLowerCase().contains("ubuntu"))
				vEngineNameOS += "ubuntu";
			else{
				logger.warn("The OS type of "+curVM.name+" in sub-topology "+egiSubTopology.topologyName+" is not supported yet!");
				continue;
			}
			try {
				Object sEngine = Class.forName(vEngineNameOS).newInstance();
				((EGIVEngine)sEngine).cmd = "connection";
				((EGIVEngine)sEngine).curVM = curVM;
				((EGIVEngine)sEngine).egiAgent = this.egiAgent;
				((EGIVEngine)sEngine).privateKeyString = egiSubTopology.accessKeyPair.privateKeyString;
				((EGIVEngine)sEngine).topConnectors = subTopologyInfo.connectors;
				
				executor4conf.execute(((Runnable)sEngine));
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
				if(count > 200*egiSubTopology.components.size()){
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
		EGISubTopology egiSubTopology = (EGISubTopology)subTopologyInfo.subTopology;
		if(subTopologyInfo.connectors == null || subTopologyInfo.connectors.size() == 0)
			return true;
		ExecutorService executor4del = Executors.newFixedThreadPool(subTopologyInfo.connectors.size());
		for(int vi = 0 ; vi < subTopologyInfo.connectors.size() ; vi++){
			TopConnectionPoint curTCP = subTopologyInfo.connectors.get(vi);
			if(curTCP.peerTCP.ethName != null)   ////This means the peer sub-topology is not failed
				continue;
			
			ArrayList<TopConnectionPoint> curConnector = new ArrayList<TopConnectionPoint>();
			curConnector.add(curTCP);
			EGIVM curVM = ((EGIVM)curTCP.belongingVM);
			String vEngineNameOS = "provisioning.engine.VEngine.EGI.EGIVEngine_";
			if(curVM.OStype.toLowerCase().contains("ubuntu"))
				vEngineNameOS += "ubuntu";
			else{
				logger.warn("The OS type of "+curVM.name+" in sub-topology "+egiSubTopology.topologyName+" is not supported yet!");
				continue;
			}
			try {
				Object sEngine = Class.forName(vEngineNameOS).newInstance();
				((EGIVEngine)sEngine).cmd = "remove";
				((EGIVEngine)sEngine).curVM = curVM;
				((EGIVEngine)sEngine).egiAgent = this.egiAgent;
				((EGIVEngine)sEngine).privateKeyString = egiSubTopology.accessKeyPair.privateKeyString;
				((EGIVEngine)sEngine).topConnectors = curConnector;   ///only one element in this arraylist.
				
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
		return true;
	}

	@Override
	public boolean stop(SubTopologyInfo subTopologyInfo, Credential credential,
			Database database) {
		EGISubTopology egiSubTopology = (EGISubTopology)subTopologyInfo.subTopology;
		EGICredential egiCredential = (EGICredential)credential;
		boolean returnResult = true;
		
		if(egiAgent == null)
			egiAgent = new EGIAgent(egiCredential.proxyFilePath, egiCredential.trustedCertPath);	
		egiAgent.initClient(subTopologyInfo.endpoint);
		logger.debug("Set endpoint for '"+subTopologyInfo.topology+"' as "+subTopologyInfo.endpoint);
		
		for(int vi = 0 ; vi < egiSubTopology.components.size() ; vi++){
			EGIVM curVM = egiSubTopology.components.get(vi);
			if(curVM.VMResourceID == null){
				logger.error("The resource location of "+curVM.name+" is unknown!");
				returnResult = false;
			}else{
				if(!egiAgent.stopVM(curVM.VMResourceID)){
					logger.error("VM '"+curVM.name+"' cannot be stopped!");
					returnResult = false;
				}
			}
		}
		return returnResult;
	}
	
	
	private boolean startSubTopology(SubTopologyInfo subTopologyInfo, Credential credential, Database database){
		EGISubTopology egiSubTopology = (EGISubTopology)subTopologyInfo.subTopology;
		EGICredential egiCredential = (EGICredential)credential;
		
		if(egiAgent == null)
			egiAgent = new EGIAgent(egiCredential.proxyFilePath, egiCredential.trustedCertPath);
		egiAgent.initClient(subTopologyInfo.endpoint);
		logger.debug("Set endpoint for '"+subTopologyInfo.topology+"' as "+subTopologyInfo.endpoint);
		
		long startingUpStart = System.currentTimeMillis();
		
		///Normally, the accessKeyPair should not be null
		if(egiSubTopology.accessKeyPair == null){
			logger.warn("The key pair of a stopped sub-topology should not be null!");
			String keyPairId = UUID.randomUUID().toString();
			String currentDir = CommonTool.getPathDir(egiSubTopology.loadingPath);
			String sshKeyDir = currentDir+keyPairId+File.separator;
			File keyDir = new File(sshKeyDir);
			if(!keyDir.exists()){
				logger.info("There is no ssh key pair for sub-topology '"+egiSubTopology.topologyName+"'! Generating!");
				if(!CommonTool.rsaKeyGenerate(sshKeyDir))
					return false;
			}else
				logger.info("The ssh key pair for sub-topology '"+egiSubTopology.topologyName+"' has already exist!");
			
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
			egiSubTopology.accessKeyPair = new SSHKeyPair();
			egiSubTopology.accessKeyPair.publicKeyString = publicKeyString;
			egiSubTopology.accessKeyPair.privateKeyString = privateKeyString;
			egiSubTopology.accessKeyPair.SSHKeyPairId = keyPairId;
			egiSubTopology.accessKeyPair.publicKeyId = keyPairId;
		}else
			egiSubTopology.accessKeyPair.publicKeyId = egiSubTopology.accessKeyPair.SSHKeyPairId;
		
		int vmPoolSize = egiSubTopology.components.size();
		ExecutorService executor4vm = Executors.newFixedThreadPool(vmPoolSize);
		for(int vi = 0 ; vi<egiSubTopology.components.size() ; vi++){
			EGIVM curVM = egiSubTopology.components.get(vi);
			if(curVM.VMResourceID == null){
				logger.error("The resource location of '"+curVM.name+"' in sub-topology '"+egiSubTopology.topologyName+"' cannot be achieved!");
				return false;
			}
			EGIVEngine_startVM egiStartVM = new EGIVEngine_startVM(
					egiAgent, curVM, 
					egiSubTopology.accessKeyPair.privateKeyString);
			executor4vm.execute(egiStartVM);
		}
		
		executor4vm.shutdown();
		try {
			int count = 0;
			while (!executor4vm.awaitTermination(2, TimeUnit.SECONDS)){
				count++;
				if(count > 100*vmPoolSize){
					logger.error("Unknown error! Some subnet cannot be set up!");
					return false;
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
			logger.error("Unexpected error!");
			return false;
		}
		long startingUpEnd = System.currentTimeMillis();
		
		boolean allSuccess = true;
		for(int vi = 0 ; vi<egiSubTopology.components.size() ; vi++){
			EGIVM curVM = egiSubTopology.components.get(vi);
			if(curVM.publicAddress == null){
				allSuccess = false;
				logger.error(curVM.name+" of sub-topology '"+egiSubTopology.topologyName+"' is not fully started!");
			}
		}
		if(!allSuccess){   ///Errors happen during starting up
			return false;
		}else{
			long overhead = startingUpEnd - startingUpStart;
			logger.debug("All the vms have been started! Starting overhead: "+overhead);
			subTopologyInfo.status = "running";
			subTopologyInfo.statusInfo = "starting overhead: "+overhead;
		}
		
		////Just configure all the inner connections
		ExecutorService executor4conf = Executors.newFixedThreadPool(vmPoolSize);
		for(int vi = 0 ; vi < egiSubTopology.components.size() ; vi++){
			EGIVM curVM = egiSubTopology.components.get(vi);
			String vEngineNameOS = "provisioning.engine.VEngine.EGI.EGIVEngine_";
			if(curVM.OStype.toLowerCase().contains("ubuntu"))
				vEngineNameOS += "ubuntu";
			else{
				logger.warn("The OS type of "+curVM.name+" in sub-topology "+egiSubTopology.topologyName+" is not supported yet!");
				continue;
			}
			try {
				Object sEngine = Class.forName(vEngineNameOS).newInstance();
				((EGIVEngine)sEngine).cmd = "connection";
				((EGIVEngine)sEngine).curVM = curVM;
				((EGIVEngine)sEngine).egiAgent = this.egiAgent;
				((EGIVEngine)sEngine).privateKeyString = egiSubTopology.accessKeyPair.privateKeyString;
				((EGIVEngine)sEngine).publicKeyString = subTopologyInfo.publicKeyString;
				((EGIVEngine)sEngine).userName = subTopologyInfo.userName;
				((EGIVEngine)sEngine).subConnections = egiSubTopology.connections;
				((EGIVEngine)sEngine).currentDir = CommonTool.getPathDir(egiSubTopology.loadingPath);
				executor4conf.execute(((Runnable)sEngine));
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
		long configureOverhead = configureEnd - startingUpEnd;
		subTopologyInfo.statusInfo += "; configuration overhead: " + configureOverhead;
		
		if(!egiSubTopology.overwirteControlOutput()){
			logger.error("Control information of '"+egiSubTopology.topologyName+"' has not been overwritten to the origin file!");
			return false;
		}
		logger.info("The control information of "+egiSubTopology.topologyName+" has been written back!");
		return true;
	}

	@Override
	public boolean start(SubTopologyInfo subTopologyInfo,
			Credential credential, Database database) {
		if(!subTopologyInfo.status.trim().toLowerCase().equals("stopped")){
			logger.warn("The sub-topology '"+subTopologyInfo.topology+"' is not in the status of 'stopped'!");
			return false;
		}
		if(startSubTopology(subTopologyInfo, credential, database))
			return true;
		else 
			return false;
	}

	@Override
	public boolean delete(SubTopologyInfo subTopologyInfo,
			Credential credential, Database database) {
		EGISubTopology egiSubTopology = (EGISubTopology)subTopologyInfo.subTopology;
		EGICredential egiCredential = (EGICredential)credential;
		boolean returnResult = true;
		
		if(egiAgent == null)
			egiAgent = new EGIAgent(egiCredential.proxyFilePath, egiCredential.trustedCertPath);
		egiAgent.initClient(subTopologyInfo.endpoint);
		logger.debug("Set endpoint for '"+subTopologyInfo.topology+"' as "+subTopologyInfo.endpoint);
		
		for(int vi = 0 ; vi < egiSubTopology.components.size() ; vi++){
			EGIVM curVM = egiSubTopology.components.get(vi);
			if(curVM.VMResourceID == null){
				logger.error("The resource location of "+curVM.name+" is unknown!");
				returnResult = false;
			}else{
				if(!egiAgent.deleteVM(curVM.VMResourceID)){
					logger.error("VM '"+curVM.name+"' cannot be deleted");
					returnResult = false;
				}
			}
		}
		
		////clear all the information
		for(int vi = 0 ; vi < egiSubTopology.components.size() ; vi++){
			EGIVM curVM = egiSubTopology.components.get(vi);
			curVM.publicAddress = null;
			curVM.VMResourceID = null;
		}
		return returnResult;
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
		
		EGISubTopology egiSubTopology = new EGISubTopology();
		EGISubTopology tempSubTopology = (EGISubTopology)scalingTemplate.subTopology;
		SSHKeyPair kp = null;
		if((kp = userCredential.sshAccess.get(domain.trim().toLowerCase())) == null){
			generatedSTI.sshKeyPairId = null;
			egiSubTopology.accessKeyPair = null;
		}else{
			generatedSTI.sshKeyPairId = kp.SSHKeyPairId;
			egiSubTopology.accessKeyPair = kp;
		}
		
		String currentDir = CommonTool.getPathDir(tempSubTopology.loadingPath);
		egiSubTopology.loadingPath = currentDir + generatedSTI.topology + ".yml";
		egiSubTopology.topologyName = generatedSTI.topology;
		egiSubTopology.topologyType = "EGI";
		egiSubTopology.components = new ArrayList<EGIVM>();
		for(int vi = 0 ; vi < tempSubTopology.components.size() ; vi++){
			EGIVM curVM = (EGIVM)tempSubTopology.components.get(vi);
			EGIVM newVM = new EGIVM();
			newVM.dockers = curVM.dockers;
			newVM.name = curVM.name;
			newVM.nodeType = curVM.nodeType;
			newVM.OStype = curVM.OStype;
			newVM.role = curVM.role;
			newVM.script = curVM.script;
			newVM.type = curVM.type;
			newVM.v_scriptString = curVM.v_scriptString;
			newVM.defaultSSHAccount = curVM.defaultSSHAccount;
			newVM.OS_occi_ID = curVM.OS_occi_ID;
			newVM.Res_occi_ID = curVM.Res_occi_ID;
			newVM.ethernetPort = new ArrayList<Eth>();
			for(int ei = 0 ; ei < curVM.ethernetPort.size() ; ei++){
				Eth newEth = new Eth();
				newEth.address = curVM.ethernetPort.get(ei).address;
				newEth.connectionName = curVM.ethernetPort.get(ei).connectionName;
				newEth.name = curVM.ethernetPort.get(ei).name;
				newEth.subnetName = curVM.ethernetPort.get(ei).subnetName;
				newVM.ethernetPort.add(newEth);
				
			}
			
			egiSubTopology.components.add(newVM);
		}
		
		
		if(tempSubTopology.connections != null){
			egiSubTopology.connections = new ArrayList<SubConnection>();
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
				
				egiSubTopology.connections.add(newConnection);
			}
		}
		generatedSTI.subTopology = egiSubTopology;
		
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
			VM vmInfo = egiSubTopology.getVMinSubClassbyName(VMName);
			if(vmInfo == null){
				logger.error("There is no VM called "+VMName+" in "+egiSubTopology.topologyName);
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
