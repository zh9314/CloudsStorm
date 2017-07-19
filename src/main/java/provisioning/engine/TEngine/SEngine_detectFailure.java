package provisioning.engine.TEngine;

import org.apache.log4j.Logger;

import provisioning.credential.Credential;
import provisioning.database.Database;
import provisioning.engine.SEngine.SEngine;
import provisioning.engine.SEngine.SEngineCoreMethod;
import topologyAnalysis.dataStructure.SubTopologyInfo;

////This class is used to detach this running sub-topology with the failed 
////stopped or deleted sub-topologies.
public class SEngine_detectFailure implements Runnable {
	
	private static final Logger logger = Logger.getLogger(SEngine_detectFailure.class);

	private SubTopologyInfo subTopologyInfo;
	private Credential credential;
	private Database database;
	/**
	 * This class is used for this sub-topology to 
	 * identify all the top connections that connected with 
	 * the 'failed' sub-topology.
	 * @param subTopologyInfo
	 * @param credential
	 * @param database
	 */
	public SEngine_detectFailure(SubTopologyInfo subTopologyInfo, 
			Credential credential, Database database){
		this.subTopologyInfo = subTopologyInfo;
		this.credential = credential;
		this.database = database;
	}
	
	@Override
	public void run() {
		try {
			Object sEngine = Class.forName(database.toolInfo.get("sengine")).newInstance();
			if(!((SEngine)sEngine).commonRuntimeCheck(subTopologyInfo)){
				logger.error("Some information is missing for detaching sub-topology '"+subTopologyInfo.topology+"'!");
				return ;
			}
			if(!((SEngineCoreMethod)sEngine).runtimeCheckandUpdate(subTopologyInfo, database)){
				logger.error("Sub-topology '"+subTopologyInfo.topology+"' cannot pass the runtime check before detaching!");
				return ;
			}
			if(!((SEngineCoreMethod)sEngine).markFailure(subTopologyInfo, credential, database)){
				logger.error("Some errors happen during removing the ethName for '"+subTopologyInfo.topology+"'!");
				
				return ;
			}else
				logger.info("Running sub-topology '"+subTopologyInfo.topology+"' has disconnected with the 'failed', 'stopped' or 'deleted' sub-topology!");
		} catch (InstantiationException | IllegalAccessException
				| ClassNotFoundException e) {
			e.printStackTrace();
			logger.error("The S-Engine for sub-topology '"+subTopologyInfo.topology+"' cannot be found!");
		}
	}

}
