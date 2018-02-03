package provisioning.database;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import commonTool.CommonTool;

/**
 * This class contains all the different databases 
 * describing different cloud providers.
 *
 */
public class UserDatabase {
	
	private static final Logger logger = Logger.getLogger(UserDatabase.class);
	
	/**
	 * The key is the cloud provider name (all are in lower case).
	 * Currently they are 'ec2', 'exogeni' and 'egi'. 
	 * The value is the content of the specific cloud database. 
	 */
	@JsonIgnore
	public Map<String, Database> databases;
	
	public ArrayList<DatabaseInfo> cloudDBs;
	
	/**
	 * This is used to load the user's all cloud database information from the YAML file.
	 * Fill out the 'databases' and than free the memory of 'cloudDBs'.
	 * @return
	 */
	public boolean loadCloudDBs(String dbsPath){
		ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
		this.databases = new HashMap<String, Database>();
		String curDir = CommonTool.getPathDir(dbsPath);
        try {
        UserDatabase userDatabase = mapper.readValue(new File(dbsPath), UserDatabase.class);
        	if(userDatabase == null){
        		logger.error("Users's Cloud databases from "+dbsPath+" is invalid!");
            	return false;
        	}
        	boolean allLoaded = true;
        	for(int i = 0 ; i < userDatabase.cloudDBs.size() ; i++){
        		DatabaseInfo databaseInfo = userDatabase.cloudDBs.get(i);
        		String cp = databaseInfo.cloudProvider.toLowerCase().trim();
        		if(cp == null){
        			logger.warn("Cloud provider must be set!");
        			allLoaded = false;
        			continue;
        		}
        		if(databaseInfo.dbInfoFile == null){
        			logger.warn("The 'dbInfoPath' of Cloud provider "+cp+" is missing!");
        			allLoaded = false;
        			continue;
        		}
        		String className = "";
        		String dbInfoPath = curDir + databaseInfo.dbInfoFile;
        		if(databaseInfo.dbClassPath == null){
        			String packagePrefix = "provisioning.database";
        			if(cp.equals("ec2"))
        				className = packagePrefix+".EC2.EC2Database";
        			else if(cp.equals("exogeni"))
        				className = packagePrefix+".ExoGENI.ExoGENIDatabase";
        			else if(cp.equals("egi"))
        				className = packagePrefix+".EGI.EGIDatabase";
        			else{
        				logger.warn("Cloud provider of "+cp+" has not been supported yet!");
        				allLoaded = false;
        				continue;
        			}
        		}else
        			className = databaseInfo.dbClassPath;
        		
    			try {
    				Object cloudDB = Class.forName(className).newInstance();
    				boolean loaded = ((Database)cloudDB).loadDatabase(dbInfoPath, this.databases);
    				if(!loaded){
    					logger.warn("Databse for Cloud provider "+cp+" cannot be loaded!");
        				allLoaded = false;
        				continue;
    				}
    			} catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
    				e.printStackTrace();
    				logger.warn("Cannot load the database class for "+cp+": "+e.getMessage());
    				allLoaded = false;
    				continue;
    			}
        	}
        	if(allLoaded)
        		logger.info("User's Cloud databases from "+dbsPath+" are all loaded!");
        	else
        		logger.error("Some database information cannot be loaded!");
        	return allLoaded;
        } catch (Exception e) {
            logger.error(e.toString());
            e.printStackTrace();
            return false;
        }
	}
}
