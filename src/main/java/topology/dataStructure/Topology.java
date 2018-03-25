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

import com.fasterxml.jackson.annotation.JsonIgnore;

public abstract class Topology {
	
	/**
	 * The path of the file where this topology object loads.
	 * Used for controlling information output.
	 */
	@JsonIgnore
	public String loadingPath;
	

	/*
	 * The user name defined by the user.
	 * This is corresponding to the ssh key.
	 */
	public String userName;
	
	/**
	 * This field can be <br/>
	 * 1. the url of the ssh key file  <br/>
	 * 2. the absolute path of the file on the local machine <br/>
	 * 3. the file name of the file. By default, this file will be at the same 
	 * folder of the description files. <br/>
	 * Examples: url@http://www.mydomain.com/pathToFile/myId_dsa <br/>
	 * file@/home/id_dsa (the file path is absolute path)<br/>
	 * name@id_rsa.pub (just fileName) <br/>
	 * null <br/>
	 * This is not case sensitive.
	 * The file must exist. Otherwise, there will be a warning log message for this.
	 * And you can load these information manually later on.  <br/>
	 * All the "script" field is designed like this.
	 */
	public String publicKeyPath;
	
}
