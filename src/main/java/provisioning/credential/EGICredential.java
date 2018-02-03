package provisioning.credential;

import java.io.File;
import java.util.Map;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import commonTool.CommonTool;

public class EGICredential extends Credential{
	
	private static final Logger logger = Logger.getLogger(EGICredential.class);
	@JsonIgnore
	public String proxyFilePath;
	@JsonIgnore
	public String trustedCertPath;
	
	public String proxyFileName;
	public String trustedCertDirName;
	
	@Override
	public boolean loadCredential(String credInfoPath,
			Map<String, Credential> cloudAccess) {
		ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
		EGICredential egiCredential = null;
		try {
			egiCredential = mapper.readValue(new File(credInfoPath), EGICredential.class);
	        	if(egiCredential == null
	        		|| egiCredential.proxyFileName == null
	        		|| egiCredential.trustedCertDirName == null){
	        		logger.error("Users's EGI credentials from "+credInfoPath+" is invalid!");
	            	return false;
	        	}
		 }catch (Exception e) {
             logger.error(e.toString());
             e.printStackTrace();
             return false;
         }
		String curDir = CommonTool.getPathDir(credInfoPath);
		egiCredential.proxyFilePath = curDir + egiCredential.proxyFileName;
		egiCredential.trustedCertPath = curDir + egiCredential.trustedCertDirName;
		File certf = new File(egiCredential.proxyFilePath);
		if(!certf.exists()){
			logger.error("Cert file "+egiCredential.proxyFileName+" does not exist!");
            return false;
		}
		certf = new File(egiCredential.trustedCertPath);
		if(!certf.exists()){
			logger.error("Cert file "+egiCredential.trustedCertPath+" does not exist!");
            return false;
		}
		cloudAccess.put("egi", egiCredential);
		return true;
	}
}
