package provisioning.engine.VEngine.adapter;


import org.apache.log4j.Logger;

import commonTool.ClassDB;
import provisioning.credential.Credential;
import provisioning.database.Database;
import provisioning.engine.VEngine.VEngine;
import provisioning.engine.VEngine.VEngineOpMethod;
import topology.description.actual.VM;

public class VEngine_delete extends VEngineAdapter{
	
	private static final Logger logger = Logger.getLogger(VEngine_delete.class);

	
	public VEngine_delete(VM subjectVM, 
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
			
			if( !((VEngineOpMethod)vEngine).delete(curVM, 
					credential, database) ){
				logger.error("VM '"+curVM.name+"' cannot be deleted ");
				curSTI.logsInfo.put(curVM.name, "Delete failed!");
				opResult = false;
				return ;
			}
			curVM.publicAddress = null;
			for(int ci = 0 ; ci < curVM.vmConnectors.size() ; ci++)
				curVM.vmConnectors.get(ci).ethName = null;
			
		} catch (InstantiationException | IllegalAccessException e) {
			e.printStackTrace();
			logger.error(e.getMessage());
			curSTI.logsInfo.put(curVM.name+"#ERROR", CurVEngine.getName()+" is not valid!");
			opResult = false;
			return ;
		}
	}

}
