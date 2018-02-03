package provisioning.credential;

public class CredentialInfo {
	/**
	 * The cloud provider of this credential
	 */
	public String cloudProvider;
	
	/**
	 * This is used to identify the class path of the specific credential class 
	 * for the Cloud. It is only useful when the application wants to extend the credential 
	 * for its own Cloud. If it is 'null', than it is used the default Class path currently 
	 * supported.
	 */
	public String credClassPath;
	
	/**
	 * The content of the credential. It must be in the same folder.
	 */
	public String credInfoFile;
}
