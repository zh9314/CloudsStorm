package provisioning.database.EGI;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import provisioning.database.Database;

public class EGIDatabase extends Database {
	private static final Logger logger = Logger.getLogger(EGIDatabase.class);
	
	//all the fields are stored in lower case. 
	//The key here is the domain name, not the endpoint name. E.g. domain name can be 'CESNET', its endpoint name is 'https://carach5.ics.muni.cz:11443'
	public Map<String, DomainInfo> domainInfos = new HashMap<String, DomainInfo>();

		
	public EGIDatabase(){
		this.toolInfo.put("sengine", "provisioning.engine.SEngine.EGISEngine");
	}
	
	
	/**
	 * Load the domain information from file. The content is split with "&&".<br/>
	 * Example: <br/>
	 * CESNET&&https://carach5.ics.muni.cz:11443&&ubuntu 14.04&&medium&&http://fedcloud.egi.eu/occi/compute/flavour/1.0#medium&&http://occi.carach5.ics.muni.cz/occi/infrastructure/os_tpl#uuid_egi_ubuntu_server_14_04_lts_fedcloud_warg_131&&ubuntu<br/>
	 * CESNET&&https://carach5.ics.muni.cz:11443&&ubuntu 14.04&&extra_large&&http://schemas.fedcloud.egi.eu/occi/infrastructure/resource_tpl#extra_large&&	http://occi.carach5.ics.muni.cz/occi/infrastructure/os_tpl#uuid_egi_ubuntu_server_14_04_lts_fedcloud_warg_131&&ubuntu<br/>
	 *
	 */
	public boolean loadDomainInfoFromFile(String filePath){
		File conf = new File(filePath);
		try {
			BufferedReader in = new BufferedReader(new FileReader(conf));
			String line = null;
			while((line = in.readLine()) != null){
				String[] infos = line.split("&&");
				if(infos.length != 7){
					logger.error("Some information is wrong in the file "+filePath);
					in.close();
					return false;
				}
				String domainName = infos[0].toLowerCase().trim();
				String endpoint = infos[1].toLowerCase().trim();
				String osType = infos[2].toLowerCase().trim();
				String nodeType = infos[3].toLowerCase().trim();
				String resTpl = infos[4].toLowerCase().trim();
				String osTpl = infos[5].toLowerCase().trim();
				String defaultSSHAccount = infos[6];
				if(domainInfos.containsKey(domainName)){
					OS_Res_Tpl curOsRes = new OS_Res_Tpl();
					curOsRes.OS_occi_ID = osTpl;
					curOsRes.res_occi_ID = resTpl;
					curOsRes.defaultSSHAccount = defaultSSHAccount;
					DomainInfo curDomainInfo = domainInfos.get(domainName);
					curDomainInfo.resTpls.put(nodeType+"##"+osType, curOsRes);
				}else{
					DomainInfo newDomainInfo = new DomainInfo();
					newDomainInfo.endpoint = endpoint;
					OS_Res_Tpl curOsRes = new OS_Res_Tpl();
					curOsRes.OS_occi_ID = osTpl;
					curOsRes.res_occi_ID = resTpl;
					curOsRes.defaultSSHAccount = defaultSSHAccount;
					newDomainInfo.resTpls.put(nodeType+"##"+osType, curOsRes);
					domainInfos.put(domainName, newDomainInfo);
				}
			}
			in.close();
		} catch (IOException e) {
			e.printStackTrace();
			logger.error("The domain infomation of EGI cannot be loaded from "+filePath);
			return false;
		}
		
		return true;
	}


}
