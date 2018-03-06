package provisioning.database;

import java.util.Map;


public abstract class Database {

	public Map<String, String> extraInfo;
	
	public abstract DCMetaInfo getDCMetaInfo(String domain);
	
	public abstract String getEndpoint(String domain);
	
	public abstract VMMetaInfo getVMMetaInfo(String domain, String OS, String vmType);
	
	/**
	 * Find the most close VM type in this domain. The unit for memory must be 'G'
	 * @param domain
	 * @param OS
	 * @param vCPUNum
	 * @param mem
	 * @return
	 */
	public abstract String getVMType(String domain, String OS, double vCPUNum, double mem);
}
