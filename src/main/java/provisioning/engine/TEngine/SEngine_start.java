package provisioning.engine.TEngine;

import org.apache.log4j.Logger;

import provisioning.credential.Credential;
import provisioning.database.Database;
import provisioning.engine.SEngine.SEngine;
import provisioning.engine.SEngine.SEngineCoreMethod;
import topologyAnalysis.dataStructure.SubTopologyInfo;

public class SEngine_start implements Runnable  {
	private static final Logger logger = Logger.getLogger(SEngine_start.class);

	private SubTopologyInfo subTopologyInfo;
	private Credential credential;
	private Database database;
	
	public SEngine_start(SubTopologyInfo subTopologyInfo, 
			Credential credential, Database database){
		this.subTopologyInfo = subTopologyInfo;
		this.credential = credential;
		this.database = database;
	}
	
	@Override
	public void run() {
		try {
			Object sEngine = Class.forName(database.toolInfo.get("sengine")).newInstance();
			if(!((SEngineCoreMethod)sEngine).supportStop()){
				logger.error("The Cloud provider of '"+subTopologyInfo.topology+"' cannot support the feature of 'start'!");
				return ;
			}
			if(!((SEngine)sEngine).commonRuntimeCheck(subTopologyInfo)){
				logger.error("Some information is missing for starting sub-topology '"+subTopologyInfo.topology+"'!");
				return ;
			}
			if(!((SEngineCoreMethod)sEngine).runtimeCheckandUpdate(subTopologyInfo, database)){
				logger.error("Sub-topology '"+subTopologyInfo.topology+"' cannot pass the runtime check before starting!");
				return ;
			}
			if(!((SEngineCoreMethod)sEngine).start(subTopologyInfo, credential, database)){
				logger.error("Starting for sub-topology '"+subTopologyInfo.topology+"' failed!");
				subTopologyInfo.status = "failed";
				long curTime = System.currentTimeMillis();
				subTopologyInfo.statusInfo = "not started: "+curTime;
				if(!subTopologyInfo.subTopology.overwirteControlOutput()){
					logger.error("Control information of '"+subTopologyInfo.topology+"' cannot be overwritten to the origin file!");
				}
				return ;
			}else
				logger.info("Sub-topology '"+subTopologyInfo.topology+"' has been started!");
		} catch (InstantiationException | IllegalAccessException
				| ClassNotFoundException e) {
			e.printStackTrace();
			logger.error("The S-Engine for sub-topology '"+subTopologyInfo.topology+"' cannot be found!");
		}
	}
}
