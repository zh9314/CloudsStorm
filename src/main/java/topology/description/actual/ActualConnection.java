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

import topology.dataStructure.Connection;

//This is the connection definition in the top level description file.
public class ActualConnection extends Connection{
	
	public ActualConnectionPoint source;
	public ActualConnectionPoint target;
	
	/**
	 * This field determines whether this connection is 
	 * design for logic design purpose. When this connection is transferred from 
	 * the abstract description, this field must be 'true'. 
	 * When this connection is generated by the subnet, this field must be 'false'.
	 * If it is 'null', it means 'true' by default. 
	 */
	public String logic;

}
