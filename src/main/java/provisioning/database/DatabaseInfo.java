package provisioning.database;

public class DatabaseInfo {
	public String cloudProvider;
	
	/**
	 * This is used to identify the class path of the specific database class 
	 * for the Cloud. It is only useful when the application wants to extend the database 
	 * for its own Cloud. If it is 'null', than it is used the default Class path currently 
	 * supported.
	 */
	public String dbClassPath;
	
	public String dbInfoFile;
}
