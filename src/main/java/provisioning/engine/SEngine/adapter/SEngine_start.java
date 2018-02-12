package provisioning.engine.SEngine.adapter;

import org.apache.log4j.Logger;

import commonTool.ClassDB;
import provisioning.credential.Credential;
import provisioning.database.Database;
import provisioning.engine.SEngine.SEngineKeyMethod;
import topology.description.actual.SubTopologyInfo;

public class SEngine_start extends SEngineAdapter {
	private static final Logger logger = Logger.getLogger(SEngine_start.class);

	public SEngine_start(SubTopologyInfo subTopologyInfo, 
			Credential credential, Database database){
		this.subTopologyInfo = subTopologyInfo;
		this.credential = credential;
		this.database = database;
		this.opResult = true;
	}
	
	@Override
	public void run() {
		String cp = subTopologyInfo.cloudProvider.trim().toLowerCase();
		String sEngineClass = subTopologyInfo.subTopology.SEngineClass;
		Class<?> CurSEngine = ClassDB.getSEngine(cp, sEngineClass);
		if(CurSEngine == null){
			String msg = "SEngine cannot be loaded for '"+subTopologyInfo.topology
					+"'!";
			logger.warn(msg);
			subTopologyInfo.logsInfo.put(subTopologyInfo.topology+"#ERROR", msg);
			opResult = false;
			return ;
		}
		try {
			Object sEngine = CurSEngine.newInstance();
			
			/////some common checks on the sub-topology
			if( subTopologyInfo.status.trim().toLowerCase().equals("running") ){
				String msg = "The sub-topology '"+subTopologyInfo.topology
						+"' is already in 'running'";
				logger.warn(msg);
				subTopologyInfo.logsInfo.put(subTopologyInfo.topology+"#INFO", msg);
				return ;
			}
			
			if( !subTopologyInfo.status.trim().toLowerCase().equals("stopped") ){
				String msg = "The sub-topology '"+subTopologyInfo.topology
						+"' is not in the status of 'stopped' to be started!";
				logger.warn(msg);
				subTopologyInfo.logsInfo.put(subTopologyInfo.topology+"#WARN", msg);
				opResult = false;
				return ;
			}
			
			if(!((SEngineKeyMethod)sEngine).runtimeCheckandUpdate(subTopologyInfo, database)){
				String msg = "Sub-topology '"+subTopologyInfo.topology
						+"' cannot pass the runtime check before starting!";
				logger.error(msg);
				opResult = false;
				return ;
			}
			
			long stOpStart = System.currentTimeMillis();
			if(!((SEngineKeyMethod)sEngine).start(subTopologyInfo, credential, database)){
				logger.error("Starting for sub-topology '"+subTopologyInfo.topology+"' failed!");
				subTopologyInfo.status = "unknown";
				
				opResult = false;
				
			}else
				logger.info("Sub-topology '"+subTopologyInfo.topology+"' has been started!");
			
			if(opResult){
				subTopologyInfo.status = "running";
				long stOpEnd = System.currentTimeMillis();
				subTopologyInfo.logsInfo.put(subTopologyInfo.topology+"#Start", 
												(stOpEnd - stOpStart)+"@"+stOpStart);
			}
			
			if(!subTopologyInfo.subTopology.overwirteControlOutput()){
				String msg = "Control information of '"+subTopologyInfo.topology
						+"' has not been overwritten to the origin file!";
				logger.error(msg);
				subTopologyInfo.logsInfo.put(subTopologyInfo.topology+"#ERROR", msg);
				opResult = false;
			}
			return ;
		} catch (InstantiationException | IllegalAccessException
				 e) {
			e.printStackTrace();
			logger.error("The S-Engine for sub-topology '"+subTopologyInfo.topology+"' cannot be found!");
			subTopologyInfo.logsInfo.put(subTopologyInfo.topology+"#ERROR", 
						CurSEngine.getName()+" is not valid!");
			opResult = false;
		}
	}
}
