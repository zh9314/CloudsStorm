package provisioning.engine.TEngine;

import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import commonTool.CommonTool;
import provisioning.credential.Credential;
import provisioning.credential.UserCredential;
import provisioning.database.Database;
import provisioning.database.UserDatabase;
import provisioning.request.DeleteRequest;
import provisioning.request.ProvisionRequest;
import provisioning.request.RecoverRequest;
import provisioning.request.ScalingRequest;
import provisioning.request.StartRequest;
import provisioning.request.StopRequest;
import topologyAnalysis.dataStructure.SubTopology;
import topologyAnalysis.dataStructure.SubTopologyInfo;
import topologyAnalysis.dataStructure.TopConnection;
import topologyAnalysis.dataStructure.TopConnectionPoint;
import topologyAnalysis.dataStructure.TopTopology;
import topologyAnalysis.dataStructure.VM;

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
	public void provisionAll(TopTopology topTopology, UserCredential userCredential, UserDatabase userDatabase){
		if(userCredential.cloudAccess == null){
			logger.error("The credentials for cloud providers must be initialized!");
			return ;
		}if(userDatabase.databases == null){
			logger.error("The databases for different cloud providers must be initialized!");
			return;
		}
		
		////Using multi-thread to do provisioning
		ExecutorService executor4st = Executors.newFixedThreadPool(topTopology.topologies.size());
		for(int sti = 0 ; sti < topTopology.topologies.size() ; sti++){
			SubTopologyInfo subTopologyInfo = topTopology.topologies.get(sti);
			if(!subTopologyInfo.status.trim().toLowerCase().equals("fresh")
					&& !subTopologyInfo.status.trim().toLowerCase().equals("deleted")){
				logger.debug("Sub-topology '"+subTopologyInfo.topology+"' cannot be provisioned! Its status is '"+subTopologyInfo.status+"'!");
				continue;
			}
			if(subTopologyInfo.status.trim().toLowerCase().equals("deleted")
					&& subTopologyInfo.tag.trim().toLowerCase().equals("scaled")){
				logger.debug("The 'scaled' 'deleted' '"+subTopologyInfo.topology+"' cannot be provisioned!");
				continue;
			}
			logger.info("Provisioning sub-topology '"+subTopologyInfo.topology+"' from '"+subTopologyInfo.cloudProvider+"'");
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
			SEngine_provision sc = new SEngine_provision(subTopologyInfo, curCredential, database);
			executor4st.execute(sc);
		}
		
		executor4st.shutdown();
		try {
			int count = 0;
			while (!executor4st.awaitTermination(2, TimeUnit.SECONDS)){
				count++;
				if(count > 500*topTopology.topologies.size()){
					logger.error("Unknown error! Some sub-topology cannot be provisioned!");
					return ;
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
			logger.error("Unexpected error!");
			return ;
		}
		
		if(interConnectionConf(topTopology.topologies, userCredential, userDatabase))
			logger.info("All the running sub-topologies have been connected!");
		else
			logger.warn("Some of sub-topology may not be connected!");
		
		///overwrite the control information back to the top-topology
		if(!topTopology.overwirteControlOutput())
			logger.error("Control information of top-topology has not been overwritten to the origin file!");
	}
	
	/**
	 * Provision the sub-topologies, according to the ProvisionRequest.
	 * @param topTopology
	 * @param userCredential
	 * @param userDatabase
	 * @param provisionReqs
	 */
	public void provision(TopTopology topTopology, UserCredential userCredential, 
			UserDatabase userDatabase, ArrayList<ProvisionRequest> provisionReqs){
		if(userCredential.cloudAccess == null){
			logger.error("The credentials for cloud providers must be initialized!");
			return ;
		}if(userDatabase.databases == null){
			logger.error("The databases for different cloud providers must be initialized!");
			return;
		}
		
		if(provisionReqs == null || provisionReqs.size() == 0){
			logger.warn("Nothing needs to be provisioned!");
			return ;
		}
		ArrayList<SubTopologyInfo> provisionSTIs = new ArrayList<SubTopologyInfo>();
		for(int ri = 0 ; ri < provisionReqs.size() ; ri++){
			String provisionReqName = provisionReqs.get(ri).topologyName;
			boolean findST = false;
			for(int sti = 0 ; sti < topTopology.topologies.size() ; sti++){
				SubTopologyInfo curSTI = topTopology.topologies.get(sti);
				if(provisionReqName.equals(curSTI.topology)){
					findST = true;
					if(!curSTI.status.trim().toLowerCase().equals("fresh")
							&& !curSTI.status.trim().toLowerCase().equals("deleted")){
						logger.warn("'"+curSTI.status+"' sub-topology '"+curSTI.topology+"' cannot be provisioned!");
						break;
					}
					if(curSTI.status.trim().toLowerCase().equals("deleted")
							&& curSTI.tag.trim().toLowerCase().equals("scaled")){
						logger.warn("The 'scaled' 'deleted' '"+curSTI.topology+"' cannot be provisioned!");
						break;
					}
					provisionSTIs.add(curSTI);
					break;
				}
			}
			if(!findST)
				logger.warn("The sub-topology name '"+provisionReqName+"' in the requests cannot be found!");
		}
		
		
		if(provisionSTIs.size() == 0){
			logger.warn("None of the sub-topologies needs to be provisioned!");
			return; 
		}
		
		ExecutorService executor4ss = Executors.newFixedThreadPool(provisionSTIs.size());
		for(int sti = 0 ; sti < provisionSTIs.size() ; sti++){
			SubTopologyInfo subTopologyInfo = provisionSTIs.get(sti);
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
			SEngine_provision s_provision = new SEngine_provision(subTopologyInfo, curCredential, database);
			executor4ss.execute(s_provision);
		}
		
		executor4ss.shutdown();
		try {
			int count = 0;
			while (!executor4ss.awaitTermination(2, TimeUnit.SECONDS)){
				count++;
				if(count > 500*provisionSTIs.size()){
					logger.error("Unknown error! Some sub-topology cannot be provisioned!");
					return ;
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
			logger.error("Unexpected error!");
			return ;
		}
		
		if(interConnectionConf(topTopology.topologies, userCredential, userDatabase))
			logger.info("All the running sub-topologies have been connected!");
		else
			logger.warn("Some of sub-topology may not be connected!");
		
		///overwrite the control information back to the top-topology
		if(!topTopology.overwirteControlOutput())
			logger.error("Control information of top-topology has not been overwritten to the origin file!");
		
	}
	
	////Using multi-thread to configure inter connections
	private boolean interConnectionConf(ArrayList<SubTopologyInfo> subTopologyInfos,
			UserCredential userCredential, UserDatabase userDatabase){
		boolean returnResult = true;
		
		ExecutorService executor4conf = Executors.newFixedThreadPool(subTopologyInfos.size());
		for(int sti = 0 ; sti <subTopologyInfos.size() ; sti++){
			SubTopologyInfo subTopologyInfo = subTopologyInfos.get(sti);
			if(!subTopologyInfo.status.equals("running")){
				logger.info("'"+subTopologyInfo.status+"' sub-topology cannot be configured to connect!");
				//returnResult = false;
				continue;
			}
			logger.info("Connecting sub-topology '"+subTopologyInfo.topology+"' from '"+subTopologyInfo.cloudProvider+"'");
			Credential curCredential = userCredential.cloudAccess.get(subTopologyInfo.cloudProvider.toLowerCase());
			if(curCredential == null){
				logger.error("The credential for sub-topology '"+subTopologyInfo.topology+"' from '"+subTopologyInfo.cloudProvider+"' is unknown! SKIP!");
				returnResult = false;
				continue;
			}
			Database database = userDatabase.databases.get(subTopologyInfo.cloudProvider.toLowerCase());
			if(database == null){
				logger.error("The database for sub-topology '"+subTopologyInfo.topology+"' from '"+subTopologyInfo.cloudProvider+"' is unknown! SKIP!");
				returnResult = false;
				continue;
			}
			SEngine_conf s_conf = new SEngine_conf(subTopologyInfo, curCredential, database);
			executor4conf.execute(s_conf);
		}
		
		executor4conf.shutdown();
		try {
			int count = 0;
			while (!executor4conf.awaitTermination(2, TimeUnit.SECONDS)){
				count++;
				if(count > 500*subTopologyInfos.size()){
					logger.error("Unknown error! Some sub-topology cannot be provisioned!");
					return false;
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
			logger.error("Unexpected error!");
			return false;
		}
		return returnResult;
	}
	
	/**
	 * Starting the sub-topology, according to the StartRequest.
	 * @param topTopology
	 * @param userCredential
	 * @param userDatabase
	 * @param startReqs
	 */
	public void start(TopTopology topTopology, 
			UserCredential userCredential, UserDatabase userDatabase, 
			ArrayList<StartRequest> startReqs){
		if(userCredential.cloudAccess == null){
			logger.error("The credentials for cloud providers must be initialized!");
			return ;
		}if(userDatabase.databases == null){
			logger.error("The databases for different cloud providers must be initialized!");
			return;
		}
		
		if(startReqs == null || startReqs.size() == 0){
			logger.warn("Nothing needs to be started!");
			return ;
		}
		
		ArrayList<SubTopologyInfo> needToBeStarted = new ArrayList<SubTopologyInfo>();
		for(int ri = 0 ; ri < startReqs.size() ; ri++){
			String reqName = startReqs.get(ri).topologyName;
			boolean findST = false;
			for(int sti = 0 ; sti < topTopology.topologies.size() ; sti++){
				SubTopologyInfo curST = topTopology.topologies.get(sti);
				if(reqName.equals(curST.topology)){
					findST = true;
					if(!curST.status.toLowerCase().equals("stopped")){
						logger.warn("The status of sub-topology '"+reqName+"' is not 'stopped'");
						break;
					}
					needToBeStarted.add(curST);
					break;
				}
			}
			if(!findST)
				logger.warn("The sub-topology name '"+reqName+"' in the requests cannot be found!");
			
		}
		if(needToBeStarted.size() == 0){
			logger.error("No sub-topology needs be started!");
			return;
		}
		
		////Using multi-thread to do starting
		ExecutorService executor4st = Executors.newFixedThreadPool(topTopology.topologies.size());
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
			executor4st.execute(s_start);
		}
		
		executor4st.shutdown();
		try {
			int count = 0;
			while (!executor4st.awaitTermination(2, TimeUnit.SECONDS)){
				count++;
				if(count > 500*topTopology.topologies.size()){
					logger.error("Unknown error! Some sub-topology cannot be provisioned!");
					return ;
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
			logger.error("Unexpected error!");
			return ;
		}
		
		if(interConnectionConf(topTopology.topologies, userCredential, userDatabase))
			logger.info("All the running sub-topologies have been connected!");
		else
			logger.warn("Some of sub-topology may not be connected!");
		
		///overwrite the control information back to the top-topology
		if(!topTopology.overwirteControlOutput())
			logger.error("Control information of top-topology has not been overwritten to the origin file!");
	
	}
	
	/**
	 * Starting all the stopped sub-topology.
	 * @param topTopology
	 * @param userCredential
	 * @param userDatabase
	 */
	public void startAll(TopTopology topTopology, 
			UserCredential userCredential, UserDatabase userDatabase){
		if(userCredential.cloudAccess == null){
			logger.error("The credentials for cloud providers must be initialized!");
			return ;
		}if(userDatabase.databases == null){
			logger.error("The databases for different cloud providers must be initialized!");
			return;
		}
		
		ArrayList<SubTopologyInfo> needToBeStarted = new ArrayList<SubTopologyInfo>();
		for(int sti = 0 ; sti < topTopology.topologies.size() ; sti++){
			SubTopologyInfo curSTI = topTopology.topologies.get(sti);
			if(curSTI.status.trim().toLowerCase().equals("stopped"))
				needToBeStarted.add(curSTI);
		}
		if(needToBeStarted.size() == 0){
			logger.error("No sub-topology needs be started!");
			return;
		}
		
		////Using multi-thread to do starting
		ExecutorService executor4st = Executors.newFixedThreadPool(needToBeStarted.size());
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
			executor4st.execute(s_start);
		}
		
		executor4st.shutdown();
		try {
			int count = 0;
			while (!executor4st.awaitTermination(2, TimeUnit.SECONDS)){
				count++;
				if(count > 500*needToBeStarted.size()){
					logger.error("Unknown error! Some sub-topology cannot be provisioned!");
					return ;
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
			logger.error("Unexpected error!");
			return ;
		}
		
		if(interConnectionConf(topTopology.topologies, userCredential, userDatabase))
			logger.info("All the running sub-topologies have been connected!");
		else
			logger.warn("Some of sub-topology may not be connected!");
		
		///overwrite the control information back to the top-topology
		if(!topTopology.overwirteControlOutput())
			logger.error("Control information of top-topology has not been overwritten to the origin file!");
	
	}
	
	/**
	 * Stopping the sub-topology, according to the StopRequest.
	 * @param topTopology
	 * @param userCredential
	 * @param userDatabase
	 * @param stopReqs
	 */
	public void stop(TopTopology topTopology, 
			UserCredential userCredential, UserDatabase userDatabase, 
			ArrayList<StopRequest> stopReqs){
		if(userCredential.cloudAccess == null){
			logger.error("The credentials for cloud providers must be initialized!");
			return ;
		}if(userDatabase.databases == null){
			logger.error("The databases for different cloud providers must be initialized!");
			return;
		}
		
		if(stopReqs == null || stopReqs.size() == 0){
			logger.warn("Nothing needs to be stopped!");
			return ;
		}
		ArrayList<SubTopologyInfo> stopSTIs = new ArrayList<SubTopologyInfo>();
		ArrayList<SubTopologyInfo> runningSTIs = new ArrayList<SubTopologyInfo>();
		for(int ri = 0 ; ri < stopReqs.size() ; ri++){
			String stopReqName = stopReqs.get(ri).topologyName;
			boolean findST = false;
			for(int sti = 0 ; sti < topTopology.topologies.size() ; sti++){
				SubTopologyInfo curSTI = topTopology.topologies.get(sti);
				if(stopReqName.equals(curSTI.topology)){
					findST = true;
					if(!curSTI.status.trim().toLowerCase().equals("running")){
						logger.warn("'"+curSTI.status+"' sub-topology '"+curSTI.topology+"' cannot be stopped!");
						break;
					}
					curSTI.status = "stopped";
					stopSTIs.add(curSTI);
					break;
				}
			}
			if(!findST)
				logger.warn("The sub-topology name '"+stopReqName+"' in the requests cannot be found!");
		}
		
		for(int sti = 0 ; sti < topTopology.topologies.size() ; sti++){
			SubTopologyInfo curSTI = topTopology.topologies.get(sti);
			if(curSTI.status.trim().toLowerCase().equals("running"))
				runningSTIs.add(curSTI);
		}
		
		if(stopSTIs.size() == 0){
			logger.warn("None of the sub-topologies needs to be stopped!");
			return; 
		}
		
		ExecutorService executor4ss = Executors.newFixedThreadPool(topTopology.topologies.size());
		for(int sti = 0 ; sti < stopSTIs.size() ; sti++){
			SubTopologyInfo subTopologyInfo = stopSTIs.get(sti);
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
			///identify the connectors of the stopped sub-topology are null
			if(subTopologyInfo.connectors != null){
				for(int tcpi = 0 ; tcpi < subTopologyInfo.connectors.size() ; tcpi++)
					subTopologyInfo.connectors.get(tcpi).ethName = null;
			}
			SEngine_stop s_stop = new SEngine_stop(subTopologyInfo, curCredential, database);
			executor4ss.execute(s_stop);
		}
		////detach all the running sub-topologies with the stopped sub-topologies
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
			executor4ss.execute(sd);
		}
		
		executor4ss.shutdown();
		try {
			int count = 0;
			while (!executor4ss.awaitTermination(2, TimeUnit.SECONDS)){
				count++;
				if(count > 500*topTopology.topologies.size()){
					logger.error("Unknown error! Some sub-topology cannot be provisioned!");
					return ;
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
			logger.error("Unexpected error!");
			return ;
		}
		
		///overwrite the control information back to the top-topology
		if(!topTopology.overwirteControlOutput())
			logger.error("Control information of top-topology has not been overwritten to the origin file!");
			
	}
	
	
	/**
	 * Stopping all the 'running' sub-topologies.
	 * @param topTopology
	 * @param userCredential
	 * @param userDatabase
	 */
	public void stopAll(TopTopology topTopology, 
			UserCredential userCredential, UserDatabase userDatabase){
		if(userCredential.cloudAccess == null){
			logger.error("The credentials for cloud providers must be initialized!");
			return ;
		}if(userDatabase.databases == null){
			logger.error("The databases for different cloud providers must be initialized!");
			return;
		}
		
		ArrayList<SubTopologyInfo> stopSTIs = new ArrayList<SubTopologyInfo>();
		for(int sti = 0 ; sti < topTopology.topologies.size() ; sti++){
			SubTopologyInfo curSTI = topTopology.topologies.get(sti);
			if(curSTI.status.trim().toLowerCase().equals("running"))
				stopSTIs.add(curSTI);
		}
		if(stopSTIs.size() == 0){
			logger.warn("None of the sub-topologies needs to be stopped!");
			return; 
		}
		
		ExecutorService executor4ss = Executors.newFixedThreadPool(stopSTIs.size());
		for(int sti = 0 ; sti < stopSTIs.size() ; sti++){
			SubTopologyInfo subTopologyInfo = stopSTIs.get(sti);
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
			///identify the connectors of the stopped sub-topology are null
			if(subTopologyInfo.connectors != null){
				for(int tcpi = 0 ; tcpi < subTopologyInfo.connectors.size() ; tcpi++)
					subTopologyInfo.connectors.get(tcpi).ethName = null;
			}
			SEngine_stop s_stop = new SEngine_stop(subTopologyInfo, curCredential, database);
			executor4ss.execute(s_stop);
		}
		
		executor4ss.shutdown();
		try {
			int count = 0;
			while (!executor4ss.awaitTermination(2, TimeUnit.SECONDS)){
				count++;
				if(count > 500*stopSTIs.size()){
					logger.error("Unknown error! Some sub-topology cannot be provisioned!");
					return ;
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
			logger.error("Unexpected error!");
			return ;
		}
		
		///overwrite the control information back to the top-topology
		if(!topTopology.overwirteControlOutput())
			logger.error("Control information of top-topology has not been overwritten to the origin file!");
			
	}
	
	/**
	 * Delete the sub-topologies according to the delete requests.
	 * @param topTopology
	 * @param userCredential
	 * @param userDatabase
	 * @param stopReqs
	 */
	public void delete(TopTopology topTopology, 
			UserCredential userCredential, UserDatabase userDatabase, 
			ArrayList<DeleteRequest> deleteReqs){
		if(userCredential.cloudAccess == null){
			logger.error("The credentials for cloud providers must be initialized!");
			return ;
		}if(userDatabase.databases == null){
			logger.error("The databases for different cloud providers must be initialized!");
			return;
		}
		
		if(deleteReqs == null || deleteReqs.size() == 0){
			logger.warn("Nothing needs to be stopped!");
			return ;
		}
		
		ArrayList<SubTopologyInfo> deleteSTIs = new ArrayList<SubTopologyInfo>();
		ArrayList<SubTopologyInfo> runningSTIs = new ArrayList<SubTopologyInfo>();
		for(int ri = 0 ; ri < deleteReqs.size() ; ri++){
			String deleteReqName = deleteReqs.get(ri).topologyName;
			boolean findST = false;
			for(int sti = 0 ; sti < topTopology.topologies.size() ; sti++){
				SubTopologyInfo curSTI = topTopology.topologies.get(sti);
				if(deleteReqName.equals(curSTI.topology)){
					findST = true;
					if(!curSTI.status.trim().toLowerCase().equals("running")
							&& !curSTI.status.trim().toLowerCase().equals("stopped")){
						logger.warn("'"+curSTI.status+"' sub-topology '"+curSTI.topology+"' cannot be deleted!");
						break;
					}
					curSTI.status = "deleted";
					deleteSTIs.add(curSTI);
					break;
				}
			}
			if(!findST)
				logger.warn("The sub-topology name '"+deleteReqName+"' in the requests cannot be found!");
		}
		
		for(int sti = 0 ; sti < topTopology.topologies.size() ; sti++){
			SubTopologyInfo curSTI = topTopology.topologies.get(sti);
			if(curSTI.status.trim().toLowerCase().equals("running"))
				runningSTIs.add(curSTI);
		}
		
		if(deleteSTIs.size() == 0){
			logger.warn("None of the sub-topologies needs to be deleted!");
			return; 
		}
		
		ExecutorService executor4ss = Executors.newFixedThreadPool(topTopology.topologies.size());
		for(int sti = 0 ; sti < deleteSTIs.size() ; sti++){
			SubTopologyInfo subTopologyInfo = deleteSTIs.get(sti);
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
			///identify the connectors of the deleted sub-topology are null
			if(subTopologyInfo.connectors != null){
				for(int tcpi = 0 ; tcpi < subTopologyInfo.connectors.size() ; tcpi++)
					subTopologyInfo.connectors.get(tcpi).ethName = null;
			}
			SEngine_delete s_delete = new SEngine_delete(subTopologyInfo, curCredential, database);
			executor4ss.execute(s_delete);
		}
		////detach all the running sub-topologies with the deleted sub-topologies
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
			executor4ss.execute(sd);
		}
		
		executor4ss.shutdown();
		try {
			int count = 0;
			while (!executor4ss.awaitTermination(2, TimeUnit.SECONDS)){
				count++;
				if(count > 500*topTopology.topologies.size()){
					logger.error("Unknown error! Some sub-topology cannot be provisioned!");
					return ;
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
			logger.error("Unexpected error!");
			return ;
		}
		
		///overwrite the control information back to the top-topology
		if(!topTopology.overwirteControlOutput())
			logger.error("Control information of top-topology has not been overwritten to the origin file!");
			
	}
	
	/**
	 * Delete all the 'running' sub-topologies.
	 * @param topTopology
	 * @param userCredential
	 * @param userDatabase
	 * @param stopReqs
	 */
	public void deleteAll(TopTopology topTopology, 
			UserCredential userCredential, UserDatabase userDatabase){
		if(userCredential.cloudAccess == null){
			logger.error("The credentials for cloud providers must be initialized!");
			return ;
		}if(userDatabase.databases == null){
			logger.error("The databases for different cloud providers must be initialized!");
			return;
		}
		
		ArrayList<SubTopologyInfo> deleteSTIs = new ArrayList<SubTopologyInfo>();
		for(int sti = 0 ; sti < topTopology.topologies.size() ; sti++){
			SubTopologyInfo curSTI = topTopology.topologies.get(sti);
			if(curSTI.status.trim().toLowerCase().equals("running")
					|| curSTI.status.trim().toLowerCase().equals("stopped"))
				deleteSTIs.add(curSTI);
		}
		if(deleteSTIs.size() == 0){
			logger.warn("None of the sub-topologies needs to be deleted!");
			return; 
		}
		
		ExecutorService executor4ss = Executors.newFixedThreadPool(deleteSTIs.size());
		for(int sti = 0 ; sti < deleteSTIs.size() ; sti++){
			SubTopologyInfo subTopologyInfo = deleteSTIs.get(sti);
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
			///identify the connectors of the deleted sub-topology are null
			if(subTopologyInfo.connectors != null){
				for(int tcpi = 0 ; tcpi < subTopologyInfo.connectors.size() ; tcpi++)
					subTopologyInfo.connectors.get(tcpi).ethName = null;
			}
			SEngine_delete s_delete = new SEngine_delete(subTopologyInfo, curCredential, database);
			executor4ss.execute(s_delete);
		}
		
		executor4ss.shutdown();
		try {
			int count = 0;
			while (!executor4ss.awaitTermination(2, TimeUnit.SECONDS)){
				count++;
				if(count > 500*deleteSTIs.size()){
					logger.error("Unknown error! Some sub-topology cannot be provisioned!");
					return ;
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
			logger.error("Unexpected error!");
			return ;
		}
		
		///overwrite the control information back to the top-topology
		if(!topTopology.overwirteControlOutput())
			logger.error("Control information of top-topology has not been overwritten to the origin file!");
			
	}
	
	/**
	 * Only recover the 'failed' sub-topologies according to the recover requests.
	 * If the 'failed' sub-topology is not specified by a recover request, it 
	 * will not be recovered.
	 * @param topTopology
	 * @param userCredential
	 * @param userDatabase
	 */
	public void recover(TopTopology topTopology, UserCredential userCredential, 
			UserDatabase userDatabase, ArrayList<RecoverRequest> recoverReqs){
		if(userCredential.cloudAccess == null){
			logger.error("The credentials for cloud providers must be initialized!");
			return ;
		}if(userDatabase.databases == null){
			logger.error("The databases for different cloud providers must be initialized!");
			return;
		}
		long recoverStart = System.currentTimeMillis();
		
		if(recoverReqs == null || recoverReqs.size() == 0){
			logger.error("There is no recover request! Nothing needs to be recovered!");
			return ;
		}
		
		///This list contains all the failed sub-topologies
		ArrayList<SubTopologyInfo> failedSTs = new ArrayList<SubTopologyInfo>();
		for(int reqi = 0 ; reqi < recoverReqs.size() ; reqi++){
			String cloudProvider = recoverReqs.get(reqi).cloudProvider;
			String domain = recoverReqs.get(reqi).domain;
			String recoverTopologyName = recoverReqs.get(reqi).topologyName;
			boolean findTopology = false;
			for(int si = 0 ; si < topTopology.topologies.size() ; si++){
				if(topTopology.topologies.get(si).topology.equals(recoverTopologyName)){
					findTopology = true;
					if(!topTopology.topologies.get(si).status.equals("failed")){
						logger.warn("The sub-topology '"+recoverTopologyName+"' is not 'failed'!");
						continue;
					}
					topTopology.topologies.get(si).domain = domain;
					topTopology.topologies.get(si).cloudProvider = cloudProvider;
					if(userCredential.sshAccess.containsKey(domain)){
						topTopology.topologies.get(si).sshKeyPairId = userCredential.sshAccess.get(domain).SSHKeyPairId;
						topTopology.topologies.get(si).subTopology.accessKeyPair = userCredential.sshAccess.get(domain);
					}
					else{
						topTopology.topologies.get(si).sshKeyPairId = null;
						topTopology.topologies.get(si).subTopology.accessKeyPair = null;
					}	
					failedSTs.add(topTopology.topologies.get(si));
					break;
				}
			}
			if(!findTopology){
				logger.warn("The sub-topology '"+recoverTopologyName+"' cannot be found!");
				continue;
			}
		}
		
		if(failedSTs.size() == 0){
			logger.error("There is no sub-topology needing recovery!");
			return ;
		}
		
		////Using multi-thread to recover the failed topologies.
		ExecutorService executor4rc = Executors.newFixedThreadPool(failedSTs.size());
		for(int sti = 0 ; sti < failedSTs.size() ; sti++){
			SubTopologyInfo subTopologyInfo = failedSTs.get(sti);
			logger.info("Recovering sub-topology '"+subTopologyInfo.topology+"' from '"+subTopologyInfo.cloudProvider+"'");
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
			SEngine_recover sr = new SEngine_recover(subTopologyInfo, curCredential, database);
			executor4rc.execute(sr);
		}
		
		executor4rc.shutdown();
		try {
			int count = 0;
			while (!executor4rc.awaitTermination(2, TimeUnit.SECONDS)){
				count++;
				if(count > 500*failedSTs.size()){
					logger.error("Unknown error! Some sub-topology cannot be provisioned!");
					return ;
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
			logger.error("Unexpected error!");
			return ;
		}
		
		if(interConnectionConf(topTopology.topologies, userCredential, userDatabase))
			logger.info("All the running sub-topologies have been connected!");
		else
			logger.warn("Some of running sub-topologies may not be connected to the failed ones!");
		
		long recoverEnd = System.currentTimeMillis();
		logger.info("The total recovery overhead is "+ (recoverEnd-recoverStart));
		///overwrite the control information back to the top-topology
		if(!topTopology.overwirteControlOutput())
			logger.error("Control information of top-topology has not been overwritten to the origin file!");
	
	}
	
	/**
	 * Recover all the 'failed' sub-topologies according to the recover requests.
	 * If the some of the 'failed' sub-topologies is not specified by the recover 
	 * requests, then they will be recovered from the origin domains.
	 * If the recoverReqs is null, then all the failed sub-topologies will be 
	 * recovered from the origin domains.
	 * @param topTopology
	 * @param userCredential
	 * @param userDatabase
	 */
	public void recoverAll(TopTopology topTopology, UserCredential userCredential, 
			UserDatabase userDatabase, ArrayList<RecoverRequest> recoverReqs){
		if(userCredential.cloudAccess == null){
			logger.error("The credentials for cloud providers must be initialized!");
			return ;
		}if(userDatabase.databases == null){
			logger.error("The databases for different cloud providers must be initialized!");
			return;
		}
		
		long recoverStart = System.currentTimeMillis();
		
		////update the failed domain information according to the recovery requests
		if(recoverReqs == null){
			logger.warn("There is no recover request! All the failed sub-topology will just be recovered from the origin domain!");
			
			recoverReqs = new ArrayList<RecoverRequest>();
		}
		for(int reqi = 0 ; reqi < recoverReqs.size() ; reqi++){
			String cloudProvider = recoverReqs.get(reqi).cloudProvider;
			String domain = recoverReqs.get(reqi).domain;
			String recoverTopologyName = recoverReqs.get(reqi).topologyName;
			boolean findTopology = false;
			for(int si = 0 ; si < topTopology.topologies.size() ; si++){
				if(topTopology.topologies.get(si).topology.equals(recoverTopologyName)){
					findTopology = true;
					if(!topTopology.topologies.get(si).status.equals("failed")){
						logger.warn("The sub-topology '"+recoverTopologyName+"' is not 'failed'!");
						continue;
					}
					topTopology.topologies.get(si).domain = domain;
					topTopology.topologies.get(si).cloudProvider = cloudProvider;
					if(userCredential.sshAccess.containsKey(domain)){
						topTopology.topologies.get(si).sshKeyPairId = userCredential.sshAccess.get(domain).SSHKeyPairId;
						topTopology.topologies.get(si).subTopology.accessKeyPair = userCredential.sshAccess.get(domain);
					}
					else{
						topTopology.topologies.get(si).sshKeyPairId = null;
						topTopology.topologies.get(si).subTopology.accessKeyPair = null;
					}	
					break;
				}
			}
			if(!findTopology){
				logger.error("The sub-topology '"+recoverTopologyName+"' cannot be found!");
				return;
			}
		}
		
		
		///This list contains all the failed sub-topologies
		ArrayList<SubTopologyInfo> failedSTs = new ArrayList<SubTopologyInfo>();
		///This list contains all the original sub-topologies which are not failed and must be running.
		ArrayList<SubTopologyInfo> originalSTs = new ArrayList<SubTopologyInfo>();
		for(int sti = 0 ; sti < topTopology.topologies.size() ; sti++){
			if(topTopology.topologies.get(sti).status.equals("failed"))
				failedSTs.add(topTopology.topologies.get(sti));
			if(topTopology.topologies.get(sti).status.equals("running"))
				originalSTs.add(topTopology.topologies.get(sti));
		}
		if(failedSTs.size() == 0){
			logger.warn("There is no sub-topology needing recovery!");
			return ;
		}
		
		////Using multi-thread to recover the failed topologies.
		ExecutorService executor4rc = Executors.newFixedThreadPool(failedSTs.size());
		for(int sti = 0 ; sti < failedSTs.size() ; sti++){
			SubTopologyInfo subTopologyInfo = failedSTs.get(sti);
			logger.info("Recovering sub-topology '"+subTopologyInfo.topology+"' from '"+subTopologyInfo.cloudProvider+"'");
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
			SEngine_recover sr = new SEngine_recover(subTopologyInfo, curCredential, database);
			executor4rc.execute(sr);
		}
		
		executor4rc.shutdown();
		try {
			int count = 0;
			while (!executor4rc.awaitTermination(2, TimeUnit.SECONDS)){
				count++;
				if(count > 500*failedSTs.size()){
					logger.error("Unknown error! Some sub-topology cannot be provisioned!");
					return ;
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
			logger.error("Unexpected error!");
			return ;
		}
		
		////Configure the top connections for the failed sub-topologies
		/*if(interConnectionConf(failedSTs, userCredential, userDatabase))
			logger.info("All the failed sub-topologies have been connected!");
		else
			logger.warn("Some of failed sub-topology may not be connected!");
		
		////Configure the top connections that originally connected with failed sub-topologies.
		if(interConnectionConf(originalSTs, userCredential, userDatabase))
			logger.info("All the original running sub-topologies have been connected to the failed ones!");
		else
			logger.warn("Some of running sub-topology may not be connected to the failed ones!");
		*/
		if(interConnectionConf(topTopology.topologies, userCredential, userDatabase))
			logger.info("All the running sub-topologies have been connected!");
		else
			logger.warn("Some of running sub-topologies may not be connected to the failed ones!");
		
		long recoverEnd = System.currentTimeMillis();
		logger.info("The total recovery overhead is "+ (recoverEnd-recoverStart));
		///overwrite the control information back to the top-topology
		if(!topTopology.overwirteControlOutput())
			logger.error("Control information of top-topology has not been overwritten to the origin file!");
	
	}
	
	/**
	 * Detect the sub-topology and mark its status as 'failed' 
	 * make the 'publicAddress' as null.
	 * The most important thing is that deleting the tunnel connecting to the 'failed' sub-topology 
	 * and set 'ethName' field of the top connection point as null.
	 */
	public void detectFailure(TopTopology topTopology, 
			UserCredential userCredential, UserDatabase userDatabase, 
			String subTopologyName){
		boolean find = false;
		for(int si = 0 ; si < topTopology.topologies.size() ; si++){
			SubTopologyInfo curSTI = topTopology.topologies.get(si);
			if(curSTI.topology.equals(subTopologyName)){
				find = true;
				curSTI.status = "failed";
				long failureTime = System.currentTimeMillis();
				curSTI.statusInfo = "detect failure: " + failureTime;
				SubTopology curSubTopology = curSTI.subTopology;
				ArrayList<VM> vms = curSubTopology.getVMsinSubClass();
				for(int vi = 0 ; vi < vms.size() ; vi++)
					vms.get(vi).publicAddress = null;
				if(curSTI.connectors != null){
					for(int tcpi = 0 ; tcpi < curSTI.connectors.size() ; tcpi++)
						curSTI.connectors.get(tcpi).ethName = null;
				}
				if(!curSubTopology.overwirteControlOutput()){
					logger.error("Control information of '"+curSTI.topology+"' cannot be overwritten to the origin file!");
				}
				break;
			}
		}
		if(!find){
			logger.warn("The sub-topology '"+subTopologyName+"' cannot be found!");
			return;
		}
		
		////Using multi-thread to delete all the failed connected tunnels expect the failed sub-topology itself.
		ExecutorService executor4rm = Executors.newFixedThreadPool(topTopology.topologies.size());
		for(int sti = 0 ; sti < topTopology.topologies.size() ; sti++){
			SubTopologyInfo subTopologyInfo = topTopology.topologies.get(sti);
			if(subTopologyInfo.topology.equals(subTopologyName))
				continue;
			logger.info("Marking the failure connections for sub-topology '"+subTopologyInfo.topology+"' from '"+subTopologyInfo.cloudProvider+"'");
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
			executor4rm.execute(sd);
		}
		executor4rm.shutdown();
		try {
			int count = 0;
			while (!executor4rm.awaitTermination(2, TimeUnit.SECONDS)){
				count++;
				if(count > 200*topTopology.topologies.size()){
					logger.error("Unknown error! Some sub-topology cannot be provisioned!");
					return ;
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
			logger.error("Unexpected error!");
			return ;
		}
		
		///overwrite the control information back to the top-topology
		if(!topTopology.overwirteControlOutput())
			logger.error("Control information of top-topology has not been overwritten to the origin file!");
			
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
	public void autoScal(TopTopology topTopology, 
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
				String [] t_VM = curTCP.peerTCP.componentName.split("\\.");
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
		logger.debug("The control file of top-level topology has been overwritten!");
		for(int sti = 0 ; sti < copySTIs.size() ; sti++){
			SubTopology subTopology = copySTIs.get(sti).subTopology;
			if(subTopology == null){
				logger.error("There is a null sub-topology!");
				continue ;
			}
			if(!subTopology.overwirteControlOutput())
				return;
			logger.debug("The control file of 'scaled' sub-topology '"+subTopology.topologyName+"' has been overwritten!");
			
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
			logger.info("Recovering sub-topology '"+subTopologyInfo.topology+"' from '"+subTopologyInfo.cloudProvider+"'");
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
		
		ExecutorService executor4ss = Executors.newFixedThreadPool(topTopology.topologies.size());
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
		////detach all the running sub-topologies with the stopped or deleted sub-topologies
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
			executor4ss.execute(sd);
		}
		
		executor4ss.shutdown();
		try {
			int count = 0;
			while (!executor4ss.awaitTermination(2, TimeUnit.SECONDS)){
				count++;
				if(count > 500*topTopology.topologies.size()){
					logger.error("Unknown error! Some sub-topology cannot be provisioned!");
					return ;
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
			logger.error("Unexpected error!");
			return ;
		}
		
		
	}

}
