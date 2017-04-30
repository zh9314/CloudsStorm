package provisioning.database.EGI;

import java.util.HashMap;
import java.util.Map;

///This class describe all the information contained in one domain or datacenter
public class DomainInfo {
	
	///Describe all the resources with different types and os. e.g. key->  medium##ubuntu 14.04  value-> an element containing all the information
	public Map<String, OS_Res_Tpl> resTpls = new HashMap<String, OS_Res_Tpl>();

	public String endpoint;
}
