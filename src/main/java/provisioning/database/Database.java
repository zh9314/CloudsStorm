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
package provisioning.database;

import java.util.Map;


public abstract class Database {

	public Map<String, String> extraInfo;
	
	public abstract DCMetaInfo getDCMetaInfo(String domain);
	
	public abstract String getEndpoint(String domain);
	
	public abstract VMMetaInfo getVMMetaInfo(String domain, String OS, String vmType);
	
	/**
	 * Find the most close VM type in this domain. The unit for memory must be 'G'
	 * @param domain
	 * @param OS
	 * @param vCPUNum
	 * @param mem
	 * @return
	 */
	public abstract String getVMType(String domain, String OS, double vCPUNum, double mem);
}
