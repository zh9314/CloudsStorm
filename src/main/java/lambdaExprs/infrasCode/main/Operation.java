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
package lambdaExprs.infrasCode.main;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class Operation {
	public String Operation;
	
	/**
	 * When the Operation is 'execute', this tells which command wants to execute on 
	 * the remote node.
	 */
	public String Command;
	
	/**
	 * This contains all the options that needed by this operation.
	 * It stores the key value pairs for these options.
	 */
	public Map<String, String> Options;
	
	/**
	 * It means whether to log the output of this command. Only valid 
	 * when the operation is 'execute'.
	 */
	public String Log;
	
	
	/**
	 * The type of the object of this operation.
	 * Can be: "VM", "SubTopology"
	 */
	public String ObjectType;
	
	/**
	 * To define the set of objects of this operation.
	 * The names are split by "||" as parallel lambda calculus.
	 */
	public String Objects;
	
	
	/**
	 * This is a counter to tell the position in the code type of 'LOOP'.
	 * In the infrastructure code, it is identified as '$counter'
	 */
	@JsonIgnore
	public int loopCounter;
}
