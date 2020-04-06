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
package lambdaInfrs.request;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import commonTool.ClassSet;

///This class can be either scaling-up request.
///Currently, this request specify several VMs to scale up
public class HScalingVMRequest {
	
	public class VMScalingReqEle {
		
		/// identify this request
		public String reqID;
		
		////These two are the target datacenter, where this sub-topology is recovered. 
		public String cloudProvider;
		public String domain;
		
		////identify the name of the scaled topology. If it is null, this will be auto generated.
		public String scaledTopology;
		
		///this is a list of VM names to be scaled. They are not full names.
		public ArrayList<String> targetVMs = new ArrayList<String>();
		
		////this contains all the possible classes might be needed
		public ClassSet scaledClasses;
	}
	///The key identifies the topology name that needs to be scaled
	///Value means whether this request can be satisfied
	public Map<VMScalingReqEle, Boolean> content = new HashMap<VMScalingReqEle, Boolean>();
}
