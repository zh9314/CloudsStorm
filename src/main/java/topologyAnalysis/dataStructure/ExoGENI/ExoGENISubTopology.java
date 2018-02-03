package topologyAnalysis.dataStructure.ExoGENI;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import topologyAnalysis.dataStructure.SubTopology;
import topologyAnalysis.dataStructure.VM;
import topologyAnalysis.method.SubTopologyMethod;

public class ExoGENISubTopology extends SubTopology implements SubTopologyMethod{
	
	private static final Logger logger = Logger.getLogger(ExoGENISubTopology.class);
	
	//Used for control the topology on ExoGENI.
	public String sliceName;
	
	// A positive integer to indicate the life duration for the SubTopology. 
	// This is only valid for ExoGENI VMs. 
	// Currently, the unit is day and duration is between 1 and 13.
	public String duration;
	
	//Indicate different VMs.
	public ArrayList<ExoGENIVM> VMs;

	@Override
	public boolean loadSubTopology(String topologyPath) {
		ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        try {
        	ExoGENISubTopology exoGENISubTopology = mapper.readValue(new File(topologyPath), ExoGENISubTopology.class);
        	if(exoGENISubTopology == null){
        		logger.error("Sub-topology from "+topologyPath+" is invalid!");
            	return false;
        	}
        	this.loadingPath = topologyPath;
        	this.subnets = exoGENISubTopology.subnets;
        	this.connections = exoGENISubTopology.connections;
        	this.VMs = exoGENISubTopology.VMs;
        	this.duration = exoGENISubTopology.duration;
        	this.sliceName = exoGENISubTopology.sliceName;
        	logger.info("Sub-topology of ExoGENI from "+topologyPath+" is loaded without validation successfully!");
        } catch (Exception e) {
            logger.error(e.toString());
            e.printStackTrace();
            return false;
        }
		return true;
	}

	@Override
	public Map<String, String> generateUserOutput() {
		Map<String, String> output = new HashMap<String, String>();
		ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    	try {
    		String yamlString = mapper.writeValueAsString(this);
			String content = "";
        	String [] lines = yamlString.split("\\\n");
        	for(int i = 0 ; i<lines.length ; i++){
        		if(lines[i].trim().equals("subnetName: null")
        			|| lines[i].trim().equals("address: null")
        			|| lines[i].trim().equals("connectionName: null"))
        			continue;
        		if(lines[i].contains(":")){
					String [] contents = lines[i].split(":");
					if(contents[0].trim().equals("sliceName"))
						continue;
				}
				content += (lines[i]+"\n"); 
        	}
        	output.put("_"+this.topologyName, content);
		} catch (IOException e) {
			e.printStackTrace();
			logger.error(e.getMessage());
			return null;
		}
		return output;
	}

	@Override
	public Map<String, String> generateControlOutput() {
		Map<String, String> output = new HashMap<String, String>();
		ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    	try {
    		String yamlString = mapper.writeValueAsString(this);
			String content = "";
        	String [] lines = yamlString.split("\\\n");
        	for(int i = 0 ; i<lines.length ; i++){
        		if(lines[i].trim().equals("subnetName: null")
        			|| lines[i].trim().equals("address: null")
        			|| lines[i].trim().equals("connectionName: null"))
        			continue;
				content += (lines[i]+"\n"); 
        	}
        	output.put("_"+this.topologyName, content);
		} catch (IOException e) {
			e.printStackTrace();
			logger.error(e.getMessage());
			return null;
		}
		return output;
	}

	@Override
	public boolean overwirteControlOutput() {
		ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    	try {
    		FileWriter yamlFileOut = new FileWriter(this.loadingPath, false);
    		String yamlString = mapper.writeValueAsString(this);
			String content = "";
        	String [] lines = yamlString.split("\\\n");
        	for(int i = 0 ; i<lines.length ; i++){
        		if(lines[i].trim().equals("subnetName: null")
        			|| lines[i].trim().equals("address: null")
        			|| lines[i].trim().equals("connectionName: null"))
        			continue;
				content += (lines[i]+"\n"); 
        	}
        	yamlFileOut.write(content);
        	yamlFileOut.close();
        	
		} catch (IOException e) {
			e.printStackTrace();
			logger.error(e.getMessage());
			return false;
		}
    	return true;
	}

	@Override
	public void setTopologyInformation(String topologyName) {
		this.topologyName = topologyName;
		this.topologyType = "ExoGENI";
		//this.provisioningAgentClassName = "";
		
	}

	
	/**
	 * Specific checking for ExoGENI sub-topology.
	 * The checking items includes: <br/>
	 * 1. The 'sliceName' should be null, 
	 * if the topology status is 'fresh'. <br/>
	 * 2. Currently do not support describe a subnet on ExoGENI. <br/>
	 * 3. The value of 'diskSize' must be positive. <br/>
	 * 4. Validate the value of 'duration', whose unit is day. It must be between 1-19 (including).
	 * The default value is 1. <br/>
	 * 
	 */
	@Override
	public boolean formatChecking(String topologyStatus) {
		if(!super.commonFormatChecking(topologyStatus))
			return false;
		
		if(this.subnets != null){
			logger.error("Currently the 'subnet' definition of ExoGENI is not supported yet!");
			return false;
		}
		
		if(topologyStatus.equals("fresh") && sliceName != null){
			logger.error("Field 'sliceName' in a 'fresh' sub-topology cannot be specified!");
			return false;
		}
		
		if(this.duration == null){
			this.duration = "1";
		}else{
			try {
				int durationNum = Integer.parseInt(this.duration);
				if(durationNum<=0 || durationNum >19){
					logger.error("Field 'duration' of ExoGENI sub-topology '"+this.topologyName+"' must be between 1-19 (including)!");
					return false;
				}
				} catch (NumberFormatException e) {
					logger.error("Field 'duration' of ExoGENI sub-topology '"+this.topologyName+"' must be an integer and between 1-19 (including)!");
					return false;
				}
		}
		
		return true;
	}

	@Override
	public VM getVMinSubClassbyName(String vmName) {
		for(int i = 0 ; i<VMs.size() ; i++){
			if(VMs.get(i).name.equals(vmName)){
				return VMs.get(i);
			}
		}
		return null;
	}

	@Override
	public ArrayList<VM> getVMsinSubClass() {
		if(VMs.size() == 0)
			return null;
		ArrayList<VM> vms = new ArrayList<VM>();
		for(int i = 0 ; i<VMs.size() ; i++)
			vms.add(VMs.get(i));
		return vms;
	}


}
