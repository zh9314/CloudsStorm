package provisioning.engine.SEngine.adapter;

import provisioning.credential.Credential;
import provisioning.database.Database;
import topology.description.actual.SubTopologyInfo;

public abstract class SEngineAdapter implements Runnable{
	protected SubTopologyInfo subTopologyInfo;
	protected Credential credential;
	protected Database database;
	
	///This is used to tell the father process whether there is some failures.
	public boolean opResult;
	
}
