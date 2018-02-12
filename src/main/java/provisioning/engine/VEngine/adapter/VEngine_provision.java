package provisioning.engine.VEngine.adapter;

import org.apache.log4j.Logger;

import commonTool.ClassDB;
import provisioning.database.Database;
import provisioning.engine.VEngine.VEngine;
import provisioning.engine.VEngine.VEngineConfMethod;
import provisioning.engine.VEngine.VEngineOpMethod;
import provisioning.credential.Credential;
import topology.description.actual.VM;

/**
 * For provisioning, the default process of the VEngine is
 * 1. provision the VM
 * 2. configure the SSH account
 * 3. configure and install the app-defined the environment 
 * @author huan
 *
 */
public class VEngine_provision extends VEngineAdapter{
	
	private static final Logger logger = Logger.getLogger(VEngine_provision.class);


	public VEngine_provision(VM subjectVM, 
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
			////if the agent is not initialized, first initialize it 

			long time1 = System.currentTimeMillis();
			
			if( !((VEngineOpMethod)vEngine).provision(curVM, credential, database) ){
				logger.error("VM '"+curVM.name+"' cannot be provisioned!");
				opResult = false;
				return ;
			}
			
			long time2 = System.currentTimeMillis();
			curSTI.logsInfo.put(curVM.name+"#provision", (time2 - time1) + "@" + time1);
			
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
