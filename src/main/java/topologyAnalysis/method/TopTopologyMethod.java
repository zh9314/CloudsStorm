package topologyAnalysis.method;

import java.util.Map;


public interface TopTopologyMethod {
	
	/**
	 * This method is used for loading the top level infrastructure description 
	 * from file to the object itself.
	 * @param Input is the file path of the topology description.
	 * @return true meaning topTopology is loaded successfully.
	 */
	public boolean loadTopTopology(String topologyPath);
	
	
	
	/**
	 * Generate the output yaml string to respond user.
	 * @return A key-value pair. 
	 *         For top level description, the key is always "topLevel".
	 *         The value is the content of the description.
	 */
	public Map<String, String> generateUserOutput();
	
	
	/**
	 * Generate the output yaml String containing all the information.
	 * The generated String is used for controlling. 
	 * @return A key-value pair. 
	 *         For top level description, the key is always "topLevel".
	 *         The value is the content of all the information.
	 */
	public Map<String, String> generateControlOutput();
	
	
	/**
	 * Generate the output yaml file containing all the information.
	 * The generated file is used for controlling. 
	 * The content will be written directly back to the original file.
	 */
	public boolean overwirteControlOutput();
	
	
	/**
	 * Load public key content from the input pbContent.
	 * If the input is null, then load from the value of field 'publicKeyPath'.
	 * @return successful or not.
	 */
	public boolean loadPublicKey(String pbContent);
	
	/**
	 * This is a format checking function for top level topology. <br/>
	 * The format requirements is: <br/> <br/>
	 * 1. The field 'publicKeyPath' will not be checked here. Because it will be checked
	 * before real interaction with the Cloud. When user uploading
	 * the files, this field can be set as 'null' at first. Then the public key content must be loaded by 
	 * {@link topologyAnalysis.method.TopTopologyMethod#loadPublicKey(String) TopTopologyMethod.loadPublicKey(String)} 
	 * before executing the topology. This step is necessary, if the field 'userName' is not 'null'. <br/>
	 * 2. Field 'userName' can be set 'null', if the user does not want to access all 
	 * the VMs with his specified account. <br/>
	 * 3. Field 'cloudProvider' can be 'ec2', 'exogeni' or 'geni' currently. 
	 * It is not case sensitive. <br/>
	 * 4. Field 'domain' is not case sensitive.  <br/>
	 * 5. Field 'status' is not case sensitive. The only valid values are 'fresh', 
	 * 'running', 'failed', 'stopped' and 'deleted'. <br/>
	 * 6. Field 'tag' is not case sensitive. The only valid values are 'fixed', 'scaling' and 'scaled'. <br/>
	 * 7. Field 'address' must be private IP address.   <br/>
	 * 8. In the 'connections', field 'scalingPool' of the connection point which belongs to
	 * the sub-topology with tag 'scaling' must not be 'null'.  <br/>
	 * 9. Field 'netmask' should be valid no matter the form (number or string) of it. After loading, 
	 * all the 'netmask' will be transferred into number representation (1-31).  <br/>
	 * 10. 'scalingPool' must contains '-'. The two IP address here and value of 'address' 
	 * must be in the same subnet whose netmask is defined by field 'netmask'.  <br/>
	 * 11. All the names of 'connections' must be different with each other. <br/>
	 * 12. All the names of 'topology' must be different with each other. <br/>
	 * 13. All the addresses in the same connection should be in the same subnet 
	 * and these two addresses must be different.  <br/>
	 * 14. Two connection points of a connection must come from different sub-topology.
	 */
	public boolean formatChecking();
	

}
