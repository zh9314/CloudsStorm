package provisioning.request;


import java.util.HashMap;
import java.util.Map;

import commonTool.ClassSet;

///This class is used to identify the vertical scaling for some VMs
///The scaled VM must be in the same datacenter
public class VScalingVMRequest {
	
	public class VMVScalingReqEle {
		
		/// identify this request
		public String reqID;
		
		public String orgVMName;
		
		///the target CPU/MEM which is going to be scaled vertically
		public double targetCPU;
		public double targetMEM;
		
		////this contains all the possible classes might be needed
		public ClassSet scaledClasses;
	}
	
	///Value means whether this request can be satisfied
	public Map<VMVScalingReqEle, Boolean> content = new HashMap<VMVScalingReqEle, Boolean>();
}
