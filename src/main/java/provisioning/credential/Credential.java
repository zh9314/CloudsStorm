package provisioning.credential;

import java.util.Map;


public abstract class Credential {
	
	
	
	/**
	 * Implemented by each sub class to load the specified credential to 'cloudAccess'
	 * in the cloudAccess.
	 * @return
	 */
	public abstract boolean loadCredential(String credInfoPath, Map<String, Credential> cloudAccess);
	
}
