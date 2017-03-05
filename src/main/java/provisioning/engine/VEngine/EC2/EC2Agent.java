package provisioning.engine.VEngine.EC2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.AttachInternetGatewayRequest;
import com.amazonaws.services.ec2.model.AttachVolumeRequest;
import com.amazonaws.services.ec2.model.AttachVolumeResult;
import com.amazonaws.services.ec2.model.AuthorizeSecurityGroupIngressRequest;
import com.amazonaws.services.ec2.model.CreateInternetGatewayRequest;
import com.amazonaws.services.ec2.model.CreateInternetGatewayResult;
import com.amazonaws.services.ec2.model.CreateKeyPairRequest;
import com.amazonaws.services.ec2.model.CreateKeyPairResult;
import com.amazonaws.services.ec2.model.CreateRouteRequest;
import com.amazonaws.services.ec2.model.CreateSecurityGroupRequest;
import com.amazonaws.services.ec2.model.CreateSecurityGroupResult;
import com.amazonaws.services.ec2.model.CreateSubnetRequest;
import com.amazonaws.services.ec2.model.CreateSubnetResult;
import com.amazonaws.services.ec2.model.CreateVolumeRequest;
import com.amazonaws.services.ec2.model.CreateVolumeResult;
import com.amazonaws.services.ec2.model.CreateVpcRequest;
import com.amazonaws.services.ec2.model.CreateVpcResult;
import com.amazonaws.services.ec2.model.DeleteInternetGatewayRequest;
import com.amazonaws.services.ec2.model.DeleteSecurityGroupRequest;
import com.amazonaws.services.ec2.model.DeleteSubnetRequest;
import com.amazonaws.services.ec2.model.DeleteVpcRequest;
import com.amazonaws.services.ec2.model.DescribeAvailabilityZonesResult;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.DescribeRegionsResult;
import com.amazonaws.services.ec2.model.DescribeRouteTablesRequest;
import com.amazonaws.services.ec2.model.DescribeRouteTablesResult;
import com.amazonaws.services.ec2.model.DetachInternetGatewayRequest;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceStateName;
import com.amazonaws.services.ec2.model.IpPermission;
import com.amazonaws.services.ec2.model.KeyPair;
import com.amazonaws.services.ec2.model.ModifySubnetAttributeRequest;
import com.amazonaws.services.ec2.model.ModifyVpcAttributeRequest;
import com.amazonaws.services.ec2.model.Region;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.RouteTable;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com.amazonaws.services.ec2.model.VolumeType;

public class EC2Agent {
	
	
	private AmazonEC2Client ec2Client;
	
	public EC2Agent(String accessKey, String secretKey){
		BasicAWSCredentials credentials = 
				new BasicAWSCredentials(accessKey, secretKey);
		ec2Client = new AmazonEC2Client(credentials);
	}
	
	
	public String createKeyPair(String keyName){
		CreateKeyPairRequest createKeyPairRequest = new CreateKeyPairRequest();
		createKeyPairRequest.withKeyName(keyName);
		CreateKeyPairResult createKeyPairResult =
				  ec2Client.createKeyPair(createKeyPairRequest);
		KeyPair keyPair = new KeyPair();
		keyPair = createKeyPairResult.getKeyPair();
		String privateKey = keyPair.getKeyMaterial();
		return privateKey;
	}
	
	public void setEndpoint(String endpoint){
		ec2Client.setEndpoint(endpoint);
	}
	
	public String createVPC(String vpcCIDR){
		CreateVpcRequest request = new CreateVpcRequest().withCidrBlock(vpcCIDR);
	    CreateVpcResult result = ec2Client.createVpc(request);
	    String vpcId = result.getVpc().getVpcId();
	    return vpcId;
	}
	
	/**
	 * Create a disk from the EC2 with a specified disk size and volume type according to the IOPS required.
	 * This can be attached to a instance latter.
	 * About the EC2 volume type, can be found below. 
	 * @see <a href="ec2">https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/EBSVolumeTypes.html?icmpid=docs_ec2_console</a>
	 * 
	 * @return volumeId
	 */
	public String createVolume(int diskSize, int IOPS){
		CreateVolumeRequest createVolumeRequest = new CreateVolumeRequest();
		if(diskSize < 4){
			createVolumeRequest.setVolumeType(VolumeType.Gp2);
		}else{
			if(IOPS == 0)
				createVolumeRequest.setVolumeType(VolumeType.Gp2);
			else{
				createVolumeRequest.setVolumeType(VolumeType.Io1);
				createVolumeRequest.setIops(IOPS);
			}
		}
		createVolumeRequest.setSize(diskSize);
		DescribeAvailabilityZonesResult daz = ec2Client.describeAvailabilityZones();
		createVolumeRequest.setAvailabilityZone(daz.getAvailabilityZones().get(0).getZoneName());
		CreateVolumeResult createVolumeResult = ec2Client.createVolume(createVolumeRequest);
		return createVolumeResult.getVolume().getVolumeId();
		
	}
	
	public void attachVolume(String volumeId, String instanceId){
		AttachVolumeRequest attachVolumeRequest = new AttachVolumeRequest();
		attachVolumeRequest.setVolumeId(volumeId);
		attachVolumeRequest.setInstanceId(instanceId);
		ec2Client.attachVolume(attachVolumeRequest);
		
	}
	
	public String getAssociateRouteTableId(String vpcId){
		System.out.println(vpcId);
		DescribeRouteTablesRequest describeRouteTablesRequest = new DescribeRouteTablesRequest();
	    DescribeRouteTablesResult describeRouteTablesResult = ec2Client.describeRouteTables(describeRouteTablesRequest);
	    List<RouteTable> rTables = describeRouteTablesResult.getRouteTables();
	    String routeTableId = "";
	    for(int i = 0 ; i<rTables.size() ; i++){
	    	String tmpVpcId = rTables.get(i).getVpcId();
	    	System.out.println(tmpVpcId+" "+rTables.get(i).getRouteTableId());
	    	if(tmpVpcId.equals(vpcId)){
	    		routeTableId = rTables.get(i).getRouteTableId();
	    		break;
	    	}
	    }
		return routeTableId;
	}
	
	public String createInternetGateway(String vpcId){
		CreateInternetGatewayRequest createInternetGatewayReq = new CreateInternetGatewayRequest();
	    CreateInternetGatewayResult createInternetGatewayResult = ec2Client.createInternetGateway(createInternetGatewayReq);
	    String internetGatewayId = createInternetGatewayResult.getInternetGateway().getInternetGatewayId();
	    AttachInternetGatewayRequest attachInternetGatewayRequest = new AttachInternetGatewayRequest()
	    		.withInternetGatewayId(internetGatewayId).withVpcId(vpcId);
	    ec2Client.attachInternetGateway(attachInternetGatewayRequest);
	    return internetGatewayId;
	}
	
	public void createRouteToGate(String routeTableId, String internetGatewayId, String destionationCIDR){
		CreateRouteRequest createRouteRequest = new CreateRouteRequest()
	    		.withRouteTableId(routeTableId).withGatewayId(internetGatewayId)
	    		.withDestinationCidrBlock("0.0.0.0/0");
	    ec2Client.createRoute(createRouteRequest);
	}
	
	public void enableVpcDNSHostName(String vpcId){
		ModifyVpcAttributeRequest modifyVpcAttributeReq = new ModifyVpcAttributeRequest()
	    		.withVpcId(vpcId)
	    		.withEnableDnsHostnames(true);
	    ec2Client.modifyVpcAttribute(modifyVpcAttributeReq);
	}
	
	public String createSubnet(String vpcId, String CIDR){
		CreateSubnetRequest subnetReq = new CreateSubnetRequest().withVpcId(vpcId).withCidrBlock(CIDR);
	    CreateSubnetResult subnetResult = ec2Client.createSubnet(subnetReq);
	    String subnetId = subnetResult.getSubnet().getSubnetId();
		return subnetId;
	}
	
	public void enableMapPubAddress(String subnetId){
		ModifySubnetAttributeRequest modifySubnetAttributeReq = new ModifySubnetAttributeRequest()
	    		.withSubnetId(subnetId).withMapPublicIpOnLaunch(true);
	    ec2Client.modifySubnetAttribute(modifySubnetAttributeReq);
	}
	
	public String createBasicSecurityGroup(String vpcId, String groupName, String description){
		CreateSecurityGroupRequest csgr = new CreateSecurityGroupRequest().withVpcId(vpcId);
		csgr.withGroupName(groupName).withDescription(description);
		CreateSecurityGroupResult createSecurityGroupResult = ec2Client.createSecurityGroup(csgr);
		String securityGroupId = createSecurityGroupResult.getGroupId();
		IpPermission ipPermission = new IpPermission();
		ipPermission.withIpRanges("0.0.0.0/0")
						.withIpProtocol("-1")
			            .withFromPort(0)
			            .withToPort(65535);
		AuthorizeSecurityGroupIngressRequest authorizeSecurityGroupIngressRequest =
			    new AuthorizeSecurityGroupIngressRequest();
		authorizeSecurityGroupIngressRequest.withGroupId(securityGroupId)
			                                    .withIpPermissions(ipPermission);
		ec2Client.authorizeSecurityGroupIngress(authorizeSecurityGroupIngressRequest);
		return securityGroupId;
	}
	
	public String runInstance(String subnetId, String securityGroupId, 
			String imageId, String privateIpAddress, String instanceType, String keyName){
		RunInstancesRequest runInstancesRequest =
			      new RunInstancesRequest();

		runInstancesRequest.withImageId(imageId)
		.withSubnetId(subnetId).withSecurityGroupIds(securityGroupId).withPrivateIpAddress(privateIpAddress)
			                     .withInstanceType(instanceType)
			                     .withMinCount(1)
			                     .withMaxCount(1)
			                     .withKeyName(keyName)
			                     ;
		RunInstancesResult runInstancesResult =
			      ec2Client.runInstances(runInstancesRequest);
		Reservation rv = runInstancesResult.getReservation();
		List <Instance> ins = rv.getInstances();
		String instanceId = ins.get(0).getInstanceId();
		return instanceId;
	}
	
	
	////Public Address:Instance ID
	public ArrayList<String> getPriPubAddressPair(ArrayList<String> instanceIds){
		DescribeInstancesRequest describeInstancesRequest =  new DescribeInstancesRequest()
				.withInstanceIds(instanceIds);

		String publicIpAddress = "";
		String instanceId = "";
		ArrayList<String> PriPubAddressPair = new ArrayList<String>();
		
		while(true){
			int count = 0;
			DescribeInstancesResult describeInstancesResult = ec2Client.describeInstances(describeInstancesRequest);
		    List<Reservation> reservations = describeInstancesResult.getReservations();
	
		    for(int i = 0 ; i<reservations.size() ; i++)
		    {
		    	List<Instance> instances = reservations.get(i).getInstances();
		    	for(int j = 0 ; j<instances.size() ; j++)
		    	{
		    		instanceId = instances.get(j).getInstanceId();
			        publicIpAddress = instances.get(j).getPublicIpAddress();
			        if(publicIpAddress != null)
			        {
			        	count++;
			        	PriPubAddressPair.add(publicIpAddress+":"+instanceId);
			        }
		    	}
		    }
		    if(count == instanceIds.size())
		    	break;
		    else
		    	PriPubAddressPair.clear();
	        
	        try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	        
		}
		
		return PriPubAddressPair;
	}
	
	
	public void waitValid(ArrayList<String> addresses)
	{
		ArrayList<Boolean> tags = new ArrayList<Boolean>();
		for(int i = 0 ; i<addresses.size() ; i++)
			tags.add(Boolean.FALSE);
		int count = 0;
		while(count<300){
			boolean allActive = true;
			count++;
			for(int i = 0 ; i<addresses.size() ; i++)
			{
				if(tags.get(i))
					continue;
				String [] pub_ins = addresses.get(i).split(":");
				try{
					String os = System.getProperty("os.name");
					String cmdPrix = "ping -c 1 -w 1 ";
					if(os.contains("OS"))
						cmdPrix = "ping -c 1 -t 1 ";
					Process process = Runtime.getRuntime().exec(cmdPrix+pub_ins[0]);
					System.out.println("ping -c 1 -w 1 "+pub_ins[0]);
					InputStreamReader ir = new InputStreamReader(process.getInputStream());
					BufferedReader input = new BufferedReader (ir);
					input.readLine();
					String line = input.readLine();
					if(line == null){
						allActive = false;
						continue;
					}
					System.out.println(line);
					if(line.contains(" ms"))
						tags.set(i, Boolean.TRUE);
					else
						allActive = false;
						
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			
			}
			if(allActive)
				break;
		}
		System.out.println("All instances valid");
		
	}
	
	public void terminateInstances(ArrayList<String> instances){
		TerminateInstancesRequest tir = new TerminateInstancesRequest().withInstanceIds(instances);
		ec2Client.terminateInstances(tir);
		while(true){
			DescribeInstancesRequest dis = new DescribeInstancesRequest().withInstanceIds(instances);
			DescribeInstancesResult disr = ec2Client.describeInstances(dis);
			List<Reservation> reservations = disr.getReservations();
			boolean allTerminated = true;
			int instanceCount = 0;
			for(Reservation reservation : reservations){
				for(Instance instance : reservation.getInstances()){
					instanceCount++;
					if(!instance.getState().getName().equals(InstanceStateName.Terminated.toString()))
						allTerminated = false;
				}
			}
			if(allTerminated && instanceCount == instances.size())
				break;
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public void deleteVpc(String vpcId, String subnetId, 
			String securityGroupId, String internetGatewayId){
		DeleteSubnetRequest dsreq = new DeleteSubnetRequest().withSubnetId(subnetId);
		ec2Client.deleteSubnet(dsreq);
		DetachInternetGatewayRequest digreq = new DetachInternetGatewayRequest()
				.withInternetGatewayId(internetGatewayId)
				.withVpcId(vpcId);
		ec2Client.detachInternetGateway(digreq);
		DeleteInternetGatewayRequest deleteInternetGatewayRequest = new DeleteInternetGatewayRequest().withInternetGatewayId(internetGatewayId);
		ec2Client.deleteInternetGateway(deleteInternetGatewayRequest);
		DeleteSecurityGroupRequest dsgreq = new DeleteSecurityGroupRequest().withGroupId(securityGroupId);
		ec2Client.deleteSecurityGroup(dsgreq);
		DeleteVpcRequest dvreq = new DeleteVpcRequest().withVpcId(vpcId);
		ec2Client.deleteVpc(dvreq);
	}

	public String getAvailableRegions(String format){
		
		DescribeRegionsResult region = ec2Client.describeRegions();
		List<Region> regions = region.getRegions();
		String result = "";
		if(format.equals("plain")){
			for(int i = 0 ; i<regions.size() ; i++)
				result += regions.get(i).toString().substring(1, regions.get(i).toString().length()-1)+"\n";
		}
		
		return result;
	}


}
