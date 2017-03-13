package provisioning.database;

import java.util.HashMap;
import java.util.Map;

public abstract class Database {
	////This is a toolInfo for provisioner to find some key information.
	public Map<String, String> toolInfo = new HashMap<String, String>();
}
