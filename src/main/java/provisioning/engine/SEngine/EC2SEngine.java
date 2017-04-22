package provisioning.engine.SEngine;

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import commonTool.CommonTool;
import provisioning.credential.Credential;
import provisioning.credential.EC2Credential;
import provisioning.credential.SSHKeyPair;
import provisioning.credential.UserCredential;
import provisioning.database.Database;
import provisioning.database.EC2.EC2Database;
import provisioning.engine.VEngine.EC2.EC2Agent;
import provisioning.engine.VEngine.EC2.EC2VEngine_createSubnet;
import provisioning.engine.VEngine.EC2.EC2VEngine_createVM;
import provisioning.engine.VEngine.EC2.EC2VEngine;
import provisioning.engine.VEngine.EC2.EC2VEngine_startVM;
import topologyAnalysis.dataStructure.Eth;
import topologyAnalysis.dataStructure.SubConnection;
import topologyAnalysis.dataStructure.SubConnectionPoint;
import topologyAnalysis.dataStructure.SubTopologyInfo;
import topologyAnalysis.dataStructure.Subnet;
import topologyAnalysis.dataStructure.TopConnectionPoint;
import topologyAnalysis.dataStructure.VM;
import topologyAnalysis.dataStructure.EC2.EC2SubTopology;
import topologyAnalysis.dataStructure.EC2.EC2Subnet;
import topologyAnalysis.dataStructure.EC2.EC2VM;

public class EC2SEngine extends SEngine implements SEngineCoreMethod{
	
	private static final Logger logger = Logger.getLogger(EC2SEngine.class);

	private EC2Agent ec2Agent;
	
	private boolean setEC2Agent(EC2Credential ec2Credential){
		if(ec2Agent != null){
			logger.warn("The ec2Agent has been initid!");
			return false;
		}
		ec2Agent = new EC2Agent(ec2Credential.accessKey, ec2Credential.secretKey);
		if(ec2Agent != null)
			return true;
		else{
			logger.error("The ec2client cannot be initialized!");
			return false;
		}
	}
	
	private boolean createSubTopology(SubTopologyInfo subTopologyInfo, Credential credential, Database database){
		EC2SubTopology ec2SubTopology = (EC2SubTopology)subTopologyInfo.subTopology;
		EC2Credential ec2Credential = (EC2Credential)credential;
		
		if(ec2Agent == null){
			if(!setEC2Agent(ec2Credential))
				return false;
		}
		ec2Agent.setEndpoint(subTopologyInfo.endpoint);
		logger.debug("Set endpoint for '"+subTopologyInfo.topology+"' "+subTopologyInfo.endpoint);
		
		long provisioningStart = System.currentTimeMillis();
		
		///Define Subnet for these VMs.
		ArrayList<EC2Subnet> actualSubnets = new ArrayList<EC2Subnet>();
		int subnetIndex = -1;
		boolean first = true;
		String vpcId4all = null;
		for(int vi = 0 ; vi<ec2SubTopology.components.size() ; vi++){
			EC2VM curVM = ec2SubTopology.components.get(vi);
			//if the vm does not belong to a subnet
			if(getSubnet(curVM) == null){
				//If there exists a VM without a subnet.
				//All these VMs should be in one VPC.
				if(first){
					first = false;
					////Create the vpc for all the VM without a subnet definition
					vpcId4all = ec2Agent.createVPC("192.168.0.0/16");
					if(vpcId4all == null){
						logger.error("Error happens during creating VPC for sub-topology "+subTopologyInfo.topology );
						return false;
					}
				}
				subnetIndex++;
				EC2Subnet curEC2Subnet = new EC2Subnet();
				curEC2Subnet.org_subnet = new Subnet();
				curEC2Subnet.org_subnet.netmask = "24";
				curEC2Subnet.org_subnet.subnet = "192.168."+subnetIndex+".0";
				curEC2Subnet.vpcId = vpcId4all;
				curVM.actualPrivateAddress = "192.168."+subnetIndex+".10";
				curVM.subnetAllInfo = curEC2Subnet;
				actualSubnets.add(curEC2Subnet);
			}
		}
		for(int si = 0 ; si<ec2SubTopology.subnets.size() ; si++){
			Subnet curSubnet = ec2SubTopology.subnets.get(si);
			EC2Subnet curEC2Subnet = new EC2Subnet();
			curEC2Subnet.org_subnet = curSubnet;
			int VMinSubnet = 0;
			for(int vi = 0 ; vi<ec2SubTopology.components.size() ; vi++){
				EC2VM curVM = ec2SubTopology.components.get(vi);
				Subnet belongingSubnet = null;
				if((belongingSubnet = getSubnet(curVM)) != null){
					if(belongingSubnet.name.equals(curSubnet.name)){
						VMinSubnet++;
						curVM.actualPrivateAddress = getPrivateAddress(curVM);
						curVM.subnetAllInfo = curEC2Subnet;
					}
				}
			}
			if(VMinSubnet != 0)
				actualSubnets.add(curEC2Subnet);
		}
		
		////Create all the subnets using multi threads
		int poolSize = actualSubnets.size();
		ExecutorService executor4subnet = Executors.newFixedThreadPool(poolSize);
		for(int si = 0 ; si < actualSubnets.size() ; si++){
			EC2VEngine_createSubnet ec2createSubnet = new EC2VEngine_createSubnet(
					ec2Agent, actualSubnets.get(si));
			executor4subnet.execute(ec2createSubnet);
		}
		executor4subnet.shutdown();
		try {
			int count = 0;
			while (!executor4subnet.awaitTermination(1, TimeUnit.SECONDS)){
				count++;
				if(count > 100){
					logger.error("Unknown error! Some subnet cannot be set up!");
					return false;
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
			logger.error("Unexpected error!");
			return false;
		}
		
		logger.debug("All the subnets have been created!");
		
		////Provisioning VMs based on the feedback information of subnetId etc.
		//First, create a key pair for this subtopology, if there is none.
		if(ec2SubTopology.accessKeyPair == null){
			String keyPairId = UUID.randomUUID().toString();
			String publicKeyId = "publicKey-"+keyPairId;
			String privateKeyString = ec2Agent.createKeyPair(publicKeyId);
			if(privateKeyString == null){
				logger.error("Unexpected error for creating ssh key pair for sub-topology '"+ec2SubTopology.topologyName+"'!");
				return false;
			}
			subTopologyInfo.sshKeyPairId = keyPairId;
			ec2SubTopology.accessKeyPair = new SSHKeyPair();
			ec2SubTopology.accessKeyPair.publicKeyId = publicKeyId;
			ec2SubTopology.accessKeyPair.privateKeyString = privateKeyString;
			ec2SubTopology.accessKeyPair.SSHKeyPairId = keyPairId;
		}
		int vmPoolSize = ec2SubTopology.components.size();
		ExecutorService executor4vm = Executors.newFixedThreadPool(vmPoolSize);
		for(int vi = 0 ; vi<ec2SubTopology.components.size() ; vi++){
			EC2VM curVM = ec2SubTopology.components.get(vi);
			logger.debug(curVM.name+" "+curVM.actualPrivateAddress
					+" "+curVM.subnetAllInfo.subnetId);
			if(curVM.subnetAllInfo.vpcId == null){
				logger.error("The vpc of '"+curVM.name+"' in sub-topology '"+ec2SubTopology.topologyName+"' cannot be provisioned!");
				return false;
			}else
				curVM.vpcId = curVM.subnetAllInfo.vpcId;
			if(curVM.subnetAllInfo.subnetId == null){
				logger.error("The subnet of '"+curVM.name+"' in sub-topology '"+ec2SubTopology.topologyName+"' cannot be provisioned!");
				return false;
			}else
				curVM.subnetId = curVM.subnetAllInfo.subnetId;
			curVM.securityGroupId = curVM.subnetAllInfo.securityGroupId;
			curVM.routeTableId = curVM.subnetAllInfo.routeTableId;
			curVM.internetGatewayId = curVM.subnetAllInfo.internetGatewayId;
			EC2VEngine_createVM ec2createVM = new EC2VEngine_createVM(
					ec2Agent, curVM, 
					ec2SubTopology.accessKeyPair.publicKeyId, 
					ec2SubTopology.accessKeyPair.privateKeyString);
			executor4vm.execute(ec2createVM);
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
		for(int vi = 0 ; vi<ec2SubTopology.components.size() ; vi++){
			EC2VM curVM = ec2SubTopology.components.get(vi);
			if(curVM.instanceId == null || curVM.publicAddress == null){
				allSuccess = false;
				logger.error(curVM.name+" of sub-topology '"+ec2SubTopology.topologyName+"' is not provisioned!");
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
		for(int vi = 0 ; vi < ec2SubTopology.components.size() ; vi++){
			EC2VM curVM = ec2SubTopology.components.get(vi);
			String vEngineNameOS = "provisioning.engine.VEngine.EC2.EC2VEngine_";
			if(curVM.OStype.toLowerCase().contains("ubuntu"))
				vEngineNameOS += "ubuntu";
			else{
				logger.warn("The OS type of "+curVM.name+" in sub-topology "+ec2SubTopology.topologyName+" is not supported yet!");
				continue;
			}
			try {
				Object vEngine = Class.forName(vEngineNameOS).newInstance();
				((EC2VEngine)vEngine).cmd = "all";
				((EC2VEngine)vEngine).curVM = curVM;
				((EC2VEngine)vEngine).ec2agent = this.ec2Agent;
				((EC2VEngine)vEngine).privateKeyString = ec2SubTopology.accessKeyPair.privateKeyString;
				((EC2VEngine)vEngine).publicKeyString = subTopologyInfo.publicKeyString;
				((EC2VEngine)vEngine).userName = subTopologyInfo.userName;
				((EC2VEngine)vEngine).subConnections = ec2SubTopology.connections;
				((EC2VEngine)vEngine).currentDir = CommonTool.getPathDir(ec2SubTopology.loadingPath);
				//((EC2VEngine)sEngine).topConnectors = subTopologyInfo.connectors;
				//logger.debug("wtf?");
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
		
		if(!ec2SubTopology.overwirteControlOutput()){
			logger.error("Control information of '"+ec2SubTopology.topologyName+"' has not been overwritten to the origin file!");
			return false;
		}
		logger.info("The control information of "+ec2SubTopology.topologyName+" has been written back!");
		return true;
	}
	
	@Override
	public boolean provision(SubTopologyInfo subTopologyInfo, Credential credential, Database database) {
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
	
	//To test whether the VM belongs to a subnet.
	//If not, then return null.
	private Subnet getSubnet(EC2VM curVM){
		for(int ei = 0 ; ei<curVM.ethernetPort.size() ; ei++){
			if(curVM.ethernetPort.get(ei).subnet != null)
				return curVM.ethernetPort.get(ei).subnet;
		}
		return null;
	}
	
	//To get the actual private address of the VM, if the VM belongs to a subnet.
	//If not, then return null.
	private String getPrivateAddress(EC2VM curVM){
		for(int ei = 0 ; ei<curVM.ethernetPort.size() ; ei++){
			if(curVM.ethernetPort.get(ei).subnet != null)
				return curVM.ethernetPort.get(ei).address;
		}
		return null;
	}
	

	@Override
	public boolean stop(SubTopologyInfo subTopologyInfo, 
			Credential credential, Database database) {
		EC2SubTopology ec2SubTopology = (EC2SubTopology)subTopologyInfo.subTopology;
		boolean returnResult = true;
		ArrayList<String> instances = new ArrayList<String>();
		for(int vi = 0 ; vi < ec2SubTopology.components.size() ; vi++){
			EC2VM curVM = ec2SubTopology.components.get(vi);
			if(curVM.instanceId == null){
				logger.error("The instanceId of "+curVM.name+" is unknown!");
				returnResult = false;
			}else{
				instances.add(curVM.instanceId);
				curVM.publicAddress = null;
			}
		}
		if(instances.size() == 0){
			logger.error("These is no valid instanceId to be stopped!");
			returnResult = false;
		}else{
			if(ec2Agent == null){
				EC2Credential ec2Credential = (EC2Credential)credential;
				if(!setEC2Agent(ec2Credential))
					return false;
			}
			ec2Agent.setEndpoint(subTopologyInfo.endpoint);
			logger.debug("Set endpoint for '"+subTopologyInfo.topology+"' "+subTopologyInfo.endpoint);
			
			ec2Agent.stopInstances(instances);
		}
		
		return returnResult;
	}

	/**
	 * 1. Update the AMI information.
	 * 2. Update the endpoint information.
	 * 3. To be completed, check the validity of nodeType.
	 */
	@Override
	public boolean runtimeCheckandUpdate(SubTopologyInfo subTopologyInfo,
			Database database) {
		
		///Update the endpoint information
		EC2Database ec2Database = (EC2Database)database;
		EC2SubTopology ec2SubTopology = (EC2SubTopology)subTopologyInfo.subTopology;
		String domain = subTopologyInfo.domain.trim().toLowerCase();
		if((subTopologyInfo.endpoint = ec2Database.domain_endpoint.get(domain)) == null){
			logger.error("Domain '"+domain+"' of sub-topology '"+subTopologyInfo.topology+"' cannot be mapped into some EC2 endpoint!");
			return false;
		}
		
		for(int vi = 0 ; vi < ec2SubTopology.components.size() ; vi++){
			EC2VM curVM = ec2SubTopology.components.get(vi);
			if((curVM.AMI = ec2Database.getAMI(curVM.OStype, domain)) == null){
				logger.error("The EC2 AMI of 'OStype' '"+curVM.OStype+"' in domain '"+domain+"' is not known!");
				return false;
			}
		}
		
		return true;
	}

	@Override
	public boolean confTopConnection(SubTopologyInfo subTopologyInfo, Credential credential, Database database) {
		EC2SubTopology ec2SubTopology = (EC2SubTopology)subTopologyInfo.subTopology;
		EC2Credential ec2Credential = (EC2Credential)credential;
		EC2Database ec2Database = (EC2Database)database;
		String domain = subTopologyInfo.domain.trim().toLowerCase();
		if(subTopologyInfo.endpoint == null){
			if((subTopologyInfo.endpoint = ec2Database.domain_endpoint.get(domain)) == null){
				logger.error("Domain '"+domain+"' of sub-topology '"+subTopologyInfo.topology+"' cannot be mapped into some EC2 endpoint!");
				return false;
			}
		}
		
		if(ec2Agent == null){
			if(!setEC2Agent(ec2Credential))
				return false;
		}
		ec2Agent.setEndpoint(subTopologyInfo.endpoint);
		
		////Configure all the inter connections
		ExecutorService executor4conf = Executors.newFixedThreadPool(ec2SubTopology.components.size());
		for(int vi = 0 ; vi < ec2SubTopology.components.size() ; vi++){
			EC2VM curVM = ec2SubTopology.components.get(vi);
			String vEngineNameOS = "provisioning.engine.VEngine.EC2.EC2VEngine_";
			if(curVM.OStype.toLowerCase().contains("ubuntu"))
				vEngineNameOS += "ubuntu";
			else{
				logger.warn("The OS type of "+curVM.name+" in sub-topology "+ec2SubTopology.topologyName+" is not supported yet!");
				continue;
			}
			try {
				Object sEngine = Class.forName(vEngineNameOS).newInstance();
				((EC2VEngine)sEngine).cmd = "connection";
				((EC2VEngine)sEngine).curVM = curVM;
				((EC2VEngine)sEngine).ec2agent = this.ec2Agent;
				((EC2VEngine)sEngine).privateKeyString = ec2SubTopology.accessKeyPair.privateKeyString;
				((EC2VEngine)sEngine).topConnectors = subTopologyInfo.connectors;
				
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
				if(count > 200*ec2SubTopology.components.size()){
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
		EC2SubTopology ec2SubTopology = (EC2SubTopology)subTopologyInfo.subTopology;
		if(subTopologyInfo.connectors == null || subTopologyInfo.connectors.size() == 0)
			return true;
		ExecutorService executor4del = Executors.newFixedThreadPool(subTopologyInfo.connectors.size());
		for(int vi = 0 ; vi < subTopologyInfo.connectors.size() ; vi++){
			TopConnectionPoint curTCP = subTopologyInfo.connectors.get(vi);
			if(curTCP.peerTCP.ethName != null)   ////This means the peer sub-topology is not failed
				continue;
			
			ArrayList<TopConnectionPoint> curConnector = new ArrayList<TopConnectionPoint>();
			curConnector.add(curTCP);
			EC2VM curVM = ((EC2VM)curTCP.belongingVM);
			String vEngineNameOS = "provisioning.engine.VEngine.EC2.EC2VEngine_";
			if(curVM.OStype.toLowerCase().contains("ubuntu"))
				vEngineNameOS += "ubuntu";
			else{
				logger.warn("The OS type of "+curVM.name+" in sub-topology "+ec2SubTopology.topologyName+" is not supported yet!");
				continue;
			}
			try {
				Object sEngine = Class.forName(vEngineNameOS).newInstance();
				((EC2VEngine)sEngine).cmd = "remove";
				((EC2VEngine)sEngine).curVM = curVM;
				((EC2VEngine)sEngine).ec2agent = this.ec2Agent;
				((EC2VEngine)sEngine).privateKeyString = ec2SubTopology.accessKeyPair.privateKeyString;
				((EC2VEngine)sEngine).topConnectors = curConnector;   ///only one element in this arraylist.
				
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
			Credential credential, Database database){
		if(!detachSubTopology(subTopologyInfo, credential, database))
			return false;
		else
			return true;
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
		
		EC2SubTopology ec2subTopology = new EC2SubTopology();
		EC2SubTopology tempSubTopology = (EC2SubTopology)scalingTemplate.subTopology;
		SSHKeyPair kp = null;
		if((kp = userCredential.sshAccess.get(domain.trim().toLowerCase())) == null){
			generatedSTI.sshKeyPairId = null;
			ec2subTopology.accessKeyPair = null;
		}else{
			generatedSTI.sshKeyPairId = kp.SSHKeyPairId;
			ec2subTopology.accessKeyPair = kp;
		}
		
		String currentDir = CommonTool.getPathDir(tempSubTopology.loadingPath);
		ec2subTopology.loadingPath = currentDir + generatedSTI.topology + ".yml";
		ec2subTopology.topologyName = generatedSTI.topology;
		ec2subTopology.topologyType = "EC2";
		ec2subTopology.components = new ArrayList<EC2VM>();
		for(int vi = 0 ; vi < tempSubTopology.components.size() ; vi++){
			EC2VM curVM = (EC2VM)tempSubTopology.components.get(vi);
			EC2VM newVM = new EC2VM();
			newVM.diskSize = curVM.diskSize;
			newVM.dockers = curVM.dockers;
			newVM.IOPS = curVM.IOPS;
			newVM.name = curVM.name;
			newVM.nodeType = curVM.nodeType;
			newVM.OStype = curVM.OStype;
			newVM.role = curVM.role;
			newVM.script = curVM.script;
			newVM.type = curVM.type;
			newVM.v_scriptString = curVM.v_scriptString;
			newVM.ethernetPort = new ArrayList<Eth>();
			for(int ei = 0 ; ei < curVM.ethernetPort.size() ; ei++){
				Eth newEth = new Eth();
				newEth.address = curVM.ethernetPort.get(ei).address;
				newEth.connectionName = curVM.ethernetPort.get(ei).connectionName;
				newEth.name = curVM.ethernetPort.get(ei).name;
				newEth.subnetName = curVM.ethernetPort.get(ei).subnetName;
				newVM.ethernetPort.add(newEth);
				
			}
			
			ec2subTopology.components.add(newVM);
		}
		
		
		if(tempSubTopology.subnets != null){
			ec2subTopology.subnets = new ArrayList<Subnet>();
			for(int si = 0 ; si < tempSubTopology.subnets.size() ; si++){
				Subnet newSubnet = new Subnet();
				Subnet curSubnet = tempSubTopology.subnets.get(si);
				newSubnet.name = curSubnet.name;
				newSubnet.netmask = curSubnet.netmask;
				newSubnet.subnet = curSubnet.subnet;
				ec2subTopology.subnets.add(newSubnet);
			}
		}
		
		if(tempSubTopology.connections != null){
			ec2subTopology.connections = new ArrayList<SubConnection>();
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
				
				ec2subTopology.connections.add(newConnection);
			}
		}
		generatedSTI.subTopology = ec2subTopology;
		
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
			VM vmInfo = ec2subTopology.getVMinSubClassbyName(VMName);
			if(vmInfo == null){
				logger.error("There is no VM called "+VMName+" in "+ec2subTopology.topologyName);
				return null;
			}
			newTCP.belongingVM = vmInfo;
			
			generatedSTI.connectors.add(newTCP);
		}
		return generatedSTI;
	}

	@Override
	public boolean scaleUp(SubTopologyInfo subTopologyInfo, Credential credential,
			Database database) {
		if(!subTopologyInfo.status.equals("fresh") && !subTopologyInfo.tag.equals("scaled")){
			logger.warn("The sub-topology '"+subTopologyInfo.topology+"' is not a 'scaled' part!");
			return false;
		}
		if(createSubTopology(subTopologyInfo, credential, database))
			return true;
		else 
			return false;
	}

	@Override
	public boolean supportStop() {
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
	
	private boolean startSubTopology(SubTopologyInfo subTopologyInfo, Credential credential, Database database){
		EC2SubTopology ec2SubTopology = (EC2SubTopology)subTopologyInfo.subTopology;
		EC2Credential ec2Credential = (EC2Credential)credential;
		
		if(ec2Agent == null){
			if(!setEC2Agent(ec2Credential))
				return false;
		}
		ec2Agent.setEndpoint(subTopologyInfo.endpoint);
		logger.debug("Set endpoint for '"+subTopologyInfo.topology+"' "+subTopologyInfo.endpoint);
		
		long startingUpStart = System.currentTimeMillis();
		
		///Normally, the accessKeyPair should not be null
		if(ec2SubTopology.accessKeyPair == null){
			logger.warn("The key pair of a stopped sub-topology should not be null!");
			String keyPairId = UUID.randomUUID().toString();
			String publicKeyId = "publicKey-"+keyPairId;
			String privateKeyString = ec2Agent.createKeyPair(publicKeyId);
			if(privateKeyString == null){
				logger.error("Unexpected error for creating ssh key pair for sub-topology '"+ec2SubTopology.topologyName+"'!");
				return false;
			}
			subTopologyInfo.sshKeyPairId = keyPairId;
			ec2SubTopology.accessKeyPair = new SSHKeyPair();
			ec2SubTopology.accessKeyPair.publicKeyId = publicKeyId;
			ec2SubTopology.accessKeyPair.privateKeyString = privateKeyString;
			ec2SubTopology.accessKeyPair.SSHKeyPairId = keyPairId;
		}
		
		int vmPoolSize = ec2SubTopology.components.size();
		ExecutorService executor4vm = Executors.newFixedThreadPool(vmPoolSize);
		for(int vi = 0 ; vi<ec2SubTopology.components.size() ; vi++){
			EC2VM curVM = ec2SubTopology.components.get(vi);
			if(curVM.instanceId == null){
				logger.error("The instanceId of '"+curVM.name+"' in sub-topology '"+ec2SubTopology.topologyName+"' cannot be achieved!");
				return false;
			}
			EC2VEngine_startVM ec2startVM = new EC2VEngine_startVM(
					ec2Agent, curVM, 
					ec2SubTopology.accessKeyPair.privateKeyString);
			executor4vm.execute(ec2startVM);
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
		for(int vi = 0 ; vi<ec2SubTopology.components.size() ; vi++){
			EC2VM curVM = ec2SubTopology.components.get(vi);
			if(curVM.publicAddress == null){
				allSuccess = false;
				logger.error(curVM.name+" of sub-topology '"+ec2SubTopology.topologyName+"' is not fully started!");
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
		for(int vi = 0 ; vi < ec2SubTopology.components.size() ; vi++){
			EC2VM curVM = ec2SubTopology.components.get(vi);
			String vEngineNameOS = "provisioning.engine.VEngine.EC2.EC2VEngine_";
			if(curVM.OStype.toLowerCase().contains("ubuntu"))
				vEngineNameOS += "ubuntu";
			else{
				logger.warn("The OS type of "+curVM.name+" in sub-topology "+ec2SubTopology.topologyName+" is not supported yet!");
				continue;
			}
			try {
				Object sEngine = Class.forName(vEngineNameOS).newInstance();
				((EC2VEngine)sEngine).cmd = "connection";
				((EC2VEngine)sEngine).curVM = curVM;
				((EC2VEngine)sEngine).ec2agent = this.ec2Agent;
				((EC2VEngine)sEngine).privateKeyString = ec2SubTopology.accessKeyPair.privateKeyString;
				((EC2VEngine)sEngine).publicKeyString = subTopologyInfo.publicKeyString;
				((EC2VEngine)sEngine).userName = subTopologyInfo.userName;
				((EC2VEngine)sEngine).subConnections = ec2SubTopology.connections;
				((EC2VEngine)sEngine).currentDir = CommonTool.getPathDir(ec2SubTopology.loadingPath);
				//((EC2VEngine)sEngine).topConnectors = subTopologyInfo.connectors;
				//logger.debug("wtf?");
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
		
		if(!ec2SubTopology.overwirteControlOutput()){
			logger.error("Control information of '"+ec2SubTopology.topologyName+"' has not been overwritten to the origin file!");
			return false;
		}
		logger.info("The control information of "+ec2SubTopology.topologyName+" has been written back!");
		return true;
	}

	@Override
	public boolean delete(SubTopologyInfo subTopologyInfo,
			Credential credential, Database database) {
		EC2SubTopology ec2SubTopology = (EC2SubTopology)subTopologyInfo.subTopology;
		boolean returnResult = true;
		class VpcElement{
			public String vpcId;
			public ArrayList<String> subnetIds = new ArrayList<String>();
			public ArrayList<String> securityGroupIds = new ArrayList<String>();
			public ArrayList<String> internetGatewayIds = new ArrayList<String>();
		} 
		ArrayList<VpcElement> vpcEles = new ArrayList<VpcElement>();
		ArrayList<EC2VM> ec2VMs = new ArrayList<EC2VM>();
		for(int vi = 0 ; vi < ec2SubTopology.components.size() ; vi++){
			EC2VM curVM = ec2SubTopology.components.get(vi);
			if(curVM.instanceId == null){
				logger.error("The instanceId of "+curVM.name+" is unknown!");
				returnResult = false;
			}else{
				ec2VMs.add(curVM);
				boolean findVpc = false;
				for(int vpci = 0 ; vpci < vpcEles.size() ; vpci++){
					VpcElement curVpcEle = vpcEles.get(vpci);
					if(curVpcEle.vpcId.equals(curVM.vpcId)){
						findVpc = true;
						curVpcEle.subnetIds.add(curVM.subnetId);
						curVpcEle.securityGroupIds.add(curVM.securityGroupId);
						curVpcEle.internetGatewayIds.add(curVM.internetGatewayId);
						break;
					}
				}
				if(!findVpc){
					VpcElement newVpcEle = new VpcElement();
					newVpcEle.vpcId = curVM.vpcId;
					newVpcEle.subnetIds.add(curVM.subnetId);
					newVpcEle.securityGroupIds.add(curVM.securityGroupId);
					newVpcEle.internetGatewayIds.add(curVM.internetGatewayId);
					vpcEles.add(newVpcEle);
				}
				
			}
		}
		if(ec2VMs.size() == 0){
			logger.error("These is no valid instanceId to be deleted!");
			returnResult = false;
		}else{
			if(ec2Agent == null){
				EC2Credential ec2Credential = (EC2Credential)credential;
				if(!setEC2Agent(ec2Credential))
					return false;
			}
			ec2Agent.setEndpoint(subTopologyInfo.endpoint);
			logger.debug("Set endpoint for '"+subTopologyInfo.topology+"' "+subTopologyInfo.endpoint);
			
			if(!ec2Agent.terminateInstances(ec2VMs)){
				logger.error("Some instances cannot be deleted!");
				return false;
			}
		}
		for(int vpci = 0 ; vpci < vpcEles.size() ; vpci++){
			VpcElement curVpcEle = vpcEles.get(vpci);
			ec2Agent.deleteVpc(curVpcEle.vpcId, curVpcEle.subnetIds, 
					curVpcEle.securityGroupIds, curVpcEle.internetGatewayIds);
		}
		
		////clear all the information
		for(int vi = 0 ; vi < ec2SubTopology.components.size() ; vi++){
			EC2VM curVM = ec2SubTopology.components.get(vi);
			curVM.publicAddress = null;
			curVM.volumeId = null;
			curVM.instanceId = null;
			curVM.securityGroupId = null;
			curVM.internetGatewayId = null;
			curVM.vpcId = null;
			curVM.routeTableId = null;
			curVM.subnetId = null;
			curVM.actualPrivateAddress = null;
		}
		return returnResult;
	}
	
	



}
