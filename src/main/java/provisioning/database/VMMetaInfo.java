package provisioning.database;

import java.util.Map;

public abstract class VMMetaInfo {
	/**
	 * For example, 'ubuntu 14.04'
	 */
	public String OS;
	
	/**
	 * A positive number to identify how many vcores. 
	 */
	public String CPU;
	
	/**
	 * A number to identify how much memory, the unit is 'G'.
	 */
	public String MEM;
	
	/**
	 * The type of this VM for this Cloud, such as medium, small. 
	 * However, they have different names for different Clouds.
	 */
	public String VMType;
	
	/**
	 * Counted by dollars per hour.
	 */
	public String Price;
	
	/**
	 * Used for ssh to login when provisioned by default.
	 */
	public String DefaultSSHAccount;
	
	///reserved for performance model
	//public String ProvisionCost;
	
	/**
	 * For EC2 put:
	 * AMI
	 * 
	 * For ExoGENI put:
	 * OSurl
	 * OSguid
	 * DiskSize
	 * 
	 * For EGI put:
	 * OS_occi_ID
	 * OS_occi_ID
	 */
	public Map<String, String> extraInfo;
	
}
