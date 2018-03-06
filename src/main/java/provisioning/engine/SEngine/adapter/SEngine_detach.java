package provisioning.engine.SEngine.adapter;


import org.apache.log4j.Logger;

import provisioning.credential.Credential;
import provisioning.database.Database;
import provisioning.engine.SEngine.SEngineKeyMethod;
import topology.description.actual.SubTopologyInfo;
import commonTool.ClassDB;
import commonTool.Values;

public class SEngine_detach extends SEngineAdapter{
	private static final Logger logger = Logger.getLogger(SEngine_detach.class);

	
	public SEngine_detach(SubTopologyInfo subTopologyInfo, 
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
			if( !subTopologyInfo.status.trim().toLowerCase().equals(Values.STStatus.running) ){
				String msg = "The sub-topology '"+subTopologyInfo.topology
						+"' is not in the status of 'running' to detach with others!";
				logger.info(msg);
				return ;
			}
			
			if( subTopologyInfo.subTopology.accessKeyPair == null
					|| subTopologyInfo.subTopology.accessKeyPair.privateKeyString == null){
				String msg = "SSH key information is missing for detaching sub-topology '"
						+subTopologyInfo.topology+"'!";
				logger.error(msg);
				subTopologyInfo.logsInfo.put(subTopologyInfo.topology+"#ERROR", msg);
				opResult = false;
				return ;
			}
			
			long stOpStart = System.currentTimeMillis();
			if(!((SEngineKeyMethod)sEngine).detach(subTopologyInfo, credential, database)){
				logger.error("Detaching for sub-topology '"+subTopologyInfo.topology+"' failed!");
				opResult = false;
			}
			
			if(opResult){
				long stOpEnd = System.currentTimeMillis();
				subTopologyInfo.logsInfo.put(subTopologyInfo.topology+"#Detach", 
												(stOpEnd - stOpStart)+"@"+stOpStart);
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
