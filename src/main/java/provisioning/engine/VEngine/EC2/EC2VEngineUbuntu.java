package provisioning.engine.VEngine.EC2;



import provisioning.credential.Credential;
import provisioning.database.Database;
import provisioning.engine.VEngine.OS.ubuntu.VEngineUbuntu;
import topology.description.actual.VM;

public class EC2VEngineUbuntu extends VEngineUbuntu{
	

	@Override
	public boolean provision(VM subjectVM,
			Credential credential, Database database) {
		if(EC2VEngine.provision(subjectVM, credential, database))
			return true;
		else
			return false;
	}

	@Override
	public boolean supportStop() {
		return true;
	}

	@Override
	public boolean delete(VM subjectVM,
			Credential credential, Database database) {
		if(EC2VEngine.delete(subjectVM, credential, database))
			return true;
		else
			return false;
	}


	@Override
	public boolean stop(VM subjectVM, Credential credential, Database database) {
		if(EC2VEngine.stop(subjectVM, credential, database))
			return true;
		else
			return false;
	}

	@Override
	public boolean start(VM subjectVM, Credential credential, Database database) {
		if(EC2VEngine.start(subjectVM, credential, database))
			return true;
		else
			return false;
	}

	
	
	
	
}
