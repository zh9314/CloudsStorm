package provisioning.engine.SEngine;

import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import commonTool.CommonTool;

import provisioning.credential.Credential;
import provisioning.credential.EC2Credential;
import provisioning.credential.SSHKeyPair;
import provisioning.database.Database;
import provisioning.database.EC2.EC2Database;
import provisioning.engine.VEngine.EC2.EC2Agent;
import provisioning.engine.VEngine.EC2.EC2VEngine_createSubnet;
import provisioning.engine.VEngine.EC2.EC2VEngine_createVM;
import provisioning.engine.VEngine.EC2.EC2VEngine;
import topologyAnalysis.dataStructure.SubTopology;
import topologyAnalysis.dataStructure.SubTopologyInfo;
import topologyAnalysis.dataStructure.Subnet;
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
	
	@Override
	public boolean provision(SubTopologyInfo subTopologyInfo, Credential credential, Database database) {
		EC2SubTopology ec2SubTopology = (EC2SubTopology)subTopologyInfo.subTopology;
		EC2Credential ec2Credential = (EC2Credential)credential;
		if(!subTopologyInfo.status.trim().toLowerCase().equals("fresh")){
			logger.warn("The sub-topology '"+subTopologyInfo.topology+"' has ever been provisioned!");
			return false;
		}
		
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
				Object sEngine = Class.forName(vEngineNameOS).newInstance();
				((EC2VEngine)sEngine).cmd = "all";
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
		long configureOverhead = configureEnd - provisioningEnd;
		subTopologyInfo.statusInfo += "; configuration overhead: " + configureOverhead;
		
		if(!ec2SubTopology.overwirteControlOutput()){
			logger.error("Control information of '"+ec2SubTopology.topologyName+"' has not been overwritten to the origin file!");
			return false;
		}
		logger.info("The control information of "+ec2SubTopology.topologyName+" has been written back!");
		return true;
		
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
	public boolean stop(SubTopology subTopology, Credential credential, Database database) {
		
		return false;
	}

	/**
	 * 1. Update the AMI information.
	 * 2. To be completed, check the validity of nodeType.
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

}
