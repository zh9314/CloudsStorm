package provisioning.request;

import java.util.HashMap;
import java.util.Map;

public class StartRequest {
	

	///The key identifies the topology name that needs to be started
	///Value means whether this request can be satisfied
	public Map<String, Boolean> content = new HashMap<String, Boolean>();

}
