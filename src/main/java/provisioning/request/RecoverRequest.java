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
package provisioning.request;

import java.util.HashMap;
import java.util.Map;

import commonTool.ClassSet;

public class RecoverRequest {
	
	public class RecoverReqEle {
		
		////These two are the target datacenter, where this sub-topology is recovered. 
		public String cloudProvider;
		public String domain;
		
		///Identify the topology name that needs to be recovered
		public String topologyName;
		
		////this contains all the possible classes might be needed
		public ClassSet scaledClasses;
	}
	
	///The key identifies the topology name that needs to be recovered
	///Value means whether this request can be satisfied
	public Map<RecoverReqEle, Boolean> content = new HashMap<RecoverReqEle, Boolean>();
}
