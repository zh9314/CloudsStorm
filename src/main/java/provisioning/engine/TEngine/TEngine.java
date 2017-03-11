package provisioning.engine.TEngine;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import provisioning.credential.Credential;
import provisioning.credential.UserCredential;
import provisioning.database.Database;
import provisioning.database.UserDatabase;
import topologyAnalysis.dataStructure.SubTopology;
import topologyAnalysis.dataStructure.SubTopologyInfo;
import topologyAnalysis.dataStructure.TopTopology;
import topologyAnalysis.dataStructure.VM;

public class TEngine {
	private static final Logger logger = Logger.getLogger(TEngine.class);

	/**
	 * This is a method to make the whole 'fresh' topology 
	 * go into the state of 'running' or some of them may fail. 
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
			logger.info("All the sub-topologies have been connected!");
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
	 * Recover all the 'failed' sub-topology
	 * @param topTopology
	 * @param userCredential
	 * @param userDatabase
	 */
	public void recoverAll(TopTopology topTopology, UserCredential userCredential, UserDatabase userDatabase){
		if(userCredential.cloudAccess == null){
			logger.error("The credentials for cloud providers must be initialized!");
			return ;
		}if(userDatabase.databases == null){
			logger.error("The databases for different cloud providers must be initialized!");
			return;
		}
		
		long recoverStart = System.currentTimeMillis();
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
		if(interConnectionConf(failedSTs, userCredential, userDatabase))
			logger.info("All the failed sub-topologies have been connected!");
		else
			logger.warn("Some of failed sub-topology may not be connected!");
		
		////Configure the top connections that originally connected with failed sub-topologies.
		if(interConnectionConf(originalSTs, userCredential, userDatabase))
			logger.info("All the original running sub-topologies have been connected to the failed ones!");
		else
			logger.warn("Some of running sub-topology may not be connected to the failed ones!");
		
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
	

}
