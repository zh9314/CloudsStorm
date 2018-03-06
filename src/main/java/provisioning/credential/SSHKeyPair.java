package provisioning.credential;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import commonTool.CommonTool;



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
	
	public boolean loadSSHKeyPair(String sshKeyPairId, String keyDir){
		String sshKeyDir = CommonTool.formatDirWithSep(keyDir);
		String privateKeyPath = sshKeyDir + "id_rsa";
        File privateKeyFile = new File(privateKeyPath);
        String publicKeyPath = sshKeyDir + "id_rsa.pub";
        File publicKeyFile = new File(publicKeyPath);
        String publicKeyIdPath = sshKeyDir + "name.pub";
        File publicKeyIdFile = new File(publicKeyIdPath);
        String privateKeyString = null, publicKeyString = null, publicKeyIdString = null;
        try {
        		if(privateKeyFile.exists())
        			privateKeyString = FileUtils.readFileToString(privateKeyFile, "UTF-8");
        		else
        			return false;
        		boolean atLeastOne = false;
        		if(publicKeyFile.exists()){
        			publicKeyString = FileUtils.readFileToString(publicKeyFile, "UTF-8");
        			atLeastOne = true;
        		}
        		if(publicKeyIdFile.exists()){
        			publicKeyIdString = FileUtils.readFileToString(publicKeyIdFile, "UTF-8");
        			atLeastOne = true;
        		}
        		if(!atLeastOne)
        			return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        this.publicKeyString = publicKeyString;
        this.privateKeyString = privateKeyString;
        this.publicKeyId = publicKeyIdString;
        this.SSHKeyPairId = sshKeyPairId;
        return true;
	}

}
