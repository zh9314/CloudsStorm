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
package lambdaInfrs.engine.VEngine.EC2;



import lambdaInfrs.credential.Credential;
import lambdaInfrs.database.Database;
import lambdaInfrs.engine.VEngine.OS.ubuntu.VEngineUbuntu;
import topology.description.actual.VM;

public class EC2VEngineUbuntu extends VEngineUbuntu{
	

	@Override
	public boolean provision(VM subjectVM,
			Credential credential, Database database) {
		if(EC2VEngine.provision(subjectVM, credential, database))
			return true;
		else
			return false;
	}

	@Override
	public boolean supportStop() {
		return true;
	}

	@Override
	public boolean delete(VM subjectVM,
			Credential credential, Database database) {
		if(EC2VEngine.delete(subjectVM, credential, database))
			return true;
		else
			return false;
	}


	@Override
	public boolean stop(VM subjectVM, Credential credential, Database database) {
		if(EC2VEngine.stop(subjectVM, credential, database))
			return true;
		else
			return false;
	}

	@Override
	public boolean start(VM subjectVM, Credential credential, Database database) {
		if(EC2VEngine.start(subjectVM, credential, database))
			return true;
		else
			return false;
	}

	
	
	
	
}
