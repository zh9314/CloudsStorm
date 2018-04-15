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
package lambdaInfrs.engine.VEngine.adapter;


import lambdaInfrs.credential.Credential;
import lambdaInfrs.database.Database;
import lambdaInfrs.engine.VEngine.VEngine;
import lambdaInfrs.engine.VEngine.VEngineOpMethod;

import org.apache.log4j.Logger;

import commonTool.ClassDB;
import topology.description.actual.VM;

public class VEngine_delete extends VEngineAdapter{
	
	private static final Logger logger = Logger.getLogger(VEngine_delete.class);

	
	public VEngine_delete(VM subjectVM, 
			Credential credential, Database database){
		this.curVM = subjectVM;
		this.credential = credential;
		this.curSTI = subjectVM.ponintBack2STI;
		this.database = database;
		this.opResult = true;
	}

	@Override
	public void run() {
		Class<?> CurVEngine = ClassDB.getVEngine(curSTI.cloudProvider, 
				curVM.VEngineClass, curVM.OStype);
		if(CurVEngine == null){
			logger.error("VEngine cannot be loaded for '"+curVM.name+"'!");
			curSTI.logsInfo.put(curVM.name+"#ERROR", "VEngine not found!");
			opResult = false;
			return ;
		}
		try {
			Object vEngine = (VEngine)CurVEngine.newInstance();
			
			if( !((VEngineOpMethod)vEngine).delete(curVM, 
					credential, database) ){
				logger.error("VM '"+curVM.name+"' cannot be deleted ");
				curSTI.logsInfo.put(curVM.name, "Delete failed!");
				opResult = false;
				return ;
			}
			curVM.publicAddress = null;
			for(int ci = 0 ; ci < curVM.vmConnectors.size() ; ci++)
				curVM.vmConnectors.get(ci).ethName = null;
			
		} catch (InstantiationException | IllegalAccessException e) {
			e.printStackTrace();
			logger.error(e.getMessage());
			curSTI.logsInfo.put(curVM.name+"#ERROR", CurVEngine.getName()+" is not valid!");
			opResult = false;
			return ;
		}
	}

}
