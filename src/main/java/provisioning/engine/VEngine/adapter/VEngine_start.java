package provisioning.engine.VEngine.adapter;

import org.apache.log4j.Logger;

import commonTool.ClassDB;
import provisioning.credential.Credential;
import provisioning.database.Database;
import provisioning.engine.VEngine.VEngine;
import provisioning.engine.VEngine.VEngineOpMethod;
import topology.description.actual.VM;

public class VEngine_start extends VEngineAdapter{
	
	private static final Logger logger = Logger.getLogger(VEngine_start.class);

	
	public VEngine_start(VM subjectVM, 
			Credential credential, Database database){
		this.curVM = subjectVM;
		this.credential = credential;
		this.database = database;
		this.curSTI = subjectVM.ponintBack2STI;
		this.opResult = true;
	}
	
	@Override
	public void run() {
		Class<?> CurVEngine = ClassDB.getVEngine(curSTI.cloudProvider, 
				curVM.VEngineClass, curVM.OStype);
		if(CurVEngine == null){
			logger.error("VEngine cannot be loaded for '"+curVM.name+"'!");
			curSTI.logsInfo.put(curVM.name+"#ERROR", "VEngine not found!");
			opResult = false;
			return ;
		}
		try {
			Object vEngine = (VEngine)CurVEngine.newInstance();
			
			if( !((VEngineOpMethod)vEngine).supportStop() ){
				logger.warn("VM '"+curVM.name+"' does not support start!");
				opResult = false;
				return ;
			}

			long time1 = System.currentTimeMillis();
			
			if( !((VEngineOpMethod)vEngine).start(curVM, 
					credential, database) ){
				logger.error("VM '"+curVM.name+"' cannot be started!");
				opResult = false;
				return ;
			}
			long time2 = System.currentTimeMillis();
			curSTI.logsInfo.put(curVM.name+"#start", (time2 - time1) + "@" + time1);
			
		} catch (InstantiationException | IllegalAccessException e) {
			e.printStackTrace();
			logger.error(e.getMessage());
			curSTI.logsInfo.put(curVM.name+"#ERROR", CurVEngine.getName()+" is not valid!");
			return ;
		}
	}

}
