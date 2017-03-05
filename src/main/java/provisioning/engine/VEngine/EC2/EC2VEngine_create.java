package provisioning.engine.VEngine.EC2;

import org.apache.log4j.Logger;

import topologyAnalysis.dataStructure.EC2.EC2VM;

public class EC2VEngine_create implements Runnable{
	
	private static final Logger logger = Logger.getLogger(EC2VEngine_create.class);
	
	private EC2Agent ec2agent;
	private EC2VM curVM;
	
	public EC2VEngine_create(EC2Agent ec2agent, EC2VM curVM, 
			String vpcCIDR, String privateAddress, String publicKeyId){
		
	}

	//The publicKeyId means the name of the public key in that datacenter.
	/*public static boolean createVM(){
		String vpcId = ec2agent.createVPC(vpcCIDR);
		if(vpcId == null){
			logger.error("Cannot create vpc for "+curVM.name);
			return false;
		}
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		String routeTableId = ec2agent.getAssociateRouteTableId(vpcId);
		if(routeTableId == null){
			logger.error("Cannot create routeTable for "+curVM.name);
			return false;
		}
		String internetGatewayId = ec2agent.createInternetGateway(vpcId);
		if(internetGatewayId == null){
			logger.error("Cannot create internetGateway for "+curVM.name);
			return false;
		}
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		ec2agent.createRouteToGate(routeTableId, internetGatewayId, "0.0.0.0/0");
		ec2agent.enableVpcDNSHostName(vpcId);
		String subnetId = ec2agent.createSubnet(vpcId, vpcCIDR);
		if(subnetId == null){
			logger.error("Cannot create subnet "+vpcCIDR+" for "+curVM.name);
			return false;
		}
		ec2agent.enableMapPubAddress(subnetId);
		String securityGroupId = ec2agent.createBasicSecurityGroup(vpcId, "AllTrafficGroup", "AllTraffic");
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		String instanceId = ec2agent.runInstance(subnetId, securityGroupId, curVM.AMI,
				privateAddress, curVM.nodeType.trim().toLowerCase(), publicKeyId);
		if(instanceId == null){
			logger.error("Cannot create an instance for "+curVM.name+" with '"+privateAddress+"' and '"+curVM.nodeType.toLowerCase()+"'!");
			return false;
		}
		String volumeId = ec2agent.createVolume(Integer.valueOf(curVM.diskSize), Integer.valueOf(curVM.IOPS));
		if(volumeId == null){
			logger.error("Cannot create a volume for "+curVM.name+" with '"+curVM.diskSize+"' GB and '"+curVM.IOPS+"'!");
			return false;
		}
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		ec2agent.attachVolume(volumeId, instanceId);
		
		curVM.vpcId = vpcId;
		curVM.instanceId = instanceId;
		curVM.internetGatewayId = internetGatewayId;
		curVM.routeTableId = routeTableId;
		curVM.securityGroupId = securityGroupId;
		curVM.subnetId = subnetId;
		curVM.volumeId = volumeId;
		
		return true;
	}*/
		
		
	@Override
	public void run() {
		
	}

}
