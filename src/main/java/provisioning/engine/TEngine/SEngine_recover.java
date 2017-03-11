package provisioning.engine.TEngine;

import org.apache.log4j.Logger;

import provisioning.credential.Credential;
import provisioning.database.Database;
import provisioning.engine.SEngine.SEngine;
import provisioning.engine.SEngine.SEngineCoreMethod;
import topologyAnalysis.dataStructure.SubTopologyInfo;

public class SEngine_recover implements Runnable {
	private static final Logger logger = Logger.getLogger(SEngine_recover.class);

	private SubTopologyInfo subTopologyInfo;
	private Credential credential;
	private Database database;
	
	public SEngine_recover(SubTopologyInfo subTopologyInfo, 
			Credential credential, Database database){
		this.subTopologyInfo = subTopologyInfo;
		this.credential = credential;
		this.database = database;
	}

	@Override
	public void run() {
		try {
			Object sEngine = Class.forName(subTopologyInfo.subTopology.provisioningAgentClassName).newInstance();
			if(!((SEngine)sEngine).commonRuntimeCheck(subTopologyInfo)){
				logger.error("Some information is missing for recovering sub-topology '"+subTopologyInfo.topology+"'!");
				return ;
			}
			if(!((SEngineCoreMethod)sEngine).runtimeCheckandUpdate(subTopologyInfo, database)){
				logger.error("Sub-topology '"+subTopologyInfo.topology+"' cannot pass the runtime check before recovering!");
				return ;
			}
			if(!((SEngineCoreMethod)sEngine).recover(subTopologyInfo, credential, database)){
				logger.error("Recovering for sub-topology '"+subTopologyInfo.topology+"' failed!");
				subTopologyInfo.status = "failed";
				long curTime = System.currentTimeMillis();
				subTopologyInfo.statusInfo = "not recoverd: "+curTime;
				if(!subTopologyInfo.subTopology.overwirteControlOutput()){
					logger.error("Control information of '"+subTopologyInfo.topology+"' cannot be overwritten to the origin file!");
				}
				return ;
			}else
				logger.info("Sub-topology '"+subTopologyInfo.topology+"' has been recovered!");
		} catch (InstantiationException | IllegalAccessException
				| ClassNotFoundException e) {
			e.printStackTrace();
			logger.error("The S-Engine for sub-topology '"+subTopologyInfo.topology+"' cannot be found!");
		}
	}
}
