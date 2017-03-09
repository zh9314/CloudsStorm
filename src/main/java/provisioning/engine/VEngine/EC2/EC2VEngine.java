package provisioning.engine.VEngine.EC2;

import java.util.ArrayList;

import topologyAnalysis.dataStructure.SubConnection;
import topologyAnalysis.dataStructure.TopConnection;
import topologyAnalysis.dataStructure.EC2.EC2VM;



public class EC2VEngine {
	
	public EC2Agent ec2agent;
	public EC2VM curVM;
	
	
	//This is field is for user to choose running which operation
	//Possible value: "all", "connection", "script", "ssh"
	public String cmd;
	public ArrayList<SubConnection> subConnections;
	public ArrayList<TopConnection> topConnections;
	public String privateKeyString;
	////Used for generating the log file of executing the script on that VM
	public String currentDir;
	
	//below are used for ssh configuration. 
	//They can be set as null when just configure the inter connection
	public String userName;
	public String publicKeyString;
}
