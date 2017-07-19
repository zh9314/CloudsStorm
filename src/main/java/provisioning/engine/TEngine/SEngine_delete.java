package provisioning.engine.TEngine;

import org.apache.log4j.Logger;

import provisioning.credential.Credential;
import provisioning.database.Database;
import provisioning.engine.SEngine.SEngine;
import provisioning.engine.SEngine.SEngineCoreMethod;
import topologyAnalysis.dataStructure.SubTopologyInfo;

public class SEngine_delete implements Runnable  {
	private static final Logger logger = Logger.getLogger(SEngine_delete.class);

	private SubTopologyInfo subTopologyInfo;
	private Credential credential;
	private Database database;
	
	public SEngine_delete(SubTopologyInfo subTopologyInfo, 
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
				logger.error("Some information is missing for deleting sub-topology '"+subTopologyInfo.topology+"'!");
				return ;
			}
			if(!((SEngineCoreMethod)sEngine).runtimeCheckandUpdate(subTopologyInfo, database)){
				logger.error("Sub-topology '"+subTopologyInfo.topology+"' cannot pass the runtime check before deleting!");
				return ;
			}
			if(!((SEngineCoreMethod)sEngine).delete(subTopologyInfo, credential, database)){
				logger.error("Deleting for sub-topology '"+subTopologyInfo.topology+"' failed!");
				subTopologyInfo.status = "failed";
				long curTime = System.currentTimeMillis();
				subTopologyInfo.statusInfo = "not deleted: "+curTime;
				if(!subTopologyInfo.subTopology.overwirteControlOutput()){
					logger.error("Control information of '"+subTopologyInfo.topology+"' cannot be overwritten to the origin file!");
				}
				return ;
			}else{
				long curTime = System.currentTimeMillis();
				subTopologyInfo.status = "deleted";
				subTopologyInfo.statusInfo = "deleted: "+curTime;
				if(!subTopologyInfo.subTopology.overwirteControlOutput()){
					logger.error("Control information of '"+subTopologyInfo.topology+"' cannot be overwritten to the origin file!");
				}
				logger.info("Sub-topology '"+subTopologyInfo.topology+"' has been deleted!");
			}
		} catch (InstantiationException | IllegalAccessException
				| ClassNotFoundException e) {
			e.printStackTrace();
			logger.error("The S-Engine for sub-topology '"+subTopologyInfo.topology+"' cannot be found!");
		}
	}
}
