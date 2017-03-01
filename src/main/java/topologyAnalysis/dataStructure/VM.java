package topologyAnalysis.dataStructure;


import java.util.ArrayList;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonIgnore;

import commonTool.CommonTool;

public abstract class VM {
	
	private static final Logger logger = Logger.getLogger(VM.class);
	
	public String name;
	public String type;
	public String nodeType;
	public String OStype;
	
	//Currently, the SIDE subsystem uses this field for GUI.
	public String script;
	
	/**
	 * Indicate the real content of the script.
	 * Examples: url@http://www.mydomain.com/pathToFile/myId_dsa 
	 * or file@/home/id_dsa or null (the file path is absolute path).
	 * This is not case sensitive.
	 * The file must be exists. Otherwise, the topology will not be loaded. 
	 */
	@JsonIgnore
	public String scriptString;
	
	
	//The role of this node in docker cluster. 
	//The possible value can only be "null", "slave" and "master". 
	//This is not case sensitive. 
	public String role;
	
	//The name of the docker in repository, which can be "null". 
	public String dockers;
	
	//Do not need to be the same with the node name any more.
	//The initial value should be "null", which means the public is not determined. 
	//When the status of the sub-topology is stopped, deleted or failed, this field should also be "null".
	public String publicAddress;
	
	public ArrayList<Eth> ethernetPort;
	
	/**
	 * Load script content from input parameter string for current VM.
	 * If it is null, then load from the field 'script'.
	 */
	public boolean loadScript(String scriptContent){
		if(scriptContent == null){
			if(script == null){
				logger.error("Please configure the script path first!");
				return false;
			}
			
			if((scriptContent = CommonTool.getFileContent(script)) == null){
				logger.error("File of script cannot be loaded!");
				return false;
			}
			
			return true;
		}else{
			scriptString = scriptContent;
			return true;
		}
	}
	
}
