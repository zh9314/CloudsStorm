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
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import lambdaInfrs.credential.SSHKeyPair;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import topology.analysis.method.SubTopologyMethod;
import topology.analysis.method.TopologyMethod;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import commonTool.ClassDB;
import commonTool.CommonTool;
import commonTool.Values;

public abstract class SubTopology implements SubTopologyMethod, TopologyMethod, Cloneable{
	
	private static Logger logger = Logger.getLogger(SubTopology.class);
	
	//The path of the file where this topology object loads.
	//Used for controlling information output.
	@JsonIgnore
	public String loadingPath;
	
	//The sub-topology name is defined in the top level description.
	@JsonIgnore
	public String topologyName;
	
	/**
	 * Indicate what the Cloud of this sub-topology is. Indicate the cloud provider of this sub-topology. 
	 * For instance, ec2, exogeni and egi.
	 */
	@JsonIgnore
	public String cloudProvider;
	
	/**
	 * Get this information from the SubTopologyInfo, in order 
	 * to check and get the member variables in the sub-class of 
	 * this SubTopology.
	 */
	@JsonIgnore
	public String subTopologyClass;

    /**
     * Specify the S-Engine Class to handle this sub-topology. Useful for extension to currently unsupported Clouds.
     * If it is null, than S-Engine for current supported Clouds will be loaded by default.
     */
    public String SEngineClass;
    
    /**
     * Some extra key-value information needed for some specified Cloud. 
     * This is useful for extension.
     */
	public Map<String, String> extraInfo;
	
	@JsonIgnore
	public SSHKeyPair accessKeyPair;
	
	
	/**
	 * This is the common format checking, 
	 * no matter where the sub-topology comes from. 
	 * This is invoked at the beginning of the formatChecking(String) method in sub classes. 
	 * The checking items includes: <br/>
	 * 1. Make sure that there is 'VMs' defined in the sub-class.
	 * 2. Checking all the node names whether in a correct format, which should not exceed 20 characters 
	 * and do not contain '.' or '_'.
	 * 3. All the node names should be different. <br/>
	 * 4. The VM cannot have public address, if the status of the sub-topology
	 * is 'fresh'.
	 * 5. The VM must have public address, if the status of the sub-topology 
	 * is 'running'.
	 * 
	 * Input is the status of the sub-topology. 
	 * @return
	 */
	@JsonIgnore
	public final boolean commonFormatChecking(String topologyStatus){
		logger.info("Validation on the sub-topology '"+this.topologyName+"'");
		
		if(topologyStatus == null)
			return false;
		
		//// to make sure that there is 'VMs' field defined in the sub-topology
		//// this is essential to the sub-topology definition
		Class<?> XSubTopology = ClassDB.getSubTopology(
									cloudProvider , this.subTopologyClass);
		try {
			if(XSubTopology.getField("VMs") == null){
				logger.error("VM must be defined in the sub-topology '"
							+topologyName+"' description!");
				return false;
			}
		} catch (NoSuchFieldException | SecurityException e) {
			logger.error(e.getMessage());
			e.printStackTrace();
			return false;
		}
		
		//check all the basic information of the VMs.
		ArrayList<VM> vms = this.getVMsinSubClass();
		if(vms == null){
			logger.error("At least one VM must be defined in a sub-topology!");
			return false;
		}
		
		for(int i = 0 ; i<vms.size() ; i++){
			//Checking the VM name
			VM curVM = vms.get(i);
			String vn = curVM.name;
			if(vn == null || vn.trim().equals("")){
				logger.error("The VM name must be specified and cannot be set as 'null'!");
				return false;
			}
			if(vn.contains(".") || vn.contains("_")){
				logger.error("The VM name '"+vn+"' should not contain '.' or '_'!");
				return false;
			}
			if(vn.length() > 20){
				logger.error("The VM name '"+vn+"' should not exceed 20 characters!");
				return false;
			}
			
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
			
			if(topologyStatus.equals(Values.STStatus.fresh) && (curVM.publicAddress != null)){
				logger.error("VM '"+curVM.name+"' cannot have public address in 'fresh' status!");
				return false;
			}
			
			if(topologyStatus.trim().equalsIgnoreCase(Values.STStatus.fresh)
					|| topologyStatus.trim().equalsIgnoreCase(Values.STStatus.deleted)){
				if(curVM.fake != null && curVM.fake.trim().equalsIgnoreCase("true")){
					logger.error("There cannot be 'fake' VM in the status of 'fresh' or 'deleted'!");
					return false;
				}else
					curVM.fake = null;
			}
			
			if(curVM.fake != null && curVM.fake.trim().equalsIgnoreCase("true"))
				continue;
			if(topologyStatus.equals("running") && (curVM.publicAddress == null)){
				logger.error("VM '"+curVM.name+"' must have public address in 'running' status!");
				return false;
			}
		}
		return true;
	}


	@Override   @JsonIgnore
	public Map<String, String> generateUserOutput() {
		Map<String, String> output = new HashMap<String, String>();
		ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
		try {
	    		String yamlString = mapper.writeValueAsString(this);
			String content = "";
	        	String [] lines = yamlString.split("\\\n");
	        	for(int i = 0 ; i<lines.length ; i++){
	        		if(lines[i].contains("null"))
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


	@Override  @JsonIgnore
	public Map<String, String> generateControlOutput() {
		Map<String, String> output = new HashMap<String, String>();
		ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
		try {
	    		String yamlString = mapper.writeValueAsString(this);
			String content = "";
	        	String [] lines = yamlString.split("\\\n");
	        	for(int i = 0 ; i<lines.length ; i++){
	        		if(lines[i].contains("null"))
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


	@Override  @JsonIgnore
	public boolean overwirteControlOutput() {
		ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
		try {
	    		FileWriter yamlFileOut = new FileWriter(this.loadingPath, false);
	    		String yamlString = mapper.writeValueAsString(this);
	        	yamlFileOut.write(yamlString);
	        	yamlFileOut.close();
	        	
	        	if(!this.saveSSHKeys())
	        		return false;
	    	
		} catch (IOException e) {
			e.printStackTrace();
			logger.error(e.getMessage());
			return false;
		}
		return true;
	}
	
 	/**
 	 * Write the ssh key pair to files, if needed. This key pair
 	 * is used for accessing the sub-topology
 	 * in this Data center.
 	 * @return
 	 */
	@JsonIgnore
	public boolean saveSSHKeys(){
		try{
			SSHKeyPair curKey = 	this.accessKeyPair;
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
                                priKeyFile.setReadOnly();
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


	@Override  @JsonIgnore
	public boolean formatChecking(String topologyStatus) {
		
		return true;
	}

	 @JsonIgnore
	public boolean setVMsInSubClass(ArrayList<VM> setVMs){
		Class<?> XSubTopology = ClassDB.getSubTopology(
										cloudProvider , this.subTopologyClass);
		try {
			Field XVMs = XSubTopology.getDeclaredField("VMs");
			if(XVMs == null){
				logger.error("VM must be defined in the sub-topology '"
							+topologyName+"' description!");
				return false;
			}
			XVMs.setAccessible(true);
			XVMs.set(this, setVMs);
		} catch (NoSuchFieldException | SecurityException 
				| IllegalArgumentException | IllegalAccessException e) {
			logger.error(e.getMessage());
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	@JsonIgnore
	public boolean addVMInSubClass(VM newVM){
		Class<?> XSubTopology = ClassDB.getSubTopology(
									cloudProvider , this.subTopologyClass);
		try {
			Field XVMs = XSubTopology.getDeclaredField("VMs");
			if(XVMs == null){
				logger.error("VM must be defined in the sub-topology '"
							+topologyName+"' description!");
				return false;
			}
			Object VMList = XVMs.get(this);
			Method add = ArrayList.class.getDeclaredMethod("add", Object.class);
			add.invoke(VMList, newVM);
		} catch (NoSuchFieldException | SecurityException 
				| IllegalArgumentException | IllegalAccessException 
				| NoSuchMethodException | InvocationTargetException e) {
			logger.error(e.getMessage());
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	@JsonIgnore
	public boolean removeVMInSubClass(VM rmVM){
		Class<?> XSubTopology = ClassDB.getSubTopology(
									cloudProvider , this.subTopologyClass);
		try {
			Field XVMs = XSubTopology.getDeclaredField("VMs");
			if(XVMs == null){
				logger.error("VM must be defined in the sub-topology '"
							+topologyName+"' description!");
				return false;
			}
			Object VMList = XVMs.get(this);
			Method rm = ArrayList.class.getDeclaredMethod("remove", Object.class);
			rm.invoke(VMList, rmVM);
		} catch (NoSuchFieldException | SecurityException 
				| IllegalArgumentException | IllegalAccessException 
				| NoSuchMethodException | InvocationTargetException e) {
			logger.error(e.getMessage());
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	/**
	 * Get the VM Class path in this type of sub-topology 
	 * @return
	 */
	@JsonIgnore
	public Class<?> getVMClass(){
		Class<?> XSubTopology = ClassDB.getSubTopology(
									cloudProvider, this.subTopologyClass);
		try {
			Field XVMs = XSubTopology.getDeclaredField("VMs");
			if(XVMs == null){
				logger.error("VM must be defined in the sub-topology '"
							+topologyName+"' description!");
				return null;
			}
			ParameterizedType VMListType = (ParameterizedType) XVMs.getGenericType();
			Class<?> VMClass = (Class<?>) VMListType.getActualTypeArguments()[0];
			return VMClass;
		} catch (NoSuchFieldException | SecurityException 
				| IllegalArgumentException e) {
			logger.error(e.getMessage());
			e.printStackTrace();
			return null;
		}
	}
	
	@Override @JsonIgnore
	public VM getVMinSubClassbyName(String vmName)
	{
		Class<?> XSubTopology = ClassDB.getSubTopology(
									cloudProvider, this.subTopologyClass);
		try {
			Field XVMs = XSubTopology.getField("VMs");
			if(XVMs == null){
				logger.error("VM must be defined in the sub-topology '"
							+topologyName+"' description!");
				return null;
			}
			@SuppressWarnings("unchecked")
			ArrayList<VM> vms = (ArrayList<VM>)XVMs.get(this);
			if(vms == null)
				return null;
			for(int vi = 0 ; vi < vms.size() ; vi++){
				if(vms.get(vi).name.equals(vmName))
					return vms.get(vi);
			}
		} catch (NoSuchFieldException | SecurityException 
				| IllegalArgumentException | IllegalAccessException e) {
			logger.error(e.getMessage());
			e.printStackTrace();
			return null;
		}
		return null;
	}
	
	@Override  @JsonIgnore
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


	@Override @JsonIgnore
	public  ArrayList<VM> getVMsinSubClass() 
	{
		Class<?> XSubTopology = ClassDB.getSubTopology(
									cloudProvider , this.subTopologyClass);
		try {
			Field XVMs = XSubTopology.getField("VMs");
			if(XVMs == null){
				logger.error("VM must be defined in the sub-topology '"
							+topologyName+"' description!");
				return null;
			}
			@SuppressWarnings("unchecked")
			ArrayList<VM> vms = (ArrayList<VM>)XVMs.get(this);
			return vms;
		} catch (NoSuchFieldException | SecurityException 
				| IllegalArgumentException | IllegalAccessException e) {
			logger.error(e.getMessage());
			e.printStackTrace();
			return null;
		}
	}
	
	@Override @JsonIgnore
	public Object clone() {  
		try {
			return super.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}  
		return null;
	}  

}
