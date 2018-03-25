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
package infrastructureCode.main;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import provisioning.credential.UserCredential;
import provisioning.database.UserDatabase;
import provisioning.request.HScalingSTRequest;
import provisioning.request.HScalingVMRequest;
import provisioning.request.VScalingVMRequest;
import topology.description.actual.TopTopology;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

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
	
	public ICYAML(TopTopology topTopology, UserCredential userCredential, UserDatabase userDatabase
			,FileWriter icLogger){
		this.topTopology = topTopology;
		this.userCredential = userCredential;
		this.userDatabase = userDatabase;
		this.icLogger = icLogger;
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
	
	public void setICLogger(FileWriter iclogger){
		this.icLogger = iclogger;
	}
}
