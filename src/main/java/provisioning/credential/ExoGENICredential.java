package provisioning.credential;

import java.io.File;
import java.util.Map;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import commonTool.CommonTool;

public class ExoGENICredential extends Credential{
	
	private static final Logger logger = Logger.getLogger(ExoGENICredential.class);
	
	@JsonIgnore
	public String userKeyPath;
	
	/**
	 * The key files must be in the same directory with the yml files.
	 */
	public String userKeyName;
	public String keyAlias;
	public String keyPassword;
	@Override
	public boolean loadCredential(String credInfoPath,
			Map<String, Credential> cloudAccess) {
		ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
		ExoGENICredential exoGENICredential = null;
		try {
			exoGENICredential = mapper.readValue(new File(credInfoPath), ExoGENICredential.class);
	        	if(exoGENICredential == null
	        		|| exoGENICredential.userKeyName == null
	        		|| exoGENICredential.keyAlias == null
	        		|| exoGENICredential.keyPassword == null){
	        		logger.error("Users's ExoGENI credentials from "+credInfoPath+" is invalid!");
	            	return false;
	        	}
		 }catch (Exception e) {
             logger.error(e.toString());
             e.printStackTrace();
             return false;
         }
		String curDir = CommonTool.getPathDir(credInfoPath);
		exoGENICredential.userKeyPath = curDir + exoGENICredential.userKeyName;
		File certf = new File(exoGENICredential.userKeyPath);
		if(!certf.exists()){
			logger.error("Cert file "+exoGENICredential.userKeyName+" does not exist!");
            return false;
		}
		cloudAccess.put("exogeni", exoGENICredential);
		return true;
	}
	
}
