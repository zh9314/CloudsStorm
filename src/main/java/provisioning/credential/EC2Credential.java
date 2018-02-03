package provisioning.credential;

import java.io.File;
import java.util.Map;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

public class EC2Credential extends Credential{
	
	private static final Logger logger = Logger.getLogger(EC2Credential.class);
	
	public String accessKey;
	public String secretKey;
	

	@Override
	public boolean loadCredential(String credInfoPath,
			Map<String, Credential> cloudAccess) {
		ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
		EC2Credential ec2Credential = null;
		try {
			ec2Credential = mapper.readValue(new File(credInfoPath), EC2Credential.class);
	        	if(ec2Credential == null){
	        		logger.error("Users's EC2 credentials from "+credInfoPath+" is invalid!");
	            	return false;
	        	}
		 }catch (Exception e) {
             logger.error(e.toString());
             e.printStackTrace();
             return false;
         }
		cloudAccess.put("ec2", ec2Credential);
		return true;
	}
	

}
