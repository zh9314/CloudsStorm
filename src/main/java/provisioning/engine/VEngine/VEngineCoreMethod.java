package provisioning.engine.VEngine;

/**
 * Mainly used for configuring the VMs to be connected with each other
 * as the developer designs and running the scripts, etc.
 */
public interface VEngineCoreMethod {

	/**
	 * Configuration on the connection to  
	 */
	public void connectionConf();
	
	/**
	 * Configuration on the SSH account and public keys
	 */
	public void sshConf();
	
	/**
	 * Run the script predefined by user.
	 */
	public void runScript();
	
	/**
	 * This method is to remove the tunnel connected to the failed 
	 * sub-topology.
	 */
	public void removeEth();
	
}
