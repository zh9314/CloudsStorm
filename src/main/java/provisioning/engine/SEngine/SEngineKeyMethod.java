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
package provisioning.engine.SEngine;

import provisioning.credential.Credential;
import provisioning.database.Database;
import topology.description.actual.SubTopologyInfo;

public interface SEngineKeyMethod {
	
	/**
	 * This method should always be invoked before real provisioning.<br/>
	 * All the VMs also should be checked here. If the VM is checked before provisioning 
	 * in its VEngine, it is too late. Because some VM may start and others are not, which is 
	 * not be able to synchronize. 
	 * For different cloud providers, there may be some different checking and updating items.
	 *
	 */
	public boolean runtimeCheckandUpdate(SubTopologyInfo subTopologyInfo, Database database);
	
	/**
	 * This method is used to automatically generate a SSH key for application to access the VM 
	 * in this data center. By default, this is done locally.
	 * @param subTopologyInfo
	 * @return
	 */
	public boolean createAccessSSHKey(SubTopologyInfo subTopologyInfo,
			Credential credential, Database database);
	
	
	/**
	 * This is a method to provision the sub-topology specific to some Cloud.
	 * The status of the sub-topology will be from
	 * 'fresh' -> 'running'  
	 * or 'fresh' -> 'failed'
	 * @param subTopologyInfo one of the sub-topology defined in the description files.
	 * credential contains the credentials information to operate the Cloud.
	 * database contains the information of the Cloud, AMI for instance.
	 * @return successful or not. If the provisioning is not successful, 
	 * the error log information can be found in the file, which is set by 
	 * {@link commonTool.Log4JUtils#setErrorLogFile(String) setErrorLogFile}. If the 
	 * provisioning is successful, the controlling information will be written back 
	 * to the original files, for example public address, instance id etc. 
	 */
	public boolean provision(SubTopologyInfo subTopologyInfo, Credential credential, Database database);
	
	
	/**
	 * Configure the network connections
	 * @param subTopologyInfo
	 * @param credential
	 * @param database
	 * @return
	 */
	public boolean networkConf(SubTopologyInfo subTopologyInfo, Credential credential, Database database);
	
	/**
	 * 
	 * Install the application-defined script to configure the VM environment 
	 * for the application to run.
	 * @param subTopologyInfo
	 * @param credential
	 * @param database
	 * @return
	 */
	public boolean envConf(SubTopologyInfo subTopologyInfo, Credential credential, Database database);
	
	
	/**
	 * When detect some sub-topology is unavailable which
	 * indicates that data center is down. Mark the sub-topology as failed. 
	 * Make the 'ethName' as null, in order to make other sub-topologies to know 
	 * that this connection is closed. when this tunnel 
	 * is connected to the failed sub-topology.  
	 * @param subTopologyInfo
	 * @param credential
	 * @param database
	 * @return
	 */
	public boolean markFailure(SubTopologyInfo subTopologyInfo,
			Credential credential, Database database);
	
	/**
	 * Detach this sub-topology from all the other failed or deleted sub-topologies.  
	 * @param subTopologyInfo
	 * @param credential
	 * @param database
	 * @return
	 */
	public boolean detach(SubTopologyInfo subTopologyInfo,
			Credential credential, Database database);
	
	/**
	 * This is a method demonstrate whether this cloud provider 
	 * can support the feature of "stop". By default, the value is returned from 
	 * the application-defined XVEngine.supportStop()
	 * @return
	 */
	public boolean supportStop(SubTopologyInfo subTopologyInfo);
	
	
	/**
	 * This is a method to stop the sub-topology. All the issues related with 
	 * the sub-topology will also be stopped.
	 * @return 
	 */
	public boolean stop(SubTopologyInfo subTopologyInfo, 
			Credential credential, Database database);
	
	/**
	 * This is a method to start the sub-topology. 
	 * @return 
	 */
	public boolean start(SubTopologyInfo subTopologyInfo, 
			Credential credential, Database database);
	
	/**
	 * This is a method to delete the sub-topology. 
	 * @return 
	 */
	public boolean delete(SubTopologyInfo subTopologyInfo, 
			Credential credential, Database database);
	
	/**
	 * This tells whether this sub-topology supports separate management 
	 * for its VMs. Default value is true.
	 * @return 
	 */
	public boolean supportSeparate();


}
