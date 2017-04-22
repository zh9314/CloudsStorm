package provisioning.engine.VEngine.EGI;

import provisioning.engine.VEngine.VEngine;
import topologyAnalysis.dataStructure.EGI.EGIVM;

public class EGIVEngine extends VEngine{
	public EGIAgent egiAgent;
	public EGIVM curVM;
	
	
	//This is field is for user to choose running which operation
	//Possible value: "all", "connection", "script", "ssh"
	public String cmd;
}
