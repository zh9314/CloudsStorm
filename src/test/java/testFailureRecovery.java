import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.log4j.Logger;

import provisioning.credential.Credential;
import provisioning.credential.EC2Credential;
import provisioning.credential.EGICredential;
import provisioning.credential.ExoGENICredential;
import provisioning.credential.SSHKeyPair;
import provisioning.credential.UserCredential;
import provisioning.database.Database;
import provisioning.database.UserDatabase;
import provisioning.database.EC2.EC2Database;
import provisioning.database.EGI.EGIDatabase;
import provisioning.database.ExoGENI.ExoGENIDatabase;
import provisioning.engine.TEngine.TEngine;
import provisioning.request.RecoverRequest;
import topologyAnalysis.TopologyAnalysisMain;
import commonTool.CommonTool;
import commonTool.Log4JUtils;


public class testFailureRecovery {

		
	private static final Logger logger = Logger.getLogger(testFailureRecovery.class);
	
	public static void main(String[] args) {
		Log4JUtils.setErrorLogFile("error.log");
		Log4JUtils.setInfoLogFile("info.log");
		
		String topTopologyLoadingPath = "ES/infocomExp/failureRecovery/zh_all_test.yml";
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
		ec2Credential.accessKey = "AKIAITY3KHZUQ6M7YBSQ";
		ec2Credential.secretKey = "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";
		EGICredential egiCredential = new EGICredential();
		egiCredential.proxyFilePath = "/tmp/x509up_u501";
		egiCredential.trustedCertPath = "/etc/grid-security/certificates";
		ExoGENICredential exoGENICredential = new ExoGENICredential();
		exoGENICredential.keyAlias = "zhgeni";
		exoGENICredential.keyPassword = "131452";
		exoGENICredential.userKeyPath = "ES/geni/user.jks";
		
		if(userCredential.cloudAccess == null)
			userCredential.cloudAccess = new HashMap<String, Credential>();
		userCredential.cloudAccess.put("ec2", ec2Credential);
		userCredential.cloudAccess.put("egi", egiCredential);
		userCredential.cloudAccess.put("exogeni", exoGENICredential);
		
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
		ec2Database.loadDomainInfoFromFile(currentDir+"domains");
		ec2Database.loadAmiFromFile(currentDir+"OS_Domain_AMI");
		EGIDatabase egiDatabase = new EGIDatabase();
		egiDatabase.loadDomainInfoFromFile(currentDir+"EGI_Domain_Info");
		ExoGENIDatabase exoGENIDatabase = new ExoGENIDatabase();
		
		if(userDatabase.databases == null)
			userDatabase.databases = new HashMap<String, Database>();
		userDatabase.databases.put("ec2", ec2Database);
		userDatabase.databases.put("egi", egiDatabase);
		userDatabase.databases.put("exogeni", exoGENIDatabase);

		TEngine tEngine = new TEngine();
		//tEngine.provisionAll(tam.wholeTopology, userCredential, userDatabase);
		tEngine.deleteAll(tam.wholeTopology, userCredential, userDatabase);
		
		
		/*try {
			FileWriter fw = new FileWriter("con.sh");
			fw.write("ssh -i /Users/zh9314/Documents/workspace/DRIPProvisioningAgent/ES/infocomExp/failureRecovery/cb2f3d24-37bf-4168-a3dd-bb70b29d0e52/id_rsa ubuntu@34.201.82.245 \"java -jar /home/ubuntu/testCon.jar 192.168.3.12\"\n");
			fw.close();
			Runtime.getRuntime().exec("bash con.sh");
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		tEngine.detectFailure(tam.wholeTopology, userCredential, userDatabase, "ec2_zh_b");
		
		ArrayList<RecoverRequest> recoverReqs = new ArrayList<RecoverRequest>();
		RecoverRequest rq = new RecoverRequest();
		rq.cloudProvider = "ec2";
		rq.domain = "Ohio";
		rq.topologyName = "ec2_zh_b";
		recoverReqs.add(rq);
		
		tEngine.recoverAll(tam.wholeTopology, userCredential, userDatabase, recoverReqs);*/
	}

}
