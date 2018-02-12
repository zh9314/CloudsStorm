package provisioning.database;

import java.util.Map;


public abstract class Database {

	public Map<String, String> extraInfo;
	
	public abstract DCMetaInfo getDCMetaInfo(String domain);
	
	public abstract String getEndpoint(String domain);
	
	public abstract VMMetaInfo getVMMetaInfo(String domain, String OS, String vmType);
}
