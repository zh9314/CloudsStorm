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
package provisioning.credential;

import java.io.File;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonIgnore;
import commonTool.CommonTool;

public class EGICredential extends BasicCredential{
	
	private static final Logger logger = Logger.getLogger(EGICredential.class);
	@JsonIgnore
	public String proxyFilePath;
	@JsonIgnore
	public String trustedCertPath;
	
	public String proxyFileName;
	public String trustedCertDirName;
	
	@Override
	public boolean validateCredential(String credInfoPath) {
		
		if(!super.validateCredential(credInfoPath))
			return false;
		
		String curDir = CommonTool.getPathDir(credInfoPath);
		this.proxyFilePath = curDir + this.proxyFileName;
		this.trustedCertPath = curDir + this.trustedCertDirName;
		File certf = new File(this.proxyFilePath);
		if(!certf.exists()){
			logger.error("Cert file "+this.proxyFileName+" does not exist!");
            return false;
		}
		certf = new File(this.trustedCertPath);
		if(!certf.exists()){
			logger.error("Cert file "+this.trustedCertPath+" does not exist!");
            return false;
		}
		return true;
	}
}
