package provisioning.credential;

import java.io.File;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonIgnore;
import commonTool.CommonTool;

public class EGICredential extends BasicCredential{
	
	private static final Logger logger = Logger.getLogger(EGICredential.class);
	@JsonIgnore
	public String proxyFilePath;
	@JsonIgnore
	public String trustedCertPath;
	
	public String proxyFileName;
	public String trustedCertDirName;
	
	@Override
	public boolean validateCredential(String credInfoPath) {
		
		if(!super.validateCredential(credInfoPath))
			return false;
		
		String curDir = CommonTool.getPathDir(credInfoPath);
		this.proxyFilePath = curDir + this.proxyFileName;
		this.trustedCertPath = curDir + this.trustedCertDirName;
		File certf = new File(this.proxyFilePath);
		if(!certf.exists()){
			logger.error("Cert file "+this.proxyFileName+" does not exist!");
            return false;
		}
		certf = new File(this.trustedCertPath);
		if(!certf.exists()){
			logger.error("Cert file "+this.trustedCertPath+" does not exist!");
            return false;
		}
		return true;
	}
}
