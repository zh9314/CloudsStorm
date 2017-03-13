package provisioning.engine.SEngine;

import topologyAnalysis.dataStructure.SubTopologyInfo;


public abstract class SEngine {

	
	/**
	 *  This is must be invoked before provisioning. It includes: <br/>
	 *  1. If the sub-topology status is not 'fresh', the 'sshKeyPairId' to access the cloud 
	 * cannot be null.
	 */
	public boolean commonRuntimeCheck(SubTopologyInfo subTopologyInfo){
		if(subTopologyInfo.status.trim().toLowerCase().equals("running") && 
				subTopologyInfo.sshKeyPairId == null){
			return false;
		}
		return true;
	}
}
