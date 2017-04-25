import java.util.ArrayList;
import java.util.HashMap;

import org.apache.log4j.Logger;

import provisioning.credential.Credential;
import provisioning.credential.EC2Credential;
import provisioning.credential.SSHKeyPair;
import provisioning.credential.UserCredential;
import provisioning.database.Database;
import provisioning.database.UserDatabase;
import provisioning.database.EC2.EC2Database;
import provisioning.engine.TEngine.TEngine;
import provisioning.request.ScalingRequest;
import topologyAnalysis.TopologyAnalysisMain;
import commonTool.CommonTool;
import commonTool.Log4JUtils;


public class testAutoScaling {

	private static final Logger logger = Logger.getLogger(testAutoScaling.class);
	
	public static void main(String[] args) {
		Log4JUtils.setErrorLogFile("error.log");
		Log4JUtils.setInfoLogFile("info.log");
		
		String topTopologyLoadingPath = "ES/scaling/zh_all_test.yml";
		String currentDir = CommonTool.getPathDir(topTopologyLoadingPath);
	      
		TopologyAnalysisMain tam = new TopologyAnalysisMain(topTopologyLoadingPath);
		if(!tam.fullLoadWholeTopology())
		{
			logger.error("sth wrong!");
			return;
		}
		
		////Initial credentials and ssh key pairs
		UserCredential userCredential = new UserCredential();
		EC2Credential ec2Credential = new EC2Credential();
		ec2Credential.accessKey = "sdfsdfsdfsd";
		ec2Credential.secretKey = "sdfsdfsdfsdfwefwefwef";
		if(userCredential.cloudAccess == null)
			userCredential.cloudAccess = new HashMap<String, Credential>();
		userCredential.cloudAccess.put("ec2", ec2Credential);
		ArrayList<SSHKeyPair> sshKeyPairs = userCredential.loadSSHKeyPairFromFile(currentDir);
		if(sshKeyPairs == null){
			logger.error("Error happens during loading ssh key pairs!");
			return;
		}
		if(sshKeyPairs.size() == 0){
			logger.warn("No ssh key pair is loaded!");
		}else{
			if(!userCredential.initial(sshKeyPairs, tam.wholeTopology)){
				logger.error("Error happens during initializing the ssh keys for accessing the clouds!");
				return ;
			}
		}
		
		///Initial Database
		UserDatabase userDatabase = new UserDatabase();
		EC2Database ec2Database = new EC2Database();
		ec2Database.loadDomainFromFile(currentDir+"domains");
		ec2Database.loadAmiFromFile(currentDir+"OS_Domain_AMI");
		if(userDatabase.databases == null)
			userDatabase.databases = new HashMap<String, Database>();
		userDatabase.databases.put("ec2", ec2Database);

		TEngine tEngine = new TEngine();
		
		ArrayList<ScalingRequest> scalDCs = new ArrayList<ScalingRequest>();
		ScalingRequest scalReq = new ScalingRequest();
		/*scalReq.cloudProvider = "ec2";
		scalReq.domain = "Ohio";
		scalReq.satisfied = false;
		scalDCs.add(scalReq);
		
		scalReq = new ScalingRequest();
		scalReq.cloudProvider = "ec2";
		scalReq.domain = "Ohio";
		scalReq.satisfied = false;
		scalDCs.add(scalReq);*/
		
		scalReq = new ScalingRequest();
		scalReq.cloudProvider = "ec2";
		scalReq.domain = "Ohio";
		scalReq.satisfied = false;
		scalDCs.add(scalReq);
		
		tEngine.autoScal(tam.wholeTopology, userCredential, userDatabase, "ec2_zh_b", true, scalDCs);
	}

}
