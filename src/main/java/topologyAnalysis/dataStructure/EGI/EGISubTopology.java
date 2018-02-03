package topologyAnalysis.dataStructure.EGI;

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
import topologyAnalysis.dataStructure.SubTopology;
import topologyAnalysis.dataStructure.VM;
import topologyAnalysis.method.SubTopologyMethod;

public class EGISubTopology extends SubTopology implements SubTopologyMethod{
	
private static final Logger logger = Logger.getLogger(EGISubTopology.class);
	
	//Indicate different VMs.
	public ArrayList<EGIVM> VMs;
	
	public EGISubTopology(){
		
	}

	@Override
	public boolean loadSubTopology(String topologyPath) {
		ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        try {
        EGISubTopology egiSubTopology = mapper.readValue(new File(topologyPath), EGISubTopology.class);
        	if(egiSubTopology == null){
        		logger.error("Sub-topology from "+topologyPath+" is invalid!");
            	return false;
        	}
        	this.loadingPath = topologyPath;
        	this.subnets = egiSubTopology.subnets;
        	this.connections = egiSubTopology.connections;
        	this.VMs = egiSubTopology.VMs;
        	logger.info("Sub-topology of EGI from "+topologyPath+" is loaded without validation successfully!");
        } catch (Exception e) {
            logger.error(e.toString());
            e.printStackTrace();
            return false;
        }
        return true;
	}

	@Override
	public void setTopologyInformation(String topologyName) {
		this.topologyName = topologyName;
		this.topologyType = "EGI";
	}

	@Override
	public VM getVMinSubClassbyName(String vmName) {
		if(this.VMs == null)
			return null;
		for(int i = 0 ; i<VMs.size() ; i++){
			if(VMs.get(i).name.equals(vmName)){
				return VMs.get(i);
			}
		}
		return null;
	}

	@Override
	public ArrayList<VM> getVMsinSubClass() {
		if(this.VMs == null || VMs.size() == 0)
			return null;
		ArrayList<VM> vms = new ArrayList<VM>();
		for(int i = 0 ; i<VMs.size() ; i++)
			vms.add(VMs.get(i));
		return vms;
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
	public boolean formatChecking(String topologyStatus) {
		if(!super.commonFormatChecking(topologyStatus))
			return false;
		
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
			
			//checking the subnet, EGI cannot support subnet 
			if(this.subnets != null){
				logger.error("EGI Clouds don't support the definition of 'subnet'");
				return false;
			}
		}
		return true;
	}

}
