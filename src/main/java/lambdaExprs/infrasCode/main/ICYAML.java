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
package lambdaExprs.infrasCode.main;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import lambdaExprs.infrasCode.interpreter.ICInterpreter;
import lambdaInfrs.credential.UserCredential;
import lambdaInfrs.database.UserDatabase;
import lambdaInfrs.engine.TEngine.TEngine;
import lambdaInfrs.request.HScalingSTRequest;
import lambdaInfrs.request.HScalingVMRequest;
import lambdaInfrs.request.ProvisionRequest;
import lambdaInfrs.request.VScalingVMRequest;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.output.NullOutputStream;
import org.apache.log4j.Logger;

import topology.description.actual.SubTopologyInfo;
import topology.description.actual.TopTopology;
import topology.description.actual.VM;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.jcabi.ssh.SSH;
import com.jcabi.ssh.Shell;

import commonTool.CommonTool;
import commonTool.TARGZ;

public class ICYAML {
	private static final Logger logger = Logger.getLogger(ICYAML.class);
	
	/**
	 * This tells that how this infrastructure code runs: "LOCAL", running in this local machine without 
	 * a remote controller; "CTRL", all the applications will be controlled by a remote controller. 
	 */
	public String Mode;
	
	public ArrayList<Code> InfrasCodes;
	
	@JsonIgnore
	public UserDatabase userDatabase;
	
	@JsonIgnore
	public UserCredential userCredential;
	
	@JsonIgnore
	public TopTopology topTopology;
	
	@JsonIgnore
	public FileWriter icLogger;
	
	@JsonIgnore
	public HScalingSTRequest hscalSTReq = new HScalingSTRequest();
	
	@JsonIgnore
	public HScalingVMRequest hscalVMReq = new HScalingVMRequest();
	
	@JsonIgnore
	public VScalingVMRequest vscalVMReq = new VScalingVMRequest();
	
	public ICYAML(){
		
	}
	
	public ICYAML(TopTopology topTopology, UserCredential userCredential, UserDatabase userDatabase){
		this.topTopology = topTopology;
		this.userCredential = userCredential;
		this.userDatabase = userDatabase;
	}
	
	
	public boolean loadInfrasCodes(String IC){
		ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        try {
        	ICYAML icYaml = mapper.readValue(new File(IC), ICYAML.class);
        	if(icYaml == null){
        		logger.error("Infrastructure code from " + IC + " is invalid!");
            	return false;
        	}
        	InfrasCodes = icYaml.InfrasCodes;
        	Mode = icYaml.Mode;
        	logger.info("Infrastructure code from " + IC + " is loaded successfully!");
        	return true;
        } catch (Exception e) {
            logger.error(e.toString());
            e.printStackTrace();
            return false;
        }
	}
	
	/**
	 * Execute this infrastructure code according to the mode.
	 * @param appRootDir the root directory of this infrastructure code
	 * @param ctrlPath this is the file path of control agent definition
	 * @param logsDir set the directory of output logs
	 */
	public void execute(String appRootDir, String ctrlFPath, String logsDir){
		if(this.InfrasCodes == null){
			logger.error("Infrastructure code must be initialized first!");
			return ;
		}
		if(logsDir == null || logsDir.trim().equals("")){
			logger.error("Directory of logs must be set!");
			return ;
		}
		
		///by default the mode is remote 
		if(this.Mode != null && this.Mode.trim().equalsIgnoreCase("LOCAL")){
			File logsDirF = new File(logsDir);
			if(!logsDirF.exists())
				logsDirF.mkdir();
			
			String icLogPath = logsDir + "InfrasCode.log";
			FileWriter icLoggerFW = null;
			try {
				icLoggerFW = new FileWriter(icLogPath, false);
				icLoggerFW.write("LOGs:\n");
				icLoggerFW.flush();
			} catch (IOException e1) {
				e1.printStackTrace();
				logger.error("Cannot build log file for "+icLogPath);
				return ;
			}
			this.icLogger = icLoggerFW;
			
			ICInterpreter icInterpreter = new ICInterpreter();
			icInterpreter.execute(this);
			
		}else if(this.Mode == null || this.Mode.trim().equalsIgnoreCase("CTRL")){
			File ctrlF = new File(ctrlFPath);
			if(!ctrlF.exists()){
				System.out.println("ERROR! Control agent must be defined in 'CTRL' mode!");
				return ;
			}
			
			ProvisionRequest pq = new ProvisionRequest();
			pq.content.put("_ctrl", false); 
			
			TEngine tEngine = new TEngine();
			
			if( !tEngine.provision(topTopology, userCredential, userDatabase, pq) ){
				logger.error("The controller cannot be provisioned!");
				return ;
			}
			
			////Get the IP address of the controller if it is running
			SubTopologyInfo ctrlST = topTopology.getSubtopology("_ctrl");
			if(ctrlST == null || !ctrlST.status.trim().equals("running")){
				logger.error("The controller is not provisioned! EXIT!");
				return ;
			}
			VM ctrlVM = ctrlST.subTopology.getVMinSubClassbyName("ctrl");
			String tmpFilePath = System.getProperty("java.io.tmpdir") + File.separator + "AppInfs.tar.gz";
			
			try {
				TARGZ.compress(tmpFilePath, new File(appRootDir));
			} catch (IOException e) {
				e.printStackTrace();
				return ;
			}
			
			String rootDirName = CommonTool.getDirName(appRootDir);
			
			Shell shell;
			try {
				shell = new SSH(ctrlVM.publicAddress, 22, ctrlVM.defaultSSHAccount, ctrlST.subTopology.accessKeyPair.privateKeyString);
				File appTARGZ = new File(tmpFilePath);
				String appDir = "/root/" + System.currentTimeMillis() ;
				new Shell.Safe(shell).exec(
						  "sudo mkdir "+ appDir + "/",
						  null,
						  new NullOutputStream(), new NullOutputStream()
						);
				new Shell.Safe(shell).exec(
						  "sudo cat > "+ appDir + "/AppInfs.tar.gz",
						  new FileInputStream(appTARGZ),
						  new NullOutputStream(), new NullOutputStream()
						);
				logger.warn("Now application is deploying and running on the remote! You can terminate this and check the info on remote controller!");
				new Shell.Safe(shell).exec(
						  "sudo nohup java -jar /root/CloudsStorm.jar go "+ appDir + "/AppInfs.tar.gz "+rootDirName+" &",
						  null,
						  new NullOutputStream(), new NullOutputStream()
						);
				FileUtils.deleteQuietly(appTARGZ);
			} catch (IOException e) {
				e.printStackTrace();
				return ;
			}
			
		}else
			logger.error("Unrecognized 'Mode' for executing infrastructure code!");
	}
	
	/**
	 * Execute the infrastructure code only in 'LOCAL' mode ignore the definition.
	 * @param logsDir
	 */
	public void run(String logsDir){
		File logsDirF = new File(logsDir);
		if(!logsDirF.exists())
			logsDirF.mkdir();
		
		String icLogPath = logsDir + "InfrasCode.log";
		FileWriter icLoggerFW = null;
		try {
			icLoggerFW = new FileWriter(icLogPath, false);
			icLoggerFW.write("LOGs:\n");
			icLoggerFW.flush();
		} catch (IOException e1) {
			e1.printStackTrace();
			logger.error("Cannot build log file for "+icLogPath);
			return ;
		}
		this.icLogger = icLoggerFW;
		
		ICInterpreter icInterpreter = new ICInterpreter();
		icInterpreter.execute(this);
		
	}
}
