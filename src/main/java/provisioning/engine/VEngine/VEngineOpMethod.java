package provisioning.engine.VEngine;

import provisioning.credential.Credential;
import provisioning.database.Database;
import topology.description.actual.VM;

/**
 * This interface is more cloud related method.
 * It indicates which specific operation methods that this VEngine 
 * should have, including create, delete, stop, delete. Basically, 
 * for a new infrastructure, user only needs to implement these 6
 * basic operation functions. 
 * @author huan
 *
 */
public interface VEngineOpMethod {
	
	public boolean provision(VM subjectVM,
			Credential credential, Database database);
	
	public boolean delete(VM subjectVM,
			Credential credential, Database database);
	
	/**
	 * stop/start operations are optional.
	 * Some Cloud does not support stop/start.
	 * @return
	 */
	public boolean stop(VM subjectVM,
			Credential credential, Database database);
	
	public boolean start(VM subjectVM,
			Credential credential, Database database);
	

	/**
	 * Indicate whether the instance can be stopped.
	 * @return
	 */
	public boolean supportStop();
	
}
