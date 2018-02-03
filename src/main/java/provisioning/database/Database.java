package provisioning.database;

import java.util.HashMap;
import java.util.Map;


public abstract class Database {
	////This is a toolInfo for provisioner to find some key information.
	public Map<String, String> toolInfo = new HashMap<String, String>();

	//public abstract boolean loadDomainInfoFromFile(String filePath);
	
	public abstract boolean loadDatabase(String dbInfoFile, Map<String, Database> databases);
	
	public abstract String getEndpoint(String domain);
	
	public abstract VMMetaInfo getVMMetaInfo(String domain, String OS, String vmType);
}
