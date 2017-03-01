package topologyAnalysis.dataStructure.EC2;

import com.fasterxml.jackson.annotation.JsonIgnore;

import topologyAnalysis.dataStructure.VM;

public class EC2VM extends VM{

	//The unit is GigaByte and must be a positive integer. 
	//This field is only valid when this sub-topology is from EC2. 
	//The disk size of node in the ExoGENI is fixed.
	public String diskSize;
		
	//Possible to be added later to satisfy the IOPS requirements of the developer.
	//public String diskType;
	
	//The following fields are used for deleting the sub-topology. 
	//Only the field of instanceId can be written to the response file.
	//All the fields should be written to the control file, which is not returned to user.
	public String vpcId;
	
	public String subnetId;
	
	public String securityGroupId;
	
	public String instanceId;
	
	
	//This is only valid during provisioning. 
	//This will be loaded depending on the domain, OStype and VMtype.
	@JsonIgnore
	public String AMI;

}
