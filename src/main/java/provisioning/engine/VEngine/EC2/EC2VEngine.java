package provisioning.engine.VEngine.EC2;



import java.util.ArrayList;

import org.apache.log4j.Logger;

import commonTool.CommonTool;
import provisioning.credential.Credential;
import provisioning.credential.EC2Credential;
import provisioning.database.Database;
import provisioning.engine.VEngine.VEngine;
import topology.dataStructure.EC2.EC2SubTopology;
import topology.dataStructure.EC2.EC2Subnet;
import topology.dataStructure.EC2.EC2VM;
import topology.description.actual.SubTopologyInfo;
import topology.description.actual.VM;



public class EC2VEngine extends VEngine{
	private static final Logger logger = Logger.getLogger(EC2VEngine.class);
	
	public static boolean provision(VM subjectVM,
			Credential credential, Database database) {
		SubTopologyInfo subTopologyInfo = subjectVM.ponintBack2STI;
		EC2Credential ec2Credential = (EC2Credential)credential;
		EC2Agent ec2agent = new EC2Agent(ec2Credential.accessKey, ec2Credential.secretKey);
		if(subTopologyInfo.endpoint == null)
			return false;
		
		ec2agent.setEndpoint(subTopologyInfo.endpoint);
		
		EC2VM curVM = (EC2VM)subjectVM;
		EC2SubTopology curST = (EC2SubTopology)subTopologyInfo.subTopology;
		if(curST.accessKeyPair == null 
				|| curST.accessKeyPair.publicKeyId == null){
			String msg = "There is no public key. "
					+ "Please create SSH key pair at domain '"+subTopologyInfo.domain+"' first!";
			logger.error(msg);
			subTopologyInfo.logsInfo.put("ERROR", msg);
			return false;
		}
		if(curVM.subnetId == null || curVM.securityGroupId == null
				|| curVM.AMI == null || curVM.actualPrivateAddress == null
				|| curVM.nodeType == null){
			String msg = "Missing key information to provision VM '"+curVM.name+"'";
			logger.error(msg);
			subTopologyInfo.logsInfo.put(curVM.name, "Missing info!");
			return false;
		}
		
		String instanceId = ec2agent.runInstance(curVM.subnetId, curVM.securityGroupId, curVM.AMI,
				curVM.actualPrivateAddress, curVM.nodeType.trim().toLowerCase(), curST.accessKeyPair.publicKeyId);
		if(instanceId == null){
			logger.error("Cannot run instance for "+curVM.name);
			subTopologyInfo.logsInfo.put(curVM.name, "Provision failed!");
			return false;
		}
		curVM.instanceId = instanceId;
		boolean attachNeeded = false;
		if((Integer.valueOf(curVM.diskSize) - 8) > 0){
			String volumeId = ec2agent.createVolume(
					Integer.valueOf(curVM.diskSize), 
					Integer.valueOf(curVM.IOPS),
					curVM.subnetId);
			if(volumeId == null){
				logger.error("Cannot create volume for "+curVM.name);
				subTopologyInfo.logsInfo.put(curVM.name, "More disk volume failed");
				return false;
			}
			curVM.volumeId = volumeId;
			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			attachNeeded = true;
		}
		
		String publicAddress = ec2agent.getPublicAddress(instanceId);
		if(publicAddress == null){
			logger.error("Cannot get public address from '"+instanceId+"' of '"+curVM.name+"'");
			subTopologyInfo.logsInfo.put(curVM.name, "Public address failed");
			return false;
		}
		logger.info("Get '"+instanceId+"' <-> "+publicAddress);
		
		//Wait for 5min (300s) for maximum.
		long sshStartTime = System.currentTimeMillis();
		long sshEndTime = System.currentTimeMillis();
		while((sshEndTime - sshStartTime) < 300000){
			if(VEngine.isAlive(publicAddress, 22, 
					curST.accessKeyPair.privateKeyString, curVM.defaultSSHAccount)){
				curVM.publicAddress = publicAddress;
				logger.info(curVM.name+" ("+publicAddress+") is activated!");
				
				if(attachNeeded){
					ec2agent.attachVolume(curVM.volumeId, instanceId);
					logger.debug("Volume '"+curVM.volumeId+"' is attached!");
				}
				return true;
			}
			sshEndTime = System.currentTimeMillis();
		}
		curVM.publicAddress = null;
		subTopologyInfo.logsInfo.put(curVM.name, "SSH timeout!");
		return false;
	}
	
	public static boolean delete(VM subjectVM,
			Credential credential, Database database) {
		SubTopologyInfo subTopologyInfo = subjectVM.ponintBack2STI;
		EC2Credential ec2Credential = (EC2Credential)credential;
		EC2Agent ec2agent = new EC2Agent(ec2Credential.accessKey, ec2Credential.secretKey);
		if(subTopologyInfo.endpoint == null)
			return false;
		
		ec2agent.setEndpoint(subTopologyInfo.endpoint);
		
		EC2VM curVM = (EC2VM)subjectVM;
		if(!ec2agent.terminateInstance(curVM)){
			logger.error(curVM.name + " cannot be deleted!");
			curVM.ponintBack2STI.logsInfo.put(curVM.name, "Delete failed!");
			return false;
		}
		curVM.publicAddress = null;
		curVM.instanceId = null;
		
		return true;
	}
	
	public static boolean start(VM subjectVM,
			Credential credential, Database database) {
		SubTopologyInfo subTopologyInfo = subjectVM.ponintBack2STI;
		EC2Credential ec2Credential = (EC2Credential)credential;
		EC2VM curVM = (EC2VM)subjectVM;
		EC2Agent ec2agent = new EC2Agent(ec2Credential.accessKey, ec2Credential.secretKey);
		if(subTopologyInfo.endpoint == null)
			return false;
		ec2agent.setEndpoint(subTopologyInfo.endpoint);
		
		if(curVM.instanceId == null)
			return false;
		
		long vmStartStart = System.currentTimeMillis();
		ec2agent.startInstance(curVM.instanceId);
		
		String publicAddress = ec2agent.getPublicAddress(curVM.instanceId);
		if(publicAddress == null){
			logger.error("Cannot get public address from '"+curVM.instanceId+"' of '"+curVM.name+"'");
			return false;
		}
		logger.info("Get '"+curVM.instanceId+"' <-> "+publicAddress);
		
		//Wait for 5min (300s) for maximum.
		long sshStartTime = System.currentTimeMillis();
		long sshEndTime = System.currentTimeMillis();
		while((sshEndTime - sshStartTime) < 300000){
			if(VEngine.isAlive(publicAddress, 22, subTopologyInfo.subTopology.accessKeyPair.privateKeyString,
					curVM.defaultSSHAccount)){
				curVM.publicAddress = publicAddress;
				logger.info(curVM.name+" ("+publicAddress+") is activated!");
				long vmStartEnd = System.currentTimeMillis();
				subTopologyInfo.logsInfo.put(curVM.name+"#StartOverhead",
						String.valueOf(vmStartEnd - vmStartStart));
				return true;
			}
		}
		
		curVM.publicAddress = null;
		return false;
	}
	
	
	public static boolean stop(VM subjectVM,
			Credential credential, Database database) {
		SubTopologyInfo subTopologyInfo = subjectVM.ponintBack2STI;
		EC2Credential ec2Credential = (EC2Credential)credential;
		EC2Agent ec2agent = new EC2Agent(ec2Credential.accessKey, ec2Credential.secretKey);
		if(subTopologyInfo.endpoint == null)
			return false;
		
		ec2agent.setEndpoint(subTopologyInfo.endpoint);
		
		EC2VM curVM = (EC2VM)subjectVM;
		ArrayList<String> instances = new ArrayList<String>();
		instances.add(curVM.instanceId);
		ec2agent.stopInstances(instances);
		return true;
	}
	
	
	
	
	/**
	 * The return value is the private key string, which is downloaded from EC2
	 * @return
	 */
	public static String createSSHKeyPair(SubTopologyInfo subTopologyInfo,
			Credential credential, String publicKeyId){
		EC2Credential ec2Credential = (EC2Credential)credential;
		EC2Agent ec2agent = new EC2Agent(ec2Credential.accessKey, ec2Credential.secretKey);
		ec2agent.setEndpoint(subTopologyInfo.endpoint);
		String privateKeyString = ec2agent.createKeyPair(publicKeyId);
		return privateKeyString;
	}
	
	public static boolean deleteVPC(SubTopologyInfo subTopologyInfo,
			Credential credential){
		
		EC2Credential ec2Credential = (EC2Credential)credential;
		EC2Agent ec2agent = new EC2Agent(ec2Credential.accessKey, ec2Credential.secretKey);
		ec2agent.setEndpoint(subTopologyInfo.endpoint);
		
		
		ArrayList<EC2VM> vms = ((EC2SubTopology)subTopologyInfo.subTopology).VMs;
		EC2VM vm1 = vms.get(0);
		ArrayList<String> subnetIds = new ArrayList<String>();
		subnetIds.add(vm1.subnetId);
		ArrayList<String> securityGroupIds = new ArrayList<String>();
		securityGroupIds.add(vm1.securityGroupId);
		ArrayList<String> internetGatewayIds = new ArrayList<String>();
		internetGatewayIds.add(vm1.internetGatewayId);
		ec2agent.deleteVpc(vm1.vpcId, subnetIds, 
				securityGroupIds, internetGatewayIds);
		
		for(int vi = 0 ; vi<vms.size() ; vi++){
			EC2VM curVM = vms.get(vi);
			curVM.vpcId = null;
			curVM.subnetId = null;
			curVM.securityGroupId = null;
			curVM.volumeId = null;
			curVM.routeTableId = null;
			curVM.internetGatewayId = null;
		}
		
		return true;
		
	}
	
	
	public static boolean createCommonSubnet(SubTopologyInfo subTopologyInfo, Credential credential){
		EC2SubTopology ec2SubTopology = (EC2SubTopology)subTopologyInfo.subTopology;
		EC2Credential ec2Credential = (EC2Credential)credential;
		EC2Agent ec2agent = new EC2Agent(ec2Credential.accessKey, ec2Credential.secretKey);
		ec2agent.setEndpoint(subTopologyInfo.endpoint);
		String vpcId4all = null;
		vpcId4all = ec2agent.createVPC("172.31.0.0/16");
		if(vpcId4all == null){
			logger.error("Error happens during creating VPC for sub-topology "+subTopologyInfo.topology );
			return false;
		}
		EC2Subnet ec2Subnet4all = new EC2Subnet();
		ec2Subnet4all.netmask = "17";
		ec2Subnet4all.subnet = "172.31.0.0";
		ec2Subnet4all.vpcId = vpcId4all;
		
		///create the subnet for all the VMs
		String routeTableId = null;
		int count = 0;
		while((routeTableId = ec2agent.getAssociateRouteTableId(ec2Subnet4all.vpcId)) == null){
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
				logger.error(e.getMessage());
				return false;
			}
			count++;
			if(count > 200){
				logger.error("Unexpected: Too long to wait!");
				return false;
			}
		}
		String internetGatewayId = ec2agent.createInternetGateway(ec2Subnet4all.vpcId);
		if(internetGatewayId == null){
			logger.error("Cannot create a Internet Gateway!");
			return false;
		}
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			e.printStackTrace();
			return false;
		}
		ec2Subnet4all.routeTableId = routeTableId;
		ec2Subnet4all.internetGatewayId = internetGatewayId;
		ec2agent.createRouteToGate(routeTableId, internetGatewayId, "0.0.0.0/0");
		ec2agent.enableVpcDNSHostName(ec2Subnet4all.vpcId);
		String subnetCIDR = ec2Subnet4all.subnet+"/"+ec2Subnet4all.netmask;
		String subnetId = ec2agent.createSubnet(ec2Subnet4all.vpcId, subnetCIDR);
		ec2agent.enableMapPubAddress(subnetId);
		String securityGroupId = ec2agent.createBasicSecurityGroup(ec2Subnet4all.vpcId, "AllTrafficGroup", "AllTraffic");
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			e.printStackTrace();
			return false;
		}
		ec2Subnet4all.securityGroupId = securityGroupId;
		ec2Subnet4all.subnetId = subnetId;
		
		for(int vi = 0 ; vi<ec2SubTopology.VMs.size() ; vi++){
			EC2VM curVM = ec2SubTopology.VMs.get(vi);
			curVM.actualPrivateAddress = CommonTool.getFullAddress("172.31.0.0", 17, vi+10);
			if(curVM.actualPrivateAddress == null){
				logger.error("There is no enough IP address "
						+ "in subnet 172.31.0.0/17 of "+subTopologyInfo.topology);
				return false;
			}
			curVM.internetGatewayId = ec2Subnet4all.internetGatewayId;
			curVM.routeTableId = ec2Subnet4all.routeTableId;
			curVM.securityGroupId = ec2Subnet4all.securityGroupId;
			curVM.subnetId = ec2Subnet4all.subnetId;
			curVM.vpcId = ec2Subnet4all.vpcId;
		}
		
		logger.debug("Subnet has been created!");
		return true;
	}
	
	
	
}
