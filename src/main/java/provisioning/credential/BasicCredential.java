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

import java.util.Map;

import org.apache.log4j.Logger;

public class BasicCredential extends Credential{
	
	private static final Logger logger = Logger.getLogger(BasicCredential.class);
	

	@Override
	public boolean validateCredential(String credInfoPath) {
		if(credInfo == null)
			return true;
		for(Map.Entry<String, String> entry: credInfo.entrySet())
			if(entry.getValue() == null){
				logger.error("Value of "+entry.getKey()+" should not be 'null'");
				return false;
			}

		return true;
	}


}
