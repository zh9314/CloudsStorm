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
package topology.dataStructure;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * This is a member definition of the VM in one subnet.	
 * @author huan
 *
 */
public class Member {
	/**
	 * The name in the definition of SubTopology, such as "hadoop_3nodes_1.Node1".
	 */
	public String vmName;
	
	/**
	 * This is the real VM name
	 */
	@JsonIgnore
	public String absVMName;
	
	/**
	 * The IP address of this VM in this subnet.
	 */
	public String address;
	
	/**
	 * Indicate the adjacent VMs that connects with this VM.
	 * In order to create the non-logic connection according to the 
	 * subnet. The key is also the VM full name.
	 */
	@JsonIgnore
	public Map<String, Member> adjacentNodes = new HashMap<String, Member>();
}
