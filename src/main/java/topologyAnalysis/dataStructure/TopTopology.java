package topologyAnalysis.dataStructure;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import provisioning.credential.EC2Credential;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import commonTool.CommonTool;
import topologyAnalysis.method.TopTopologyMethod;

public class TopTopology implements TopTopologyMethod{
	
	private static final Logger logger = Logger.getLogger(TopTopology.class);
	
	//The path of the file where this topology object loads.
	//Used for controlling information output.
	@JsonIgnore
	public String loadingPath;
	
	//The content of the publicKey. 
	//It is loaded, when loading the topology.
	@JsonIgnore
	public String publicKeyString;
	
	/**
	 * This field can be <br/>
	 * 1. the url of the ssh key file  <br/>
	 * 2. the absolute path of the file on the local machine <br/>
	 * 3. the file name of the file. By default, this file will be at the same 
	 * folder of the description files. <br/>
	 * Examples: url@http://www.mydomain.com/pathToFile/myId_dsa <br/>
	 * file@/home/id_dsa (the file path is absolute path)<br/>
	 * name@id_rsa.pub (just fileName) <br/>
	 * null <br/>
	 * This is not case sensitive.
	 * The file must exist. Otherwise, there will be a warning log message for this.
	 * And you can load these information manually later on.  <br/>
	 * All the "script" field is designed like this.
	 */
	public String publicKeyPath;
	
	/**
	 * This is used to identify a pair of SSH keys for the inner connection 
	 * among all the VM in the cluster. 
	 */
	//public String clusterKeyId;
	
	
	/*
	 * The user name defined by the user.
	 * This is corresponding to the ssh key.
	 */
	public String userName;
	
	@JsonIgnore
	public EC2Credential ec2Credential;
	
	public ArrayList<SubTopologyInfo> topologies;
	
	public ArrayList<TopConnection> connections;
	

	@Override
	public boolean loadTopTopology(String topologyPath) {
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
			/*String content = "";
        	String [] lines = yamlString.split("\\\n");
        	for(int i = 0 ; i<lines.length ; i++){
        		if(lines[i].contains(":")){
					String [] contents = lines[i].split(":");
					if(!contents[0].trim().equals("statusInfo"))
						content += (lines[i]+"\n");
				}else
					content += (lines[i]+"\n"); 
        	}*/
        	output.put("topLevel", yamlString);
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
        	output.put("topLevel", yamlString);
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
	
	/**
	 * This is a function to check format of all the netmask to a number.
	 * This function must be invoked in the beginning of function of 'formatChecking'.
	 * @return false if there is invalid netmask.
	 */
	private boolean netmaskFormating(){
		if(this.connections == null)
			return true;
		for(int i = 0 ; i<this.connections.size() ; i++){
			String netmask = "";
			TopConnectionPoint tcpSource = this.connections.get(i).source;
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
					if(netmaskNum<=0 || netmaskNum>=32){
						logger.error("Field 'netmask' of source connection "+this.connections.get(i).name+" should be between 1 and 31 (included)!");
						return false;
					}
					} catch (NumberFormatException e) {
						logger.error("Field 'netmask' of source connection "+this.connections.get(i).name+" is not valid!");
						return false;
					}
			}
			tcpSource.netmask = String.valueOf(netmaskNum);
			
			TopConnectionPoint tcpTarget = this.connections.get(i).target;
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
					if(netmaskNum<=0 || netmaskNum>=32){
						logger.error("Field 'netmask' of target connection "+this.connections.get(i).name+" should be between 1 and 31 (included)!");
						return false;
					}
					} catch (NumberFormatException e) {
						logger.error("Field 'netmask' of target connection "+this.connections.get(i).name+" is not valid!");
						return false;
					}
			}
			tcpTarget.netmask = String.valueOf(netmaskNum);
			
		}
		return true;
	}
	
	private boolean privateAddressChecking(){
		if(connections == null)
			return true;
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
		return true;
	}
	
	
	/**
	 * Validate the field 'scalingPool' and generate the scaling pool for the sub-topology 
	 * and generate the address pool for each topology with tag 'scaling'.
	 */
	private boolean scalingPoolChecking(){
		for(int i = 0 ; i<topologies.size() ; i++){
			if(topologies.get(i).tag.trim().toLowerCase().equals("fixed")){
				if(topologies.get(i).copyOf != null){
					logger.error("The 'fixed' sub-topology '"+topologies.get(i).topology+"' cannot define value for 'copyOf'");
					return false;
				}
				if(topologies.get(i).connectors != null){
					for(int j = 0 ; j<topologies.get(i).connectors.size() ; j++){
						TopConnectionPoint curTcp = topologies.get(i).connectors.get(j);
						if(curTcp.scalingPool != null){
							logger.error("The 'fixed' sub-topology '"+topologies.get(i).topology+"' cannot define value for 'scalingPool'");
							return false;
						}
					}
				}
			}
			
			//generate the scaling address pool
			if(topologies.get(i).tag.trim().toLowerCase().equals("scaling")){
				if(topologies.get(i).copyOf != null){
					logger.error("The 'scaling' sub-topology '"+topologies.get(i).topology+"' cannot define value for 'copyOf'");
					return false;
				}
				if(topologies.get(i).connectors != null){
					for(int j = 0 ; j<topologies.get(i).connectors.size() ; j++){
						TopConnectionPoint curTcp = topologies.get(i).connectors.get(j);
						if(curTcp.scalingPool == null){
							logger.error("The 'scaling' sub-topology "+topologies.get(i).topology+" must define value for 'scalingPool'");
							return false;
						}
						if(!curTcp.scalingPool.contains("-")){
							logger.error("The 'scalingPool' of sub-topology "+topologies.get(i).topology+" must define as 'IP1-IP2'");
							return false;
						}
						
						//analyze the scalingPool value string: 'IP1-IP2'
						String [] IPs = curTcp.scalingPool.split("-");
						if(!CommonTool.checkPrivateIPaddress(IPs[0]) 
							|| !CommonTool.checkPrivateIPaddress(IPs[1]) ){
							logger.error("The 'scalingPool' ("+curTcp.scalingPool+") of sub-topology "+topologies.get(i).topology+" must be defined as private IP address!");
							return false;
						}
						
						String subnetOfConnector = CommonTool.getSubnet(curTcp.address, Integer.valueOf(curTcp.netmask));
						String subnetOfIP1 = CommonTool.getSubnet(IPs[0], Integer.valueOf(curTcp.netmask));
						String subnetOfIP2 = CommonTool.getSubnet(IPs[1], Integer.valueOf(curTcp.netmask));
						if(!subnetOfConnector.equals(subnetOfIP1) || !subnetOfConnector.equals(subnetOfIP2)){
							logger.error("The 'scalingPool' ("+curTcp.scalingPool+") of sub-topology "+topologies.get(i).topology+" is not valid!");
							return false;
						}
						
						//Put the available address into HashMap.
						int iP1HostNum = CommonTool.getHostInfo(IPs[0], Integer.valueOf(curTcp.netmask));
						int iP2HostNum = CommonTool.getHostInfo(IPs[1], Integer.valueOf(curTcp.netmask));
						int bigNum, smallNum;
						if(iP1HostNum > iP2HostNum){
							bigNum = iP1HostNum;
							smallNum = iP2HostNum;
						}else{
							bigNum = iP2HostNum;
							smallNum = iP1HostNum;
						}
						if(topologies.get(i).scalingAddressPool == null)
							topologies.get(i).scalingAddressPool = new HashMap<String, Boolean>();
						
						for(int hostNum = smallNum ; hostNum <= bigNum ; hostNum++){
							String fullAddress = CommonTool.getFullAddress(subnetOfConnector, Integer.valueOf(curTcp.netmask), hostNum);
							topologies.get(i).scalingAddressPool.put(fullAddress, true);
						}
						
						if(topologies.get(i).scalingAddressPool.containsKey(curTcp.address)){
							logger.debug("In sub-topology"+topologies.get(i).topology+" ,the address ("+curTcp.address+") of the connector has already been contained in the scaling pool!");
							topologies.get(i).scalingAddressPool.put(curTcp.address, false);
						}
						if(topologies.get(i).scalingAddressPool.containsKey(curTcp.peerTCP.address)){
							logger.debug("In sub-topology"+topologies.get(i).topology+" ,the peer address ("+curTcp.peerTCP.address+") of the connector has already been contained in the scaling pool!");
							topologies.get(i).scalingAddressPool.put(curTcp.peerTCP.address, false);
						}
					}
				}
			}
		}
		
		
		
		////Checking the scaled sub-topology, to see how many addresses have been occupied.
		////However, this addresses used in 'scaled' 'deleted' sub-topology are not counted in the scalingAddressPool.
		for(int i = 0 ; i<topologies.size() ; i++){
			if(topologies.get(i).tag.trim().toLowerCase().equals("scaled")){
				if(topologies.get(i).connectors != null){
					for(int j = 0 ; j<topologies.get(i).connectors.size() ; j++){
						TopConnectionPoint curTcp = topologies.get(i).connectors.get(j);
						if(curTcp.scalingPool != null){
							logger.error("The 'scaled' sub-topology '"+topologies.get(i).topology+"' cannot define value for 'scalingPool'");
							return false;
						}
						//Here curTcp.fatherTopology must not be null due to the checking before.
						if(topologies.get(i).fatherTopology.scalingAddressPool != null){
							if(topologies.get(i).fatherTopology.scalingAddressPool.containsKey(curTcp.address)){
								if(!topologies.get(i).status.trim().toLowerCase().equals("deleted")){
									logger.debug("In sub-topology"+topologies.get(i).topology+" ,the address ("+curTcp.address+") of the connector occupied the scaling pool!");
									topologies.get(i).fatherTopology.scalingAddressPool.put(curTcp.address, false);
								}else
									logger.debug("Ignore the address '"+curTcp.address+"', because it is in the 'scaled' 'deleted' sub-topology '"+topologies.get(i).topology+"'");
							}else{
								logger.error("The address ("+curTcp.address+") in sub-topology "+topologies.get(i).topology+" is out of the scope of scaling pool!");
								return false;
							}
						}
					}
				}
			}
		}
		
		return true;
	}

	@Override
	public boolean formatChecking() {
		//First, format all the netmask into number and check whether they are private network address.
		if(!netmaskFormating())
			return false;
		if(!privateAddressChecking())
			return false;
		
		//Checking the existing of the 'publicKeyPath', if the 'userName' is set.
		if(this.userName != null && !this.userName.equals("")){
			String currentDir = CommonTool.getPathDir(this.loadingPath);
			if(!this.loadPublicKey(currentDir)){
				logger.warn("The public key cannot be loaded! You can set it later!");
			}else{
				logger.info("Public key "+this.publicKeyPath+" is loaded!");
			}
		}
		
		//Checking the names of different sub-topologies.
		Map<String, String> topologyNameCheck = new HashMap<String, String>();
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
			
			//check the connectors and the tunnelName
			if(curInfo.status.equals("fresh") || curInfo.status.equals("stopped")
				|| curInfo.status.equals("deleted") || curInfo.status.equals("failed")){
				if(curInfo.connectors != null){
					for(int ci = 0 ; ci < curInfo.connectors.size() ; ci++){
						if(curInfo.connectors.get(ci).ethName != null){
							logger.error("The '"+curInfo.status+"' sub-topology '"+curInfo.topology+"' cannot have a valid ethName!");
							return false;
						}
					}
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
			if(topologyNameCheck.containsKey(tn)){
				logger.error("There are two same topology name '"+tn+"' in top level description.");
				return false;
			}else
				topologyNameCheck.put(tn, "");
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
		Map<String, String> conNameCheck = new HashMap<String, String>();
		if(connections != null){
			for(int i = 0 ; i<connections.size() ; i++){
				String cn = connections.get(i).name;
				if(cn == null){
					logger.error("The connection name must be specified and cannot be set as 'null'!");
					return false;
				}
				if(conNameCheck.containsKey(cn)){
					logger.error("There are two same connection name '"+cn+"' in top level description.");
					return false;
				}else
					conNameCheck.put(cn, "");
				
				//Checking: all the addresses in the same connection should be in the same subnet.
				//And these two addresses must be different. 
				TopConnectionPoint tcpSource = this.connections.get(i).source;
				TopConnectionPoint tcpTarget = this.connections.get(i).target;
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
			}
		}
		
		
		
		//Checking whether some values are correct.
		for(int i = 0 ; i<this.topologies.size() ; i++){
			//String cp = this.topologies.get(i).cloudProvider.trim().toLowerCase();
			String status = this.topologies.get(i).status.trim().toLowerCase();
			String tag = this.topologies.get(i).tag.trim().toLowerCase();
			//Do not need check the cloud provider. Before format checking, there is a simpleLoadTopology  
			//if(!cp.equals("ec2") && !cp.equals("geni")  && !cp.equals("exogeni"))
				//return false;
			if(!status.equals("fresh") && !status.equals("running")
				&& !status.equals("failed") && !status.equals("stopped") && !status.equals("deleted")){
				logger.error("The field 'status' of "+this.topologies.get(i).topology+" is not valid!");
				return false;
			}
			if(!tag.equals("fixed") && !tag.equals("scaling") && !tag.equals("scaled")){
				logger.error("The field 'tag' of "+this.topologies.get(i).topology+" is not valid!");
				return false;
			}
			/*if(status.equals("fresh") && tag.equals("scaled")){
				logger.error("The 'fresh' subTopology "+this.topologies.get(i).topology+" cannot be 'scaled'!");
				return false;
			}*/
		}
		
		if(!scalingPoolChecking())
			return false;
		
		return true;
	}


	@Override
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

	
}
