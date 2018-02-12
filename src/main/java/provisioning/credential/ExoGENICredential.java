package provisioning.credential;

import java.io.File;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonIgnore;
import commonTool.CommonTool;

public class ExoGENICredential extends BasicCredential{
	
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
	public boolean validateCredential(String credInfoPath) {
		
		if(!super.validateCredential(credInfoPath))
			return false;
		
		String curDir = CommonTool.getPathDir(credInfoPath);
		this.userKeyPath = curDir + this.userKeyName;
		File certf = new File(this.userKeyPath);
		if(!certf.exists()){
			logger.error("Cert file "+this.userKeyName+" does not exist!");
            return false;
		}
		return true;
	}
	
}
