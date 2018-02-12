package provisioning.engine.VEngine.adapter;

import org.apache.log4j.Logger;

import provisioning.credential.Credential;
import provisioning.database.Database;
import provisioning.engine.VEngine.VEngine;
import provisioning.engine.VEngine.VEngineConfMethod;
import topology.description.actual.VM;
import commonTool.ClassDB;

public class VEngine_connect extends VEngineAdapter {

	private static final Logger logger = Logger.getLogger(VEngine_connect.class);

	
	public VEngine_connect(VM subjectVM, 
			Credential credential, Database database){
		this.curVM = subjectVM;
		this.credential = credential;
		this.curSTI = subjectVM.ponintBack2STI;
		this.database = database;
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
			////for the configuration methods, the agent initialization is not needed!
			
			long time1 = System.currentTimeMillis();
			
			if( !((VEngineConfMethod)vEngine).confVNF(curVM) ){
				logger.error("VM '"+curVM.name+"' cannot be configured to "
						+ "connect as designed!");
				opResult = false;
				return ;
			}
			
			long time2 = System.currentTimeMillis();
			curSTI.logsInfo.put(curVM.name+"#network", (time2 - time1) + "@" + time1);
			
		} catch (InstantiationException | IllegalAccessException e) {
			e.printStackTrace();
			logger.error(e.getMessage());
			curSTI.logsInfo.put(curVM.name+"#ERROR", CurVEngine.getName()+" is not valid!");
			opResult = false;
			return ;
		}
	}

}
