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

import topology.dataStructure.ConnectionPoint;

import com.fasterxml.jackson.annotation.JsonIgnore;

//This is the connection point definition in the top level description file.
public class ActualConnectionPoint extends ConnectionPoint{

	
	//Used for recording the eth name on the VM.
	//When the other part of this connection is failed. We need deleted this ethName.
	//Originally, it should be null. This field is just used for controlling.
	public String ethName;
	
	//Point to the VM that this connection point belongs to. 
	//Main goal is to get the public address after provisioning.
	@JsonIgnore
	public VM belongingVM;
	
	/**
	 * The name of the sub-topology which this VM
	 * belongs to.
	 */
	@JsonIgnore
	public String belongingSubT;
	
	
	//Record the address of the peer in this connection.
	//Used for generating the available scaling pool.
	//@JsonIgnore
	//public String peerAddress;
	
	//Record the address of the peer in this connection.
	//Used for generating the available scaling pool.
	@JsonIgnore
	public ActualConnectionPoint peerACP;
	
}