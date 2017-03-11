package topologyAnalysis.dataStructure.EC2;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import provisioning.credential.SSHKeyPair;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import commonTool.CommonTool;
import topologyAnalysis.dataStructure.Eth;
import topologyAnalysis.dataStructure.SubTopology;
import topologyAnalysis.dataStructure.VM;
import topologyAnalysis.method.SubTopologyMethod;

public class EC2SubTopology extends SubTopology implements SubTopologyMethod{
	
	private static final Logger logger = Logger.getLogger(EC2SubTopology.class);
	
	//Indicate different VMs.
	public ArrayList<EC2VM> components;
	
	public EC2SubTopology(){
		
	}

	@Override
	public boolean loadSubTopology(String topologyPath) {
		ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        try {
        	EC2SubTopology ec2SubTopology = mapper.readValue(new File(topologyPath), EC2SubTopology.class);
        	if(ec2SubTopology == null){
        		logger.error("Sub-topology from "+topologyPath+" is invalid!");
            	return false;
        	}
        	this.loadingPath = topologyPath;
        	this.subnets = ec2SubTopology.subnets;
        	this.connections = ec2SubTopology.connections;
        	this.components = ec2SubTopology.components;
        	logger.info("Sub-topology of EC2 from "+topologyPath+" is loaded without validation successfully!");
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
					if(contents[0].trim().equals("vcpId") ||
						contents[0].trim().equals("subnetId") ||
						  contents[0].trim().equals("securityGroupId"))
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
        	
        	////Write the ssh key pair to files, if needed
        SSHKeyPair curKey = 	((SubTopology)this).accessKeyPair;
        if(curKey == null)
        		logger.info("There is no ssh key for sub-topology '"+this.topologyName+"'");
        else{
	        	String currentDir = CommonTool.getPathDir(this.loadingPath);
	        	if(curKey.SSHKeyPairId == null){
	        		logger.error("Invalid key pair because of 'null' keyPairId!");
	        		return false;
	        	}
	        	String keyDirPath = currentDir+curKey.SSHKeyPairId+File.separator;
	        	File keyDir = new File(keyDirPath);
	        	if(keyDir.exists()){
	        		logger.info("The key pair '"+curKey.SSHKeyPairId+"' has already been stored!");
	        		return true;
	        	}
	        	if(!keyDir.mkdir()){
	        		logger.error("Cannot create directory "+keyDirPath);
	        		return false;
	        	}
	        	if(curKey.privateKeyString == null){
	        		logger.error("Invalid ssh key pairs ("+curKey.SSHKeyPairId+") because of 'null' privateKeyString!");
	        		return false;
	        	}
	        	if(curKey.publicKeyId == null && curKey.publicKeyString == null){
	        		logger.equals("Invalid ssh key pairs ("+curKey.SSHKeyPairId+") because of 'null' publicKeyString or publicKeyId!");
	        		return false;
	        	}
	        	File priKeyFile = new File(keyDirPath+"id_rsa");
	        	FileUtils.writeStringToFile(priKeyFile, curKey.privateKeyString, "UTF-8", false);
	        	
	        	if(curKey.publicKeyId != null){
	        		File pubKeyIdFile = new File(keyDirPath+"name.pub");
	        		FileUtils.writeStringToFile(pubKeyIdFile, 
	        				curKey.publicKeyId, "UTF-8", false);
	        	}
	        	
	        	if(curKey.publicKeyString != null){
	        		File pubKeyFile = new File(keyDirPath+"id_rsa.pub");
	        		FileUtils.writeStringToFile(pubKeyFile, 
	        				curKey.publicKeyString, "UTF-8", false);
	        	}
        }
   
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
		this.topologyType = "EC2";
		this.provisioningAgentClassName = "provisioning.engine.SEngine.EC2SEngine";
		
	}
	

	/**
	 * Specific checking for EC2 sub-topology.
	 * The checking items includes: <br/>
	 * 1. The 'instanceId', 'vpcId', 'subnetId' and 'securityGroupId' should be null, 
	 * if the topology status is 'fresh'. <br/>
	 * 2. The 'instanceId', 'vpcId', 'subnetId' and 'securityGroupId' must not be null, 
	 * if the topology status is 'running'. <br/>
	 * 3. If the 'diskSize' is null, then the default value is 8. <br/>
	 * 4. The value of 'diskSize', whose unit is GigaBytes, must be positive. 
	 * For EC2, the 'diskSize' cannot be smaller than 8.  <br/>
	 * 5. One VM can only belong to one subnet. <br/>
	 * 
	 */
	@Override
	public boolean formatChecking(String topologyStatus) {
		if(!super.commonFormatChecking(topologyStatus))
			return false;
		
		for(int vmi = 0 ; vmi < this.components.size() ; vmi++){
			EC2VM curVM = components.get(vmi);
			if(topologyStatus.equals("fresh")){
				if(curVM.instanceId != null
					|| curVM.securityGroupId != null
					|| curVM.vpcId != null
					|| curVM.subnetId != null){
					logger.error("Some information in VM '"+curVM.name+"' cannot be defined in a 'fresh' sub-topology!");
					return false;
				}
			}
			
			if(topologyStatus.equals("running")){
				if(curVM.instanceId == null
					|| curVM.securityGroupId == null
					|| curVM.vpcId == null
					|| curVM.subnetId == null){
					logger.error("Some information in VM '"+curVM.name+"' must be defined in a 'running' sub-topology!");
					return false;
				}
			}
			
			if(curVM.diskSize == null){
				curVM.diskSize = "8";
			}else{
				try {
					int diskSize = Integer.parseInt(curVM.diskSize);
					if(diskSize<8){
						logger.error("The minimum number for field 'diskSize' of EC2VM '"+curVM.name+"' is 8!");
						return false;
					}
					if(diskSize > 16000){   ///Maxium is 16TB.
						logger.error("Field 'diskSize' of EC2VM '"+curVM.name+"' out of the range according to current capability!");
						return false;
					}
					} catch (NumberFormatException e) {
						logger.error("Field 'diskSize' of EC2VM '"+curVM.name+"' must be a positive number!");
						return false;
					}
			}
			
			//check the IOPS
			if(curVM.IOPS == null){
				curVM.IOPS = "0";
			}else{
				try {
					int IOPS = Integer.parseInt(curVM.IOPS);
					if(IOPS<0){
						logger.error("Field 'IOPS' of EC2VM '"+curVM.name+"' must be positive!");
						return false;
					}
					if(IOPS>20000){
						logger.error("Field 'IOPS' of EC2VM '"+curVM.name+"' out of the range according to current capability!");
						return false;
					}
					} catch (NumberFormatException e) {
						logger.error("Field 'IOPS' of EC2VM '"+curVM.name+"' must be a positive number!");
						return false;
					}
			}
			
			//checking the subnet
			int subnetCount = 0;
			for(int ei = 0 ; ei<curVM.ethernetPort.size() ; ei++){
				Eth curEth = curVM.ethernetPort.get(ei);
				if(curEth.subnet != null)
					subnetCount++;
			}
			if(subnetCount > 1){
				logger.error("One VM cannot be put into two subnets on EC2!");
				return false;
			}
			
			
		}
		
		return true;
	}

	@Override
	public VM getVMinSubClassbyName(String vmName) {
		if(this.components == null)
			return null;
		for(int i = 0 ; i<components.size() ; i++){
			if(components.get(i).name.equals(vmName)){
				return components.get(i);
			}
		}
		return null;
	}
	
	@Override
	public ArrayList<VM> getVMsinSubClass() {
		if(this.components == null || components.size() == 0)
			return null;
		ArrayList<VM> vms = new ArrayList<VM>();
		for(int i = 0 ; i<components.size() ; i++)
			vms.add(components.get(i));
		return vms;
	}



}
