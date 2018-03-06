package provisioning.request;

import java.util.HashMap;
import java.util.Map;

import commonTool.ClassSet;

public class RecoverRequest {
	
	public class RecoverReqEle {
		
		////These two are the target datacenter, where this sub-topology is recovered. 
		public String cloudProvider;
		public String domain;
		
		///Identify the topology name that needs to be recovered
		public String topologyName;
		
		////this contains all the possible classes might be needed
		public ClassSet scaledClasses;
	}
	
	///The key identifies the topology name that needs to be recovered
	///Value means whether this request can be satisfied
	public Map<RecoverReqEle, Boolean> content = new HashMap<RecoverReqEle, Boolean>();
}
