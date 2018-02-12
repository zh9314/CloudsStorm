package provisioning.database.EC2;



import provisioning.database.BasicDCMetaInfo;
import provisioning.database.BasicDatabase;
import provisioning.database.BasicVMMetaInfo;

public class EC2Database extends BasicDatabase {
	
	
	/**
	 * Get the AMI string according to the OS and domain.
	 * Null is returned if there is no such ami.
	 */
	public String getAMI(String OS, String domain){
		if(OS == null || domain == null)
			return null;
		for(int di = 0 ; di<DCMetaInfo.size() ; di++){
			BasicDCMetaInfo curDCMetaInfo = DCMetaInfo.get(di);
			if(curDCMetaInfo.domain != null
				&& domain.trim().equalsIgnoreCase(curDCMetaInfo.domain.trim())){
				for(int vi = 0 ; vi<curDCMetaInfo.VMMetaInfo.size() ; vi++){
					BasicVMMetaInfo curVMMetaInfo = curDCMetaInfo.VMMetaInfo.get(vi);
					if(curVMMetaInfo.OS != null
						&& OS.trim().equalsIgnoreCase(curVMMetaInfo.OS.trim())){
						return curVMMetaInfo.extraInfo.get("AMI");
					}
				}
			}
			
		}
		return null;
	}

	

}
