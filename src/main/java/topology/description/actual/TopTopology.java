package topology.description.actual;

import java.io.File;
import java.io.FileWriter;
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
import topology.analysis.method.TopologyMethod;
import topology.dataStructure.Member;
import topology.dataStructure.Subnet;
import topology.dataStructure.Topology;

public class TopTopology extends Topology implements TopologyMethod{
	
	private static final Logger logger = Logger.getLogger(TopTopology.class);
	
	/**
	 * The content of the publicKey. 
	 * It is loaded, when loading the topology.
	 */
	@JsonIgnore
	public String publicKeyString;
	
	public ArrayList<SubTopologyInfo> topologies;
	
	public ArrayList<ActualConnection> connections;
	
	public ArrayList<Subnet> subnets;
	
	/**
	 * This is a VM hash table to index a specified VM according to the VM name.
	 */
	@JsonIgnore
	public Map<String, VM> VMIndex = new HashMap<String, VM>();
	
	
	@JsonIgnore
	public Map<String, ActualConnection> connectionIndex = new HashMap<String, ActualConnection>();
	
	@JsonIgnore
	public Map<String, SubTopologyInfo> subTopologyIndex = new HashMap<String, SubTopologyInfo>();
	
	@JsonIgnore
	public Map<String, Subnet> subnetIndex = new HashMap<String, Subnet>();
	

	
	public boolean loadTopology(String topologyPath) {
		ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        try {
        	TopTopology topTopology = mapper.readValue(new File(topologyPath), TopTopology.class);
        	if(topTopology == null){
        		logger.error("Top topology from "+topologyPath+" is invalid!");
            	return false;
        	}
        	this.publicKeyPath = topTopology.publicKeyPath;
        	this.userName = topTopology.userName;
        	this.topologies = topTopology.topologies;
        	this.connections = topTopology.connections;
        	this.subnets = topTopology.subnets;
        	this.loadingPath = topologyPath;
        	logger.info("Top topology from "+topologyPath+" is loaded without validation successfully!");
        	return true;
        } catch (Exception e) {
            logger.error(e.toString());
            e.printStackTrace();
            return false;
        }
	}
	

	@Override
	public Map<String, String> generateUserOutput() {
		ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
		Map<String, String> output = new HashMap<String, String>();
    	try {
    		String yamlString = mapper.writeValueAsString(this);
		String content = "";
        	String [] lines = yamlString.split("\\\n");
        	for(int i = 0 ; i<lines.length ; i++){
        		if(lines[i].contains("null"))
        			continue;
        		content += (lines[i]+"\n"); 
        	}
        	output.put("_top", content);
		} catch (IOException e) {
			e.printStackTrace();
			logger.error(e.getMessage());
			return null;
		}
    	return output;
	}

	@Override
	public Map<String, String> generateControlOutput() {
		ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
		Map<String, String> output = new HashMap<String, String>();
    	try {
    		String yamlString = mapper.writeValueAsString(this);
        	output.put("_top", yamlString);
		} catch (IOException e) {
			e.printStackTrace();
			logger.error(e.getMessage());
			return null;
		}
    	return output;
	}

	@Override
	public boolean overwirteControlOutput() {
		File yamlFileOut = new File(this.loadingPath);
		ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
		try {
			String yamlString = mapper.writeValueAsString(this);
			FileUtils.writeStringToFile(yamlFileOut, 
					yamlString, "UTF-8", false);
		} catch (IOException e) {
			e.printStackTrace();
			logger.error(e.getMessage());
			return false;
		}
		return true;
	}
	
	@Override
	public boolean outputControlInfo(String filePath) {
		ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    		try {
	    		FileWriter yamlFileOut = new FileWriter(filePath, false);
	    		String yamlString = mapper.writeValueAsString(this);
	        	yamlFileOut.write(yamlString);
	        	yamlFileOut.close();
	      
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
    		return true;
	}
	
	
	public SubTopologyInfo getSubtopology(String subTopologyName){
		if(this.topologies == null)
			return null;
		for(int si = 0 ; si < this.topologies.size() ; si++)
			if(this.topologies.get(si).topology.trim().equals(subTopologyName))
				return this.topologies.get(si);
		
		return null;
	}
	
	/**
	 * This is a function to check format of all the netmask to a number.
	 * This function must be invoked in the beginning of function of 'formatChecking'.
	 * @return false if there is an invalid netmask.
	 */
	private boolean netmaskFormating(){
		if(this.connections != null){
			for(int i = 0 ; i<this.connections.size() ; i++){
				String netmask = "";
				ActualConnectionPoint tcpSource = this.connections.get(i).source;
				netmask = tcpSource.netmask;
				int netmaskNum = 0;
				if(netmask == null){
					logger.error("Field 'netmask' of source connection "+this.connections.get(i).name+" must be specified!");
					return false;
				}
				if(netmask.contains(".")){
					if((netmaskNum = CommonTool.netmaskStringToInt(netmask)) == -1){
						logger.error("Field 'netmask' of source connection "+this.connections.get(i).name+" is not valid!");
						return false;
					}
				}else{
					try {
						netmaskNum = Integer.parseInt(netmask);
						if(netmaskNum<1 || netmaskNum>32){
							logger.error("Field 'netmask' of source connection "+this.connections.get(i).name+" should be between 1 and 31 (included)!");
							return false;
						}
						} catch (NumberFormatException e) {
							logger.error("Field 'netmask' of source connection "+this.connections.get(i).name+" is not valid!");
							return false;
						}
				}
				tcpSource.netmask = String.valueOf(netmaskNum);
				
				ActualConnectionPoint tcpTarget = this.connections.get(i).target;
				netmask = tcpTarget.netmask;
				if(netmask == null){
					logger.error("Field 'netmask' of target connection "+this.connections.get(i).name+" must be specified!");
					return false;
				}
				if(netmask.contains(".")){
					if((netmaskNum = CommonTool.netmaskStringToInt(netmask)) == -1){
						logger.error("Field 'netmask' of target connection "+this.connections.get(i).name+" is not valid!");
						return false;
					}
				}else{
					try {
						netmaskNum = Integer.parseInt(netmask);
						if(netmaskNum < 1 || netmaskNum > 32){
							logger.error("Field 'netmask' of target connection "+this.connections.get(i).name+" should be between 1 and 32 (included)!");
							return false;
						}
						} catch (NumberFormatException e) {
							logger.error("Field 'netmask' of target connection "+this.connections.get(i).name+" is not valid!");
							return false;
						}
				}
				tcpTarget.netmask = String.valueOf(netmaskNum);	
			}
		}
		if(this.subnets != null){
			for(int si = 0 ; si<subnets.size() ; si++){
				Subnet curSubnet = subnets.get(si);
				String netmask = curSubnet.netmask;
				int netmaskNum = -1;
				if(netmask == null){
					logger.error("Field 'netmask' of subnet "+curSubnet.name+" must be specified!");
					return false;
				}
				if(netmask.contains(".")){
					if((netmaskNum = CommonTool.netmaskStringToInt(netmask)) == -1){
						logger.error("Field 'netmask' of subnet "+curSubnet.name+" is not valid!");
						return false;
					}
				}else{
					try {
						netmaskNum = Integer.parseInt(netmask);
						if(netmaskNum < 1 || netmaskNum > 32){
							logger.error("Field 'netmask' of subnet "+curSubnet.name+" should be between 1 and 32 (included)!");
							return false;
						}
					} catch (NumberFormatException e) {
						logger.error("Field 'netmask' of subnet "+curSubnet.name+" is not valid!");
						return false;
					}
				}
				curSubnet.netmask = String.valueOf(netmaskNum);
			}
		}
		return true;
	}
	
	private boolean addressChecking(){
		if(connections != null){
			for(int i = 0 ; i<this.connections.size() ; i++){
				String networkAddress = this.connections.get(i).source.address;
				if(networkAddress == null){
					logger.error("The filed 'address' of source connection "+this.connections.get(i).name+" cannot be null!");
					return false;
				}
				if(!CommonTool.checkPrivateIPaddress(networkAddress)){
					logger.error("The filed 'address' (value='"+networkAddress+"') of source connection "+this.connections.get(i).name+" should be private network address!");
					return false;
				}
				
				networkAddress = this.connections.get(i).target.address;
				if(networkAddress == null){
					logger.error("The filed 'address' of target connection "+this.connections.get(i).name+" cannot be null!");
					return false;
				}
				if(!CommonTool.checkPrivateIPaddress(networkAddress)){
					logger.error("The filed 'address' (value='"+networkAddress+"') of target connection "+this.connections.get(i).name+" should be private network address!");
					return false;
				}
			}
		}
		
		if(subnets != null){
			for(int si = 0 ; si < subnets.size() ; si++){
				Subnet curSubnet = subnets.get(si);
				int netmaskNum = Integer.valueOf(curSubnet.netmask);
				if(curSubnet.subnet == null){
					logger.error("The field 'subnet' must be set in subnet "+curSubnet.name);
					return false;
				}
				String genSubnet = CommonTool.getSubnet(curSubnet.subnet, netmaskNum);
				if(!curSubnet.subnet.equals(genSubnet)){
					logger.error("'subnet' "+curSubnet.subnet+" and 'netmask' "+curSubnet.netmask
							+" does not match in Subnet "+curSubnet.name+"!");
					return false;
				}
				if(curSubnet.members == null)
					curSubnet.members = new ArrayList<Member>();
				for(int mi = 0 ; mi<curSubnet.members.size() ; mi++){
					Member curMember = curSubnet.members.get(mi);
					String networkAddress = curMember.address;
					if(networkAddress == null){
						logger.error("The filed 'address' in subnet "+curSubnet.name+" cannot be null!");
						return false;
					}
					if(!CommonTool.checkPrivateIPaddress(networkAddress)){
						logger.error("The filed 'address' (value='"+networkAddress+"') in subnet "+curSubnet.name+" should be private network address!");
						return false;
					}
					genSubnet = CommonTool.getSubnet(networkAddress, netmaskNum);
					if(!curSubnet.subnet.equals(genSubnet)){
						logger.error("'address' (value='"+networkAddress+"')  of member "+curMember.vmName+" in subnet "
								+curSubnet.name+" is not in Subnet "+curSubnet.subnet+"!");
						return false;
					}
				}
				
			}
		}
		return true;
	}
	
	private boolean checkConnection(ActualConnection testCon){
		for(Map.Entry<String, ActualConnection> entry : this.connectionIndex.entrySet()){
			ActualConnection curCon = entry.getValue();
			if(curCon.source.vmName.equals(testCon.source.vmName)
				&& curCon.target.vmName.equals(testCon.target.vmName)
				&& curCon.source.address.equals(testCon.source.address)
				&& curCon.target.address.equals(testCon.target.address))
				return false;
			if(curCon.source.vmName.equals(testCon.target.vmName)
					&& curCon.target.vmName.equals(testCon.source.vmName)
					&& curCon.source.address.equals(testCon.target.address)
					&& curCon.target.address.equals(testCon.source.address))
				return false;
		}
		return true;
	}
	
	private boolean checkSubnet(Subnet testSubnet){
		for(Map.Entry<String, Subnet> entry: this.subnetIndex.entrySet()){
			Subnet curSubnet = entry.getValue();
			if(curSubnet.subnet.equals(testSubnet.subnet)
				&& curSubnet.netmask.equals(testSubnet.netmask))
				return false;
		}
		return true;
	}

	@Override
	public boolean formatChecking(String topologyStatus) {
		//First, format all the netmask into number and check whether they are private network address.
		if(!netmaskFormating())
			return false;
		if(!addressChecking())
			return false;
				
		//Checking the existing of the 'publicKeyPath', if the 'userName' is set.
		if(this.userName != null && !this.userName.equals("")){
			String currentDir = CommonTool.getPathDir(this.loadingPath);
			if(!this.loadPublicKey(currentDir)){
				logger.error("The public key of the corresponding user "+userName+" cannot be loaded! EXIT!");
				return false;
			}else{
				logger.info("Public key "+this.publicKeyPath+" is loaded!");
			}
		}
		
		//Checking the names of different sub-topologies.
		if(topologies == null){
			logger.error("At least one topology should be defined in the top level description!");
			return false;
		}
		
		boolean runningST = false;
		for(int i = 0 ; i<topologies.size() ; i++){
			//Update the subTopologyInfo after checking the userName and publicKeyString
			SubTopologyInfo curInfo = topologies.get(i);
			curInfo.userName = this.userName;
			curInfo.publicKeyString = this.publicKeyString;
			
			////The folder of 'clusterKeyPair' must exist, 
			////if there is a running sub-topology.
			if(curInfo.status.equals("running")){
				runningST = true;
				String currentDir = CommonTool.getPathDir(this.loadingPath);
				String clusterKeyDir = currentDir+"clusterKeyPair"+File.separator;
				File keyDir = new File(clusterKeyDir);
				if(!keyDir.exists()){
					logger.error("The folder of 'clusterKeyPair' must exist, because there is a running sub-topology '"+curInfo.topology+"'!");
					return false;
				}
			}
			
			
			String tn = topologies.get(i).topology;
			if(tn == null){
				logger.error("The sub-topology name must be specified and cannot be set as 'null'!");
				return false;
			}
			if(tn.contains(".")){
				logger.error("Invaild topology of "+tn+"! It cannot contain '.'!");
				return false;
			}
			if(subTopologyIndex.containsKey(tn)){
				logger.error("There are two same topology name '"+tn+"' in top level description.");
				return false;
			}else
				subTopologyIndex.put(tn, curInfo);
		}
		///When there is no running sub-topology, check and generate the cluster key pair.
		if(!runningST){
			String currentDir = CommonTool.getPathDir(this.loadingPath);
			String clusterKeyDir = currentDir+"clusterKeyPair"+File.separator;
			File keyDir = new File(clusterKeyDir);
			if(!keyDir.exists()){
				logger.info("There is no cluster key pair for this top-topology! Generating!");
				if(!CommonTool.rsaKeyGenerate(clusterKeyDir))
					return false;
			}else
				logger.info("The cluster key pair for this top-topology has already exist!");
		}
		
		//Checking the connection name
		if(connections != null){
			for(int i = 0 ; i<connections.size() ; i++){
				ActualConnection curCon = connections.get(i);
				String cn = curCon.name;
				if(cn == null){
					logger.error("The connection name must be specified and cannot be set as 'null'!");
					return false;
				}
				if(connectionIndex.containsKey(cn)){
					logger.error("There are two same connection name '"+cn+"' in top level description.");
					return false;
				}else{
					if(!checkConnection(curCon)){
						logger.error("There are two same connections of "+curCon.name+" in this description!");
						return false;
					}
					connectionIndex.put(cn, curCon);
				}
				
				//Checking: all the addresses in the same connection should be in the same subnet.
				//And these two addresses must be different. 
				ActualConnectionPoint tcpSource = curCon.source;
				ActualConnectionPoint tcpTarget = curCon.target;
				if(tcpSource.address.equals(tcpTarget.address)){
					logger.error("Two connection points in connection "+connections.get(i).name+" must have different address!" );
					return false;
				}
				String sourceSubnet = CommonTool.getSubnet(tcpSource.address, Integer.valueOf(tcpSource.netmask));
				String targetSubnet = CommonTool.getSubnet(tcpTarget.address, Integer.valueOf(tcpTarget.netmask));
				if(!sourceSubnet.equals(targetSubnet)){
					logger.error("Two connection points in connection "+connections.get(i).name+" should be in the same subnet!" );
					return false;
				}
				
				//check the tunnelName
				String [] t_VM = tcpSource.vmName.split("\\.");
				if(t_VM[0].trim().equals("") || t_VM.length != 2){
					logger.error("The format of connection point "+tcpSource.vmName+" is not correct!");
					return false;
				}
				String sourceTopologyName = t_VM[0];
				SubTopologyInfo curInfo = subTopologyIndex.get(sourceTopologyName);
				if(curInfo == null){
					logger.error("The sub-topology of connection point "+tcpSource.vmName+" doesn't exist!");
					return false;
				}
				if(curInfo.status.equals("fresh") || curInfo.status.equals("stopped")
						|| curInfo.status.equals("deleted") || curInfo.status.equals("failed")){
						if(tcpSource.ethName != null){
							logger.error("The '"+curInfo.status+"' sub-topology '"+curInfo.topology+"' cannot have a valid ethName!");
							return false;
						}
				}
				
				t_VM = tcpTarget.vmName.split("\\.");
				if(t_VM[0].trim().equals("") || t_VM.length != 2){
					logger.error("The format of connection point "+tcpTarget.vmName+" is not correct!");
					return false;
				}
				String targetTopologyName = t_VM[0];
				curInfo = subTopologyIndex.get(targetTopologyName);
				if(curInfo == null){
					logger.error("The sub-topology of connection point "+tcpTarget.vmName+" doesn't exist!");
					return false;
				}
				if(curInfo.status.equals("fresh") || curInfo.status.equals("stopped")
						|| curInfo.status.equals("deleted") || curInfo.status.equals("failed")){
						if(tcpTarget.ethName != null){
							logger.error("The '"+curInfo.status+"' sub-topology '"+curInfo.topology+"' cannot have a valid ethName!");
							return false;
						}
				}
				
				//Checking whether the value for field 'logic' is correct: null, 'true' or 'false'
				if(curCon.logic != null){
					if(!curCon.logic.trim().equalsIgnoreCase("true")
							&& !curCon.logic.trim().equalsIgnoreCase("false")){
						logger.error("The value of field 'logic' must be 'true' or 'false', instead of "
							+curCon.logic);
					}
				}else
					curCon.logic = "true";
			}
		}
		
		///checking the subnet, the address checking work has been done in the previous function addressChecking()
		if(subnets != null){
			for(int si = 0 ; si < subnets.size() ; si++){
				Subnet curSubnet = subnets.get(si);
				if(curSubnet.name == null){
					logger.error("The subnet name must be set!");
					return false;
				}
				if(subnetIndex.containsKey(curSubnet.name)){
					logger.error("There are two same subnet name '"+curSubnet.name+"' in top level description.");
					return false;
				}else{
					if(!checkSubnet(curSubnet)){
						logger.error("There are two same subnet as "+curSubnet.name);
						return false;
					}
					subnetIndex.put(curSubnet.name, curSubnet);
				}
				
				///check the member VM names
				for(int mi = 0 ; mi<curSubnet.members.size() ; mi++){
					Member curMember = curSubnet.members.get(mi);
					String [] t_VM = curMember.vmName.split("\\.");
					if(t_VM[0].trim().equals("") || t_VM.length != 2){
						logger.error("The format of member "+curMember.vmName
								+" in subnet "+curSubnet.name+" is not correct!");
						return false;
					}
					if(subTopologyIndex.get(t_VM[0]) == null){
						logger.error("The sub-topology '"+t_VM[0]+"' does not exist!");
						return false;
					}
				}
			}
		}
		
		
		//Checking whether some values are correct.
		for(int i = 0 ; i<this.topologies.size() ; i++){
			//String cp = this.topologies.get(i).cloudProvider.trim().toLowerCase();
			String status = this.topologies.get(i).status.trim().toLowerCase();
			//Do not need check the cloud provider. Before format checking, there is a simpleLoadTopology  
			if(!status.equals("fresh") && !status.equals("running")
				&& !status.equals("failed") && !status.equals("stopped") && !status.equals("deleted")){
				logger.error("The field 'status' of "+this.topologies.get(i).topology+" is not valid!");
				return false;
			}
		}
		
		return true;
	}


	public boolean loadPublicKey(String currentDir) {
		if(publicKeyPath == null){
			logger.warn("Please configure the public key path first!");
			return false;
		}
			
		if((publicKeyString = CommonTool.getFileContent(publicKeyPath, currentDir)) == null){
			logger.warn("File of "+publicKeyPath+" cannot be loaded!");
			return false;
		}
			
		return true;
	}
	
	/**
	 * Generate a subnet name which is not in current list.
	 * @return
	 */
	public String generateSubnetName(){
		String sbName = null;
		for(int i = 0; i<Integer.MAX_VALUE ; i++){
			sbName = "s"+i;
			if(!this.subnetIndex.containsKey(sbName))
				return sbName;
		}
		return null;
	}
	
	
	/**
	 * Generate a connection name which is not in current list.
	 * @return
	 */
	public String generateConnectionName(){
		String conName = null;
		for(int i = 0; i<Integer.MAX_VALUE ; i++){
			conName = "c"+i;
			if(!this.connectionIndex.containsKey(conName))
				return conName;
		}
		return null;
	}
	
	/**
	 * Generate a VM name which is not in current list.
	 * This is used for scaling
	 * @return
	 */
	public String generateVMName(String VM){
		String VMName = null;
		for(int i = 0; i<Integer.MAX_VALUE ; i++){
			VMName = VM+"S"+i;
			if(!this.VMIndex.containsKey(VMName))
				return VMName;
		}
		return null;
	}

	
}
