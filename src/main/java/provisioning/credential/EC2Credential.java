package provisioning.credential;



public class EC2Credential extends BasicCredential{
	
	public String accessKey;
	public String secretKey;
	

	@Override
	public boolean validateCredential(String credInfoPath) {
		
		if(!super.validateCredential(credInfoPath))
			return false;
		
		if(accessKey == null || secretKey == null)
			return false;
		return true;
	}
	

}
