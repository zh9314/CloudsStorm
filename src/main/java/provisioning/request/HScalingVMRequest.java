package provisioning.request;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import commonTool.ClassSet;

///This class can be either scaling-up request.
///Currently, this request specify several VMs to scale up
public class HScalingVMRequest {
	
	public class VMScalingReqEle {
		
		/// identify this request
		public String reqID;
		
		////These two are the target datacenter, where this sub-topology is recovered. 
		public String cloudProvider;
		public String domain;
		
		////identify the name of the scaled topology. If it is null, this will be auto generated.
		public String scaledTopology;
		
		///this is a list of VM names to be scaled. They are not full names.
		public ArrayList<String> targetVMs = new ArrayList<String>();
		
		////this contains all the possible classes might be needed
		public ClassSet scaledClasses;
	}
	///The key identifies the topology name that needs to be scaled
	///Value means whether this request can be satisfied
	public Map<VMScalingReqEle, Boolean> content = new HashMap<VMScalingReqEle, Boolean>();
}
