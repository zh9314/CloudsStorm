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
package lambdaExprs.infrasCode.log;

import java.util.Map;

import lambdaExprs.infrasCode.main.Operation;

public class Log {
	/**
	 * The time of the event happens which is a Long value.
	 */
	public String Time;
	
	/**
	 * This is the operation overhead, which record the time that this operation takes.
	 * The unit is in second. 
	 */
	public String Overhead;
	
	public Operation Event;
	
	/**
	 * The detailed content of the log.
	 */
	public Map<String, String> LOG;
	
}
