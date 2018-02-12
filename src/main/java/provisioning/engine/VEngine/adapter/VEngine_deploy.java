package provisioning.engine.VEngine.adapter;

import org.apache.log4j.Logger;

import commonTool.ClassDB;
import provisioning.credential.Credential;
import provisioning.database.Database;
import provisioning.engine.VEngine.VEngine;
import provisioning.engine.VEngine.VEngineConfMethod;
import topology.description.actual.VM;

/**
 * 
 * Configure the VM environment.
 * @author huan
 *
 */
public class VEngine_deploy extends VEngineAdapter{

	private static final Logger logger = Logger.getLogger(VEngine_deploy.class);

	
	public VEngine_deploy(VM subjectVM, 
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
			
			long time1 = System.currentTimeMillis();
			
			if( !((VEngineConfMethod)vEngine).confENV(curVM) ){
				logger.warn("Environment on VM '" + curVM.name 
						+ "' might not be properly configured! ");
				opResult = false;
				return ;
			}
			
			long time2 = System.currentTimeMillis();
			curSTI.logsInfo.put(curVM.name+"#deploy", (time2 - time1) + "@" + time1);
			
		} catch (InstantiationException | IllegalAccessException e) {
			e.printStackTrace();
			logger.error(e.getMessage());
			curSTI.logsInfo.put(curVM.name+"#ERROR", CurVEngine.getName()+" is not valid!");
			opResult = false;
			return ;
		}
	}
	
	
}
