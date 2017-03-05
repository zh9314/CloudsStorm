package provisioning.database;

import java.util.Map;

/**
 * This class contains all the different databases 
 * describing different cloud providers.
 *
 */
public class UserDatabase {
	/**
	 * The key is the cloud provider name (all are in lower case).
	 * Currently they are 'ec2', ('exogeni', 'geni'). 
	 * The value is the content of the specific cloud database. 
	 */
	public Map<String, Database> databases;
}
