package provisioning.database;

import java.io.File;
import java.util.Map;

import org.apache.log4j.Logger;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import commonTool.ClassDB;

public class DatabaseInfo {
	
	private static final Logger logger = Logger.getLogger(DatabaseInfo.class);
	
	
	public String cloudProvider;
	
	/**
	 * This is used to identify the class path of the specific database class 
	 * for the Cloud. It is only useful when the application wants to extend the database 
	 * for its own Cloud. If it is 'null', than it is used the default Class path currently 
	 * supported.
	 */
	public String dbClass;
	
	public String dbInfoFile;
	
	public boolean loadDatabase(String dbInfoPath, Map<String, Database> databases) {
		if(cloudProvider == null){
			logger.error("Cloud provider must be set!");
			return false;
		}
		String cp = cloudProvider.trim().toLowerCase();
		ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
		Class<?> XDatabase = ClassDB.getDatabase(cp, this.dbClass);
		if(XDatabase == null){
			logger.error("Cannot load database class for Cloud '"
					+cloudProvider+"' with Class name '"+dbClass+"'!");
			return false;
		}
		Object xDatabase = null;
		try {
			xDatabase = XDatabase.newInstance();
			xDatabase = mapper.readValue(new File(dbInfoPath), XDatabase);
	        	if(xDatabase == null){
	        		logger.error("Users's database from "+dbInfoPath+" is invalid!");
	            	return false;
	        	}
		 }catch (Exception e) {
             logger.error(e.getMessage());
             e.printStackTrace();
             return false;
         }
		databases.put(cp, (Database)xDatabase);
		return true;
	}
}
