/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * Copyright © Huan Zhou (SNE, University of Amsterdam) and contributors
 */
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

import commonTool.ClassDB;
import commonTool.ClassSet;
import commonTool.CommonTool;
import commonTool.Values;
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
	

	@Override @JsonIgnore
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

	@Override @JsonIgnore
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

	@Override @JsonIgnore
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
	
	@Override @JsonIgnore
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
	
	@JsonIgnore
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

	@Override @JsonIgnore
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
		//the name of the sub-topology cannot start with '_'
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
			if(curInfo.status.equals(Values.STStatus.running)){
				runningST = true;
				String currentDir = CommonTool.getPathDir(this.loadingPath);
				String clusterKeyDir = currentDir+"clusterKeyPair"+File.separator;
				File keyDir = new File(clusterKeyDir);
				if(!keyDir.exists()){
					logger.error("The folder of 'clusterKeyPair' must exist, because there is a running sub-topology '"+curInfo.topology+"'!");
					return false;
				}
			}
			
			
			String tn = topologies.get(i).topology.trim();
			if(tn == null){
				logger.error("The sub-topology name must be specified and cannot be set as 'null'!");
				return false;
			}
			if(tn.contains(".")){
				logger.error("Invaild topology of "+tn+"! It cannot contain '.'!");
				return false;
			}
			if(tn.startsWith("_") && !tn.equals("_ctrl")){
				logger.error("Sub-topology '"+curInfo.topology
						+"' cannot start with '_'!");
				return false;
			}
			if(subTopologyIndex.containsKey(tn)){
				logger.error("There are two same topology name '"+tn+"' in top level description.");
				return false;
			}else
				subTopologyIndex.put(tn, curInfo);
			topologies.get(i).topology = tn;
		}
		///test whether the scaled topology exist!
		for(int i = 0 ; i<topologies.size() ; i++){
			SubTopologyInfo curInfo = topologies.get(i);
			if(curInfo.scaledFrom != null
					&& !curInfo.scaledFrom.equals("_none")){
				if(!subTopologyIndex.containsKey(curInfo.scaledFrom)){
					String actualSTName = null;
					if(curInfo.scaledFrom.endsWith("_vscale")){
						actualSTName = curInfo.scaledFrom.split("_vscale")[0];
					}
					if(!subTopologyIndex.containsKey(actualSTName)){
						logger.error("There is no sub-topology '"+curInfo.scaledFrom+"' to be scaled!");
						return false;
					}else
						curInfo.scaledFrom = actualSTName;
				}
			}
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
				if(curInfo.status.equals(Values.STStatus.fresh) || curInfo.status.equals(Values.STStatus.stopped)
						|| curInfo.status.equals(Values.STStatus.deleted) || curInfo.status.equals(Values.STStatus.failed)){
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
				if(curInfo.status.equals(Values.STStatus.fresh) || curInfo.status.equals(Values.STStatus.stopped)
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
			if(!status.equals(Values.STStatus.fresh) && !status.equals(Values.STStatus.running)
				&& !status.equals(Values.STStatus.failed) && !status.equals(Values.STStatus.stopped) 
				&& !status.equals(Values.STStatus.deleted)){
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
	@JsonIgnore
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
	@JsonIgnore
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
	@JsonIgnore
	public String generateVMName(String VM){
		String VMName = null;
		for(int i = 0; i<Integer.MAX_VALUE ; i++){
			VMName = VM+"S"+i;
			if(!this.VMIndex.containsKey(VMName))
				return VMName;
		}
		return null;
	}
	
	/**
	 * Generate a sub-topology name which is not in current list.
	 * This is used for scaling from a sub-topology
	 * @return
	 */
	@JsonIgnore
	public String generateSTName(String orgSTName){
		String STName = null;
		for(int i = 0; i<Integer.MAX_VALUE ; i++){
			STName = orgSTName+"S"+i;
			if(!this.subTopologyIndex.containsKey(STName))
				return STName;
		}
		return null;
	}
	
	/**
	 * Generate a sub-topology name which is not in current list.
	 * This is used for scaling from some VMs, which may not in one sub-topology.
	 * @return
	 */
	@JsonIgnore
	public String generateSTName(){
		String STName = null;
		for(int i = 0; i<Integer.MAX_VALUE ; i++){
			STName = "scaled"+"S"+i;
			if(!this.subTopologyIndex.containsKey(STName))
				return STName;
		}
		return null;
	}
	
	/**
	 * Complete the connection information from the subnet definitions
	 * @return
	 */
	@JsonIgnore
	public boolean completeConInfoFromSubnet(){
		if(this.subnets != null){
			for(int si = 0 ; si < this.subnets.size() ; si++){
				Subnet curSubnet = this.subnets.get(si);
				for(int mi = 0 ; mi<curSubnet.members.size() ; mi++){
					Member curMember = curSubnet.members.get(mi);
					String [] t_VM = curMember.vmName.split("\\.");
					if(t_VM[0].trim().equals("") || t_VM.length != 2){
						logger.error("The format of member "+curMember.vmName+" in subnet "
								+curSubnet.name+" is not correct!");
						return false;
					}
					String VMName = t_VM[1]; String subTopologyName = t_VM[0];
					SubTopologyInfo sti = this.subTopologyIndex.get(subTopologyName);
					if(sti == null){
						logger.error("The sub-topology of subnet "+curMember.vmName+" doesn't exist!");
						return false;
					}
					//Get the VM in the sub-topology
					VM vmInfo = this.VMIndex.get(VMName);
					if(vmInfo == null){
						logger.error("There is no VM called "+VMName+" in "+subTopologyName);
						return false;
					}if(vmInfo.selfEthAddresses == null)
						vmInfo.selfEthAddresses = new HashMap<String, String>();
					if(curSubnet.netmask == null){
						logger.error("Field 'netmask' of target connection "+curMember.address+" must be specified!");
						return false;
					} String nm = curSubnet.netmask;
					if(( nm = CommonTool.netmaskIntToString(Integer.valueOf(curSubnet.netmask))) == null){
						logger.error("Field 'netmask' of target connection "+curMember.address+" is not valid!");
						return false;
					}
					String ethKey = curMember.address+"/"+nm;
					if(!vmInfo.selfEthAddresses.containsKey(ethKey))
						vmInfo.selfEthAddresses.put(curMember.address+"/"+nm, null);
					curMember.absVMName = VMName;
				}
			}
			
			for(int si = 0 ; si < this.subnets.size() ; si++){
				Subnet curSubnet = this.subnets.get(si);
				//// the number of the nodes in this subnet
				int nodesNum = curSubnet.members.size();
				for(int mi = 0 ; mi<curSubnet.members.size() ; mi++){
					Member curMember = curSubnet.members.get(mi);
					////This means there are still missing some connections for this subnet  
					if(curMember.adjacentNodes.size() != (nodesNum-1)){
						for(int allm = 0 ; allm < curSubnet.members.size() ; allm++){
							Member peerMember = curSubnet.members.get(allm);
							////skip the node itself
							if(peerMember.vmName
									.equals(curMember.vmName))
								continue;
							if(!curMember.adjacentNodes
									.containsKey(peerMember.vmName)){
								String [] t_VM = curMember.vmName.split("\\.");
								String sourceSubT = t_VM[0];
								t_VM = peerMember.vmName.split("\\.");
								String targetSubT = t_VM[0];
								
								
								ActualConnection newCon = new ActualConnection();
								newCon.logic = "false";
								newCon.name = this.generateConnectionName();
								newCon.source = new ActualConnectionPoint();
								newCon.target = new ActualConnectionPoint();
								
								newCon.source.peerACP = newCon.target;
								newCon.target.peerACP = newCon.source;
								
								newCon.source.address = curMember.address;
								newCon.source.netmask = curSubnet.netmask;
								newCon.source.belongingVM 
									= this.VMIndex.get(curMember.absVMName);
								newCon.source.belongingSubT = sourceSubT;
								newCon.source.vmName = curMember.vmName;
								
								if(this.subTopologyIndex.get(sourceSubT).connectors == null)
									this.subTopologyIndex.get(sourceSubT).connectors 
										= new ArrayList<ActualConnectionPoint>();
								this.subTopologyIndex.get(sourceSubT).connectors.add(newCon.source);
								if(newCon.source.belongingVM.vmConnectors == null)
									newCon.source.belongingVM.vmConnectors
										= new ArrayList<ActualConnectionPoint>();
								newCon.source.belongingVM.vmConnectors.add(newCon.source);
								
								newCon.target.address = peerMember.address;
								newCon.target.netmask = curSubnet.netmask;
								newCon.target.belongingVM 
									= this.VMIndex.get(peerMember.absVMName);
								newCon.target.belongingSubT = targetSubT;
								newCon.target.vmName = peerMember.vmName;
								
								if(this.subTopologyIndex.get(targetSubT).connectors == null)
									this.subTopologyIndex.get(targetSubT).connectors 
										= new ArrayList<ActualConnectionPoint>();
								this.subTopologyIndex.get(targetSubT).connectors.add(newCon.target);
								if(newCon.target.belongingVM.vmConnectors == null)
									newCon.target.belongingVM.vmConnectors
										= new ArrayList<ActualConnectionPoint>();
								newCon.target.belongingVM.vmConnectors.add(newCon.target);
								
								if(this.connections == null)
									this.connections = new ArrayList<ActualConnection>();
								this.connections.add(newCon);
								this.connectionIndex.put(newCon.name, newCon);
								
								curMember.adjacentNodes.put(peerMember.vmName, peerMember);
								peerMember.adjacentNodes.put(curMember.vmName, curMember);
							}
						}
					}
				}
			}
		}
		return true;
	}
	
	/*
	 * To find a scaling IP address in the subnet.
	 * The rule is to find the available IP address from low to high.
	 * If there is no IP available, then return null. 
	 */
	private String findScalingIPinSubnet(Subnet curSubnet){
		String subnet = curSubnet.subnet;
		int netmaskNum = Integer.valueOf(curSubnet.netmask);
		int hostNum = 1;
		while(true){
			String scalingIP = CommonTool.getFullAddress(subnet, 
											netmaskNum, hostNum);
			////means there is no available IP in this subnet
			if(scalingIP == null)
				return null;
			boolean found = false;
			for(int mi = 0 ; mi < curSubnet.members.size() ; mi++){
				if(curSubnet.members.get(mi).address.equals(scalingIP)){
					found = true;
					break;
				}
			}
			if(found)
				hostNum++;
			else
				return scalingIP;
		}
	}
	
	/**
	 * This is used to totally delete the scaled copy
	 * @return
	 */
	@JsonIgnore
	public void deleteScaledCopy(){
		if(connections != null){
			for(int ci = 0 ; ci<connections.size() ; ci++){
				ActualConnection curCon = connections.get(ci);
				ActualConnectionPoint sourceACP = curCon.source;
				ActualConnectionPoint targetACP = curCon.target;
				
				SubTopologyInfo sourceSTI = this.subTopologyIndex.get(sourceACP.belongingSubT);
				SubTopologyInfo targetSTI = this.subTopologyIndex.get(targetACP.belongingSubT);
				/*boolean valid = true;
				if(sourceACP.ethName == null && targetACP.ethName == null)
					valid = false;*/
				if((sourceSTI.scaledFrom != null
						&& sourceSTI.status.equalsIgnoreCase(Values.STStatus.deleted))
					|| (targetSTI.scaledFrom != null
							&& targetSTI.status.equalsIgnoreCase(Values.STStatus.deleted))){
					boolean vscaling = false;
					///first test whether this connection belongs to a vertical scaling sub-topology
					if(sourceSTI.topology.endsWith("_vscale"))
						vscaling = true;
					if(targetSTI.topology.endsWith("_vscale"))
						vscaling = true;
					if(vscaling)
						continue;
					
					///if it is a logic connection designed by the application, it should not be deleted
					///only when it is a logic connection, it can be delted
					if(curCon.logic.trim().equalsIgnoreCase("false") ){// && !valid){
						CommonTool.rmKeyInMap(this.connectionIndex, curCon.name);
						connections.remove(ci--);
					}
				}
			}
		}
		for(int si = 0 ; si<topologies.size() ; si++){
			SubTopologyInfo curSTI = topologies.get(si);
			if(curSTI.scaledFrom != null
					&& curSTI.status.equalsIgnoreCase(Values.STStatus.deleted)){
				///this is a vertical scaling copy
				boolean vscaling = false;
				if(curSTI.topology.endsWith("_vscale"))
					vscaling = true;
					
				
				///these are hscale
				if(!vscaling){
					///delete all the VMs related connections
					ArrayList<VM> vms = curSTI.subTopology.getVMsinSubClass();
					for(int vi = 0 ; vi<vms.size() ; vi++){
						VM curVM = vms.get(vi);
						for(int sbti = 0 ; sbti<this.subnets.size() ; sbti++)
							subnets.get(sbti).rmMember(curVM.name);
						
						if(curVM.vmConnectors != null){
							for(int vci = 0 ; vci<curVM.vmConnectors.size() ; vci++){
								ActualConnectionPoint curACP = curVM.vmConnectors.get(vci);
								ActualConnectionPoint peerACP = curACP.peerACP;
								VM peerVM = peerACP.belongingVM;
								SubTopologyInfo peerSTI = peerVM.ponintBack2STI;
								////not the same sub-topology
								if(peerSTI != null
										&& peerSTI != curSTI){
									if(peerVM.vmConnectors != null)
										peerVM.vmConnectors.remove(peerACP);
									if(peerSTI.connectors != null)
										peerSTI.connectors.remove(peerACP);
								}
							}
						}
						CommonTool.rmKeyInMap(this.VMIndex, curVM.name);
					}
				}
				
				CommonTool.rmKeyInMap(this.subTopologyIndex, curSTI.topology);
				topologies.remove(si--);
				
				///if this sub-topology is a scaled copy. totally delete it, including files
				File stFile = new File(curSTI.subTopology.loadingPath);
				FileUtils.deleteQuietly(stFile);
					
			}
		}
	}
	
	/**
	 * Generate a scaling sub-topology from the input sub-topology which is the target to be scaled
	 * @return
	 */
	@JsonIgnore
	public SubTopologyInfo genScalingSTFromST(String targetSTName, String scaledSTName, 
								String cloudProvider, String domain, ClassSet scaledClasses){
		if(!subTopologyIndex.containsKey(targetSTName)){
			logger.error("Target scaling sub-topology "+targetSTName+" is invalid!");
			return null;
		}
		if(scaledSTName != null && subTopologyIndex.containsKey(scaledSTName)){
			logger.error("The scaled sub-topology "+scaledSTName+" exists!");
			return null;
		}
		if(scaledClasses == null)
			scaledClasses = new ClassSet();
		SubTopologyInfo targetSTI = subTopologyIndex.get(targetSTName);
		SubTopology targetST = subTopologyIndex.get(targetSTName).subTopology;
		ArrayList<VM> targetVMs = targetST.getVMsinSubClass();
		Class<?> scaledSTClass = ClassDB.getSubTopology(cloudProvider, scaledClasses.SubTopologyClass);
		if(scaledSTClass == null){
			logger.error(cloudProvider + " is not supported!");
			return null;
		}
		SubTopologyInfo scaledSTI = new SubTopologyInfo();
		try {
			SubTopology scaledST = (SubTopology)scaledSTClass.newInstance();
			scaledST.cloudProvider = cloudProvider;
			scaledST.subTopologyClass = scaledClasses.SubTopologyClass;
			scaledST.SEngineClass = scaledClasses.SEngineClass;
			if(scaledSTName == null)
				scaledST.topologyName = generateSTName(targetST.topologyName);
			else
				scaledST.topologyName = scaledSTName.trim();
			String currentDir = CommonTool.getPathDir(targetST.loadingPath);
			scaledST.loadingPath = currentDir + scaledST.topologyName + ".yml";
			
			Class<?> scaledVMClass = scaledST.getVMClass();
			if(scaledVMClass == null){
				logger.error("Invalid VM Class for sub-topology '"
										+scaledST.topologyName+"'!");
				return null;
			}
			ArrayList<VM> scaledVMs = new ArrayList<VM>();
			for(int vi = 0 ; vi < targetVMs.size() ; vi++){
				VM targetVM = targetVMs.get(vi);
				if(targetVM.fake != null && targetVM.fake.trim().equalsIgnoreCase("true")){
					String vmName = targetVM.name;
					targetVM = this.VMIndex.get(vmName);
				}else
					targetVM.fake = null;
				
				VM scaledVM = (VM)scaledVMClass.newInstance();
				scaledVM.name = generateVMName(targetVM.name);
				scaledVM.type = targetVM.type;
				scaledVM.nodeType = targetVM.nodeType;
				scaledVM.CPU = targetVM.CPU;
				scaledVM.Mem = targetVM.Mem;
				scaledVM.OStype = targetVM.OStype;
				scaledVM.defaultSSHAccount = null;
				scaledVM.script = targetVM.script;
				scaledVM.v_scriptString = targetVM.v_scriptString;
				scaledVM.publicAddress = null;
				scaledVM.VEngineClass = scaledClasses.VEngineClass;
				scaledVM.scaledFrom = targetVM.name;
				scaledVM.ponintBack2STI = scaledSTI;
				
				scaledVMs.add(scaledVM);
				this.VMIndex.put(scaledVM.name, scaledVM);
				
				///update the network information
				String targetVMFullName = targetST.topologyName+"."+targetVM.name;
				if(subnets != null){
					for(int si = 0 ; si < subnets.size() ; si++){
						Subnet curSubnet = subnets.get(si);
						if(curSubnet.memberIndex.containsKey(targetVMFullName)){
							Member scaledMember = new Member();
							scaledMember.absVMName = scaledVM.name;
							scaledMember.vmName = scaledST.topologyName+"."+scaledVM.name;
							scaledMember.address  = findScalingIPinSubnet(curSubnet);
							if(scaledMember.address == null){
								logger.error("There is no available IP address in "+curSubnet.name);
								return null;
							}
							curSubnet.members.add(scaledMember);
							curSubnet.memberIndex.put(scaledMember.vmName, scaledMember);
						}
					}
				}
			}
			scaledST.setVMsInSubClass(scaledVMs);
			
			
			scaledSTI.subTopology = scaledST;
			scaledSTI.cloudProvider = cloudProvider;
			scaledSTI.domain = domain;
			scaledSTI.status = Values.STStatus.fresh;
			scaledSTI.topology = scaledST.topologyName;
			scaledSTI.userName = this.userName;
			scaledSTI.publicKeyString = this.publicKeyString;
			scaledSTI.subTopologyClass = scaledST.subTopologyClass;
			scaledSTI.logsInfo = new HashMap<String, String>();
			
			if(scaledSTI.cloudProvider.equalsIgnoreCase(targetSTI.cloudProvider)
					&& scaledSTI.domain.equalsIgnoreCase(targetSTI.domain)
					&& targetSTI.status.equalsIgnoreCase(Values.STStatus.running)){
				scaledSTI.sshKeyPairId = targetSTI.sshKeyPairId;
				scaledST.accessKeyPair = targetST.accessKeyPair;
			}
			
			scaledSTI.scaledFrom = targetSTI.topology;
			
			///update the default values!
			if(!scaledST.formatChecking(scaledSTI.status)){
				logger.error("Upexpected Error when updating the default values of "+scaledST.topologyName);
				return null;
			}
			////save the generated information into the file
			if(!scaledST.overwirteControlOutput()){
				logger.error("Cannot overwrite the sub-topology file at "+scaledST.loadingPath);
				return null;
			}
			
			this.topologies.add(scaledSTI);
			this.subTopologyIndex.put(scaledSTI.topology, scaledSTI);

		} catch (InstantiationException | IllegalAccessException e) {
			e.printStackTrace();
			logger.error(e.getMessage());
			return null;
		}
		return scaledSTI;
	}
	
	/**
	 * Generate a scaling sub-topology from a list of VMs which is the target to be scaled
	 * @return
	 */
	@JsonIgnore
	public SubTopologyInfo genScalingSTFromVM(ArrayList<String> targetVMs, String scaledSTName, 
								String cloudProvider, String domain,
								ClassSet scaledClasses){
		if(scaledSTName != null && subTopologyIndex.containsKey(scaledSTName)){
			logger.error("The scaled sub-topology "+scaledSTName+" exists!");
			return null;
		}
		if(cloudProvider == null || domain == null){
			logger.error("cloudProvider and domain muster be specified in the reqest!");
			return null;
		}
		if(scaledClasses == null)
			scaledClasses = new ClassSet();
		Class<?> scaledSTClass = ClassDB.getSubTopology(cloudProvider, scaledClasses.SubTopologyClass);
		if(scaledSTClass == null){
			logger.error(cloudProvider + " is not supported!");
			return null;
		}
		SubTopologyInfo scaledSTI = new SubTopologyInfo();
		try {
			SubTopology scaledST = (SubTopology)scaledSTClass.newInstance();
			scaledST.cloudProvider = cloudProvider;
			scaledST.subTopologyClass = scaledClasses.SubTopologyClass;
			scaledST.SEngineClass = scaledClasses.SEngineClass;
			if(scaledSTName == null)
				scaledST.topologyName = generateSTName();
			else
				scaledST.topologyName = scaledSTName.trim();
			String currentDir = CommonTool.getPathDir(this.loadingPath);
			scaledST.loadingPath = currentDir + scaledST.topologyName + ".yml";
			
			Class<?> scaledVMClass = scaledST.getVMClass();
			if(scaledVMClass == null){
				logger.error("Invalid VM Class for sub-topology '"
										+scaledST.topologyName+"'!");
				return null;
			}
			ArrayList<VM> scaledVMs = new ArrayList<VM>();
			for(int vi = 0 ; vi < targetVMs.size() ; vi++){
				VM targetVM = this.VMIndex.get(targetVMs.get(vi));
				if(targetVM == null){
					logger.error("There is no VM named '"+targetVMs.get(vi)+"'!");
					return null;
				}
				VM scaledVM = (VM)scaledVMClass.newInstance();
				scaledVM.name = generateVMName(targetVM.name);
				scaledVM.type = targetVM.type;
				scaledVM.nodeType = targetVM.nodeType;
				scaledVM.CPU = targetVM.CPU;
				scaledVM.Mem = targetVM.Mem;
				scaledVM.OStype = targetVM.OStype;
				scaledVM.defaultSSHAccount = null;
				scaledVM.script = targetVM.script;
				scaledVM.v_scriptString = targetVM.v_scriptString;
				scaledVM.publicAddress = null;
				scaledVM.VEngineClass = scaledClasses.VEngineClass;
				scaledVM.scaledFrom = targetVM.name;
				scaledVM.ponintBack2STI = scaledSTI;
				
				scaledVMs.add(scaledVM);
				this.VMIndex.put(scaledVM.name, scaledVM);
				
				///update the network information
				String targetVMFullName = targetVM.ponintBack2STI.topology+"."+targetVM.name;
				if(subnets != null){
					for(int si = 0 ; si < subnets.size() ; si++){
						Subnet curSubnet = subnets.get(si);
						if(curSubnet.memberIndex.containsKey(targetVMFullName)){
							Member scaledMember = new Member();
							scaledMember.absVMName = scaledVM.name;
							scaledMember.vmName = scaledST.topologyName+"."+scaledVM.name;
							scaledMember.address  = findScalingIPinSubnet(curSubnet);
							if(scaledMember.address == null){
								logger.error("There is no available IP address in "+curSubnet.name);
								return null;
							}
							curSubnet.members.add(scaledMember);
							curSubnet.memberIndex.put(scaledMember.vmName, scaledMember);
						}
					}
				}
			}
			scaledST.setVMsInSubClass(scaledVMs);
			
			
			scaledSTI.subTopology = scaledST;
			scaledSTI.cloudProvider = cloudProvider;
			scaledSTI.domain = domain;
			scaledSTI.status = Values.STStatus.fresh;
			scaledSTI.topology = scaledST.topologyName;
			scaledSTI.userName = this.userName;
			scaledSTI.publicKeyString = this.publicKeyString;
			scaledSTI.subTopologyClass = scaledST.subTopologyClass;
			scaledSTI.scaledFrom = "_none";
			scaledSTI.logsInfo = new HashMap<String, String>();
			
			///update the default values!
			if(!scaledST.formatChecking(scaledSTI.status)){
				logger.error("Upexpected Error when updating the default values of "+scaledST.topologyName);
				return null;
			}
			////save the generated information into the file
			if(!scaledST.overwirteControlOutput()){
				logger.error("Cannot overwrite the sub-topology file at "+scaledST.loadingPath);
				return null;
			}
			
			this.topologies.add(scaledSTI);
			this.subTopologyIndex.put(scaledSTI.topology, scaledSTI);
			
		} catch (InstantiationException | IllegalAccessException e) {
			e.printStackTrace();
			logger.error(e.getMessage());
			return null;
		}
		return scaledSTI;
	}

	/**
	 * Migrate the VM to the target sub-topology.
	 * This is usually used to scale up vertically. 
	 * @param vmName
	 * @param tmpSTName
	 * @return
	 */
	@JsonIgnore
	public boolean migrateVM2STI(String orgVMName, String targetSTName){
		VM orgVM = this.VMIndex.get(orgVMName);
		if(orgVM == null){
			logger.error("Invalid VM name '"+orgVMName+"'!");
			return false;
		}
		SubTopologyInfo orgSTI = orgVM.ponintBack2STI;
		SubTopology orgST = orgSTI.subTopology;
		SubTopologyInfo targetSTI = this.subTopologyIndex.get(targetSTName);
		if(targetSTI == null){
			////this is ok, if it is a tmp or vscale sub-topology
			if(!targetSTName.contains("_tmp_")
					&& !targetSTName.endsWith("_vscale")){
				logger.error("Cannot find sub-topology "+targetSTName);
				return false;
			}
			targetSTI = new SubTopologyInfo();
			targetSTI.topology = targetSTName;
			targetSTI.cloudProvider = orgSTI.cloudProvider;
			targetSTI.domain = orgSTI.domain;
			targetSTI.endpoint = orgSTI.endpoint;
			targetSTI.publicKeyString = orgSTI.publicKeyString;
			targetSTI.userName = orgSTI.userName;
			targetSTI.status = orgSTI.status;
			targetSTI.sshKeyPairId = orgSTI.sshKeyPairId;
			targetSTI.subTopologyClass = orgSTI.subTopologyClass;
			targetSTI.connectors = new ArrayList<ActualConnectionPoint>();
			targetSTI.logsInfo = new HashMap<String, String>();
			
			Class<?> orgSTClass = ClassDB.getSubTopology(orgSTI.cloudProvider,
												orgSTI.subTopologyClass);
			if(orgSTClass == null){
				logger.error(orgSTI.cloudProvider + " is not supported!");
				return false;
			}
			//SubTopology targetST = (SubTopology)orgST.clone();
			SubTopology targetST = null;
			try {
				targetST = (SubTopology)orgSTClass.newInstance();
			} catch (InstantiationException | IllegalAccessException e) {
				e.printStackTrace();
				logger.error(e.getMessage());
				return false;
			}
			targetST.cloudProvider = orgST.cloudProvider;
			targetST.extraInfo = null;
			targetST.accessKeyPair = orgST.accessKeyPair;
			targetST.SEngineClass = orgST.SEngineClass;
			targetST.subTopologyClass = orgST.subTopologyClass;
			targetST.topologyName = targetSTName;
			String currentDir = CommonTool.getPathDir(this.loadingPath);
			targetST.loadingPath = currentDir + targetST.topologyName + ".yml";
			
			ArrayList<VM> VMs = new ArrayList<VM>();
			VMs.add(orgVM);
			targetST.setVMsInSubClass(VMs);
			
			targetSTI.subTopology = targetST;
			
			///update the default values!
			if(!targetST.formatChecking(targetSTI.status)){
				logger.error("Upexpected Error when updating the default values of "+targetST.topologyName);
				return false;
			}
			
			this.topologies.add(targetSTI);
			this.subTopologyIndex.put(targetSTI.topology, targetSTI);
			
			
		}else{
			if(!targetSTI.subTopology.addVMInSubClass(orgVM))
				return false;
		}
		
		orgST.removeVMInSubClass(orgVM);
		
		if(orgSTI.connectors != null){
			for(int ci = 0 ; ci<orgSTI.connectors.size() ; ci++){
				ActualConnectionPoint curACP = orgSTI.connectors.get(ci);
				if(curACP.belongingVM.name.equals(orgVMName)){
					curACP.belongingSubT = targetSTI.topology;
					if(targetSTI.connectors == null)
						targetSTI.connectors = new ArrayList<ActualConnectionPoint>();
					targetSTI.connectors.add(curACP);
					orgSTI.connectors.remove(ci--);
				}
			}
		}
		
		return true;
	}
}
