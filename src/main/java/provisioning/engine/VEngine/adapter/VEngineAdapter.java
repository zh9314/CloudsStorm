package provisioning.engine.VEngine.adapter;

import provisioning.credential.Credential;
import provisioning.database.Database;
import topology.description.actual.SubTopologyInfo;
import topology.description.actual.VM;

public abstract class VEngineAdapter implements Runnable {
	protected VM curVM;
	protected Credential credential;
	protected SubTopologyInfo curSTI;
	protected Database database;
	
	public boolean opResult;
}
