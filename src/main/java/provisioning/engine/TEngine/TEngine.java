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
package provisioning.engine.TEngine;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import commonTool.ClassDB;
import commonTool.ClassSet;
import commonTool.CommonTool;
import commonTool.Values;

import org.apache.log4j.Logger;

import provisioning.credential.Credential;
import provisioning.credential.UserCredential;
import provisioning.database.Database;
import provisioning.database.UserDatabase;
import provisioning.engine.SEngine.adapter.SEngineAdapter;
import provisioning.engine.SEngine.adapter.SEngine_connect;
import provisioning.engine.SEngine.adapter.SEngine_delete;
import provisioning.engine.SEngine.adapter.SEngine_detach;
import provisioning.engine.SEngine.adapter.SEngine_provision;
import provisioning.engine.SEngine.adapter.SEngine_start;
import provisioning.engine.SEngine.adapter.SEngine_stop;
import provisioning.engine.SEngine.SEngineKeyMethod;
import provisioning.request.DeleteRequest;
import provisioning.request.HScalingSTRequest;
import provisioning.request.HScalingSTRequest.STScalingReqEle;
import provisioning.request.HScalingVMRequest;
import provisioning.request.HScalingVMRequest.VMScalingReqEle;
import provisioning.request.ProvisionRequest;
import provisioning.request.RecoverRequest;
import provisioning.request.RecoverRequest.RecoverReqEle;
import provisioning.request.StartRequest;
import provisioning.request.StopRequest;
import provisioning.request.VScalingVMRequest;
import provisioning.request.VScalingVMRequest.VMVScalingReqEle;
import topology.description.actual.SubTopology;
import topology.description.actual.SubTopologyInfo;
import topology.description.actual.TopTopology;
import topology.description.actual.VM;

public class TEngine {
	private static final Logger logger = Logger.getLogger(TEngine.class);

	/**
	 * This is a method to make all the 'fresh' or 'deleted' sub-topologies 
	 * go into the state of 'running' or some of them may fail. 
	 * It is important to point out here that the 'deleted' sub-topology only 
	 * with the tag of 'fixed' and 'scaling' can be provisioned. The 'deleted' 
	 * 'scaled' sub-topology cannot be provisioned again.
	 * @param topTopology
	 * @param userCredential
	 * @param userDatabase
	 */
	public boolean provisionAll(TopTopology topTopology, UserCredential userCredential, UserDatabase userDatabase){
		if(userCredential.cloudAccess == null){
			logger.error("The credentials for cloud providers must be initialized!");
			return false;
		}if(userDatabase.databases == null){
			logger.error("The databases for different cloud providers must be initialized!");
			return false;
		}
		
		boolean opResult = true;
		////Using multi-thread to do provisioning
		ExecutorService executor4st = Executors.newFixedThreadPool(topTopology.topologies.size());
		ArrayList<SEngineAdapter> sEAs = new ArrayList<SEngineAdapter>();
		for(int sti = 0 ; sti < topTopology.topologies.size() ; sti++){
			SubTopologyInfo subTopologyInfo = topTopology.topologies.get(sti);
			if(!subTopologyInfo.status.trim().toLowerCase().equals("fresh")
					&& !subTopologyInfo.status.trim().toLowerCase().equals("deleted")){
				logger.info("Sub-topology '"+subTopologyInfo.topology
						+"' cannot be provisioned! Its status is '"
						+subTopologyInfo.status+"'!");
				continue;
			}
			logger.info("Provisioning sub-topology '"+subTopologyInfo.topology+"' from '"+subTopologyInfo.cloudProvider+"'");
			Credential curCredential = userCredential.cloudAccess.get(subTopologyInfo.cloudProvider.toLowerCase());
			if(curCredential == null){
				logger.error("The credential for sub-topology '"+subTopologyInfo.topology+"' from '"+subTopologyInfo.cloudProvider+"' is unknown! SKIP!");
				opResult = false;
				continue;
			}
			Database database = userDatabase.databases.get(subTopologyInfo.cloudProvider.toLowerCase());
			if(database == null){
				logger.error("The database for sub-topology '"+subTopologyInfo.topology+"' from '"+subTopologyInfo.cloudProvider+"' is unknown! SKIP!");
				opResult = false;
				continue;
			}
			SEngine_provision s_provision = new SEngine_provision(subTopologyInfo, curCredential, database);
			sEAs.add((SEngineAdapter)s_provision);
			executor4st.execute(s_provision);
		}
		
		executor4st.shutdown();
		try {
			int count = 0;
			while (!executor4st.awaitTermination(2, TimeUnit.SECONDS)){
				count++;
				if(count > 500*topTopology.topologies.size()){
					logger.error("Unknown error! "
							+ "Some sub-topology cannot be provisioned!");
					return false;
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
			logger.error("Unexpected error!");
			return false;
		}
		
		if(sEAs.size() != 0){
			if(connectionConf(topTopology.topologies, userCredential, userDatabase))
				logger.info("All the running sub-topologies have been connected!");
			else{
				logger.warn("Some of sub-topology may not be connected!");
				opResult = false;
			}
		}
		
		///overwrite the control information back to the top-topology
		if(!topTopology.overwirteControlOutput()){
			logger.error("Control information of top-topology has not been overwritten to the origin file!");
			return false;
		}
		
		if(!checkSEnginesResults(sEAs))
			return false;
		
		if(!opResult)
			return false;
		
		return true;
	}
	
	/**
	 * Provision the sub-topologies, according to the ProvisionRequest.
	 * @param topTopology
	 * @param userCredential
	 * @param userDatabase
	 * @param provisionReqs
	 */
	public boolean provision(TopTopology topTopology, UserCredential userCredential, 
			UserDatabase userDatabase, ProvisionRequest provisionReq){
		if(userCredential.cloudAccess == null){
			logger.error("The credentials for cloud providers must be initialized!");
			return false;
		}if(userDatabase.databases == null){
			logger.error("The databases for different cloud providers must be initialized!");
			return false;
		}
		
		if(provisionReq == null || provisionReq.content.size() == 0){
			logger.warn("Nothing needs to be provisioned!");
			return true;
		}
		
		boolean opResult = true;
		ArrayList<SubTopologyInfo> provisionSTIs = new ArrayList<SubTopologyInfo>();
		for(int sti = 0 ; sti < topTopology.topologies.size() ; sti++){
			SubTopologyInfo curSTI = topTopology.topologies.get(sti);
			if(provisionReq.content.containsKey(curSTI.topology)){
				provisionReq.content.put(curSTI.topology, true);
				if(!curSTI.status.trim().toLowerCase().equals("fresh")
						&& !curSTI.status.trim().toLowerCase().equals("deleted")){
					logger.warn("Sub-topology '"+curSTI.topology
							+"' cannot be provisioned! Its status is '"
							+curSTI.status+"'!");
				}else
					provisionSTIs.add(curSTI);
			}
		}
		
		
		if(provisionSTIs.size() == 0){
			logger.warn("None of the sub-topologies needs to be provisioned!");
			return true; 
		}
		
		ExecutorService executor4ss = Executors.newFixedThreadPool(provisionSTIs.size());
		ArrayList<SEngineAdapter> sEAs = new ArrayList<SEngineAdapter>();
		for(int sti = 0 ; sti < provisionSTIs.size() ; sti++){
			SubTopologyInfo subTopologyInfo = provisionSTIs.get(sti);
			Credential curCredential = userCredential.cloudAccess.get(subTopologyInfo.cloudProvider.toLowerCase());
			if(curCredential == null){
				logger.error("The credential for sub-topology '"+subTopologyInfo.topology+"' from '"+subTopologyInfo.cloudProvider+"' is unknown! SKIP!");
				opResult = false;
				continue;
			}
			Database database = userDatabase.databases.get(subTopologyInfo.cloudProvider.toLowerCase());
			if(database == null){
				logger.error("The database for sub-topology '"+subTopologyInfo.topology+"' from '"+subTopologyInfo.cloudProvider+"' is unknown! SKIP!");
				opResult = false;
				continue;
			}
			SEngine_provision s_provision = new SEngine_provision(
								subTopologyInfo, curCredential, database);
			sEAs.add((SEngineAdapter)s_provision);
			executor4ss.execute(s_provision);
		}
		
		executor4ss.shutdown();
		try {
			int count = 0;
			while (!executor4ss.awaitTermination(2, TimeUnit.SECONDS)){
				count++;
				if(count > 500*provisionSTIs.size()){
					logger.error("Unknown error! Some sub-topology cannot be provisioned!");
					return false;
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
			logger.error("Unexpected error!");
			return false;
		}
		
		////if some of the sub-topology cannot be provisioned. delete them
		if(!checkSEnginesResults(sEAs)){
			////delete all the failed sub-topologies during the provisioning
			ExecutorService executor4dvm = Executors.newFixedThreadPool(provisionSTIs.size());
			
			for(int sti = 0 ; sti < provisionSTIs.size() ; sti++){
				SubTopologyInfo subTopologyInfo = provisionSTIs.get(sti);
				if(!subTopologyInfo.status.equals("unknown"))
					continue;
				
				Credential curCredential = userCredential.cloudAccess.get(subTopologyInfo.cloudProvider.toLowerCase());
				if(curCredential == null){
					logger.error("The credential for sub-topology '"+subTopologyInfo.topology+"' from '"+subTopologyInfo.cloudProvider+"' is unknown! SKIP!");
					opResult = false;
					continue;
				}
				Database database = userDatabase.databases.get(subTopologyInfo.cloudProvider.toLowerCase());
				if(database == null){
					logger.error("The database for sub-topology '"+subTopologyInfo.topology+"' from '"+subTopologyInfo.cloudProvider+"' is unknown! SKIP!");
					opResult = false;
					continue;
				}
				SEngine_delete s_delete = new SEngine_delete(
									subTopologyInfo, curCredential, database);
				sEAs.add((SEngineAdapter)s_delete);
				executor4dvm.execute(s_delete);
			}
			
			executor4dvm.shutdown();
			try {
				int count = 0;
				while (!executor4dvm.awaitTermination(2, TimeUnit.SECONDS)){
					count++;
					if(count > 500*provisionSTIs.size()){
						logger.error("Unknown error! Some sub-topology cannot be provisioned!");
						return false;
					}
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
				logger.error("Unexpected error!");
				return false;
			}
		}
		
		if(connectionConf(topTopology.topologies, userCredential, userDatabase))
			logger.info("All the running sub-topologies have been connected!");
		else{
			logger.warn("Some of sub-topology may not be connected!");
			opResult = false;
		}
		
		///overwrite the control information back to the top-topology
		if(!topTopology.overwirteControlOutput()){
			logger.error("Control information of top-topology has not been overwritten to the origin file!");
			return false;
		}
		if(!opResult)
			return false;
		
		return true;
	}
	
	////Using multi-thread to configure all the connections of all the 'running' sub-topologies
	public boolean connectionConf(ArrayList<SubTopologyInfo> subTopologyInfos,
			UserCredential userCredential, UserDatabase userDatabase){
		if(subTopologyInfos == null || subTopologyInfos.size() == 0)
			return true;
		
		boolean returnResult = true;
		ExecutorService executor4networkConf = Executors.newFixedThreadPool(subTopologyInfos.size());
		ArrayList<SEngineAdapter> sEAs = new ArrayList<SEngineAdapter>();
		for(int sti = 0 ; sti <subTopologyInfos.size() ; sti++){
			SubTopologyInfo subTopologyInfo = subTopologyInfos.get(sti);
			if(!subTopologyInfo.status.equals("running")){
				logger.info("'"+subTopologyInfo.status+"' sub-topology "+
						subTopologyInfo.topology+" cannot be configured. It is not running!");
				continue;
			}
			logger.info("Connecting sub-topology '"+subTopologyInfo.topology
							+"' from '"+subTopologyInfo.cloudProvider+"'");
			Credential curCredential = userCredential.cloudAccess.get(subTopologyInfo.cloudProvider.toLowerCase());
			if(curCredential == null){
				logger.error("The credential for sub-topology '"+subTopologyInfo.topology
						+"' from '"+subTopologyInfo.cloudProvider+"' is unknown! SKIP!");
				returnResult = false;
				continue;
			}
			Database database = userDatabase.databases.get(subTopologyInfo.cloudProvider.toLowerCase());
			if(database == null){
				logger.error("The database for sub-topology '"+subTopologyInfo.topology
						+"' from '"+subTopologyInfo.cloudProvider+"' is unknown! SKIP!");
				returnResult = false;
				continue;
			}
			SEngine_connect s_connect = new SEngine_connect(subTopologyInfo, curCredential, database);
			sEAs.add(s_connect);
			executor4networkConf.execute(s_connect);
		}
		
		executor4networkConf.shutdown();
		try {
			int count = 0;
			while (!executor4networkConf.awaitTermination(2, TimeUnit.SECONDS)){
				count++;
				if(count > 500*subTopologyInfos.size()){
					logger.error("Unknown error! Some sub-topology cannot be connected!");
					return false;
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
			logger.error("Unexpected error!");
			return false;
		}
		if(!checkSEnginesResults(sEAs))
			returnResult = false;
		
		return returnResult;
	}
	
	/**
	 * Starting the sub-topology, according to the StartRequest.
	 * @param topTopology
	 * @param userCredential
	 * @param userDatabase
	 * @param startReqs
	 */
	public boolean start(TopTopology topTopology, 
			UserCredential userCredential, UserDatabase userDatabase, 
			StartRequest startReq){
		if(userCredential.cloudAccess == null){
			logger.error("The credentials for cloud providers must be initialized!");
			return false;
		}if(userDatabase.databases == null){
			logger.error("The databases for different cloud providers must be initialized!");
			return false;
		}
		
		if(startReq == null || startReq.content.size() == 0){
			logger.warn("Nothing needs to be started!");
			return true;
		}
		
		boolean opResult = true;
		ArrayList<SubTopologyInfo> needToBeStarted = new ArrayList<SubTopologyInfo>();
		for(int sti = 0 ; sti < topTopology.topologies.size() ; sti++){
			SubTopologyInfo curSTI = topTopology.topologies.get(sti);
			if(startReq.content.containsKey(curSTI.topology)){
				startReq.content.put(curSTI.topology, true);
				if( !curSTI.status.trim().toLowerCase().equals("stopped") ){
					String msg = "The sub-topology '"+curSTI.topology
							+"' is not in the status of 'stopped' to be started!";
					logger.warn(msg);
				}
				else	
					needToBeStarted.add(curSTI);
			}
		}
			
		
		if(needToBeStarted.size() == 0){
			logger.warn("No sub-topology needs be started!");
			return true;
		}
		
		////Using multi-thread to do starting
		ExecutorService executor4st = Executors.newFixedThreadPool(topTopology.topologies.size());
		ArrayList<SEngineAdapter> sEAs = new ArrayList<SEngineAdapter>();
		for(int sti = 0 ; sti < needToBeStarted.size() ; sti++){
			SubTopologyInfo subTopologyInfo = needToBeStarted.get(sti);
			logger.info("Starting sub-topology '"+subTopologyInfo.topology+"' from '"+subTopologyInfo.cloudProvider+"'");
			Credential curCredential = userCredential.cloudAccess.get(subTopologyInfo.cloudProvider.toLowerCase());
			if(curCredential == null){
				logger.error("The credential for sub-topology '"+subTopologyInfo.topology+"' from '"+subTopologyInfo.cloudProvider+"' is unknown! SKIP!");
				opResult = false;
				continue;
			}
			Database database = userDatabase.databases.get(subTopologyInfo.cloudProvider.toLowerCase());
			if(database == null){
				logger.error("The database for sub-topology '"+subTopologyInfo.topology+"' from '"+subTopologyInfo.cloudProvider+"' is unknown! SKIP!");
				opResult = false;
				continue;
			}
			SEngine_start s_start = new SEngine_start(subTopologyInfo, curCredential, database);
			sEAs.add((SEngineAdapter)s_start);
			executor4st.execute(s_start);
		}
		
		executor4st.shutdown();
		try {
			int count = 0;
			while (!executor4st.awaitTermination(2, TimeUnit.SECONDS)){
				count++;
				if(count > 500*topTopology.topologies.size()){
					logger.error("Unknown error! Some sub-topology cannot be started!");
					return false;
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
			logger.error("Unexpected error!");
			return false;
		}
		
		if(connectionConf(topTopology.topologies, userCredential, userDatabase))
			logger.info("All the running sub-topologies have been connected!");
		else{
			logger.warn("Some of sub-topology may not be connected!");
			opResult = false;
		}
		
		///overwrite the control information back to the top-topology
		if(!topTopology.overwirteControlOutput()){
			logger.error("Control information of top-topology has not been overwritten to the origin file!");
			return false;
		}
			
		if(!checkSEnginesResults(sEAs))
			return false;
		if(!opResult)
			return false;
		return true;
	}
	
	/**
	 * Starting all the stopped sub-topology.
	 * @param topTopology
	 * @param userCredential
	 * @param userDatabase
	 */
	public boolean startAll(TopTopology topTopology, 
			UserCredential userCredential, UserDatabase userDatabase){
		if(userCredential.cloudAccess == null){
			logger.error("The credentials for cloud providers must be initialized!");
			return false;
		}if(userDatabase.databases == null){
			logger.error("The databases for different cloud providers must be initialized!");
			return false;
		}
		
		ArrayList<SubTopologyInfo> needToBeStarted = new ArrayList<SubTopologyInfo>();
		//ArrayList<SubTopologyInfo> alreadyRunningSTIs = new ArrayList<SubTopologyInfo>();  
		for(int sti = 0 ; sti < topTopology.topologies.size() ; sti++){
			SubTopologyInfo curSTI = topTopology.topologies.get(sti);
			if(curSTI.status.trim().toLowerCase().equals("stopped"))
				needToBeStarted.add(curSTI);
			//if(curSTI.status.trim().toLowerCase().equals("running"))
			//	alreadyRunningSTIs.add(curSTI);
		}
		if(needToBeStarted.size() == 0){
			logger.error("No sub-topology needs be started!");
			return true;
		}
		
		boolean opResult = true;
		////Using multi-thread to do starting
		ExecutorService executor4st = Executors.newFixedThreadPool(needToBeStarted.size());
		ArrayList<SEngineAdapter> sEAs = new ArrayList<SEngineAdapter>();
		for(int sti = 0 ; sti < needToBeStarted.size() ; sti++){
			SubTopologyInfo subTopologyInfo = needToBeStarted.get(sti);
			logger.info("Starting sub-topology '"+subTopologyInfo.topology+"' from '"+subTopologyInfo.cloudProvider+"'");
			Credential curCredential = userCredential.cloudAccess.get(subTopologyInfo.cloudProvider.toLowerCase());
			if(curCredential == null){
				logger.error("The credential for sub-topology '"+subTopologyInfo.topology+"' from '"+subTopologyInfo.cloudProvider+"' is unknown! SKIP!");
				opResult = false;
				continue;
			}
			Database database = userDatabase.databases.get(subTopologyInfo.cloudProvider.toLowerCase());
			if(database == null){
				logger.error("The database for sub-topology '"+subTopologyInfo.topology+"' from '"+subTopologyInfo.cloudProvider+"' is unknown! SKIP!");
				opResult = false;
				continue;
			}
			SEngine_start s_start = new SEngine_start(subTopologyInfo, curCredential, database);
			sEAs.add((SEngineAdapter)s_start);
			executor4st.execute(s_start);
		}
		
		executor4st.shutdown();
		try {
			int count = 0;
			while (!executor4st.awaitTermination(2, TimeUnit.SECONDS)){
				count++;
				if(count > 500*needToBeStarted.size()){
					logger.error("Unknown error! Some sub-topology cannot be started!");
					return false;
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
			logger.error("Unexpected error!");
			return false;
		}
		
		if(sEAs.size() == 0){
			if(connectionConf(topTopology.topologies, userCredential, userDatabase))
				logger.info("All the running sub-topologies have been connected!");
			else{
				logger.warn("Some of sub-topology may not be connected!");
				opResult = false;
			}
		}
		
		///overwrite the control information back to the top-topology
		if(!topTopology.overwirteControlOutput()){
			logger.error("Control information of top-topology has not been overwritten to the origin file!");
			return false;
		}
		if(!checkSEnginesResults(sEAs))
			return false;
		if(!opResult)
			return false;
		return true;
	}
	
	/**
	 * Stopping the sub-topology, according to the StopRequest.
	 * @param topTopology
	 * @param userCredential
	 * @param userDatabase
	 * @param stopReqs
	 */
	public boolean stop(TopTopology topTopology, 
			UserCredential userCredential, UserDatabase userDatabase, 
			StopRequest stopReq){
		if(userCredential.cloudAccess == null){
			logger.error("The credentials for cloud providers must be initialized!");
			return false;
		}if(userDatabase.databases == null){
			logger.error("The databases for different cloud providers must be initialized!");
			return false;
		}
		
		if(stopReq == null || stopReq.content.size() == 0){
			logger.warn("Nothing needs to be stopped!");
			return true;
		}
		ArrayList<SubTopologyInfo> stopSTIs = new ArrayList<SubTopologyInfo>();
		ArrayList<SubTopologyInfo> runningSTIs = new ArrayList<SubTopologyInfo>();
		for(int sti = 0 ; sti < topTopology.topologies.size() ; sti++){
			SubTopologyInfo curSTI = topTopology.topologies.get(sti);
			if(stopReq.content.containsKey(curSTI.topology)){
				stopReq.content.put(curSTI.topology, true);
				if(!curSTI.status.trim().toLowerCase().equals("running")){
					logger.warn("'"+curSTI.status+"' sub-topology '"
									+curSTI.topology+"' cannot be stopped!");
				}else
					stopSTIs.add(curSTI);
			}else{
				if(curSTI.status.trim().toLowerCase().equals("running"))
					runningSTIs.add(curSTI);
			}
		}
		
		
		
		if(stopSTIs.size() == 0){
			logger.warn("None of the sub-topologies needs to be stopped!");
			return true; 
		}
		
		boolean opResult = true;
		ExecutorService executor4ss = Executors.newFixedThreadPool(topTopology.topologies.size());
		ArrayList<SEngineAdapter> sEAs = new ArrayList<SEngineAdapter>();
		for(int sti = 0 ; sti < stopSTIs.size() ; sti++){
			SubTopologyInfo subTopologyInfo = stopSTIs.get(sti);
			Credential curCredential = userCredential.cloudAccess.get(subTopologyInfo.cloudProvider.toLowerCase());
			if(curCredential == null){
				logger.error("The credential for sub-topology '"+subTopologyInfo.topology+"' from '"+subTopologyInfo.cloudProvider+"' is unknown! SKIP!");
				opResult = false;
				continue;
			}
			Database database = userDatabase.databases.get(subTopologyInfo.cloudProvider.toLowerCase());
			if(database == null){
				logger.error("The database for sub-topology '"+subTopologyInfo.topology+"' from '"+subTopologyInfo.cloudProvider+"' is unknown! SKIP!");
				opResult = false;
				continue;
			}
			///identify the connectors of the stopped sub-topology are null
			if(subTopologyInfo.connectors != null){
				for(int tcpi = 0 ; tcpi < subTopologyInfo.connectors.size() ; tcpi++)
					subTopologyInfo.connectors.get(tcpi).ethName = null;
			}
			SEngine_stop s_stop = new SEngine_stop(subTopologyInfo, curCredential, database);
			sEAs.add((SEngineAdapter)s_stop);
			executor4ss.execute(s_stop);
		}
		////detach all the running sub-topologies with the stopped sub-topologies
		for(int sti = 0 ; sti < runningSTIs.size() ; sti++){
			SubTopologyInfo subTopologyInfo = runningSTIs.get(sti);
			Credential curCredential = userCredential.cloudAccess.get(subTopologyInfo.cloudProvider.toLowerCase());
			if(curCredential == null){
				logger.error("The credential for sub-topology '"+subTopologyInfo.topology+"' from '"+subTopologyInfo.cloudProvider+"' is unknown! SKIP!");
				opResult = false;
				continue;
			}
			Database database = userDatabase.databases.get(subTopologyInfo.cloudProvider.toLowerCase());
			if(database == null){
				logger.error("The database for sub-topology '"+subTopologyInfo.topology+"' from '"+subTopologyInfo.cloudProvider+"' is unknown! SKIP!");
				opResult = false;
				continue;
			}
			SEngine_detach sd = new SEngine_detach(subTopologyInfo, curCredential, database);
			executor4ss.execute(sd);
		}
		
		executor4ss.shutdown();
		try {
			int count = 0;
			while (!executor4ss.awaitTermination(2, TimeUnit.SECONDS)){
				count++;
				if(count > 500*topTopology.topologies.size()){
					logger.error("Unknown error! Some sub-topology cannot be stopped!");
					return false;
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
			logger.error("Unexpected error!");
			return false;
		}
		
		///overwrite the control information back to the top-topology
		if(!topTopology.overwirteControlOutput()){
			logger.error("Control information of top-topology has not been overwritten to the origin file!");
			return false;
		}
		if(!checkSEnginesResults(sEAs))
			return false;
		if(!opResult)
			return false;
		return true;
		
	}
	
	
	/**
	 * Stopping all the 'running' sub-topologies.
	 * @param topTopology
	 * @param userCredential
	 * @param userDatabase
	 */
	public boolean stopAll(TopTopology topTopology, 
			UserCredential userCredential, UserDatabase userDatabase){
		if(userCredential.cloudAccess == null){
			logger.error("The credentials for cloud providers must be initialized!");
			return false;
		}if(userDatabase.databases == null){
			logger.error("The databases for different cloud providers must be initialized!");
			return false;
		}
		
		ArrayList<SubTopologyInfo> stopSTIs = new ArrayList<SubTopologyInfo>();
		for(int sti = 0 ; sti < topTopology.topologies.size() ; sti++){
			SubTopologyInfo curSTI = topTopology.topologies.get(sti);
			if(curSTI.status.trim().toLowerCase().equals("running"))
				stopSTIs.add(curSTI);
		}
		if(stopSTIs.size() == 0){
			logger.warn("None of the sub-topologies needs to be stopped!");
			return true; 
		}
		boolean opResult = true;
		ExecutorService executor4ss = Executors.newFixedThreadPool(stopSTIs.size());
		ArrayList<SEngineAdapter> sEAs = new ArrayList<SEngineAdapter>();
		for(int sti = 0 ; sti < stopSTIs.size() ; sti++){
			SubTopologyInfo subTopologyInfo = stopSTIs.get(sti);
			Credential curCredential = userCredential.cloudAccess.get(subTopologyInfo.cloudProvider.toLowerCase());
			if(curCredential == null){
				logger.error("The credential for sub-topology '"+subTopologyInfo.topology+"' from '"+subTopologyInfo.cloudProvider+"' is unknown! SKIP!");
				opResult = false;
				continue;
			}
			Database database = userDatabase.databases.get(subTopologyInfo.cloudProvider.toLowerCase());
			if(database == null){
				logger.error("The database for sub-topology '"+subTopologyInfo.topology+"' from '"+subTopologyInfo.cloudProvider+"' is unknown! SKIP!");
				opResult = false;
				continue;
			}
			///identify the connectors of the stopped sub-topology are null
			if(subTopologyInfo.connectors != null){
				for(int tcpi = 0 ; tcpi < subTopologyInfo.connectors.size() ; tcpi++)
					subTopologyInfo.connectors.get(tcpi).ethName = null;
			}
			SEngine_stop s_stop = new SEngine_stop(subTopologyInfo, curCredential, database);
			sEAs.add((SEngineAdapter)s_stop);
			executor4ss.execute(s_stop);
		}
		
		executor4ss.shutdown();
		try {
			int count = 0;
			while (!executor4ss.awaitTermination(2, TimeUnit.SECONDS)){
				count++;
				if(count > 500*stopSTIs.size()){
					logger.error("Unknown error! Some sub-topology cannot be stopped!");
					return false;
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
			logger.error("Unexpected error!");
			return false;
		}
		
		///overwrite the control information back to the top-topology
		if(!topTopology.overwirteControlOutput()){
			logger.error("Control information of top-topology has not been overwritten to the origin file!");
			return false;
		}
		if(!checkSEnginesResults(sEAs))
			return false;
		if(!opResult)
			return false;
		return true;
	}
	
	/**
	 * Delete the sub-topologies according to the delete requests.
	 * @param topTopology
	 * @param userCredential
	 * @param userDatabase
	 * @param stopReqs
	 */
	public boolean delete(TopTopology topTopology, 
			UserCredential userCredential, UserDatabase userDatabase, 
			DeleteRequest deleteReq){
		if(userCredential.cloudAccess == null){
			logger.error("The credentials for cloud providers must be initialized!");
			return false ;
		}if(userDatabase.databases == null){
			logger.error("The databases for different cloud providers must be initialized!");
			return false;
		}
		
		if(deleteReq == null || deleteReq.content.size() == 0){
			logger.warn("Nothing needs to be stopped!");
			return true;
		}
		
		DeleteRequest actualDelReq = new DeleteRequest();
		///the vscaled copy must be deleted at the same time
		for(Map.Entry<String, Boolean> entry: deleteReq.content.entrySet()){
			String delSTName = entry.getKey();
			SubTopologyInfo delSTI = topTopology.subTopologyIndex.get(delSTName);
			if(delSTI == null)
				continue;
			actualDelReq.content.put(delSTName, false);
			///this is the master copy
			if(!delSTName.endsWith("_vscale")){
				boolean vscaling = false;
				ArrayList<VM> vms = delSTI.subTopology.getVMsinSubClass(); 
				for(int vi = 0 ; vi<vms.size() ; vi++){
					VM curVM = vms.get(vi);
					if(curVM.fake != null && curVM.fake.trim().equalsIgnoreCase("true")){
						VM actualVM = topTopology.VMIndex.get(curVM.name);
						actualDelReq.content.put(actualVM.ponintBack2STI.topology, false);
						vscaling = true;
					}
				}
				if(vscaling)
					for(int si = 0; si<topTopology.topologies.size() ; si++)
						if(topTopology.topologies.get(si).topology.contains(delSTName))
							actualDelReq.content.put(topTopology.topologies.get(si).topology, false);
					
			}
			//this is the slave copy
			if(delSTName.endsWith("_vscale") && delSTI.scaledFrom != null){
				SubTopologyInfo scaledSTI = topTopology.subTopologyIndex.get(delSTI.scaledFrom);
				if(scaledSTI == null)
					continue;
				boolean vscaling = false; boolean inUse = false;
				ArrayList<VM> vms2 = scaledSTI.subTopology.getVMsinSubClass(); 
				for(int vi = 0 ; vi<vms2.size() ; vi++){
					VM curVM = vms2.get(vi);
					
					if(curVM.fake != null && curVM.fake.trim().equalsIgnoreCase("true")){
						/////update it with the newest node type
						curVM.nodeType = topTopology.VMIndex.get(curVM.name).nodeType;
						vscaling = true;
					}else
						inUse = true;
				}
				////only when this slave copy is in use. then delete all the related sub-topologies
				if(vscaling && inUse){
					String [] actualNames = delSTName.split("_vscale");
					String actualName = actualNames[0];
					for(int si = 0; si<topTopology.topologies.size() ; si++)
						if(topTopology.topologies.get(si).topology.contains(actualName))
							actualDelReq.content.put(topTopology.topologies.get(si).topology, false);
				}
			}
		}
		
		boolean opResult = true;
		ArrayList<SubTopologyInfo> deleteSTIs = new ArrayList<SubTopologyInfo>();
		ArrayList<SubTopologyInfo> runningSTIs = new ArrayList<SubTopologyInfo>();
		for(int sti = 0 ; sti < topTopology.topologies.size() ; sti++){
			SubTopologyInfo curSTI = topTopology.topologies.get(sti);
			if(actualDelReq.content.containsKey(curSTI.topology)){
				actualDelReq.content.put(curSTI.topology, true);
				if(!curSTI.status.trim().toLowerCase().equals(Values.STStatus.running)
						&& !curSTI.status.trim().toLowerCase().equals(Values.STStatus.stopped)
						&& !curSTI.status.trim().toLowerCase().equals(Values.STStatus.failed)){
					logger.warn("'"+curSTI.status+"' sub-topology '"
									+curSTI.topology+"' cannot be deleted!");
					
				}else
					deleteSTIs.add(curSTI);
			}else{
				////get the 'running' sub-topologies from all the other sub-topologies not in the delete list
				if(curSTI.status.trim().toLowerCase().equals(Values.STStatus.running))
					runningSTIs.add(curSTI);
			}
		}
		
		if(deleteSTIs.size() == 0){
			logger.warn("None of the sub-topologies needs to be deleted!");
			return true; 
		}
		
		ExecutorService executor4ss = Executors.newFixedThreadPool(topTopology.topologies.size());
		ArrayList<SEngineAdapter> sEAs = new ArrayList<SEngineAdapter>();
		for(int sti = 0 ; sti < deleteSTIs.size() ; sti++){
			SubTopologyInfo subTopologyInfo = deleteSTIs.get(sti);
			Credential curCredential = userCredential.cloudAccess.get(subTopologyInfo.cloudProvider.toLowerCase());
			if(curCredential == null){
				logger.error("The credential for sub-topology '"+subTopologyInfo.topology+"' from '"+subTopologyInfo.cloudProvider+"' is unknown! SKIP!");
				opResult = false;
				continue;
			}
			Database database = userDatabase.databases.get(subTopologyInfo.cloudProvider.toLowerCase());
			if(database == null){
				logger.error("The database for sub-topology '"+subTopologyInfo.topology+"' from '"+subTopologyInfo.cloudProvider+"' is unknown! SKIP!");
				opResult = false;
				continue;
			}
			///identify the connectors of the deleted sub-topology are null
			if(subTopologyInfo.connectors != null){
				for(int tcpi = 0 ; tcpi < subTopologyInfo.connectors.size() ; tcpi++)
					subTopologyInfo.connectors.get(tcpi).ethName = null;
			}
			SEngine_delete s_delete = new SEngine_delete(subTopologyInfo, curCredential, database);
			sEAs.add((SEngineAdapter)s_delete);
			executor4ss.execute(s_delete);
		}
		////detach all the running sub-topologies with the deleted sub-topologies
		for(int sti = 0 ; sti < runningSTIs.size() ; sti++){
			SubTopologyInfo subTopologyInfo = runningSTIs.get(sti);
			Credential curCredential = userCredential.cloudAccess.get(subTopologyInfo.cloudProvider.toLowerCase());
			if(curCredential == null){
				logger.error("The credential for sub-topology '"
							+subTopologyInfo.topology+"' from '"
							+subTopologyInfo.cloudProvider+"' is unknown! SKIP!");
				opResult = false;
				continue;
			}
			Database database = userDatabase.databases.get(subTopologyInfo.cloudProvider.toLowerCase());
			if(database == null){
				logger.error("The database for sub-topology '"
							+subTopologyInfo.topology+"' from '"
							+subTopologyInfo.cloudProvider+"' is unknown! SKIP!");
				opResult = false;
				continue;
			}
			SEngine_detach sd = new SEngine_detach(subTopologyInfo, curCredential, database);
			sEAs.add((SEngineAdapter)sd);
			executor4ss.execute(sd);
		}
		
		executor4ss.shutdown();
		try {
			int count = 0;
			while (!executor4ss.awaitTermination(2, TimeUnit.SECONDS)){
				count++;
				if(count > 500*topTopology.topologies.size()){
					logger.error("Unknown error! Some sub-topology cannot be deleted!");
					return false;
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
			logger.error("Unexpected error!");
			return false;
		}
		
		topTopology.deleteScaledCopy();
		
		///overwrite the control information back to the top-topology
		if(!topTopology.overwirteControlOutput()){
			logger.error("Control information of top-topology has not been overwritten to the origin file!");
			return false;
		}
		if(!checkSEnginesResults(sEAs))
			return false;
		
		if(!opResult)
			return false;
		
		return true;
	}
	
	/**
	 * Delete all the 'running', 'stopped' and 'failed' sub-topologies.
	 * @param topTopology
	 * @param userCredential
	 * @param userDatabase
	 * @param stopReqs
	 */
	public boolean deleteAll(TopTopology topTopology, 
			UserCredential userCredential, UserDatabase userDatabase){
		if(userCredential.cloudAccess == null){
			logger.error("The credentials for cloud providers must be initialized!");
			return false;
		}if(userDatabase.databases == null){
			logger.error("The databases for different cloud providers must be initialized!");
			return false;
		}
		
		ArrayList<SubTopologyInfo> deleteSTIs = new ArrayList<SubTopologyInfo>();
		for(int sti = 0 ; sti < topTopology.topologies.size() ; sti++){
			SubTopologyInfo curSTI = topTopology.topologies.get(sti);
			if(curSTI.status.trim().toLowerCase().equals("running")
					|| curSTI.status.trim().toLowerCase().equals("stopped")
					|| curSTI.status.trim().toLowerCase().equals("failed"))
				deleteSTIs.add(curSTI);
		}
		if(deleteSTIs.size() == 0){
			logger.warn("None of the sub-topologies needs to be deleted!");
			return true; 
		}
		boolean opResult = true;
		ExecutorService executor4ss = Executors.newFixedThreadPool(deleteSTIs.size());
		ArrayList<SEngineAdapter> sEAs = new ArrayList<SEngineAdapter>();
		for(int sti = 0 ; sti < deleteSTIs.size() ; sti++){
			SubTopologyInfo subTopologyInfo = deleteSTIs.get(sti);
			Credential curCredential = userCredential.cloudAccess.get(subTopologyInfo.cloudProvider.toLowerCase());
			if(curCredential == null){
				logger.error("The credential for sub-topology '"+subTopologyInfo.topology+"' from '"+subTopologyInfo.cloudProvider+"' is unknown! SKIP!");
				opResult = false;
				continue;
			}
			Database database = userDatabase.databases.get(subTopologyInfo.cloudProvider.toLowerCase());
			if(database == null){
				logger.error("The database for sub-topology '"+subTopologyInfo.topology+"' from '"+subTopologyInfo.cloudProvider+"' is unknown! SKIP!");
				opResult = false;
				continue;
			}
			///identify the connectors of the deleted sub-topology are null
			if(subTopologyInfo.connectors != null){
				for(int tcpi = 0 ; tcpi < subTopologyInfo.connectors.size() ; tcpi++)
					subTopologyInfo.connectors.get(tcpi).ethName = null;
			}
			SEngine_delete s_delete = new SEngine_delete(subTopologyInfo, curCredential, database);
			sEAs.add((SEngineAdapter)s_delete);
			executor4ss.execute(s_delete);
		}
		
		executor4ss.shutdown();
		try {
			int count = 0;
			while (!executor4ss.awaitTermination(2, TimeUnit.SECONDS)){
				count++;
				if(count > 500*deleteSTIs.size()){
					logger.error("Unknown error! Some sub-topology cannot be deleted!");
					return false;
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
			logger.error("Unexpected error!");
			return false;
		}
		
		topTopology.deleteScaledCopy();
		
		///overwrite the control information back to the top-topology
		if(!topTopology.overwirteControlOutput()){
			logger.error("Control information of top-topology has not been overwritten to the origin file!");
			return false;
		}
		
		if(!checkSEnginesResults(sEAs))
			return false;
		if(!opResult)
			return false;
		return true;
	}
	
	/**
	 * Only recover the 'failed' sub-topologies according to the recover requests.
	 * If the 'failed' sub-topology is not specified by a recover request, it 
	 * will not be recovered.
	 * @param topTopology
	 * @param userCredential
	 * @param userDatabase
	 */
	public boolean recover(TopTopology topTopology, UserCredential userCredential, 
			UserDatabase userDatabase, RecoverRequest recoverReqs){
		if(userCredential.cloudAccess == null){
			logger.error("The credentials for cloud providers must be initialized!");
			return false;
		}if(userDatabase.databases == null){
			logger.error("The databases for different cloud providers must be initialized!");
			return false;
		}
		long recoverStart = System.currentTimeMillis();
		
		if(recoverReqs == null || recoverReqs.content.size() == 0){
			logger.warn("There is no recover request! Nothing needs to be recovered!");
			return true;
		}
		
		///This list contains all the failed sub-topologies
		ArrayList<SubTopologyInfo> failedSTs = new ArrayList<SubTopologyInfo>();
		for(int si = 0 ; si < topTopology.topologies.size() ; si++){
			SubTopologyInfo curSTI = topTopology.topologies.get(si);
			for(Map.Entry<RecoverReqEle, Boolean> entry: recoverReqs.content.entrySet()){
				RecoverReqEle curEle = entry.getKey();
				if(curEle.topologyName.equals(curSTI.topology)
						&& !entry.getValue()){
					if(!topTopology.topologies.get(si).status.equals("failed")){
						logger.warn("The sub-topology '"+curSTI.topology+"' is not 'failed'! Ignore!");
						continue;
					}
					////by default, it recovers from the original datacenter
					if(curEle.cloudProvider == null
							|| curEle.domain == null){
						curEle.cloudProvider = curSTI.cloudProvider;
						curEle.domain = curSTI.domain;
					}
					recoverReqs.content.put(curEle, true);
					////if the recovered sub-topology is recovered from another cloud provider
					////the VM type also needs to be updated. For example, 't2.small' for EC2. 'XOSmall' for ExoGENI.
					if(!curSTI.cloudProvider.equals(curEle.cloudProvider)){
						Database database = userDatabase.databases.get(curEle.cloudProvider.toLowerCase());
						if(database == null){
							logger.error("The database of provider '"+curEle.cloudProvider+"' is unknown!");
							return false;
						}
						if(!updateTheVMType(curSTI.subTopology, database, curEle.domain))
							return false;
					}
					curSTI.status = "deleted"; ///change the status in order to provision
					curSTI.domain = curEle.domain;
					curSTI.cloudProvider = curEle.cloudProvider;
					if(userCredential.sshAccess.containsKey(curEle.domain)){
						curSTI.sshKeyPairId = userCredential.sshAccess.get(curEle.domain).SSHKeyPairId;
						curSTI.subTopology.accessKeyPair = userCredential.sshAccess.get(curEle.domain);
					}
					else{
						curSTI.sshKeyPairId = null;
						curSTI.subTopology.accessKeyPair = null;
					}	
					failedSTs.add(topTopology.topologies.get(si));
					
					////update the classes content
					ArrayList<VM> vms = curSTI.subTopology.getVMsinSubClass();
					if(curEle.scaledClasses == null)
						curEle.scaledClasses = new ClassSet();
					
					for(int vi = 0 ; vi<vms.size() ; vi++)
						vms.get(vi).VEngineClass = curEle.scaledClasses.VEngineClass;
					curSTI.subTopologyClass = curEle.scaledClasses.SubTopologyClass;
					curSTI.subTopology.SEngineClass = curEle.scaledClasses.SEngineClass;
					curSTI.subTopology.subTopologyClass = curEle.scaledClasses.SubTopologyClass;
				}
			}
		}
		
		if(failedSTs.size() == 0){
			logger.error("There is no sub-topology need to be recovered!");
			return true;
		}
		boolean opResult = true;
		////Using multi-thread to recover the failed topologies.
		ExecutorService executor4rc = Executors.newFixedThreadPool(failedSTs.size());
		ArrayList<SEngineAdapter> sEAs = new ArrayList<SEngineAdapter>();
		for(int sti = 0 ; sti < failedSTs.size() ; sti++){
			SubTopologyInfo subTopologyInfo = failedSTs.get(sti);
			logger.info("Recovering sub-topology '"+subTopologyInfo.topology+"' from '"+subTopologyInfo.cloudProvider+"'");
			Credential curCredential = userCredential.cloudAccess.get(subTopologyInfo.cloudProvider.toLowerCase());
			if(curCredential == null){
				logger.error("The credential for sub-topology '"+subTopologyInfo.topology+"' from '"+subTopologyInfo.cloudProvider+"' is unknown! SKIP!");
				opResult = false;
				continue;
			}
			Database database = userDatabase.databases.get(subTopologyInfo.cloudProvider.toLowerCase());
			if(database == null){
				logger.error("The database for sub-topology '"+subTopologyInfo.topology+"' from '"+subTopologyInfo.cloudProvider+"' is unknown! SKIP!");
				opResult = false;
				continue;
			}
			SEngine_provision sr = new SEngine_provision(subTopologyInfo, curCredential, database);
			sEAs.add((SEngineAdapter)sr);
			executor4rc.execute(sr);
		}
		
		executor4rc.shutdown();
		try {
			int count = 0;
			while (!executor4rc.awaitTermination(2, TimeUnit.SECONDS)){
				count++;
				if(count > 500*failedSTs.size()){
					logger.error("Unknown error! Some sub-topology cannot be provisioned!");
					return false;
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
			logger.error("Unexpected error!");
			return false;
		}
		
		if(connectionConf(topTopology.topologies, userCredential, userDatabase))
			logger.info("All the running sub-topologies have been connected!");
		else{
			logger.warn("Some of running sub-topologies may not be connected to the failed ones!");
			opResult = false;
		}
		long recoverEnd = System.currentTimeMillis();
		logger.info("The total recovery overhead is "+ (recoverEnd-recoverStart));
		///overwrite the control information back to the top-topology
		if(!topTopology.overwirteControlOutput()){
			logger.error("Control information of top-topology has not been overwritten to the origin file!");
			return false;
		}
		if(!checkSEnginesResults(sEAs))
			return false;
		if(!opResult)
			return false;
		return true;
	}
	
	/**
	 * Recover all the 'failed' sub-topologies according to the recover requests.
	 * If the some of the 'failed' sub-topologies is not specified by the recover 
	 * requests, then they will be recovered from the origin domains.
	 * If the recoverReqs is null, then all the failed sub-topologies will be 
	 * recovered from the original domains.
	 * @param topTopology
	 * @param userCredential
	 * @param userDatabase
	 */
	public boolean recoverAll(TopTopology topTopology, UserCredential userCredential, 
			UserDatabase userDatabase, RecoverRequest recoverReqs){
		if(userCredential.cloudAccess == null){
			logger.error("The credentials for cloud providers must be initialized!");
			return false;
		}if(userDatabase.databases == null){
			logger.error("The databases for different cloud providers must be initialized!");
			return false;
		}
		
		long recoverStart = System.currentTimeMillis();
		
		////update the failed domain information according to the recovery requests
		if(recoverReqs == null || recoverReqs.content.size() == 0){
			logger.warn("There is no recover request! All the failed sub-topology will just be recovered from the origin domain!");
		}
		///This list contains all the failed sub-topologies
		ArrayList<SubTopologyInfo> failedSTs = new ArrayList<SubTopologyInfo>();
		for(int si = 0 ; si < topTopology.topologies.size() ; si++){
			SubTopologyInfo curSTI = topTopology.topologies.get(si);
			for(Map.Entry<RecoverReqEle, Boolean> entry: recoverReqs.content.entrySet()){
				RecoverReqEle curEle = entry.getKey();
				if(curEle.topologyName.equals(curSTI.topology)
						&& !entry.getValue()){
					if(!curSTI.status.equals("failed")){
						logger.warn("The sub-topology '"+curSTI.topology+"' is not 'failed'! Ignore!");
						continue;
					}
					////by default, it recovers from the original datacenter
					if(curEle.cloudProvider == null
							|| curEle.domain == null){
						curEle.cloudProvider = curSTI.cloudProvider;
						curEle.domain = curSTI.domain;
					}
					recoverReqs.content.put(curEle, true);
					////if the recovered sub-topology is recovered from another cloud provider
					////the VM type also needs to be updated. For example, 't2.small' for EC2. 'XOSmall' for ExoGENI.
					if(!curSTI.cloudProvider.equals(curEle.cloudProvider)){
						Database database = userDatabase.databases.get(curEle.cloudProvider.toLowerCase());
						if(database == null){
							logger.error("The database of provider '"+curEle.cloudProvider+"' is unknown!");
							return false;
						}
						if(!updateTheVMType(curSTI.subTopology, database, curEle.domain))
							return false;
					}
					curSTI.status = "deleted"; ///change the status in order to provision
					curSTI.domain = curEle.domain;
					curSTI.cloudProvider = curEle.cloudProvider;
					if(userCredential.sshAccess.containsKey(curEle.domain)){
						curSTI.sshKeyPairId = userCredential.sshAccess.get(curEle.domain).SSHKeyPairId;
						curSTI.subTopology.accessKeyPair = userCredential.sshAccess.get(curEle.domain);
					}
					else{
						curSTI.sshKeyPairId = null;
						curSTI.subTopology.accessKeyPair = null;
					}	
					failedSTs.add(topTopology.topologies.get(si));
				}else{
					if(curSTI.status.equals("failed")){
						logger.warn("The sub-topology '"+curSTI.topology+"' will be recovered from the origin region!");
						curSTI.status = "deleted";
						failedSTs.add(curSTI);
						
						////update the classes content
						if(curEle.scaledClasses == null)
							curEle.scaledClasses = new ClassSet();
						ArrayList<VM> vms = curSTI.subTopology.getVMsinSubClass();
						for(int vi = 0 ; vi<vms.size() ; vi++)
							vms.get(vi).VEngineClass = curEle.scaledClasses.VEngineClass;
						curSTI.subTopologyClass = curEle.scaledClasses.SubTopologyClass;
						curSTI.subTopology.SEngineClass = curEle.scaledClasses.SEngineClass;
						curSTI.subTopology.subTopologyClass = curEle.scaledClasses.SubTopologyClass;
					}
				}
			}
		}

		if(failedSTs.size() == 0){
			logger.error("There is no sub-topology need to be recovered!");
			return true;
		}
		boolean opResult = false;
		
		////Using multi-thread to recover the failed topologies.
		ExecutorService executor4rc = Executors.newFixedThreadPool(failedSTs.size());
		ArrayList<SEngineAdapter> sEAs = new ArrayList<SEngineAdapter>();
		for(int sti = 0 ; sti < failedSTs.size() ; sti++){
			SubTopologyInfo subTopologyInfo = failedSTs.get(sti);
			logger.info("Recovering sub-topology '"+subTopologyInfo.topology+"' from '"+subTopologyInfo.cloudProvider+"'");
			Credential curCredential = userCredential.cloudAccess.get(subTopologyInfo.cloudProvider.toLowerCase());
			if(curCredential == null){
				logger.error("The credential for sub-topology '"+subTopologyInfo.topology+"' from '"+subTopologyInfo.cloudProvider+"' is unknown! SKIP!");
				opResult = false;
				continue;
			}
			Database database = userDatabase.databases.get(subTopologyInfo.cloudProvider.toLowerCase());
			if(database == null){
				logger.error("The database for sub-topology '"+subTopologyInfo.topology+"' from '"+subTopologyInfo.cloudProvider+"' is unknown! SKIP!");
				opResult = false;
				continue;
			}
			SEngine_provision sr = new SEngine_provision(subTopologyInfo, curCredential, database);
			sEAs.add((SEngineAdapter)sr);
			executor4rc.execute(sr);
		}
		
		executor4rc.shutdown();
		try {
			int count = 0;
			while (!executor4rc.awaitTermination(2, TimeUnit.SECONDS)){
				count++;
				if(count > 500*failedSTs.size()){
					logger.error("Unknown error! Some sub-topology cannot be provisioned!");
					return false;
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
			logger.error("Unexpected error!");
			return false;
		}
		
		if(connectionConf(topTopology.topologies, userCredential, userDatabase))
			logger.info("All the running sub-topologies have been connected!");
		else{
			logger.warn("Some of running sub-topologies may not be connected to the failed ones!");
			opResult = false;
		}
		long recoverEnd = System.currentTimeMillis();
		logger.info("The total recovery overhead is "+ (recoverEnd-recoverStart));
		///overwrite the control information back to the top-topology
		if(!topTopology.overwirteControlOutput()){
			logger.error("Control information of top-topology has not been overwritten to the origin file!");
			return false;
		}
		if(!checkSEnginesResults(sEAs))
			return false;
		if(!opResult)
			return false;
		return true;
	}
	
	/**
	 * Output all the controlling information to the current directory
	 * in order to back up these information for future human to check.
	 * Then control S-Engine to mark this sub-topology as failed and other 
	 * sub-topologies to disconnect with this part.
	 */
	public boolean detectFailure(TopTopology topTopology, 
			UserCredential userCredential, UserDatabase userDatabase, 
			String subTopologyName){
		
		boolean find = false;
		for(int si = 0 ; si < topTopology.topologies.size() ; si++){
			SubTopologyInfo curSTI = topTopology.topologies.get(si);
			if(curSTI.topology.equals(subTopologyName)){
				find = true;
				SubTopology curSubTopology = curSTI.subTopology;
				String currentDir = CommonTool.getPathDir(topTopology.topologies.get(si).subTopology.loadingPath);
				if(!curSubTopology.outputControlInfo(currentDir
						+"_failed_"+curSTI.topology+"_"+System.currentTimeMillis()+".yml")){
					logger.error("Cannot back up failed sub-topology "+curSTI.topology+"!");
					return false;
				}
				Credential credential = userCredential.cloudAccess.get(curSTI.cloudProvider.toLowerCase());
				if(credential == null){
					logger.error("The credential for sub-topology '"
								+curSTI.topology+"' from '"+curSTI.cloudProvider
								+"' is unknown! SKIP!");
					return false;
				}
				Database database = userDatabase.databases.get(curSTI.cloudProvider.toLowerCase());
				if(database == null){
					logger.error("The database for sub-topology '"
								+curSTI.topology+"' from '"+curSTI.cloudProvider
								+"' is unknown! SKIP!");
					return false;
				}
				Object xSEngine;
				try {
					xSEngine = ClassDB.getSEngine(curSTI.cloudProvider, curSubTopology.SEngineClass).newInstance();
				} catch (InstantiationException | IllegalAccessException e) {
					e.printStackTrace();
					logger.error(e.getMessage());
					return false;
				}
				
				
				if(!((SEngineKeyMethod)xSEngine).markFailure(curSTI, credential, database)){
					logger.error("'"+curSTI.topology+"' cannot be marked as 'failed'!");
					return false;
				}
				break;
			}
		}
		if(!find){
			logger.warn("The sub-topology '"+subTopologyName+"' cannot be found!");
			return false;
		}
		
		boolean opResult = true;
		////Using multi-thread to delete all the failed connected tunnels expect the failed sub-topology itself.
		ExecutorService executor4rm = Executors.newFixedThreadPool(topTopology.topologies.size());
		for(int sti = 0 ; sti < topTopology.topologies.size() ; sti++){
			SubTopologyInfo subTopologyInfo = topTopology.topologies.get(sti);
			if(subTopologyInfo.topology.equals(subTopologyName))
				continue;
			logger.info("Marking the failure connections for sub-topology '"
						+subTopologyInfo.topology+"' from '"+subTopologyInfo.cloudProvider+"'");
			Credential curCredential = userCredential.cloudAccess.get(subTopologyInfo.cloudProvider.toLowerCase());
			if(curCredential == null){
				logger.error("The credential for sub-topology '"+subTopologyInfo.topology+"' from '"+subTopologyInfo.cloudProvider+"' is unknown! SKIP!");
				opResult = false;
				continue;
			}
			Database database = userDatabase.databases.get(subTopologyInfo.cloudProvider.toLowerCase());
			if(database == null){
				logger.error("The database for sub-topology '"+subTopologyInfo.topology+"' from '"+subTopologyInfo.cloudProvider+"' is unknown! SKIP!");
				opResult = false;
				continue;
			}
			SEngine_detach sd = new SEngine_detach(subTopologyInfo, curCredential, database);
			executor4rm.execute(sd);
		}
		executor4rm.shutdown();
		try {
			int count = 0;
			while (!executor4rm.awaitTermination(2, TimeUnit.SECONDS)){
				count++;
				if(count > 200*topTopology.topologies.size()){
					logger.error("Unknown error! Some sub-topology cannot be provisioned!");
					return false;
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
			logger.error("Unexpected error!");
			return false;
		}
		
		///overwrite the control information back to the top-topology
		if(!topTopology.overwirteControlOutput()){
			logger.error("Control information of top-topology has not been overwritten to the origin file!");
			return false;
		}
		if(!opResult)
			return false;
		return true;
			
	}
	
	private boolean checkSEnginesResults(ArrayList<SEngineAdapter> sEngineAdapters){
		for(int si = 0 ; si<sEngineAdapters.size() ; si++)
			if(!sEngineAdapters.get(si).opResult)
				return false;

		return true;
	}
	
	/**
	 * Update the corresponding VM type in 'curST' for the cloud provider 'cp' 
	 * and datacenter name 'dm'. If the corresponding VM cannot be found, then return
	 * false 
	 * @return
	 */
	private boolean updateTheVMType(SubTopology curST, 
			Database database, String dm){
		ArrayList<VM> vms = curST.getVMsinSubClass();
		for(int vi = 0 ; vi < vms.size() ; vi++){
			VM curVM = vms.get(vi);
			curVM.nodeType = database.getVMType(dm, curVM.OStype,
							Double.valueOf(curVM.CPU), Double.valueOf(curVM.Mem));
			if(curVM.nodeType == null){
				logger.error("The most close VM information cannot be updated!"
						+ " To find CPU:"+curVM.CPU+" MEM:"+curVM.Mem+" in "+dm);
				return false;
			}
			logger.debug("Update the most close VM type '"+curVM.nodeType+"' for "
					+ curVM.name+" in "+dm);
		}
		return true;
	}
	
	/**
	 * This is a horizontal scaling up or down in the sub-topology level. 
	 */
	public boolean horizontalScaleSLevel(TopTopology topTopology, 
			UserCredential userCredential, UserDatabase userDatabase, 
			HScalingSTRequest scaleReqs, boolean scalingUpDown){
		if(scaleReqs == null || scaleReqs.content.size() == 0){
			logger.warn("There is nothing to be scaled!");
			return true;
		}
		
		///This list contains all the sub-topologies to be scaled
		///If it is to scale up, this list contains all the scaling copy
		///otherwise, it contains all the original sub-topologies to be scaled down
		ArrayList<SubTopologyInfo> scaledSTIsToStart = new ArrayList<SubTopologyInfo>();
		ArrayList<SubTopologyInfo> scaledSTIsToProvision = new ArrayList<SubTopologyInfo>();
		ArrayList<SubTopologyInfo> scaledSTIsDown = new ArrayList<SubTopologyInfo>();
		for(int si = 0 ; si < topTopology.topologies.size() ; si++){
			SubTopologyInfo curSTI = topTopology.topologies.get(si);
			for(Map.Entry<STScalingReqEle, Boolean> entry: scaleReqs.content.entrySet()){
				STScalingReqEle curEle = entry.getKey();
				if(curEle.targetTopology.equals(curSTI.topology)
						&& !entry.getValue()){
					////if it is to scale up, generate the scaling copy
					if(scalingUpDown){
						if(curEle.cloudProvider == null
								|| curEle.domain == null){
							logger.warn("The cloud provider and domain both should be "
									+ "specified in the request! Scale "+curEle.targetTopology
									+" at original domain by default!");
							curEle.cloudProvider = curSTI.cloudProvider;
							curEle.domain = curSTI.domain;
						}
						if(curEle.scaledTopology == null){
							if(curSTI.status.equals(Values.STStatus.stopped)
									&& curSTI.cloudProvider.trim().equalsIgnoreCase(curEle.cloudProvider)
									&& curSTI.domain.trim().equalsIgnoreCase(curEle.domain)){
								scaledSTIsToStart.add(curSTI);
								scaleReqs.content.put(curEle, true);
								continue;
							}
							if((curSTI.status.equals(Values.STStatus.deleted) 
									|| curSTI.status.equals(Values.STStatus.fresh))
									&& curSTI.cloudProvider.trim().equalsIgnoreCase(curEle.cloudProvider)
									&& curSTI.domain.trim().equalsIgnoreCase(curEle.domain)){
								scaledSTIsToProvision.add(curSTI);
								scaleReqs.content.put(curEle, true);
								continue;
							}
						}
						////if the sub-topology is 'running' and 'failed', the scaling one
						////must be always a generated copy to be provisioned
						SubTopologyInfo scaledSTI = topTopology.genScalingSTFromST(curEle.targetTopology, 
														curEle.scaledTopology, curEle.cloudProvider, curEle.domain,
														curEle.scaledClasses);
						if(scaledSTI == null)
							return false;
						
						if(!curSTI.cloudProvider.equals(curEle.cloudProvider)){
							Database database = userDatabase.databases.get(
													curEle.cloudProvider.toLowerCase());
							if(database == null){
								logger.error("The database of provider '"
												+curEle.cloudProvider+"' is unknown!");
								return false;
							}
							if(!updateTheVMType(curSTI.subTopology, database, curEle.domain))
								return false;
						}
						scaledSTIsToProvision.add(scaledSTI);
					}else
						scaledSTIsDown.add(curSTI);
					
					scaleReqs.content.put(curEle, true);
				}
			}
		}
		if(scaledSTIsToProvision.size() == 0
				&& scaledSTIsToStart.size() == 0
				&& scaledSTIsDown.size() == 0){
			logger.warn("No sub-topology needs to be scaled!");
			return true;
		}
		
		long scalingStart = System.currentTimeMillis();
		if(scalingUpDown){
			if(!scaleUp(topTopology, userCredential, userDatabase, 
					scaledSTIsToProvision, scaledSTIsToStart))
				return false;
		}else{
			if(!scaleDown(topTopology, userCredential, userDatabase, 
					scaledSTIsDown))
				return false;
		}
		long scalingEnd = System.currentTimeMillis();
		logger.info("The total S-Level horisontal scaling overhead is "+ (scalingEnd-scalingStart));
		///overwrite the control information back to the top-topology
		if(!topTopology.overwirteControlOutput()){
			logger.error("Control information of top-topology has not been overwritten to the origin file!");
			return false;
		}
		return true;
	}
	
	/**
	 * This is a horizontal scaling up in the VM level. No scaling down
	 * for this level. 
	 */
	public boolean horizontalScaleVLevel(TopTopology topTopology, 
			UserCredential userCredential, UserDatabase userDatabase, 
			HScalingVMRequest scaleReqs){
		if(scaleReqs == null || scaleReqs.content.size() == 0){
			logger.warn("There is nothing to be scaled!");
			return true;
		}
		
		long scalingStart = System.currentTimeMillis();
		///This list contains all generated sub-topologies to be scaled up
		ArrayList<SubTopologyInfo> scaledSTIs = new ArrayList<SubTopologyInfo>();
		for(Map.Entry<VMScalingReqEle, Boolean> entry: scaleReqs.content.entrySet()){
			ArrayList<String> targetVMs = entry.getKey().targetVMs;
			if(targetVMs == null || targetVMs.size() == 0){
				logger.error("Invalid VM scaling request of "+entry.getKey().reqID+" for on VM!");
				return false;
			}
			SubTopologyInfo scaledSTI = topTopology.genScalingSTFromVM(targetVMs, entry.getKey().scaledTopology, 
									entry.getKey().cloudProvider, entry.getKey().domain, 
									entry.getKey().scaledClasses);
			if(scaledSTI == null)
				return false;
			scaledSTIs.add(scaledSTI);
			scaleReqs.content.put(entry.getKey(), true);
		}
		///only provision the sub-topologies here
		if(!scaleUp(topTopology, userCredential, userDatabase, 
				scaledSTIs, null))
			return false;
		
		long scalingEnd = System.currentTimeMillis();
		logger.info("The total V-Level horisontal scaling overhead is "+ (scalingEnd-scalingStart));
		
		///overwrite the control information back to the top-topology
		if(!topTopology.overwirteControlOutput()){
			logger.error("Control information of top-topology has not been overwritten to the origin file!");
			return false;
		}
		return true;
	}
	
	public boolean verticalScale(TopTopology topTopology, 
			UserCredential userCredential, UserDatabase userDatabase, 
			VScalingVMRequest scaleReqs){
		if(scaleReqs == null || scaleReqs.content.size() == 0){
			logger.warn("There is nothing to be scaled!");
			return true;
		}
		long scalingStart = System.currentTimeMillis();
		for(Map.Entry<VMVScalingReqEle, Boolean> entry: scaleReqs.content.entrySet()){
			VMVScalingReqEle curEle = entry.getKey();
			if(curEle.targetCPU <= 0 || curEle.targetMEM <= 0
					|| curEle.orgVMName == null 
					|| !topTopology.VMIndex.containsKey(curEle.orgVMName)){
				logger.error("Invalid request for req "+curEle.reqID+"!");
				return false;
			}
			VM targetVM = topTopology.VMIndex.get(curEle.orgVMName);
			SubTopologyInfo targetSTI = targetVM.ponintBack2STI;
			if(!targetSTI.status.equals("running")){
				logger.error("The status of '"+targetSTI.topology
						+"' must be 'running' to be vertically scaled!");
				return false;
			}
		}
		DeleteRequest deleteReqs = new DeleteRequest();
		RecoverRequest recoverReqs = new RecoverRequest();
		ProvisionRequest provisionReqs = new ProvisionRequest();
		for(Map.Entry<VMVScalingReqEle, Boolean> entry: scaleReqs.content.entrySet()){
			VMVScalingReqEle curEle = entry.getKey();
			VM targetVM = topTopology.VMIndex.get(curEle.orgVMName);
			SubTopologyInfo targetSTI = targetVM.ponintBack2STI;
			
			Database database = userDatabase.databases.get(
					targetSTI.cloudProvider.toLowerCase());
			if(database == null){
				logger.error("The database of provider '"
								+targetSTI.cloudProvider+"' is unknown!");
				return false;
			}
			String newNodeType = database.getVMType(targetSTI.domain, targetVM.OStype,
					Double.valueOf(curEle.targetCPU), Double.valueOf(curEle.targetMEM));
			if(newNodeType == null)
				continue;
			if(targetVM.nodeType.trim().equals(newNodeType))
				continue;
			
			boolean supportSeparate = true;
			Class<?> CurSEngine = ClassDB.getSEngine(targetSTI.cloudProvider, 
					targetSTI.subTopology.SEngineClass);
			if(CurSEngine == null){
				String msg = "SEngine cannot be loaded for '"+targetSTI.topology+"'!";
				logger.error(msg);
				targetSTI.logsInfo.put(targetSTI.topology+"#ERROR", msg);
				return false;
			}
			try {
				Object sEngine = CurSEngine.newInstance();
				supportSeparate = ((SEngineKeyMethod)sEngine).supportSeparate();
			} catch (InstantiationException | IllegalAccessException e) {
				return false;
			}
			String tmpSTName = null;
			if(supportSeparate){
				tmpSTName = "_tmp_";
				deleteReqs.content.put(tmpSTName, false);
				provisionReqs.content.put(tmpSTName, false);
			}
			else{
				////first generate the most possible vertical scaling sub-topology name
				tmpSTName = targetSTI.topology;
				String actualSTName = null;
				if(tmpSTName.endsWith("_vscale"))
					actualSTName = tmpSTName.split("_vscale")[0];
				else 
					actualSTName = tmpSTName;
				while(true){
					tmpSTName = actualSTName+"_vscale";
					if(!topTopology.subTopologyIndex.containsKey(tmpSTName))
						break;
					else
						actualSTName = actualSTName+"_vscale";
				}
				
				RecoverReqEle recEle = recoverReqs.new RecoverReqEle();
				recEle.topologyName = tmpSTName;
				recoverReqs.content.put(recEle, false);
			}
			if(!topTopology.migrateVM2STI(curEle.orgVMName, tmpSTName))
				return false;
			
			topTopology.subTopologyIndex.get(tmpSTName).scaledFrom = targetSTI.topology;

			//// exogeni for example
			if(!supportSeparate){
				////generate a fake VM and put back to the original sub-topology
				Class<?> vmClass = targetSTI.subTopology.getVMClass();
				VM fakeVM;
				try {
					fakeVM = (VM) vmClass.newInstance();
				} catch (InstantiationException | IllegalAccessException e) {
					return false;
				}
				fakeVM.name = targetVM.name;
				fakeVM.type = targetVM.type;
				fakeVM.nodeType = newNodeType;
				fakeVM.CPU = targetVM.CPU;
				fakeVM.Mem = targetVM.Mem;
				fakeVM.OStype = targetVM.OStype;
				fakeVM.defaultSSHAccount = targetVM.defaultSSHAccount;
				fakeVM.fake  = "true";
				targetSTI.subTopology.addVMInSubClass(fakeVM);
				
				targetVM.ponintBack2STI = topTopology.subTopologyIndex.get(tmpSTName);
			}
			
		}
		
		
		for(int sti = 0 ; sti<topTopology.topologies.size() ; sti++){
			ArrayList<VM> vms = topTopology.topologies.get(sti).subTopology.getVMsinSubClass();
			boolean inUse = false;
			for(int vi = 0 ; vi<vms.size() ; vi++){
				if(vms.get(vi).fake == null || !vms.get(vi).fake.trim().equalsIgnoreCase("true")){
					inUse = true;
					break;
				}
			}if(!inUse 
					&& topTopology.topologies.get(sti).scaledFrom != null)
				deleteReqs.content.put(topTopology.topologies.get(sti).topology, false);
		}
		
		if(recoverReqs.content.size() != 0){
			for(Map.Entry<RecoverReqEle, Boolean> entry: recoverReqs.content.entrySet()){
				RecoverReqEle curEle = entry.getKey();
				if(!detectFailure(topTopology, userCredential, userDatabase, 
												curEle.topologyName)){
					logger.error(curEle.topologyName+" cannot be marked as faied!");
					return false;
				}
			}
		}
		if(deleteReqs.content.size() != 0){
			if(!delete(topTopology, userCredential, userDatabase, deleteReqs)){
				logger.error("Some vertically scaled VMs cannot be deleted firstly!");
				return false;
			}
		}
		
		for(Map.Entry<VMVScalingReqEle, Boolean> entry: scaleReqs.content.entrySet()){
			VMVScalingReqEle curEle = entry.getKey();
			VM curVM = topTopology.VMIndex.get(curEle.orgVMName);
			SubTopologyInfo curSTI = curVM.ponintBack2STI;
			Database database = userDatabase.databases.get(
					curSTI.cloudProvider.toLowerCase());
			if(database == null){
				logger.error("The database of provider '"
								+curSTI.cloudProvider+"' is unknown!");
				return false;
			}
			String newNodeType = database.getVMType(curSTI.domain, curVM.OStype,
					Double.valueOf(curEle.targetCPU), Double.valueOf(curEle.targetMEM));
			
			if(newNodeType == null)
				continue;
			if(curVM.nodeType.trim().equals(newNodeType))
				continue;
			curVM.nodeType = newNodeType;
		}
		
		///recover the ones that cannot be managed separately
		if(recoverReqs.content.size() != 0){
			if(!recover(topTopology, userCredential, userDatabase, recoverReqs)){
				logger.error("Some vertically scaled VMs cannot be recovered!");
				return false;
			}
			///update some public address information
			for(Map.Entry<VMVScalingReqEle, Boolean> entry: scaleReqs.content.entrySet()){
				VMVScalingReqEle curEle = entry.getKey();
				VM curVM = topTopology.VMIndex.get(curEle.orgVMName);
				SubTopologyInfo curSTI = curVM.ponintBack2STI;
				if(curSTI.topology.endsWith("_vscale")){
					String actualSTName = curSTI.topology.split("_vscale")[0];
					SubTopologyInfo actualSTI = topTopology.subTopologyIndex.get(actualSTName);
					if(actualSTI != null){
						VM orgVM = actualSTI.subTopology.getVMinSubClassbyName(curVM.name);
						orgVM.nodeType = curVM.nodeType;
						orgVM.CPU = curVM.CPU;
						orgVM.Mem = curVM.Mem;
						orgVM.publicAddress = curVM.publicAddress;
						actualSTI.subTopology.overwirteControlOutput();
					}
				}
			}
		}
		
		////provision the ones that can be managed separately in '_tmp_'
		if(provisionReqs.content.size() != 0){
			if(!provision(topTopology, userCredential, userDatabase, provisionReqs)){
				logger.error("Some vertically scaled VMs cannot be provisioned!");
				return false;
			}
			
			SubTopologyInfo tmpSTI = topTopology.subTopologyIndex.get("_tmp_");
			ArrayList<VM> vms = tmpSTI.subTopology.getVMsinSubClass();
			for(int tvi = 0 ; tvi<vms.size() ; tvi++ ){
				VM curVM = vms.get(tvi);
				String orgSTName = curVM.ponintBack2STI.topology;
				////migrate the VM back to the original sub-topology
				if(!topTopology.migrateVM2STI(curVM.name, orgSTName))
					return false;
			}
			
			///delete the temperate sub-topology '_tmp_'
			if(!topTopology.topologies.remove(tmpSTI)){
				logger.error("There is no sub-topology '"+tmpSTI.topology+"' in "
						+ "topTopology list!");
				return false;
			}
			topTopology.subTopologyIndex.remove(tmpSTI);
		}
		
		long scalingEnd = System.currentTimeMillis();
		logger.info("The total V-Level vertical scaling overhead is "+ (scalingEnd-scalingStart));
		
		///overwrite the control information back to the top-topology
		if(!topTopology.overwirteControlOutput()){
			logger.error("Control information of top-topology has not been overwritten to the origin file!");
			return false;
		}
		return true;
	}
	
	private boolean scaleUp(TopTopology topTopology, 
			UserCredential userCredential, UserDatabase userDatabase, 
			ArrayList<SubTopologyInfo> scaledSTIsToProvision,
			ArrayList<SubTopologyInfo> scaledSTIsToStart){
		if(!topTopology.completeConInfoFromSubnet()){
			logger.error("Connection info updated error!");
			return false;
		}
		if(scaledSTIsToProvision != null 
				&& scaledSTIsToProvision.size() != 0){
			ProvisionRequest provisionReq = new ProvisionRequest();
			for(int si = 0 ; si < scaledSTIsToProvision.size() ; si++)
				provisionReq.content.put(scaledSTIsToProvision.get(si).topology, false);
			
			if(!provision(topTopology, userCredential, 
					userDatabase, provisionReq))
				return false;
		}
		if(scaledSTIsToStart != null
				&& scaledSTIsToStart.size() != 0){
			StartRequest startReq = new StartRequest();
			for(int si = 0 ; si < scaledSTIsToStart.size() ; si++)
				startReq.content.put(scaledSTIsToStart.get(si).topology, false);
			if(!start(topTopology, userCredential, 
					userDatabase, startReq))
				return false;
		}
		return true;
	}
	
	private boolean scaleDown(TopTopology topTopology, 
			UserCredential userCredential, UserDatabase userDatabase, 
			ArrayList<SubTopologyInfo> scaledSTIsDown){
		ArrayList<SubTopologyInfo> scaledSTIsStop = new ArrayList<SubTopologyInfo>();
		ArrayList<SubTopologyInfo> scaledSTIsDelete = new ArrayList<SubTopologyInfo>();
		for(int si = 0 ; si < scaledSTIsDown.size() ; si++){
			SubTopologyInfo curSTI = scaledSTIsDown.get(si);
			String cp = curSTI.cloudProvider.trim().toLowerCase();
			String sEngineClass = curSTI.subTopology.SEngineClass;
			Class<?> CurSEngine = ClassDB.getSEngine(cp, sEngineClass);
			if(CurSEngine == null){
				String msg = "SEngine cannot be loaded for '"+curSTI.topology
						+"'!";
				logger.warn(msg);
				curSTI.logsInfo.put(curSTI.topology+"#ERROR", msg);
				return false;
			}
			try {
				Object sEngine = CurSEngine.newInstance();
				if(((SEngineKeyMethod)sEngine).supportStop(curSTI))
					scaledSTIsStop.add(curSTI);
				else
					scaledSTIsDelete.add(curSTI);
			} catch (InstantiationException | IllegalAccessException e) {
				e.printStackTrace();
				logger.error(e.getMessage());
				return false;
			}
		}
		if(scaledSTIsDelete != null 
				&& scaledSTIsDelete.size() != 0){
			DeleteRequest deleteReq = new DeleteRequest();
			for(int si = 0 ; si < scaledSTIsDelete.size() ; si++)
				deleteReq.content.put(scaledSTIsDelete.get(si).topology, false);
			
			if(!delete(topTopology, userCredential, 
					userDatabase, deleteReq))
				return false;
		}
		if(scaledSTIsStop != null
				&& scaledSTIsStop.size() != 0){
			StopRequest stopReq = new StopRequest();
			for(int si = 0 ; si < scaledSTIsStop.size() ; si++)
				stopReq.content.put(scaledSTIsStop.get(si).topology, false);
			if(!stop(topTopology, userCredential, 
					userDatabase, stopReq))
				return false;
		}
		return true;
	}
	
	/**
	 * Complete the auto-scaling for the sub-topology whose name is subTopologyName 
	 * The actual scalSize is also decided by the scaling address 
	 * pool defined by the user. When 'scalingUpDown' is true, it means scaling up. Otherwise, 
	 * it means scaling down. Then several 'scaled' 'running' sub-topologies will be stopped or deleted. 
	 * The input parameter of 'scalDCs' contains which datacenter to scale. It contains 
	 * two kinds of information: Cloud provider and domain. 
	 * The sub-topology with the name of 'subTopologyName' 
	 * must have the tag of 'scaling' and must be in the status of 'running'. 
	 */
	/*public void autoScal(TopTopology topTopology, 
			UserCredential userCredential, UserDatabase userDatabase, 
			String subTopologyName, boolean scalingUpDown, ArrayList<ScalingRequest> scalDCs){
		boolean find = false;
		SubTopologyInfo scalSTI = null;
		for(int si = 0 ; si < topTopology.topologies.size() ; si++){
			SubTopologyInfo curSTI = topTopology.topologies.get(si);
			if(curSTI.topology.equals(subTopologyName)){
				find = true;
				if(!curSTI.tag.trim().toLowerCase().equals("scaling")){
					logger.warn("The target sub-topology is not a 'scaling' one. It cannot be scaled!");
					return;
				}else{
					if(!curSTI.status.trim().toLowerCase().equals("running")){
						logger.warn("Sub-topology '"+subTopologyName+"' can only be auto-scaling when it is running!");
						return;
					}
				}
				scalSTI = curSTI;
				break;
			}
		}
		if(!find){
			logger.warn("The sub-topology '"+subTopologyName+"' cannot be found!");
			return;
		}
		
		long scalingStart = System.currentTimeMillis();
		
		if(scalingUpDown){
			scalUp(topTopology, userCredential, userDatabase, 
					scalSTI, scalDCs);
		}else{
			scalDown(topTopology, userCredential, userDatabase, 
					scalSTI, scalDCs);
		}
		
		long scalingEnd = System.currentTimeMillis();
		logger.info("The total scaling overhead is "+ (scalingEnd-scalingStart));
		///overwrite the control information back to the top-topology
		if(!topTopology.overwirteControlOutput())
			logger.error("Control information of top-topology has not been overwritten to the origin file!");
	}
	
	///It's worth to mention here that the 'stopped' 'scaled' sub-topologies will also 
	///take up the addresses in the 'scalingAddressPool'. 
	private void scalUp(TopTopology topTopology, 
			UserCredential userCredential, UserDatabase userDatabase, 
			SubTopologyInfo scalSTI, ArrayList<ScalingRequest> scalDCs){
		////first check whether there exist 'stopped' sub-topologies that come from the list of scalDCs
		////We do not consider to provision the 'deleted' 'scaled' sub-topologies, because they cannot be provisioned.
		int actualScalingSize = 0;
		
		//record the 'stopped' ones which in the scalDC requests.
		ArrayList<SubTopologyInfo> needToBeStarted = new ArrayList<SubTopologyInfo>();
		ArrayList<SubTopologyInfo> freshSTIs = new ArrayList<SubTopologyInfo>();

		for(int si = 0 ; si < topTopology.topologies.size() ; si++){
			////If the status of the sub-topology is 'stopped', then this cloud provider must support stop feature.
			if(topTopology.topologies.get(si).status.trim().toLowerCase().equals("stopped")
				&& topTopology.topologies.get(si).tag.trim().toLowerCase().equals("scaled")
				&& topTopology.topologies.get(si).fatherTopology.topology.equals(scalSTI.topology)){
				for(int ri = 0 ; ri < scalDCs.size() ; ri++){
					if(!scalDCs.get(ri).satisfied
						&& topTopology.topologies.get(si).cloudProvider.trim().toLowerCase().equals(scalDCs.get(ri).cloudProvider.trim().toLowerCase())
						&& topTopology.topologies.get(si).domain.trim().toLowerCase().equals(scalDCs.get(ri).domain.trim().toLowerCase())){
						////collect the 'stopped' sub-topologies.
						needToBeStarted.add(topTopology.topologies.get(si));
						scalDCs.get(ri).satisfied = true;
						actualScalingSize++;
					}
				}
			}
			
			////find the 'fresh' 'scaled' sub-topology.
			if(topTopology.topologies.get(si).status.trim().toLowerCase().equals("fresh")
				&& topTopology.topologies.get(si).tag.trim().toLowerCase().equals("scaled")
				&& topTopology.topologies.get(si).fatherTopology.topology.equals(scalSTI.topology)){
				for(int ri = 0 ; ri < scalDCs.size() ; ri++){
					if(topTopology.topologies.get(si).cloudProvider.trim().toLowerCase().equals(scalDCs.get(ri).cloudProvider.trim().toLowerCase())
						&& topTopology.topologies.get(si).domain.trim().toLowerCase().equals(scalDCs.get(ri).domain.trim().toLowerCase())
						){
						////collect the 'fresh' sub-topologies.
						freshSTIs.add(topTopology.topologies.get(si));
						scalDCs.get(ri).satisfied = true;
						actualScalingSize++;
					}
				}
			}
		}
		
		
		ArrayList<SubTopologyInfo> copySTIs = new ArrayList<SubTopologyInfo>();
		////generate the 'fresh' sub-topology to be the scaled one.
		for(int ri = 0 ; ri < scalDCs.size() ; ri++){
			if(!scalDCs.get(ri).satisfied){
				String domain = scalDCs.get(ri).domain;
				String cloudProvider = scalDCs.get(ri).cloudProvider;
				
				Database database = userDatabase.databases.get(cloudProvider.trim().toLowerCase());
				if(database == null){
					logger.error("The database for request to scal in '"+cloudProvider+"' is unknown! SKIP!");
					continue;
				}
				
				SubTopologyInfo copySTI = SEngine_autoScaling.generateScalingCopy(cloudProvider, 
						domain, scalSTI, userCredential, database);
				if(copySTI == null)
					continue;
				copySTIs.add(copySTI);
				topTopology.topologies.add(copySTI);
				scalDCs.get(ri).satisfied = true;
				actualScalingSize++;
			}
		}
		logger.info("Actual scaling size is "+actualScalingSize);
		///generate the top connections
		for(int ci = 0 ; ci < copySTIs.size() ; ci++){
			SubTopologyInfo curSTI = copySTIs.get(ci);
			if(curSTI.connectors == null)
				continue;
			for(int coni = 0 ; coni < curSTI.connectors.size() ; coni++){
				TopConnectionPoint curTCP = curSTI.connectors.get(coni);
				TopConnection topConnection = new TopConnection();
				TopConnection fatherConnection = 
						CommonTool.getTopConnectionByPoint(topTopology.connections,
								curTCP.peerTCP);
				if(fatherConnection == null){
					logger.error("Some unknown errors!");
					continue ;
				}
				topConnection.bandwidth = fatherConnection.bandwidth;
				topConnection.latency = fatherConnection.latency;
				topConnection.name = fatherConnection.name + "_" + UUID.randomUUID().toString();
				topConnection.source = curTCP.peerTCP;
				topConnection.target = curTCP;
				
				////add the peer top connection point to the connectors of the subTopologyInfo
				SubTopologyInfo targetSTI = null;
				String [] t_VM = curTCP.peerTCP.vmName.split("\\.");
				String topologyName = t_VM[0];
				for(int sti = 0 ; sti < topTopology.topologies.size() ; sti++){
					if(topologyName.equals(topTopology.topologies.get(sti).topology)){
						targetSTI = topTopology.topologies.get(sti);
						targetSTI.connectors.add(curTCP.peerTCP);
						break;
					}
				}
				
				topTopology.connections.add(topConnection);
			}
		}
		
		////Overwrite the scaled topology to the files and update the information of the sub-topology
		topTopology.overwirteControlOutput();
		logger.info("The control file of top-level topology has been overwritten!");
		for(int sti = 0 ; sti < copySTIs.size() ; sti++){
			SubTopology subTopology = copySTIs.get(sti).subTopology;
			if(subTopology == null){
				logger.error("There is a null sub-topology!");
				continue ;
			}
			if(!subTopology.overwirteControlOutput())
				return;
			logger.info("The control file of 'scaled' sub-topology '"+subTopology.topologyName+"' has been overwritten!");
			
			if(!subTopology.commonFormatChecking("fresh"))
				return;
		}
		
		///add all the fresh sub-topologies into the copySTIs
		for(int sti = 0 ; sti < freshSTIs.size() ; sti++)
			copySTIs.add(freshSTIs.get(sti));
				
		///do the auto-scaling, including provisioning the new generated 'fresh' ones and 
		///starting the 'stopped' ones.
		int threadPoolSize = copySTIs.size() + needToBeStarted.size();
		ExecutorService executor4as = Executors.newFixedThreadPool(threadPoolSize);
		for(int csti = 0 ; csti < copySTIs.size() ; csti++){
			SubTopologyInfo subTopologyInfo = copySTIs.get(csti);
			logger.info("Scaling up sub-topology '"+subTopologyInfo.topology+"' from '"+subTopologyInfo.cloudProvider+"'");
			Credential curCredential = userCredential.cloudAccess.get(subTopologyInfo.cloudProvider.toLowerCase());
			if(curCredential == null){
				logger.error("The credential for sub-topology '"+subTopologyInfo.topology+"' from '"+subTopologyInfo.cloudProvider+"' is unknown! SKIP!");
				continue;
			}
			Database database = userDatabase.databases.get(subTopologyInfo.cloudProvider.toLowerCase());
			if(database == null){
				logger.error("The database for sub-topology '"+subTopologyInfo.topology+"' from '"+subTopologyInfo.cloudProvider+"' is unknown! SKIP!");
				continue;
			}
			SEngine_autoScaling sr = new SEngine_autoScaling(subTopologyInfo, curCredential, database);
			executor4as.execute(sr);
		}
		for(int sti = 0 ; sti < needToBeStarted.size() ; sti++){
			SubTopologyInfo subTopologyInfo = needToBeStarted.get(sti);
			logger.info("Starting sub-topology '"+subTopologyInfo.topology+"' from '"+subTopologyInfo.cloudProvider+"'");
			Credential curCredential = userCredential.cloudAccess.get(subTopologyInfo.cloudProvider.toLowerCase());
			if(curCredential == null){
				logger.error("The credential for sub-topology '"+subTopologyInfo.topology+"' from '"+subTopologyInfo.cloudProvider+"' is unknown! SKIP!");
				continue;
			}
			Database database = userDatabase.databases.get(subTopologyInfo.cloudProvider.toLowerCase());
			if(database == null){
				logger.error("The database for sub-topology '"+subTopologyInfo.topology+"' from '"+subTopologyInfo.cloudProvider+"' is unknown! SKIP!");
				continue;
			}
			SEngine_start s_start = new SEngine_start(subTopologyInfo, curCredential, database);
			executor4as.execute(s_start);
		}
		
		executor4as.shutdown();
		try {
			int count = 0;
			while (!executor4as.awaitTermination(2, TimeUnit.SECONDS)){
				count++;
				if(count > 500*threadPoolSize){
					logger.error("Unknown error! Some sub-topology cannot be provisioned!");
					return ;
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
			logger.error("Unexpected error!");
			return ;
		}
		
		////Configure the top connections for the sub-topologies
		if(interConnectionConf(topTopology.topologies, userCredential, userDatabase))
			logger.info("All the running sub-topologies have been connected!");
		else
			logger.warn("Some of failed sub-topology may not be connected!");
		
		if(!topTopology.overwirteControlOutput())
			logger.error("Control information of top-topology has not been overwritten to the origin file!");
	
	}

	////only the 'scaled' 'running' sub-topology can use auto-scaling to shut down.
	private void scalDown(TopTopology topTopology, 
			UserCredential userCredential, UserDatabase userDatabase, 
			SubTopologyInfo scalSTI, ArrayList<ScalingRequest> scalDCs){
		int actualScalingSize = 0;
		
		ArrayList<SubTopologyInfo> scalDownSTIs = new ArrayList<SubTopologyInfo>();
		for(int sti = 0 ; sti < topTopology.topologies.size() ; sti++){
			SubTopologyInfo curSTI = topTopology.topologies.get(sti);
			if(curSTI.status.trim().toLowerCase().equals("running")
					&& curSTI.tag.trim().toLowerCase().equals("scaled")){
				for(int ri = 0 ; ri < scalDCs.size() ; ri++){
					ScalingRequest curScalReq = scalDCs.get(ri);
					if(curScalReq.cloudProvider.trim().toLowerCase().equals(curSTI.cloudProvider.trim().toLowerCase())
							&& curScalReq.domain.trim().toLowerCase().equals(curSTI.domain.trim().toLowerCase())
							&& !curScalReq.satisfied){
						scalDownSTIs.add(curSTI);
						Credential curCredential = userCredential.cloudAccess.get(curSTI.cloudProvider.toLowerCase());
						if(curCredential == null){
							logger.error("The credential for sub-topology '"+curSTI.topology+"' from '"+curSTI.cloudProvider+"' is unknown! SKIP!");
							continue;
						}
						Database database = userDatabase.databases.get(curSTI.cloudProvider.toLowerCase());
						if(database == null){
							logger.error("The database for sub-topology '"+curSTI.topology+"' from '"+curSTI.cloudProvider+"' is unknown! SKIP!");
							continue;
						}
						SEngine_stop s_stop = new SEngine_stop(curSTI, curCredential, database);
						if(s_stop.ableToBeStopped()){
							curSTI.status = "stopped";
							logger.info("Sub-topology '"+curSTI.topology+"' will be scaled down (stopped)!");
						}else{
							curSTI.status = "deleted";
							logger.info("Sub-topology '"+curSTI.topology+"' will be scaled down (deleted)!");
						}
						curScalReq.satisfied = true;
						actualScalingSize++;
					}
				}
			}
		}
		logger.info("Actual scaling size is "+actualScalingSize);
		
		ArrayList<SubTopologyInfo> runningSTIs = new ArrayList<SubTopologyInfo>();
		for(int sti = 0 ; sti < topTopology.topologies.size() ; sti++){
			SubTopologyInfo curSTI = topTopology.topologies.get(sti);
			if(curSTI.status.trim().toLowerCase().equals("running"))
				runningSTIs.add(curSTI);
		}
		
		if(scalDownSTIs.size() == 0){
			logger.warn("There is no sub-topology can be scale down to satisfy the scaling request!");
			return ;
		}
		
		ExecutorService executor4ss = Executors.newFixedThreadPool(scalDownSTIs.size());
		for(int sti = 0 ; sti < scalDownSTIs.size() ; sti++){
			SubTopologyInfo subTopologyInfo = scalDownSTIs.get(sti);
			Credential curCredential = userCredential.cloudAccess.get(subTopologyInfo.cloudProvider.toLowerCase());
			if(curCredential == null){
				logger.error("The credential for sub-topology '"+subTopologyInfo.topology+"' from '"+subTopologyInfo.cloudProvider+"' is unknown! SKIP!");
				continue;
			}
			Database database = userDatabase.databases.get(subTopologyInfo.cloudProvider.toLowerCase());
			if(database == null){
				logger.error("The database for sub-topology '"+subTopologyInfo.topology+"' from '"+subTopologyInfo.cloudProvider+"' is unknown! SKIP!");
				continue;
			}
			///identify the connectors of the 'stopped' or 'deleted' sub-topology are null
			if(subTopologyInfo.connectors != null){
				for(int tcpi = 0 ; tcpi < subTopologyInfo.connectors.size() ; tcpi++)
					subTopologyInfo.connectors.get(tcpi).ethName = null;
			}
			if(subTopologyInfo.status.equals("stopped")){
				SEngine_stop s_stop = new SEngine_stop(subTopologyInfo, curCredential, database);
				executor4ss.execute(s_stop);
			}
			if(subTopologyInfo.status.equals("deleted")){
				SEngine_delete s_delete = new SEngine_delete(subTopologyInfo, curCredential, database);
				executor4ss.execute(s_delete);
			}
		}
		executor4ss.shutdown();
		try {
			int count = 0;
			while (!executor4ss.awaitTermination(2, TimeUnit.SECONDS)){
				count++;
				if(count > 500*topTopology.topologies.size()){
					logger.error("Unknown error! Some sub-topology cannot be shut down!");
					return ;
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
			logger.error("Unexpected error!");
			return ;
		}
		
		////detach all the running sub-topologies with the stopped or deleted sub-topologies
		ExecutorService executor4ds = Executors.newFixedThreadPool(runningSTIs.size());
		for(int sti = 0 ; sti < runningSTIs.size() ; sti++){
			SubTopologyInfo subTopologyInfo = runningSTIs.get(sti);
			Credential curCredential = userCredential.cloudAccess.get(subTopologyInfo.cloudProvider.toLowerCase());
			if(curCredential == null){
				logger.error("The credential for sub-topology '"+subTopologyInfo.topology+"' from '"+subTopologyInfo.cloudProvider+"' is unknown! SKIP!");
				continue;
			}
			Database database = userDatabase.databases.get(subTopologyInfo.cloudProvider.toLowerCase());
			if(database == null){
				logger.error("The database for sub-topology '"+subTopologyInfo.topology+"' from '"+subTopologyInfo.cloudProvider+"' is unknown! SKIP!");
				continue;
			}
			SEngine_detectFailure sd = new SEngine_detectFailure(subTopologyInfo, curCredential, database);
			executor4ds.execute(sd);
		}
		executor4ds.shutdown();
		try {
			int count = 0;
			while (!executor4ds.awaitTermination(2, TimeUnit.SECONDS)){
				count++;
				if(count > 500*topTopology.topologies.size()){
					logger.error("Unknown error! Some sub-topology cannot be detached!");
					return ;
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
			logger.error("Unexpected error!");
			return ;
		}

		
		
	}*/

}
