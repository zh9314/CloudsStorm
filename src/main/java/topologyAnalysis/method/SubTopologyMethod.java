package topologyAnalysis.method;

import java.util.ArrayList;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;

import topologyAnalysis.dataStructure.VM;


public interface SubTopologyMethod {
	
	/**
	 * This method is used for loading the top level infrastructure description from file to the object itself.
	 * @param Input is the file path of the sub-topology description.
	 * @return true meaning topTopology is loaded successfully.
	 */
	public boolean loadSubTopology(String topologyPath);
	
	
	public void setTopologyInformation(String topologyName);
	
	
	/**
	 * This is used for getting a specified VM through the super class SubTopology. 
	 */
	public VM getVMinSubClassbyName(String vmName);
	
	
	/**
	 * This is used for getting all the VMs through the super class SubTopology. 
	 */
	@JsonIgnore
	public ArrayList<VM> getVMsinSubClass();
	
	
	/**
	 * Generate the output yaml strings to respond user.
	 * @return A key-value pair. 
	 *         For low level description, the key is the sub-topology name, with a prefix of '_'.
	 *         The prefix makes the key different from the topLevel key in any way.
	 *         The value is the content of the detailed description.
	 */
	public Map<String, String> generateUserOutput();
	
	
	/**
	 * Generate the output yaml String containing all the information.
	 * The generated String is used for controlling. 
	 * @return A key-value pair.
	 *         For low level description, the key is the sub-topology name, with a prefix of '_'.
	 *         The prefix makes the key different from the topLevel key in any way.
	 *         The value is the content of all the information.
	 */
	public Map<String, String> generateControlOutput();
	
	
	/**
	 * Generate the output yaml file containing all the information. 
	 * The generated file is used for controlling. 
	 * The content will be written directly back to the original files.
	 */
	public boolean overwirteControlOutput();
	
	
	/**
	 * There are some common checking items for all the sub-topologies. See:<br/>
	 * {@link topologyAnalysis.dataStructure.SubTopology#commonFormatChecking(String) SubTopology.commonFormatChecking()}<br/>
	 * Current format requirements for different cloud providers are list as below: <br/>
	 * {@link topologyAnalysis.dataStructure.EC2.EC2SubTopology#formatChecking(String) EC2} <br/>
	 * {@link topologyAnalysis.dataStructure.ExoGENI.ExoGENISubTopology#formatChecking(String) ExoGENI}
	 * 
	 * 
	 */
	public boolean formatChecking(String topologyStatus);
	
}
