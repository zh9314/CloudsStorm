package provisioning.database;

import java.util.ArrayList;


public class BasicDatabase extends Database{
	
	public ArrayList<BasicDCMetaInfo> DCMetaInfo;
	
	@Override
	public String getEndpoint(String domain) {
		if(domain == null)
			return null;
		for(int di = 0 ; di < this.DCMetaInfo.size(); di++)
			if(DCMetaInfo.get(di).domain != null
			 && domain.trim().equalsIgnoreCase(DCMetaInfo.get(di).domain.trim()))
				return DCMetaInfo.get(di).endpoint;
		return null;
	}
	
	@Override
	public DCMetaInfo getDCMetaInfo(String domain) {
		if(domain == null)
			return null;
		for(int di = 0 ; di < this.DCMetaInfo.size(); di++)
			if(DCMetaInfo.get(di).domain != null
			 && domain.trim().equalsIgnoreCase(DCMetaInfo.get(di).domain.trim()))
				return DCMetaInfo.get(di);
		return null;
	}

	@Override
	public VMMetaInfo getVMMetaInfo(String domain, String OS, String vmType) {
		if(domain == null || OS == null || vmType == null)
			return null;
		for(int di = 0 ; di < this.DCMetaInfo.size(); di++)
			if(this.DCMetaInfo.get(di).domain != null
			 && domain.trim().equalsIgnoreCase(DCMetaInfo.get(di).domain.trim())){
				for(int vi = 0 ; vi < DCMetaInfo.get(di).VMMetaInfo.size() ; vi++){
					BasicVMMetaInfo curInfo = DCMetaInfo.get(di).VMMetaInfo.get(vi);
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
