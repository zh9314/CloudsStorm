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
package lambdaInfrs.engine.SEngine.adapter;

import lambdaInfrs.credential.Credential;
import lambdaInfrs.database.Database;
import lambdaInfrs.engine.SEngine.SEngineKeyMethod;

import org.apache.log4j.Logger;

import commonTool.ClassDB;
import commonTool.Values;
import topology.description.actual.SubTopologyInfo;

/**
 * Only configure the environment, run the application-defined
 * script.
 * @author huan
 *
 */
public class SEngine_conf extends SEngineAdapter{
	
	private static final Logger logger = Logger.getLogger(SEngine_conf.class);
	
	
	public SEngine_conf(SubTopologyInfo subTopologyInfo, 
			Credential credential, Database database){
		this.subTopologyInfo = subTopologyInfo;
		this.credential = credential;
		this.database = database;
		opResult = true;
	}
	
	@Override
	public void run() {
		String cp = subTopologyInfo.cloudProvider.trim().toLowerCase();
		String sEngineClass = subTopologyInfo.subTopology.SEngineClass;
		Class<?> CurSEngine = ClassDB.getSEngine(cp, sEngineClass);
		if(CurSEngine == null){
			String msg = "SEngine cannot be loaded for '"+subTopologyInfo.topology
					+"'!";
			logger.warn(msg);
			subTopologyInfo.logsInfo.put(subTopologyInfo.topology+"#ERROR", msg);
			opResult = false;
			return ;
		}
		try {
			Object sEngine = CurSEngine.newInstance();
			
			/////some common checks on the sub-topology
			
			/// in this case, we do not say this operation is failed.
			if( !subTopologyInfo.status.trim().toLowerCase().equals(Values.STStatus.running) ){
				String msg = "The sub-topology '"+subTopologyInfo.topology
						+"' is not in the status of 'fresh' or 'deleted'!";
				logger.warn(msg);
				subTopologyInfo.logsInfo.put(subTopologyInfo.topology+"#WARN", msg);
				opResult = false;
				return ;
			}
			if( subTopologyInfo.subTopology.accessKeyPair == null
					|| subTopologyInfo.subTopology.accessKeyPair.privateKeyString == null){
				String msg = "SSH key information is missing for detaching sub-topology '"
						+subTopologyInfo.topology+"'!";
				logger.error(msg);
				subTopologyInfo.logsInfo.put(subTopologyInfo.topology+"#ERROR", msg);
				opResult = false;
				return ;
			}
			
			long stOpStart = System.currentTimeMillis();
			
			
			if(!((SEngineKeyMethod)sEngine).envConf(subTopologyInfo, credential, database)){
				logger.error("Runtime environment configuration for sub-topology '"+subTopologyInfo.topology+"' failed!");
				opResult = false;
			}
			
			if(opResult){
				long stOpEnd = System.currentTimeMillis();
				subTopologyInfo.logsInfo.put(subTopologyInfo.topology+"#Deploy", 
												(stOpEnd - stOpStart) +"@"+ stOpStart);
			}
			
			return ;
		} catch (InstantiationException | IllegalAccessException
				 e) {
			e.printStackTrace();
			logger.error("The S-Engine for sub-topology '"+subTopologyInfo.topology+"' cannot be found!");
			subTopologyInfo.logsInfo.put(subTopologyInfo.topology+"#ERROR", 
						CurSEngine.getName()+" is not valid!");
			opResult = false;
		}
	}
}
