package provisioning.engine.TEngine;

import org.apache.log4j.Logger;

import provisioning.credential.Credential;
import provisioning.database.Database;
import provisioning.engine.SEngine.SEngineCoreMethod;
import topologyAnalysis.dataStructure.SubTopologyInfo;

public class SEngine_conf implements Runnable{
	
	private static final Logger logger = Logger.getLogger(SEngine_conf.class);
	
	private SubTopologyInfo subTopologyInfo;
	private Credential credential;
	private Database database;
	
	public SEngine_conf(SubTopologyInfo subTopologyInfo, 
			Credential credential, Database database){
		this.subTopologyInfo = subTopologyInfo;
		this.credential = credential;
		this.database = database;
	}
	
	@Override
	public void run() {
		try {
			Object sEngine = Class.forName(database.toolInfo.get("sengine")).newInstance();
			
			if(!((SEngineCoreMethod)sEngine).confTopConnection(subTopologyInfo, credential, database)){
				logger.error("Provisioning for sub-topology '"+subTopologyInfo.topology+"' failed!");
				subTopologyInfo.status = "failed";
				long curTime = System.currentTimeMillis();
				subTopologyInfo.statusInfo = "not connected: "+curTime;
				return ;
			}else
				logger.info("Sub-topology '"+subTopologyInfo.topology+"' has been connected with other sub-topologies!");
		} catch (InstantiationException | IllegalAccessException
				| ClassNotFoundException e) {
			e.printStackTrace();
			logger.error("The S-Engine for sub-topology '"+subTopologyInfo.topology+"' cannot be found!");
		}
	}
}
