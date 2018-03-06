package topology.dataStructure.ExoGENI;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import topology.description.actual.SubTopology;

public class ExoGENISubTopology extends SubTopology{
	
	private static final Logger logger = Logger.getLogger(ExoGENISubTopology.class);
	
	//Indicate different VMs.
	public ArrayList<ExoGENIVM> VMs;
		
	//Used for control the topology on ExoGENI.
	public String sliceName;
	
	// A positive integer to indicate the life duration for the SubTopology. 
	// This is only valid for ExoGENI VMs. 
	// Currently, the unit is day and duration is between 1 and 13.
	public String duration;
	


	@Override
	public Map<String, String> generateUserOutput() {
		Map<String, String> output = new HashMap<String, String>();
		ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    	try {
    		String yamlString = mapper.writeValueAsString(this);
			String content = "";
        	String [] lines = yamlString.split("\\\n");
        	for(int i = 0 ; i<lines.length ; i++){
        		if(lines[i].trim().contains("OS_URL")
        			|| lines[i].trim().contains("OS_GUID")
        			|| lines[i].trim().contains("LocalEntry")
        			|| lines[i].trim().contains("extraInfo"))
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

	
	/**
	 * Specific checking for ExoGENI sub-topology.
	 * The checking items includes: <br/>
	 * 1. The 'sliceName' should be null, 
	 * if the topology status is 'fresh'. <br/>
	 * 2. The value of 'diskSize' must be positive. <br/>
	 * 3. Validate the value of 'duration', whose unit is day. It must be between 1-19 (including).
	 * The default value is 1. <br/>
	 * 
	 */
	@Override
	public boolean formatChecking(String topologyStatus) {
		
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



}
