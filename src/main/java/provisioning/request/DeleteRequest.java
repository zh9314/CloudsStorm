package provisioning.request;

import java.util.HashMap;
import java.util.Map;

public class DeleteRequest {

	///The key identifies the topology name that needs to be deleted
	///Value means whether this request can be satisfied
	public Map<String, Boolean> content = new HashMap<String, Boolean>();
}
