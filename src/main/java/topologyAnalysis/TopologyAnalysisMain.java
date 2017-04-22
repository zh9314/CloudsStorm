package topologyAnalysis;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import commonTool.CommonTool;
import topologyAnalysis.dataStructure.SubTopology;
import topologyAnalysis.dataStructure.SubTopologyInfo;
import topologyAnalysis.dataStructure.TopConnectionPoint;
import topologyAnalysis.dataStructure.TopTopology;
import topologyAnalysis.dataStructure.VM;
import topologyAnalysis.method.SubTopologyMethod;

public class TopologyAnalysisMain{
	
	private static final Logger logger = Logger.getLogger(TopologyAnalysisMain.class);
	
	public TopTopology wholeTopology;
	
	// The absolute file path of the top topology.
	private String topTopologyFilePath;
	
	public TopologyAnalysisMain(String topTopologyPath){
		topTopologyFilePath = topTopologyPath;
	}
	
	/**
	 * True indicates the topology is loaded successfully.  <br/> 
	 * All the files should be in the same directory and ending up with '.yml'  <br/> 
	 * This method is only used for generate the user output.  
	 */
	public boolean simpleLoadWholeTopology(){
		wholeTopology = new TopTopology();
		if(!wholeTopology.loadTopTopology(topTopologyFilePath))
			return false;
		
		String topologiesDir = CommonTool.getPathDir(topTopologyFilePath);
		for(int i = 0 ; i<wholeTopology.topologies.size() ; i++){
			SubTopologyInfo tmpInfo = wholeTopology.topologies.get(i);
			String cp = tmpInfo.cloudProvider.trim().toLowerCase();
			String topologyPath = topologiesDir + tmpInfo.topology + ".yml";
			String packagePrefix = "topologyAnalysis.dataStructure";
			String className = "";
			if(cp.equals("ec2"))
				className = packagePrefix+".EC2.EC2SubTopology";
			else if(cp.equals("exogeni"))
				className = packagePrefix+".ExoGENI.ExoGENISubTopology";
			else if(cp.equals("egi"))
				className = packagePrefix+".EGI.EGISubTopology";
			else{
				logger.error("Cloud provider of "+cp+" has not been supported yet!");
				return false;
			}
			try {
				Object subtopology = Class.forName(className).newInstance();
				if(!((SubTopologyMethod)subtopology).loadSubTopology(topologyPath))
					return false;
				((SubTopologyMethod)subtopology).setTopologyInformation(tmpInfo.topology);
				tmpInfo.subTopology = ((SubTopology)subtopology);
			} catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
				e.printStackTrace();
				logger.error("Cannot load the subToplogy class for "+cp+": "+e.getMessage());
				return false;
			}
				
			/*if(cp.equals("ec2")){
				EC2SubTopology ec2SubTopology = new EC2SubTopology();
				if(!ec2SubTopology.loadSubTopology(topologyPath))
					return false;
				ec2SubTopology.setTopologyInformation(tmpInfo.topology);
				tmpInfo.subTopology = ec2SubTopology;
				
			}else if(cp.equals("exogeni") || cp.equals("geni")){
				ExoGENISubTopology exoGENISubTopology = new ExoGENISubTopology();
				if(!exoGENISubTopology.loadSubTopology(topologyPath))
					return false;
				exoGENISubTopology.setTopologyInformation(tmpInfo.topology);
				tmpInfo.subTopology = exoGENISubTopology;
			}else{
				logger.error("Cloud provider of "+cp+" has not been supported yet!");
				return false;
			}*/
		}
		
		return true;
	}
	
	
	/**
	 * True indicates the topology is loaded successfully.  <br/> 
	 * All the files should be in the same directory and ending up with '.yml'  <br/> 
	 * After loading from the topologies, the full information of the 
	 * topology will be completed and the format will be checked.
	 * This method is used for doing real interacting with cloud and 
	 * generating control output.
	 */
	public boolean fullLoadWholeTopology(){
		if(!simpleLoadWholeTopology())
			return false;
		
		if(!completeFullInfo())
			return false;
		
		if(!formatChecking())
			return false;
		
		return true;
	}
	
	/**
	 * This method is used for completing all the information 
	 * after simple loading a topology. 
	 * 
	 */
	private boolean completeFullInfo() {
		//Complete the info of connectors for every sub-topology
		if(wholeTopology.connections != null){
			for(int i = 0 ; i<wholeTopology.connections.size() ; i++){
				TopConnectionPoint sourceTcp = wholeTopology.connections.get(i).source;
				TopConnectionPoint targetTcp = wholeTopology.connections.get(i).target;
				
				//Operating on the source connection point.
				if(!sourceTcp.componentName.contains(".")){
					logger.error("The source name of connection "+sourceTcp.componentName+" is not valid!");
					return false;
				}
				String [] t_VM = sourceTcp.componentName.split("\\.");
				String VMName = t_VM[1]; String sourceTopologyName = t_VM[0];
				SubTopologyInfo sti = getSubTopologyInfo(sourceTopologyName);
				if(sti == null){
					logger.error("The sub-topology of connection point "+sourceTcp.componentName+" doesn't exist!");
					return false;
				}
				String sourceSubTopologyTag = sti.tag.trim().toLowerCase();
				
				//Get the VM in the sub-topology
				VM vmInfo = sti.subTopology.getVMinSubClassbyName(VMName);
				if(vmInfo == null){
					logger.error("There is no VM called "+VMName+" in "+sourceTopologyName);
					return false;
				}
				sourceTcp.belongingVM = vmInfo;
				sourceTcp.peerTCP = targetTcp;
				if(sti.connectors == null)
					sti.connectors = new ArrayList<TopConnectionPoint>();
				sti.connectors.add(sourceTcp);
				
				//Operating on the target connection point.
				if(!targetTcp.componentName.contains(".")){
					logger.error("The target name of connection "+targetTcp.componentName+" is not valid!");
					return false;
				}
				t_VM = targetTcp.componentName.split("\\.");
				VMName = t_VM[1]; String targetTopologyName = t_VM[0];
				sti = getSubTopologyInfo(targetTopologyName);
				if(sti == null){
					logger.error("The sub-topology of connection point "+targetTcp.componentName+" doesn't exist!");
					return false;
				}
				String targetSubTopologyTag = sti.tag.trim().toLowerCase();
				
				//Get the VM in the sub-topology
				vmInfo = sti.subTopology.getVMinSubClassbyName(VMName);
				if(vmInfo == null){
					logger.error("There is no VM called "+VMName+" in "+targetTopologyName);
					return false;
				}
				targetTcp.belongingVM = vmInfo;
				targetTcp.peerTCP = sourceTcp;
				if(sti.connectors == null)
					sti.connectors = new ArrayList<TopConnectionPoint>();
				sti.connectors.add(targetTcp);
				
				if(targetTopologyName.equals(sourceTopologyName)){
					logger.error("The two connection points of '"+wholeTopology.connections.get(i).name+"' must come from two different topologies!");
					return false;
				}
				
				///The two connected sub-topologies at least one tag of 'fixed'
				if(!sourceSubTopologyTag.equals("fixed")
					&& !targetSubTopologyTag.equals("fixed")){
					logger.error("One of the two connected sub-topologies '"+sourceTopologyName+"' and '"+targetTopologyName+"' must have the tag of 'fixed'!");
					return false;
				}
			}
		}
		
		//complete the info of the father sub-topology, if that is a scaled one.
		//This is used for generating the final scaling address pool.
		if(wholeTopology.topologies == null){
			logger.error("At least one topology should be defined in the top level description!");
			return false;
		}
		for(int i = 0 ; i<wholeTopology.topologies.size() ; i++){
			SubTopologyInfo curInfo = wholeTopology.topologies.get(i);
			if(curInfo.tag.trim().toLowerCase().equals("scaled")){
				if(curInfo.copyOf == null){
					logger.error("The field 'copyOf' of 'scaled' sub-topology '"+curInfo.topology+"' must be set!");
					return false;
				}
				SubTopologyInfo sti ;
				if((sti = getSubTopologyInfo(curInfo.copyOf)) == null){
					logger.error("According to the field 'copyOf' of sub-topology '"+curInfo.topology+"'. There is no sub-topology called '"+curInfo.copyOf+"'!");
					return false;
				}
				//Father topology cannot be itself.
				if(sti.topology.equals(curInfo.topology)){
					logger.error("The origin topology of 'scaled' sub-topology '"+curInfo.topology+"' cannot be itself!");
					return false;
				}
				//Father topology must have tag of 'scaling'.
				if(!sti.tag.equals("scaling")){
					logger.error("The father topology of 'scaled' sub-topology '"+curInfo.topology+"' must have the 'tag' of 'scaling'!");
					return false;
				}
				curInfo.fatherTopology = sti;
			}
		}
		
		return true;
	}
	
	private SubTopologyInfo getSubTopologyInfo(String topologyName){
		for(int i = 0 ; i<wholeTopology.topologies.size() ; i++){
			if(wholeTopology.topologies.get(i).topology.equals(topologyName)){
				return wholeTopology.topologies.get(i);
			}
		}
		return null;
	}
	
	/**
	 * Generate the Strings for user response.
	 * @return The format of the output is a hash map. The element is a key-value pair:  <br/> 
	 * - Top level description -> key: "topLevel"	 value: description content  <br/> 
	 * - Low level description -> key: $subTopologyName  value: description content  <br/> 
	 * If some error happens, return null.
	 */
	public Map<String, String> generateUserOutputs(){
		if(wholeTopology == null){
			logger.warn("Load the topology first!");
			return null;
		}
		Map<String, String> outputs = new HashMap<String, String>();
		Map<String, String> tmpEle;
		if((tmpEle = wholeTopology.generateUserOutput()) == null)
			return null;
		
		outputs.putAll(tmpEle);
		logger.debug("The output String for user is generated!");
		
		for(int i = 0 ; i<wholeTopology.topologies.size() ; i++){
			SubTopology subTopology = wholeTopology.topologies.get(i).subTopology;
			if(subTopology == null){
				logger.error("Please load topologies first!");
				return null;
			}
			Map<String, String> curEle;
			if((curEle = subTopology.generateUserOutput()) == null)
				return null;
			outputs.putAll(curEle);
		}
		return outputs;
	}
	
	
	/**
	 * Generate the Strings for controlling usage.  <br/> 
	 * @return The format of the output is a hash map. The element is a key-value pair:  <br/> 
	 * - Top level description -> key: "topLevel"	 value: description content  <br/> 
	 * - Low level description -> key: $subTopologyName  value: description content  <br/> 
	 * If some error happens, return null.
	 */
	public Map<String, String> generateControlOutputs(){
		if(wholeTopology == null){
			logger.warn("Load the topology first!");
			return null;
		}
		Map<String, String> outputs = new HashMap<String, String>();
		Map<String, String> tmpEle;
		if((tmpEle = wholeTopology.generateControlOutput()) == null)
			return null;
		
		outputs.putAll(tmpEle);
		logger.debug("The output String of top level topology for controlling is generated!");
		
		for(int i = 0 ; i<wholeTopology.topologies.size() ; i++){
			SubTopology subTopology = wholeTopology.topologies.get(i).subTopology;
			if(subTopology == null){
				logger.error("Please load topologies first!");
				return null;
			}
			Map<String, String> curEle;
			if((curEle = subTopology.generateControlOutput()) == null)
				return null;
			outputs.putAll(curEle);
			logger.debug("The output String of sub-topology "+subTopology.topologyName+" for controlling is generated!");
		}
		return outputs;
	}
	
	/**
	 * Generate the output yaml file containing all the information. 
	 * The generated files is used for controlling. 
	 * The content will be written directly back to the original files.
	 **/
	public boolean overwiteControlFiles(){
		if(!wholeTopology.overwirteControlOutput())
			return false;
		
		logger.debug("Controlling information for top level description is overwritten");
		
		for(int i = 0 ; i<wholeTopology.topologies.size() ; i++){
			SubTopology subTopology = wholeTopology.topologies.get(i).subTopology;
			if(subTopology == null){
				logger.error("Please load topologies first!");
				return false;
			}
			
			if(!subTopology.overwirteControlOutput())
				return false;
		}
		return true;
	}
	
	/**
	 * Check the format and validate the description files. 
	 * The detailed checking items are described as below: <br/>
	 * {@link topologyAnalysis.dataStructure.TopTopology#formatChecking() Top level description format}  <br/>
	 * {@link topologyAnalysis.dataStructure.SubTopology#formatChecking(String) Low level description format}  <br/>
	 * Tips:  <br/>
	 * - When some field is not needed, do not leave it as blank.
	 * One option is to put the value as "null". Another option is to make the name
	 * of the field not appearing in the description. <br/>
	 * - All of the field names are case sensitive. <br/>
	 * - Some value of the fields are not case sensitive. Detailed information
	 * can be found through links above. Only the case sensitive ones are specified explicitly.
	 */
	private boolean formatChecking(){
		if(!wholeTopology.formatChecking())
			return false;
		
		logger.debug("Top level description is valid!");
		
		for(int si = 0 ; si<wholeTopology.topologies.size() ; si++){
			String status = wholeTopology.topologies.get(si).status.trim().toLowerCase();
			if(!wholeTopology.topologies.get(si).subTopology.formatChecking(status))
				return false;
			logger.debug("The description of sub-topology '"+wholeTopology.topologies.get(si).topology+"' is valid!");
		}
		
		return true;
	}
	
}