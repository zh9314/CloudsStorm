package provisioning.database.EGI;

import java.io.File;
import java.util.ArrayList;
import java.util.Map;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import provisioning.database.Database;
import provisioning.database.VMMetaInfo;

public class EGIDatabase extends Database {
	private static final Logger logger = Logger.getLogger(EGIDatabase.class);


	public ArrayList<EGIDCMetaInfo> DCMetaInfo;
	
	
	/*public String getOSTPL(String OS, String domain){
		if(OS == null || domain == null)
			return null;
		for(int di = 0 ; di<DCMetaInfo.size() ; di++){
			EGIDCMetaInfo egiDCMetaInfo = DCMetaInfo.get(di);
			if(egiDCMetaInfo.domain != null
				&& domain.trim().equalsIgnoreCase(egiDCMetaInfo.domain.trim())){
				for(int vi = 0 ; vi<egiDCMetaInfo.VMMetaInfo.size() ; vi++){
					EGIVMMetaInfo egiVMMetaInfo = egiDCMetaInfo.VMMetaInfo.get(vi);
					if(egiVMMetaInfo.OS != null
						&& OS.trim().equalsIgnoreCase(egiVMMetaInfo.OS.trim())){
						return egiVMMetaInfo.OS_occi_ID;
					}
				}
			}
			
		}
		return null;
	}
	
	public String getRESTPL(String vmType, String domain){
		if(vmType == null || domain == null)
			return null;
		for(int di = 0 ; di<DCMetaInfo.size() ; di++){
			EGIDCMetaInfo egiDCMetaInfo = DCMetaInfo.get(di);
			if(egiDCMetaInfo.domain != null
				&& domain.trim().equalsIgnoreCase(egiDCMetaInfo.domain.trim())){
				for(int vi = 0 ; vi<egiDCMetaInfo.VMMetaInfo.size() ; vi++){
					EGIVMMetaInfo egiVMMetaInfo = egiDCMetaInfo.VMMetaInfo.get(vi);
					if(egiVMMetaInfo.VMType != null
						&& vmType.trim().equalsIgnoreCase(egiVMMetaInfo.VMType.trim())){
						return egiVMMetaInfo.RES_occi_ID;
					}
				}
			}
			
		}
		return null;
	}*/


	@Override
	public boolean loadDatabase(String dbInfoFile,
			Map<String, Database> databases) {
		ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
		EGIDatabase egiDatabase = null;
		try {
			egiDatabase = mapper.readValue(new File(dbInfoFile), EGIDatabase.class);
	        	if(egiDatabase == null){
	        		logger.error("Users's EGI database from "+dbInfoFile+" is invalid!");
	            	return false;
	        	}
		 }catch (Exception e) {
             logger.error(e.toString());
             e.printStackTrace();
             return false;
         }
		egiDatabase.toolInfo.put("sengine", "provisioning.engine.SEngine.EGISEngine");
		databases.put("egi", egiDatabase);
		return true;
	}


	@Override
	public String getEndpoint(String domain) {
		if(domain == null)
			return null;
		for(int di = 0 ; di < DCMetaInfo.size(); di++)
			if(DCMetaInfo.get(di).domain != null
			 && domain.trim().equalsIgnoreCase(DCMetaInfo.get(di).domain.trim()))
				return DCMetaInfo.get(di).endpoint;
		
		return null;
	}

	
	@Override
	public VMMetaInfo getVMMetaInfo(String domain, String OS, String vmType) {
		if(domain == null || OS == null || vmType == null)
			return null;
		for(int di = 0 ; di < DCMetaInfo.size(); di++)
			if(DCMetaInfo.get(di).domain != null
			 && domain.trim().equalsIgnoreCase(DCMetaInfo.get(di).domain.trim())){
				for(int vi = 0 ; vi < DCMetaInfo.get(di).VMMetaInfo.size() ; vi++){
					EGIVMMetaInfo curInfo = DCMetaInfo.get(di).VMMetaInfo.get(vi);
					if(curInfo.OS != null
				      && curInfo.VMType != null
				      && OS.trim().equalsIgnoreCase(curInfo.OS.trim())
				      && vmType.trim().equalsIgnoreCase(curInfo.VMType.trim()))
						return (VMMetaInfo)curInfo;
				}
			}
				
		
		return null;
	}

}
