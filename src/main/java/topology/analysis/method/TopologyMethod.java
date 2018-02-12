package topology.analysis.method;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

public interface TopologyMethod {

	
	/**
	 * Generate the output yaml string to respond user.
	 * @return A key-value pair. 
	 *         For top level description, the key is always "_top".
	 *         The value is the content of the description.
	 *         For low level description, the key is the sub-topology name, with a prefix of '_sub'.
	 *         The prefix makes the key different from the topLevel key in any way.
	 *         The value is the content of the detailed description.
	 */
	public Map<String, String> generateUserOutput();
	
	
	/**
	 * Generate the output yaml String containing all the information.
	 * The generated String is used for controlling. 
	 * @return A key-value pair. 
	 *         For top level description, the key is always "_top".
	 *         The value is the content of all the information.
	 *         For low level description, the key is the sub-topology name, with a prefix of '_sub'.
	 *         The prefix makes the key different from the topLevel key in any way.
	 *         The value is the content of all the information.
	 */
	public Map<String, String> generateControlOutput();
	
	
	/**
	 * Generate the output yaml file containing all the information.
	 * The generated file is used for controlling. 
	 * The content will be written directly back to the original file.
	 */
	public boolean overwirteControlOutput();
	
	
	public boolean outputControlInfo(String filePath);
	
	
	/**
	 * This is a format checking function for loaded topology. <br/>
	 * If the topology is the top-topology, the input status does not matter.
	 * The format requirements is: <br/> <br/>
	 * 1. The field 'publicKeyPath' will not be checked here. Because it will be checked
	 * before real interaction with the Cloud. When user uploading
	 * the files, this field can be set as 'null' at first. Then the public key content must be loaded by 
	 * {@link topology.analysis.method.TopTopologyMethod#loadPublicKey(String) TopTopologyMethod.loadPublicKey(String)} 
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
	 * 14. Two connection points of a connection must come from different sub-topology. <br/>
	 * 15. Two connection points cannot come from two sub-topologies with the same 'scaling' tag. <br/>
	 * 16. Topology name cannot contain '.' 
	 * 17. The folder of 'clusterKeyPair' must exist, if there is a running sub-topology.
	 *
	 * For sub-topologies:
	 * There are some common checking items for all the sub-topologies. See:<br/>
	 * {@link topology.description.actual.SubTopology#commonFormatChecking(String) SubTopology.commonFormatChecking()}<br/>
	 * Current format requirements for different cloud providers are list as below: <br/>
	 * {@link topology.dataStructure.EC2.EC2SubTopology#formatChecking(String) EC2} <br/>
	 * {@link topology.dataStructure.ExoGENI.ExoGENISubTopology#formatChecking(String) ExoGENI}
	 *
	 * @param topologyStatus
	 * @return
	 */
	public boolean formatChecking(String topologyStatus);
}
