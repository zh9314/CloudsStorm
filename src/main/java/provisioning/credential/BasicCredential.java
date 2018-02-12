package provisioning.credential;

import java.util.Map;

import org.apache.log4j.Logger;

public class BasicCredential extends Credential{
	
	private static final Logger logger = Logger.getLogger(BasicCredential.class);
	

	@Override
	public boolean validateCredential(String credInfoPath) {
		if(credInfo == null)
			return true;
		for(Map.Entry<String, String> entry: credInfo.entrySet())
			if(entry.getValue() == null){
				logger.error("Value of "+entry.getKey()+" should not be 'null'");
				return false;
			}

		return true;
	}


}
