package provisioning;

///This class can be either scaling-up or scaling-down request.
public class ScalingRequest {
	public String cloudProvider;
	public String domain;
	
	///identify which IP address want to use. If it is null, then any available address will be used.
	public String address;
	
	//Indicate whether this scaling request has been satisfied. 
	public boolean satisfied;
}
