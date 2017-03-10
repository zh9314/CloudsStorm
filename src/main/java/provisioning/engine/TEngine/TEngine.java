package provisioning.engine.TEngine;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;


import provisioning.credential.Credential;
import provisioning.credential.UserCredential;
import provisioning.database.Database;
import provisioning.database.UserDatabase;
import topologyAnalysis.dataStructure.SubTopologyInfo;
import topologyAnalysis.dataStructure.TopTopology;

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
			SEngine_create sc = new SEngine_create(subTopologyInfo, curCredential, database);
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
		
		
		////Using multi-thread to configure inter connection
		ExecutorService executor4conf = Executors.newFixedThreadPool(topTopology.topologies.size());
		for(int sti = 0 ; sti < topTopology.topologies.size() ; sti++){
			SubTopologyInfo subTopologyInfo = topTopology.topologies.get(sti);
			logger.info("Connecting sub-topology '"+subTopologyInfo.topology+"' from '"+subTopologyInfo.cloudProvider+"'");
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
			SEngine_conf s_conf = new SEngine_conf(subTopologyInfo, curCredential, database);
			executor4conf.execute(s_conf);
		}
		
		executor4conf.shutdown();
		try {
			int count = 0;
			while (!executor4conf.awaitTermination(2, TimeUnit.SECONDS)){
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
		
		logger.info("All the sub-topologies have been connected!");
		
		///overwrite the control information back to the top-topology
		if(!topTopology.overwirteControlOutput())
			logger.error("Control information of top-topology has not been overwritten to the origin file!");
	}
	
	

}
