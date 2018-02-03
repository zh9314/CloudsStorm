package provisioning.database.EC2;

import java.io.File;
import java.util.ArrayList;
import java.util.Map;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import provisioning.database.Database;
import provisioning.database.VMMetaInfo;

public class EC2Database extends Database {
	
	private static final Logger logger = Logger.getLogger(EC2Database.class);
	
	public ArrayList<EC2DCMetaInfo> DCMetaInfo;
	
	
	/**
	 * Get the ami string according to the OS and domain.
	 * Null is returned if there is no such ami.
	 */
	public String getAMI(String OS, String domain){
		if(OS == null || domain == null)
			return null;
		for(int di = 0 ; di<DCMetaInfo.size() ; di++){
			EC2DCMetaInfo ec2DCMetaInfo = DCMetaInfo.get(di);
			if(ec2DCMetaInfo.domain != null
				&& domain.trim().equalsIgnoreCase(ec2DCMetaInfo.domain.trim())){
				for(int vi = 0 ; vi<ec2DCMetaInfo.VMMetaInfo.size() ; vi++){
					EC2VMMetaInfo ec2VMMetaInfo = ec2DCMetaInfo.VMMetaInfo.get(vi);
					if(ec2VMMetaInfo.OS != null
						&& OS.trim().equalsIgnoreCase(ec2VMMetaInfo.OS.trim())){
						return ec2VMMetaInfo.AMI;
					}
				}
			}
			
		}
		return null;
	}

	@Override
	public boolean loadDatabase(String dbInfoPath, Map<String, Database> databases) {
		ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
		EC2Database ec2Database = null;
		try {
			ec2Database = mapper.readValue(new File(dbInfoPath), EC2Database.class);
	        	if(ec2Database == null){
	        		logger.error("Users's EC2 database from "+dbInfoPath+" is invalid!");
	            	return false;
	        	}
		 }catch (Exception e) {
             logger.error(e.toString());
             e.printStackTrace();
             return false;
         }
		ec2Database.toolInfo.put("sengine", "provisioning.engine.SEngine.EC2SEngine");
		databases.put("ec2", ec2Database);
		return true;
	}

	@Override
	public String getEndpoint(String domain) {
		if(domain == null)
			return null;
		for(int di = 0 ; di < DCMetaInfo.size(); di++)
			if(DCMetaInfo.get(di).domain != null
			 && domain.trim().equalsIgnoreCase(DCMetaInfo.get(di).domain.trim()))
				return DCMetaInfo.get(di).endpoint;
		
		return null;
	}

	@Override
	public VMMetaInfo getVMMetaInfo(String domain, String OS, String vmType) {
		if(domain == null || OS == null || vmType == null)
			return null;
		for(int di = 0 ; di < DCMetaInfo.size(); di++)
			if(DCMetaInfo.get(di).domain != null
			 && domain.trim().equalsIgnoreCase(DCMetaInfo.get(di).domain.trim())){
				for(int vi = 0 ; vi < DCMetaInfo.get(di).VMMetaInfo.size() ; vi++){
					EC2VMMetaInfo curInfo = DCMetaInfo.get(di).VMMetaInfo.get(vi);
					if(curInfo.OS != null
				      && curInfo.VMType != null
				      && OS.trim().equalsIgnoreCase(curInfo.OS.trim())
				      && vmType.trim().equalsIgnoreCase(curInfo.VMType.trim()))
						return (VMMetaInfo)curInfo;
				}
			}
				
		
		return null;
	}

}
