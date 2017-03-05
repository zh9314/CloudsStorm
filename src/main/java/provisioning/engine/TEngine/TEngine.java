package provisioning.engine.TEngine;

import org.apache.log4j.Logger;

import provisioning.credential.Credential;
import provisioning.credential.UserCredential;
import provisioning.database.Database;
import provisioning.database.UserDatabase;
import provisioning.engine.SEngine.SEngine;
import provisioning.engine.SEngine.SEngineCoreMethod;
import topologyAnalysis.dataStructure.SubTopologyInfo;
import topologyAnalysis.dataStructure.TopTopology;

public class TEngine {
	private static final Logger logger = Logger.getLogger(TEngine.class);

	public void provisionAll(TopTopology topTopology, UserCredential userCredential, UserDatabase userDatabase){
		if(userCredential.cloudAccess == null){
			logger.error("The credentials for cloud providers must be initialized!");
			return ;
		}if(userDatabase.databases == null){
			logger.error("The databases for different cloud providers must be initialized!");
			return;
		}
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
			try {
				Object sEngine = Class.forName(subTopologyInfo.subTopology.provisioningAgentClassName).newInstance();
				if(!((SEngine)sEngine).commonRuntimeCheck(subTopologyInfo)){
					logger.error("Some information is missing for provisioning sub-topology '"+subTopologyInfo.topology+"'!");
					continue;
				}
				if(!((SEngineCoreMethod)sEngine).runtimeCheckandUpdate(subTopologyInfo, database)){
					logger.error("Sub-topology '"+subTopologyInfo.topology+"' cannot pass the runtime check before provisioning!");
					continue;
				}
				if(!((SEngineCoreMethod)sEngine).provision(subTopologyInfo, curCredential, database)){
					logger.error("Provisioning for sub-topology '"+subTopologyInfo.topology+"' failed!");
					continue;
				}else
					logger.info("Sub-topology '"+subTopologyInfo.topology+"' has been provisioned!");
			} catch (InstantiationException | IllegalAccessException
					| ClassNotFoundException e) {
				e.printStackTrace();
				logger.error("The S-Engine for sub-topology '"+subTopologyInfo.topology+"' cannot be found!");
			}
		}
	}

}
