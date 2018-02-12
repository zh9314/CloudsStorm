package topology.dataStructure.EGI;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import topology.description.actual.SubTopology;

public class EGISubTopology extends SubTopology{
	
private static final Logger logger = Logger.getLogger(EGISubTopology.class);
	
	//Indicate different VMs.
	public ArrayList<EGIVM> VMs;
	

	
	@Override
	public Map<String, String> generateUserOutput() {
		Map<String, String> output = new HashMap<String, String>();
		ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    	try {
    		String yamlString = mapper.writeValueAsString(this);
			String content = "";
        	String [] lines = yamlString.split("\\\n");
        	for(int i = 0 ; i<lines.length ; i++){
        		if(lines[i].trim().contains("RES_OCCI_ID")
            			|| lines[i].trim().contains("OS_OCCI_ID")
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

	@Override
	public boolean formatChecking(String topologyStatus) {
		
		for(int vmi = 0 ; vmi < this.VMs.size() ; vmi++){
			EGIVM curVM = VMs.get(vmi);
			if(topologyStatus.equals("fresh")){
				if(curVM.VMResourceID != null){
					logger.error("Some information in VM '"+curVM.name+"' cannot be defined in a 'fresh' sub-topology!");
					return false;
				}
			}
			
			if(topologyStatus.equals("running")){
				if(curVM.VMResourceID == null){
					logger.error("Some information in VM '"+curVM.name+"' must be defined in a 'running' sub-topology!");
					return false;
				}
			}
		}
		return true;
	}

	@Override
	public boolean outputControlInfo(String filePath) {
		// TODO Auto-generated method stub
		return false;
	}

}
