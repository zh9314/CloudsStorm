package provisioning.engine.VEngine.OS.ubuntu;

import provisioning.credential.Credential;
import provisioning.database.Database;
import topology.description.actual.VM;

public final class BasicVEngineUbuntu extends VEngineUbuntu {

	@Override
	public boolean supportStop() {
		return false;
	}

	@Override
	public boolean provision(VM subjectVM, Credential credential,
			Database database) {
		return false;
	}

	@Override
	public boolean delete(VM subjectVM, Credential credential, Database database) {
		return false;
	}

	@Override
	public boolean stop(VM subjectVM, Credential credential, Database database) {
		return false;
	}

	@Override
	public boolean start(VM subjectVM, Credential credential, Database database) {
		return false;
	}

	


	

}
