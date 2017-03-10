package topologyAnalysis.dataStructure;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import provisioning.credential.SSHKeyPair;
import topologyAnalysis.method.SubTopologyMethod;

import com.fasterxml.jackson.annotation.JsonIgnore;

import commonTool.CommonTool;

public abstract class SubTopology implements SubTopologyMethod {
	
	private static final Logger logger = Logger.getLogger(SubTopology.class);
	
	//The path of the file where this topology object loads.
	//Used for controlling information output.
	@JsonIgnore
	public String loadingPath;
	
	//The sub-topology name is defined in the top level description.
	@JsonIgnore
	public String topologyName;
	
	//Indicate what the type of this sub-topology is. 
	//For instance, ec2, exogeni.
	@JsonIgnore
	public String topologyType;
	
	@JsonIgnore
	public SSHKeyPair accessKeyPair;
	
	
	//Indicate a subnet that several can be put in.
	public ArrayList<Subnet> subnets;
	
	//Indicate how two VMs can be connected.
	public ArrayList<SubConnection> connections;
	
		
	//Indicate the fully qualified class name of the provisioning agent for this sub-topology. 
	@JsonIgnore
	public String provisioningAgentClassName;

	
	

	
	
	/**
	 * This is the common format checking, 
	 * no matter where the sub-topology comes from. 
	 * This must be invoked at the beginning of the formatChecking() method in sub classes. 
	 * The checking items includes: <br/>
	 * 1. Checking the validation of the private IP addresses and netmak.  
	 * Change all the netmask into representation of number. <br/>
	 * 2. All the connection name should be different. <br/>
	 * 3. All the subnet name should be different. <br/>
	 * 4. All the node names should be different. <br/>
	 * 5. Checking all the eth names of one VM must be different.  <br/>
	 * 6. Complete all the information of each connection. <br/>
	 * 7. The VM cannot have public address, if the status of the sub-topology
	 * is 'fresh'.
	 * 8. The VM must have public address, if the status of the sub-topology 
	 * is 'running'.
	 * 9. Update the 'belongingVM' field in the SubConnectionPoint.
	 * 10. Checking that all the private address in one subnet must be 
	 * in the same subnet. 
	 * 
	 * Input is the status of the sub-topology. 
	 * @return
	 */
	public boolean commonFormatChecking(String topologyStatus){
		logger.info("Validation on the sub-topology '"+this.topologyName+"'");
		if(!netmaskFormating())
			return false;
		
		//Checking the connection name
		if(connections != null){
			Map<String, String> conNameCheck = new HashMap<String, String>();
			for(int i = 0 ; i<connections.size() ; i++){
				String cn = connections.get(i).name;
				if(cn == null){
					logger.error("The connection name must be specified and cannot be set as 'null'!");
					return false;
				}
				if(conNameCheck.containsKey(cn)){
					logger.error("There are two same connection name '"+cn+"' in this description.");
					return false;
				}else
					conNameCheck.put(cn, "");
				
				//Checking: all the addresses in the same connection should be in the same subnet.
				//And these two addresses must be different. 
				SubConnectionPoint scpSource = this.connections.get(i).source;
				SubConnectionPoint scpTarget = this.connections.get(i).target;
				if(scpSource.address.equals(scpTarget.address)){
					logger.error("Two connection points in connection "+connections.get(i).name+" must have different address!" );
					return false;
				}
				String sourceSubnet = CommonTool.getSubnet(scpSource.address, Integer.valueOf(scpSource.netmask));
				String targetSubnet = CommonTool.getSubnet(scpTarget.address, Integer.valueOf(scpTarget.netmask));
				if(!sourceSubnet.equals(targetSubnet)){
					logger.error("Two connection points in connection "+connections.get(i).name+" should be in the same subnet!" );
					return false;
				}
				
				//Update the 'belongingVM' field in the SubConnectionPoint.
				//Get the VM in the sub-topology
				String sourceVMName = this.connections.get(i).source.componentName;
				VM vmInfo = this.getVMinSubClassbyName(sourceVMName);
				if(vmInfo == null){
					logger.error("There is no VM called "+sourceVMName+" in "+this.topologyName);
					return false;
				}
				this.connections.get(i).source.belongingVM = vmInfo;
				
				String targetVMName = this.connections.get(i).target.componentName;
				vmInfo = this.getVMinSubClassbyName(targetVMName);
				if(vmInfo == null){
					logger.error("There is no VM called "+targetVMName+" in "+this.topologyName);
					return false;
				}
				this.connections.get(i).target.belongingVM = vmInfo;
			}
		}
		
		//Checking the subnet name
		if(subnets != null){
			Map<String, String> subnetNameCheck = new HashMap<String, String>();
			for(int i = 0 ; i<subnets.size() ; i++){
				String sn = subnets.get(i).name;
				if(sn == null){
					logger.error("The subnet name must be specified and cannot be set as 'null'!");
					return false;
				}
				if(subnetNameCheck.containsKey(sn)){
					logger.error("There are two same subnet name '"+sn+"' in this description.");
					return false;
				}else
					subnetNameCheck.put(sn, "");
			}
		}
		
		//check all the basic information of the VMs.
		ArrayList<VM> vms = this.getVMsinSubClass();
		if(vms == null){
			logger.error("At least one VM must be defined in a sub-topology!");
			return false;
		}
		
		Map<String, String> vmNameCheck = new HashMap<String, String>();
		for(int i = 0 ; i<vms.size() ; i++){
			//Checking the VM name
			VM curVM = vms.get(i);
			String vn = curVM.name;
			if(vn == null){
				logger.error("The VM name must be specified and cannot be set as 'null'!");
				return false;
			}
			if(vmNameCheck.containsKey(vn)){
				logger.error("There are two same node name '"+vn+"' in this description.");
				return false;
			}else
				vmNameCheck.put(vn, "");
			
			//check the 'script' in the VM.
			if(curVM.script != null){
				String currentDir = CommonTool.getPathDir(loadingPath);
				if(!curVM.loadScript(currentDir)){
					logger.error("Cannot load the script file from "+curVM.script+"!");
					return false;
				}
				else 
					logger.info("Script of "+curVM.script+" is loaded!");
			}
			
			if(curVM.role != null){
				if(!curVM.role.trim().toLowerCase().equals("master") 
					&& !curVM.role.trim().toLowerCase().equals("slave")){
					logger.error("Field 'role' of VM '"+curVM.name+"' is invalid!");
					return false;
				}
			}
			
			if(topologyStatus.equals("fresh") && (curVM.publicAddress != null)){
				logger.error("VM '"+curVM.name+"' cannot have public address in 'fresh' status!");
				return false;
			}
			
			if(topologyStatus.equals("running") && (curVM.publicAddress == null)){
				logger.error("VM '"+curVM.name+"' must have public address in 'running' status!");
				return false;
			}
			
			//check the eths in the VM
			Map<String, String> ethNameCheck = new HashMap<String, String>();
			for(int j = 0 ; j<curVM.ethernetPort.size() ; j++){
				Eth curEth = curVM.ethernetPort.get(j);
				String en = curEth.name;
				if(en == null){
					logger.error("The eth name of VM '"+curVM.name+"' must be specified and cannot be set as 'null'!");
					return false;
				}
				//check the eth names of one VM are different
				if(ethNameCheck.containsKey(en)){
					logger.error("There are two same eth name '"+en+"' of VM '"+curVM.name+"' in this description.");
					return false;
				}else
					ethNameCheck.put(en, "");
				
				if(curEth.connectionName != null && (curEth.subnetName != null || curEth.address != null)){
					logger.error("The eth '"+en+"' of VM '"+curVM.name+"' can not be both connection or subnet!");
					return false;
				}
				
				if(curEth.subnetName != null && curEth.address == null){
					logger.error("The eth '"+en+"' belongs to a subnet must be specified an address!");
					return false;
				}
				
				if(curEth.subnetName == null && curEth.connectionName == null){
					logger.error("The eth '"+en+"' of VM '"+curVM.name+"' must belong to a connection or subnet!");
					return false;
				}
				
				//Validate the name of subnet or connection. 
				//Also update the information of the corresponding connection.
				if(curEth.subnetName != null){
					Subnet findSubnet = null;
					for(int si = 0 ; si < subnets.size() ; si++){
						if(subnets.get(si).name.equals(curEth.subnetName)){
							findSubnet = subnets.get(si);
							break;
						}
					}
					if(findSubnet == null){
						logger.error("The subnet '"+curEth.subnetName+"' of eth '"+curEth.name+"' cannnot be found!");
						return false;
					}
					curEth.subnet = findSubnet;
				}
				if(curEth.connectionName != null){
					SubConnectionPoint findScp = null;
					if(!curEth.connectionName.contains(".")){
						logger.error("Field 'connectionName' of '"+curVM.name+"' is not valid!");
						return false;
					}
					String [] con_st = curEth.connectionName.split("\\.");
					String conName = con_st[0]; String st = con_st[1];
					if(!st.equals("source") && !st.equals("target")){
						logger.equals("The connection name of '"+curEth.connectionName+"' must contains 'source' or 'target'!");
						return false;
					}
					for(int ci = 0 ; ci<this.connections.size() ; ci++){
						if(connections.get(ci).name.equals(conName)){
							if(st.equals("source"))
								findScp = connections.get(ci).source;
							else
								findScp = connections.get(ci).target;
							break;
						}
					}
					if(findScp == null){
						logger.error("The connection '"+curEth.connectionName+"' of eth '"+curEth.name+"' cannot be found in 'connections'");
						return false;
					}
					curEth.scp = findScp;
				}
			}
			
		}
		
		
		return true;
	}
	
	
	/**
	 * This is a function to check format all the netmask to a number.
	 * @return false if there is invalid netmask.
	 */
	private boolean netmaskFormating(){
		if(this.connections != null){
			for(int i = 0 ; i<this.connections.size() ; i++){
				String netmask = "";
				SubConnectionPoint scpSource = this.connections.get(i).source;
				netmask = scpSource.netmask;
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
				scpSource.netmask = String.valueOf(netmaskNum);
				String networkAddress = scpSource.address;
				if(networkAddress == null){
					logger.error("The filed 'address' of source connection "+this.connections.get(i).name+" cannot be null!");
					return false;
				}
				if(!CommonTool.checkPrivateIPaddress(networkAddress)){
					logger.error("The filed 'address' (value='"+networkAddress+"') of source connection "+this.connections.get(i).name+" should be private network address!");
					return false;
				}
				
				SubConnectionPoint scpTarget = this.connections.get(i).target;
				netmask = scpTarget.netmask;
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
				scpTarget.netmask = String.valueOf(netmaskNum);
				networkAddress = scpTarget.address;
				if(networkAddress == null){
					logger.error("The filed 'address' of target connection "+this.connections.get(i).name+" cannot be null!");
					return false;
				}
				if(!CommonTool.checkPrivateIPaddress(networkAddress)){
					logger.error("The filed 'address' (value='"+networkAddress+"') of target connection "+this.connections.get(i).name+" should be private network address!");
					return false;
				}
	
				//Two connection points of a connection should comes from different nodes.
				if(scpSource.componentName.equals(scpTarget.componentName)){
					logger.error("Two connection points of the connection '"+this.connections.get(i).name+"' should comes from different nodes!");
					return false;
				}
				
			}
		}
		
		if(this.subnets != null){
			for(int i = 0 ; i<this.subnets.size() ; i++){
				String subnet = this.subnets.get(i).subnet;
				String netmask = this.subnets.get(i).netmask;
				
				int netmaskNum = 0;
				if(netmask == null){
					logger.error("Field 'netmask' of subnet "+this.subnets.get(i).name+" must be specified!");
					return false;
				}
				if(netmask.contains(".")){
					if((netmaskNum = CommonTool.netmaskStringToInt(netmask)) == -1){
						logger.error("Field 'netmask' of subnet "+this.subnets.get(i).name+" is not valid!");
						return false;
					}
				}else{
					try {
						netmaskNum = Integer.parseInt(netmask);
						if(netmaskNum<=0 || netmaskNum>=32){
							logger.error("Field 'netmask' of subnet "+this.subnets.get(i).name+" should be between 1 and 31 (included)!");
							return false;
						}
						} catch (NumberFormatException e) {
							logger.error("Field 'netmask' of subnet "+this.subnets.get(i).name+" is not valid!");
							return false;
						}
				}
				this.subnets.get(i).netmask = String.valueOf(netmaskNum);
				
				if(subnet == null){
					logger.error("The filed 'address' of subnet "+this.subnets.get(i).name+" cannot be null!");
					return false;
				}
				if(!CommonTool.checkPrivateIPaddress(subnet)){
					logger.error("The filed 'subnet' (value='"+subnet+"') of subnet "+this.subnets.get(i).name+" should be private subnet!");
					return false;
				}
				String validdationSubent = CommonTool.getSubnet(subnet, netmaskNum);
				if(!subnet.equals(validdationSubent)){
					logger.error("The subnet and netmask number of subnet "+this.subnets.get(i)+" are not valid!");
					return false;
				}
			}
		}
		return true;
	}
	

}
