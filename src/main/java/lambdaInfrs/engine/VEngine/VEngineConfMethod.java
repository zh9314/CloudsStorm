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
package lambdaInfrs.engine.VEngine;

import topology.description.actual.VM;

/**
 * Mainly used for configuring the VMs to be connected with each other
 * as the developer designs and running the scripts, etc.
 * This interface is more OS related.
 */
public interface VEngineConfMethod {
	
	

	/**
	 * Configuration on the connection to leverage some type of
	 * VNF technique to manage the network. Hence, the private network
	 * is provisioned among public Clouds.  
	 */
	public boolean confVNF(VM subjectVM);
	
	/**
	 * Configuration on the application-defined
	 * SSH account and public keys
	 * @param subjectVM
	 * @param account
	 * @param publicKey the public key provided by the application associated with
	 * this SSH account.
	 * @return
	 */
	public boolean confSSH(VM subjectVM);
	
	/**
	 * Run the application-defined script to configure the VM
	 * environment.
	 * @param subjectVM
	 * @param currentDir Used for generating the log file of executing the script on that VM
	 * @return
	 */
	public boolean confENV(VM subjectVM);
	
	/**
	 * This method is to detach from failed sub-topologies. 
	 * It removes all the connections with the failed 
	 * sub-topologies.
	 */
	public boolean detach(VM subjectVM);
	
}
