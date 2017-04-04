package provisioning.engine.VEngine.EC2;


import provisioning.engine.VEngine.VEngine;
import topologyAnalysis.dataStructure.EC2.EC2VM;



public class EC2VEngine extends VEngine{
	
	public EC2Agent ec2agent;
	public EC2VM curVM;
	
	
	//This is field is for user to choose running which operation
	//Possible value: "all", "connection", "script", "ssh"
	public String cmd;
	
}
