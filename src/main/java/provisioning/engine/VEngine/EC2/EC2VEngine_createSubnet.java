package provisioning.engine.VEngine.EC2;

import topologyAnalysis.dataStructure.EC2.EC2Subnet;

public class EC2VEngine_createSubnet implements Runnable{
	
	private EC2Agent ec2agent;
	private EC2Subnet ec2subnet;
	
	public EC2VEngine_createSubnet(EC2Agent ec2agent, EC2Subnet ec2subnet){
		this.ec2agent = ec2agent;
		this.ec2subnet = ec2subnet;
	}

	@Override
	public void run() {
		//In this case, we need to set up vpc first.
		if(ec2subnet.vpcId == null){
			String vpcCIDR = ec2subnet.org_subnet.subnet+"/"+ec2subnet.org_subnet.netmask;
			String vpcId = ec2agent.createVPC(vpcCIDR);
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				e.printStackTrace();
				return;
			}
			ec2subnet.vpcId = vpcId;
		}
		
		String routeTableId = ec2agent.getAssociateRouteTableId(ec2subnet.vpcId);
		String internetGatewayId = ec2agent.createInternetGateway(ec2subnet.vpcId);
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			e.printStackTrace();
			return ;
		}
		ec2subnet.routeTableId = routeTableId;
		ec2subnet.internetGatewayId = internetGatewayId;
		ec2agent.createRouteToGate(routeTableId, internetGatewayId, "0.0.0.0/0");
		ec2agent.enableVpcDNSHostName(ec2subnet.vpcId);
		String subnetCIDR = ec2subnet.org_subnet.subnet+"/"+ec2subnet.org_subnet.netmask;
		String subnetId = ec2agent.createSubnet(ec2subnet.vpcId, subnetCIDR);
		ec2agent.enableMapPubAddress(subnetId);
		String securityGroupId = ec2agent.createBasicSecurityGroup(ec2subnet.vpcId, "AllTrafficGroup", "AllTraffic");
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			e.printStackTrace();
			return;
		}
		ec2subnet.securityGroupId = securityGroupId;
		ec2subnet.subnetId = subnetId;
		
	}

}
