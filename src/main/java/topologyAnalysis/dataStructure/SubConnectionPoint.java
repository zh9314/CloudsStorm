package topologyAnalysis.dataStructure;

import com.fasterxml.jackson.annotation.JsonIgnore;

//This is the connection point definition in the low level description file.
public class SubConnectionPoint {
	
	public String componentName;
	//public String portName;
	public String netmask;
	public String address;
	
	//Point to the VM that this connection point belongs to. 
	//Main goal is to get the public address after provisioning.
	@JsonIgnore
	public VM belongingVM;

}
