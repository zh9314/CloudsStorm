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
package provisioning.engine.VEngine;

import provisioning.credential.Credential;
import provisioning.database.Database;
import topology.description.actual.VM;

/**
 * This interface is more cloud related method.
 * It indicates which specific operation methods that this VEngine 
 * should have, including create, delete, stop, delete. Basically, 
 * for a new infrastructure, user only needs to implement these 6
 * basic operation functions. 
 * @author huan
 *
 */
public interface VEngineOpMethod {
	
	public boolean provision(VM subjectVM,
			Credential credential, Database database);
	
	public boolean delete(VM subjectVM,
			Credential credential, Database database);
	
	/**
	 * stop/start operations are optional.
	 * Some Cloud does not support stop/start.
	 * @return
	 */
	public boolean stop(VM subjectVM,
			Credential credential, Database database);
	
	public boolean start(VM subjectVM,
			Credential credential, Database database);
	

	/**
	 * Indicate whether the instance can be stopped.
	 * @return
	 */
	public boolean supportStop();
	
}
