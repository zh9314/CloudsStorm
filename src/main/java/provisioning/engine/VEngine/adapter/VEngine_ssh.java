package provisioning.engine.VEngine.adapter;

import org.apache.log4j.Logger;

import commonTool.ClassDB;
import provisioning.database.Database;
import provisioning.engine.VEngine.VEngine;
import provisioning.engine.VEngine.VEngineConfMethod;
import provisioning.credential.Credential;
import topology.description.actual.VM;

/**
 * This class does the basic configuration just after provisioning.
 * Including:
 * 1. configure an overall cluster key pair to enable nodes can ssh
 * to each other.
 * 2. configure an unified SSH account according the application defined.
 * 3. hostname
 * @author huan
 *
 */
public class VEngine_ssh extends VEngineAdapter{
	
	private static final Logger logger = Logger.getLogger(VEngine_ssh.class);

	
	public VEngine_ssh(VM subjectVM, 
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
			curSTI.logsInfo.put(curVM.name, "VEngine not found!");
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
