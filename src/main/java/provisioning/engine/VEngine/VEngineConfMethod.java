package provisioning.engine.VEngine;

import topology.description.actual.VM;

/**
 * Mainly used for configuring the VMs to be connected with each other
 * as the developer designs and running the scripts, etc.
 * This interface is more OS related.
 */
public interface VEngineConfMethod {
	
	

	/**
	 * Configuration on the connection to leverage some type of
	 * VNF technique to manage the network. Hence, the private network
	 * is provisioned among public Clouds.  
	 */
	public boolean confVNF(VM subjectVM);
	
	/**
	 * Configuration on the application-defined
	 * SSH account and public keys
	 * @param subjectVM
	 * @param account
	 * @param publicKey the public key provided by the application associated with
	 * this SSH account.
	 * @return
	 */
	public boolean confSSH(VM subjectVM);
	
	/**
	 * Run the application-defined script to configure the VM
	 * environment.
	 * @param subjectVM
	 * @param currentDir Used for generating the log file of executing the script on that VM
	 * @return
	 */
	public boolean confENV(VM subjectVM);
	
	/**
	 * This method is to detach from failed sub-topologies. 
	 * It removes all the connections with the failed 
	 * sub-topologies.
	 */
	public boolean detach(VM subjectVM);
	
}
