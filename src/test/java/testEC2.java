import java.util.ArrayList;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeSubnetsRequest;
import com.amazonaws.services.ec2.model.DescribeSubnetsResult;


public class testEC2 {

	public static void main(String[] args) {
		
		BasicAWSCredentials credentials = 
				new BasicAWSCredentials("AKIAITY3KHZUQ6M7YBSQ", "6J7uo99ifrff45sa6Gsy5vgb3bmrtwY6hBxtYt9y");
		AmazonEC2Client ec2Client = new AmazonEC2Client(credentials);
		ec2Client.setEndpoint("ec2.us-east-1.amazonaws.com");
		/*CreateKeyPairRequest createKeyPairRequest = new CreateKeyPairRequest();
		createKeyPairRequest.withKeyName("tt");
		CreateKeyPairResult createKeyPairResult =
				  ec2Client.createKeyPair(createKeyPairRequest);
		KeyPair keyPair = new KeyPair();
		keyPair = createKeyPairResult.getKeyPair();
		String privateKey = keyPair.getKeyMaterial();
		System.out.println(privateKey);*/
		
		ArrayList<String> subnetIds = new ArrayList<String>();
		subnetIds.add("subnet-e53f5ad9");
		DescribeSubnetsRequest describeSubnetsRequest = new DescribeSubnetsRequest();
		describeSubnetsRequest.setSubnetIds(subnetIds);
		DescribeSubnetsResult describeSubnetResult = ec2Client.describeSubnets(describeSubnetsRequest);
		System.out.println(describeSubnetResult.getSubnets().get(0).getAvailabilityZone());
		
		/*CreateVolumeRequest createVolumeRequest = new CreateVolumeRequest();
			createVolumeRequest.setVolumeType(VolumeType.Gp2);
		createVolumeRequest.setSize(8);
		DescribeAvailabilityZonesResult daz = ec2Client.describeAvailabilityZones();
		System.out.println(daz.toString());
		createVolumeRequest.setAvailabilityZone(daz.getAvailabilityZones().get(0).getZoneName());

		CreateVolumeResult createVolumeResult = ec2Client.createVolume(createVolumeRequest);
		System.out.println(createVolumeResult.getVolume().getVolumeId()))*/;
		
		
	}

}
