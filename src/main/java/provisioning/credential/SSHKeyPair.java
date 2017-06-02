package provisioning.credential;



/**
 * This is a class to store the ssh keys.
 *
 */
public class SSHKeyPair {
	
	
	//This is a GUID. In some case, the public and key files are store in the directory of this GUID.
	//And this directory is in the same folder of the description files.
	//The public key file name is always 'id_rsa.pub' and the private key file name is always 'id_rsa'.
	//If the public key content cannot be got, there will  be a file called 'name.pub' to store the public key name.
	public String SSHKeyPairId;
	
	public String publicKeyString;
	
	//In some case, the public key string is unknown. You can just get the key id, EC2 for instance.
	public String publicKeyId;
	
	public String privateKeyString;
	
	

}
