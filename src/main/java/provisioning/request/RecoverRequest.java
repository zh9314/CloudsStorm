package provisioning.request;

public class RecoverRequest {

	////These two are the target datacenter, where this sub-topology is recovered. 
	public String cloudProvider;
	public String domain;
	
	///Identify the topology name that needs to be recovered
	public String topologyName;
}
