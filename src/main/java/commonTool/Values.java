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
package commonTool;

public class Values {
	public class STStatus{
		public static final String running = "running";
		public static final String failed = "failed";
		public static final String unknown = "unknown";
		public static final String fresh = "fresh";
		public static final String stopped = "stopped";
		public static final String deleted = "deleted";
	}
	public class Options{
		///following are used for 'put' and 'get'
		public static final String srcPath = "Src";
		public static final String dstPath = "Dst";
		
		///following are used for 'hscale' in vm level and sub-topology level
		public static final String requstID = "ReqID";
		public static final String cloudProivder = "CP";
		public static final String domain = "DC";
		public static final String scaledTopology = "ScaledSTName";
		public static final String subTopologyClass = "STClass";
		public static final String sEngineClass = "SEClass";
		public static final String vEngineClass = "VEClass";
		public static final String scalingOutIn = "OutIn";
		
		///following are used for 'vscale'
		public static final String targetCPU = "CPU";
		public static final String targetMEM = "MEM";
	}
}
