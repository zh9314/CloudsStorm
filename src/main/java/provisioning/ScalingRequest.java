package provisioning;

///This class can be either scaling-up or scaling-down request.
public class ScalingRequest {
	public String cloudProvider;
	public String domain;
	
	//Indicate whether this scaling request has been satisfied. 
	public boolean satisfied;
}
