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
package lambdaInfrs.credential;

import java.io.File;
import java.util.Map;

import org.apache.log4j.Logger;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import commonTool.ClassDB;

public class CredentialInfo {
	
	private static final Logger logger = Logger.getLogger(CredentialInfo.class);
	
	public String cloudProvider;
	
	/**
	 * This is used to identify the class path of the specific credential class 
	 * for the Cloud. It is only useful when the application wants to extend the credential 
	 * for its own Cloud. If it is 'null', than it is used the default Class path currently 
	 * supported.
	 */
	public String credClass;
	
	/**
	 * The content of the credential. It must be in the same folder.
	 */
	public String credInfoFile;
	
	public boolean loadCredential(String credInfoPath, 
				Map<String, Credential> credentials) {
		if(cloudProvider == null){
			logger.error("Cloud provider must be set!");
			return false;
		}
		String cp = cloudProvider.trim().toLowerCase();
		ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
		Class<?> XCredential = ClassDB.getCredential(cp, this.credClass);
		if(XCredential == null){
			logger.error("Cannot load credential class for Cloud '"
						+cloudProvider+"' with Class name '"+credClass+"'!");
			return false;
		}
		Object xCredential = null;
		try {
			xCredential = XCredential.newInstance();
			xCredential = mapper.readValue(new File(credInfoPath), XCredential);
	        	if(xCredential == null){
	        		logger.error("Users's credential format from "
	        						+xCredential+" is wrong!");
	            	return false;
	        	}
	        	if( !((Credential)xCredential).validateCredential(credInfoPath) ){
	        		logger.error("Users's credential from "
    						+xCredential+" is not validated!");
	        		return false;
	        	}
		 }catch (Exception e) {
             logger.error(e.getMessage());
             e.printStackTrace();
             return false;
         }
		credentials.put(cp, (Credential)xCredential);
		return true;
	}
}
