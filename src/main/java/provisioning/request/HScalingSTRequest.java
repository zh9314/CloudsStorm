package provisioning.request;

import java.util.HashMap;
import java.util.Map;

import commonTool.ClassSet;

///This class can be either scaling-up or scaling-down request.
///Currently, this request will only specify a sub-topology to scale up or down
public class HScalingSTRequest {
	
	public class STScalingReqEle {
		
		/// identify this request
		public String reqID;
		
		////These two are the target datacenter, where this sub-topology is recovered. 
		public String cloudProvider;
		public String domain;
		
		////identify the name of the scaled topology. If it is null, this will be auto generated.
		public String scaledTopology;
		
		///Identify the topology name that needs to be scaled
		public String targetTopology;
		
		////this contains all the possible classes might be needed
		public ClassSet scaledClasses;
		
		///if it is true, means scaling up
		public boolean scalingUpDown;
	}
	
	///Value means whether this request can be satisfied
	public Map<STScalingReqEle, Boolean> content = new HashMap<STScalingReqEle, Boolean>();
}
