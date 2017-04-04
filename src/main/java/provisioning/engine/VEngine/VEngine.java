package provisioning.engine.VEngine;

import java.util.ArrayList;

import topologyAnalysis.dataStructure.SubConnection;
import topologyAnalysis.dataStructure.TopConnectionPoint;

public class VEngine {

	public ArrayList<SubConnection> subConnections;
	public ArrayList<TopConnectionPoint> topConnectors;
	public String privateKeyString;
	////Used for generating the log file of executing the script on that VM
	public String currentDir;
	
	//below are used for ssh configuration. 
	//They can be set as null when just configure the inter connection
	public String userName;
	public String publicKeyString;
	
}
