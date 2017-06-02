package provisioning.engine.VEngine.ExoGENI;

import provisioning.engine.VEngine.VEngine;
import topologyAnalysis.dataStructure.ExoGENI.ExoGENIVM;

public class ExoGENIVEngine extends VEngine{

	public ExoGENIVM curVM;
	
	
	//This is field is for user to choose running which operation
	//Possible value: "all", "connection", "script", "ssh"
	public String cmd;
}
