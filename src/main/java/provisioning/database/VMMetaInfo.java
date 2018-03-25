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

public abstract class VMMetaInfo {
	/**
	 * For example, 'ubuntu 14.04'
	 */
	public String OS;
	
	/**
	 * A positive number to identify how many vcores. 
	 */
	public String CPU;
	
	/**
	 * A number to identify how much memory, the unit is 'G'.
	 */
	public String MEM;
	
	/**
	 * The type of this VM for this Cloud, such as medium, small. 
	 * However, they have different names for different Clouds.
	 */
	public String VMType;
	
	/**
	 * Counted by dollars per hour.
	 */
	public String Price;
	
	/**
	 * Used for ssh to login when provisioned by default.
	 */
	public String DefaultSSHAccount;
	
	///reserved for performance model
	//public String ProvisionCost;
	
	/**
	 * For EC2 put:
	 * AMI
	 * 
	 * For ExoGENI put:
	 * OSurl
	 * OSguid
	 * DiskSize
	 * 
	 * For EGI put:
	 * OS_occi_ID
	 * OS_occi_ID
	 */
	public Map<String, String> extraInfo;
	
}
