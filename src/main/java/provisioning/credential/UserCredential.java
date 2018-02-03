package provisioning.credential;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import commonTool.CommonTool;
import topologyAnalysis.dataStructure.TopTopology;


/**
 * This class describes all the credential information needed by a top level topology.
 *
 */
public class UserCredential {
	
	private static final Logger logger = Logger.getLogger(UserCredential.class);
	
	/**
	 * This is a map for the user and provisioner to access some specific sub-topology through ssh.
	 * The key is the sub-topology name defined in the sub-topology. 
	 * The value is the content of the ssh key pair for accessing. 
	 */
	@JsonIgnore
	public Map<String, SSHKeyPair> sshAccess = new HashMap<String, SSHKeyPair>();
	
	/**
	 * This is a map for provisioner and user to control some specific Cloud to provision or stop etc.
	 * The key is the Cloud provider name.
	 * Currently they are 'ec2', ('exogeni', 'geni'). 
	 * The value is the content of the specific cloud credential. 
	 */
	@JsonIgnore
	public Map<String, Credential> cloudAccess;
	
	public ArrayList<CredentialInfo> cloudCreds;
	
	/**
	 * This is used to load the user's all cloud credentials from the YAML file.
	 * Fill out the 'cloudAccess' and than free the memory of 'cloudCreds'.
	 * @return
	 */
	public boolean loadCloudAccessCreds(String credsPath){
		ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
		this.cloudAccess = new HashMap<String, Credential>();
		String curDir = CommonTool.getPathDir(credsPath);
        try {
        UserCredential userCredential = mapper.readValue(new File(credsPath), UserCredential.class);
        	if(userCredential == null){
        		logger.error("Users's Cloud credentials from "+credsPath+" is invalid!");
            	return false;
        	}
        	boolean allLoaded = true;
        	for(int i = 0 ; i < userCredential.cloudCreds.size() ; i++){
        		CredentialInfo credentialInfo = userCredential.cloudCreds.get(i);
        		String cp = credentialInfo.cloudProvider.toLowerCase().trim();
        		if(cp == null){
        			logger.warn("Cloud provider must be set!");
        			allLoaded = false;
        			continue;
        		}
        		if(credentialInfo.credInfoFile == null){
        			logger.warn("The 'credInfoPath' of Cloud provider "+cp+" is missing!");
        			allLoaded = false;
        			continue;
        		}
        		String className = "";
        		String credInfoPath = curDir + credentialInfo.credInfoFile;
        		if(credentialInfo.credClassPath == null){
        			String packagePrefix = "provisioning.credential";
        			if(cp.equals("ec2"))
        				className = packagePrefix+".EC2Credential";
        			else if(cp.equals("exogeni"))
        				className = packagePrefix+".ExoGENICredential";
        			else if(cp.equals("egi"))
        				className = packagePrefix+".EGICredential";
        			else{
        				logger.warn("Cloud provider of "+cp+" has not been supported yet!");
        				allLoaded = false;
        				continue;
        			}
        		}else
        			className = credentialInfo.credClassPath;
        		
    			try {
    				Object cloudCred = Class.forName(className).newInstance();
    				boolean loaded = ((Credential)cloudCred).loadCredential(credInfoPath, this.cloudAccess);
    				if(!loaded){
    					logger.warn("Credential for Cloud provider "+cp+" cannot be loaded!");
        				allLoaded = false;
        				continue;
    				}
    			} catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
    				e.printStackTrace();
    				logger.warn("Cannot load the credential class for "+cp+": "+e.getMessage());
    				allLoaded = false;
    				continue;
    			}
        	}
        	if(allLoaded)
        		logger.info("User's Cloud credentials from "+credsPath+" are all loaded!");
        	else
        		logger.error("Some credential cannot be loaded!");
        	return allLoaded;
        } catch (Exception e) {
            logger.error(e.toString());
            e.printStackTrace();
            return false;
        }
	}
	/**
	 * Initial all the ssh keys with the top-topology
	 * @param sshKeysDir
	 * @param topTopology
	 * @return
	 */
	public boolean initalSSHKeys(String sshKeysDir, TopTopology topTopology){
		if(topTopology == null){
			logger.error("Toptopology should be first initialized!");
			return false;
		}
		ArrayList<SSHKeyPair> sshKeyPairs = loadSSHKeyPairFromFile(sshKeysDir);
		if(sshKeyPairs == null){
			logger.error("Error happens during loading ssh key pairs!");
			return false;
		}
		if(sshKeyPairs.size() == 0)
			logger.warn("No ssh key pair is loaded!");
		else{
			if(!initial(sshKeyPairs, topTopology)){
				logger.error("Error happens during initializing the ssh keys for accessing the clouds!");
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Load ssh key pairs from current directory.
	 * A directory in currentDir represents a key pair.
	 * The name of the directory represents the key pair id.
	 * @param currentDir
	 * @return an arraylist of key pairs. If some error happens, it will be null.
	 * If there is no key pairs available, the size of the array will be 0.
	 */
	private ArrayList<SSHKeyPair> loadSSHKeyPairFromFile(String currentDir){
		ArrayList<SSHKeyPair> keyPairs = new ArrayList<SSHKeyPair>();
		File curDir = new File(currentDir);
		File[] files = curDir.listFiles();
		if(files != null){
			for(File f: files){
				if(f.isDirectory()){
					SSHKeyPair kp = new SSHKeyPair();
					kp.SSHKeyPairId = f.getName();
					File priKeyFile = new File(f.getAbsolutePath()+File.separator+"id_rsa");
					File pubKeyFile = new File(f.getAbsolutePath()+File.separator+"id_rsa.pub");
					File pubKeyIdFile = new File(f.getAbsolutePath()+File.separator+"name.pub");
					
					try {
						if(priKeyFile.exists())
							kp.privateKeyString = FileUtils.readFileToString(priKeyFile, "UTF-8");
						if(pubKeyFile.exists())
							kp.publicKeyString = FileUtils.readFileToString(pubKeyFile, "UTF-8");
						if(pubKeyIdFile.exists())
							kp.publicKeyId = FileUtils.readFileToString(pubKeyIdFile, "UTF-8");
					} catch (IOException e) {
						e.printStackTrace();
						logger.error("UnKnown reason!");
					}
					
					if(!priKeyFile.exists()){
						logger.error("Missing private key file for key pair "+f.getName());
						return null;
					}
					if(!pubKeyFile.exists() && !pubKeyIdFile.exists()){
						logger.error("Both missing public key file and public key id file for key pair "+f.getName());
						return null;
					}
					
					keyPairs.add(kp);
				}
			}
		}else{
			logger.error("The directory "+currentDir+" doesn't exist!");
			return null;
		}
		return keyPairs;
	}
	
	/**
	 * This method is used to initial the field  
	 * 'sshAccess' combining the information from the TopTopology
	 * and the array of ssh key pairs loaded.
	 * @return
	 */
	private boolean initial(ArrayList<SSHKeyPair> sshKeyPairs, TopTopology topTopology){
		for(int ti = 0 ; ti < topTopology.topologies.size() ; ti++){
			if(topTopology.topologies.get(ti).sshKeyPairId != null){
				String keyPairId = topTopology.topologies.get(ti).sshKeyPairId;
				SSHKeyPair kp = getSSHKeyPair(sshKeyPairs, keyPairId);
				if(kp == null){
					logger.error("'sshKeyPairId' '"+keyPairId+"' of sub-topology '"+topTopology.topologies.get(ti).topology+"' cannot be found!");
					return false;
				}else{
					topTopology.topologies.get(ti).subTopology.accessKeyPair = kp;
					sshAccess.put(topTopology.topologies.get(ti).domain.trim().toLowerCase(), kp);
				}
			}else{
				if(topTopology.topologies.get(ti).status.trim().toLowerCase().equals("running")){
					logger.error("Missing access keys for provisioned sub-topology '"+topTopology.topologies.get(ti).topology+"'!");
					return false;
				}
			}
		}
		
		
		return true;
	}
	
	private SSHKeyPair getSSHKeyPair(ArrayList<SSHKeyPair> sshKeyPairs, String keyPairId){
		SSHKeyPair kp;
		for(int ki = 0 ; ki<sshKeyPairs.size() ; ki++){
			if(keyPairId.equals(sshKeyPairs.get(ki).SSHKeyPairId)){
				kp = sshKeyPairs.get(ki);
				return kp;
			}
		}
		return null;
		
	}

}
