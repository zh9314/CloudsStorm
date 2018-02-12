package provisioning.credential;

import java.util.Map;



public abstract class Credential {
	

	public Map<String, String> credInfo;
	
	/**
	 * Implemented by each sub class to validate the specified credential
	 * in the cloudAccess.
	 * @return
	 */
	public abstract boolean validateCredential(String credInfoPath);
	
}
