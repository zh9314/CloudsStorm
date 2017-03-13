package provisioning.engine.TEngine;

import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import commonTool.CommonTool;
import provisioning.ScalingRequest;
import provisioning.credential.Credential;
import provisioning.credential.UserCredential;
import provisioning.database.Database;
import provisioning.database.UserDatabase;
import topologyAnalysis.dataStructure.SubTopology;
import topologyAnalysis.dataStructure.SubTopologyInfo;
import topologyAnalysis.dataStructure.TopConnection;
import topologyAnalysis.dataStructure.TopConnectionPoint;
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
	
	
	/**
	 * Complete the auto-scaling for the sub-topology whose name is subTopologyName 
	 * The input parameter for scalSize means the how many extra sub-topologies will 
	 * be scaled. However, the actual scalSize is also decided by the scaling address 
	 * pool defined by the user. This input can also be negative meaning 'scaling in'. 
	 * Then several 'scaled' sub-topologies are deleted. This will be included in later 
	 * version.
	 * The input parameter of 'scalDCs' contains which datacenter to scal. It contains 
	 * two kinds of information: Cloud provider and domain. The size of this array must be 
	 * same with the the absolute value as the scalSize. 
	 * The sub-topology with the name of 'subTopologyName' 
	 * must have the tag of 'scaling'. 
	 */
	public void autoScal(TopTopology topTopology, 
			UserCredential userCredential, UserDatabase userDatabase, 
			String subTopologyName, int scalSize, ArrayList<ScalingRequest> scalDCs){
		boolean find = false;
		SubTopologyInfo scalSTI = null;
		for(int si = 0 ; si < topTopology.topologies.size() ; si++){
			SubTopologyInfo curSTI = topTopology.topologies.get(si);
			if(curSTI.topology.equals(subTopologyName)){
				find = true;
				if(!curSTI.tag.trim().toLowerCase().equals("scaling")){
					logger.error("The target sub-topology is not a 'scaling' one. It cannot be scaled!");
					return;
				}
				scalSTI = curSTI;
				break;
			}
		}
		if(!find){
			logger.warn("The sub-topology '"+subTopologyName+"' cannot be found!");
			return;
		}
		
		if(scalSize > 0){
			if(scalDCs.size() != scalSize){
				logger.error("The number of scaling requests must be the same with the 'scalSize'!");
				return;
			}
			scalUp(topTopology, userCredential, userDatabase, 
					scalSTI, scalSize, scalDCs);
		}else if(scalSize < 0){
			logger.warn("Have not implemented yet!");
			return ;
		}else{
			logger.warn("The scaling size cannot be set as 0");
			return ;
		}
		
		
		///overwrite the control information back to the top-topology
		if(!topTopology.overwirteControlOutput())
			logger.error("Control information of top-topology has not been overwritten to the origin file!");
	}
	
	private void scalUp(TopTopology topTopology, 
			UserCredential userCredential, UserDatabase userDatabase, 
			SubTopologyInfo scalSTI, int scalSize, ArrayList<ScalingRequest> scalDCs){
		////first check whether there exist 'stopped' sub-topologies that come from the list of scalDCs
		///To be completed. Currently, we just consider directly provision from the list of scalDCs.
		int actualScalingSize = 0;
		for(int si = 0 ; si < topTopology.topologies.size() ; si++){
			if(topTopology.topologies.get(si).status.trim().toLowerCase().equals("stopped")
				&& topTopology.topologies.get(si).tag.trim().toLowerCase().equals("scaled")
				&& topTopology.topologies.get(si).fatherTopology.topology.equals(scalSTI.topology)){
				for(int ri = 0 ; ri < scalDCs.size() ; ri++){
					if(topTopology.topologies.get(si).cloudProvider.trim().toLowerCase().equals(scalDCs.get(ri).cloudProvider.trim().toLowerCase())
						&& topTopology.topologies.get(si).domain.trim().toLowerCase().equals(scalDCs.get(ri).domain.trim().toLowerCase())
						){
						////multi thread here to start up the sub-topology.
						actualScalingSize++;
					}
				}
			}
		}
		
		////generate the 'fresh' sub-topology to be the scaled one.
		ArrayList<SubTopologyInfo> copySTIs = new ArrayList<SubTopologyInfo>();
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
			logger.debug("The control file of scaled sub-topology '"+subTopology.topologyName+"' has been overwritten!");
			
			if(!subTopology.commonFormatChecking("fresh"))
				return;
		}
		
		///do the auto-scaling0
		ExecutorService executor4as = Executors.newFixedThreadPool(copySTIs.size());
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
		
		executor4as.shutdown();
		try {
			int count = 0;
			while (!executor4as.awaitTermination(2, TimeUnit.SECONDS)){
				count++;
				if(count > 500*copySTIs.size()){
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
		if(interConnectionConf(topTopology.topologies, userCredential, userDatabase))
			logger.info("All the sub-topologies have been connected!");
		else
			logger.warn("Some of failed sub-topology may not be connected!");
		
		if(!topTopology.overwirteControlOutput())
			logger.error("Control information of top-topology has not been overwritten to the origin file!");
	
	}


}
