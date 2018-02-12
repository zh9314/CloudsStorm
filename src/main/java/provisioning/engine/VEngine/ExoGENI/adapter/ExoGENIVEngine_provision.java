package provisioning.engine.VEngine.ExoGENI.adapter;

import org.apache.log4j.Logger;

import commonTool.ClassDB;

import provisioning.credential.Credential;
import provisioning.database.Database;
import provisioning.engine.VEngine.VEngine;
import provisioning.engine.VEngine.VEngineConfMethod;
import provisioning.engine.VEngine.adapter.VEngineAdapter;
import topology.description.actual.VM;

public class ExoGENIVEngine_provision extends VEngineAdapter {

	private static final Logger logger = Logger.getLogger(ExoGENIVEngine_provision.class);

	public ExoGENIVEngine_provision(VM subjectVM, 
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
			
			if( !((VEngineConfMethod)vEngine).confSSH(curVM) ){
				logger.warn("SSH account for VM '" + curVM.name 
						+ "' might not be properly configured! ");
				opResult = false;
				return ;
			}
			
			long time4 = System.currentTimeMillis();
			
			if( !((VEngineConfMethod)vEngine).confENV(curVM) ){
				logger.warn("Environment on VM '" + curVM.name 
						+ "' might not be properly configured! ");
				opResult = false;
				return ;
			}
			
			long time5 = System.currentTimeMillis();
			curSTI.logsInfo.put(curVM.name+"#deploy", (time5 - time4) + "@" + time4);
			
		} catch (InstantiationException | IllegalAccessException e) {
			e.printStackTrace();
			logger.error(e.getMessage());
			curSTI.logsInfo.put(curVM.name+"#ERROR", 
					CurVEngine.getName()+" is not valid!");
			opResult = false;
			return ;
		}
	}

}
