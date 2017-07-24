import java.util.ArrayList;
import java.util.HashMap;

import org.apache.log4j.Logger;

import provisioning.credential.Credential;
import provisioning.credential.EC2Credential;
import provisioning.credential.EGICredential;
import provisioning.credential.SSHKeyPair;
import provisioning.credential.UserCredential;
import provisioning.database.Database;
import provisioning.database.UserDatabase;
import provisioning.database.EC2.EC2Database;
import provisioning.database.EGI.EGIDatabase;
import provisioning.engine.TEngine.TEngine;
import provisioning.request.ProvisionRequest;
import provisioning.request.StartRequest;
import topologyAnalysis.TopologyAnalysisMain;
import commonTool.CommonTool;
import commonTool.Log4JUtils;


public class testProvisioning {

	private static final Logger logger = Logger.getLogger(testProvisioning.class);
	
	public static void main(String[] args) {
		Log4JUtils.setErrorLogFile("error.log");
		Log4JUtils.setInfoLogFile("info.log");
		
		String topTopologyLoadingPath = "ES/infocomExp/autoProvision/ProvisionOverhead/zh_all_test.yml";
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
		if(userCredential.cloudAccess == null)
			userCredential.cloudAccess = new HashMap<String, Credential>();
		userCredential.cloudAccess.put("ec2", ec2Credential);
		userCredential.cloudAccess.put("egi", egiCredential);
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
		EGIDatabase egiDatabase = new EGIDatabase();
		egiDatabase.loadDomainInfoFromFile(currentDir+"EGI_Domain_Info");
		EC2Database ec2Database = new EC2Database();
		ec2Database.loadDomainInfoFromFile(currentDir+"domains");
		ec2Database.loadAmiFromFile(currentDir+"OS_Domain_AMI");
		if(userDatabase.databases == null)
			userDatabase.databases = new HashMap<String, Database>();
		userDatabase.databases.put("ec2", ec2Database);
		userDatabase.databases.put("egi", egiDatabase);
		
		/*ProvisionRequest pq = new ProvisionRequest();
		pq.topologyName = "ec2_zh_b";
		ArrayList<ProvisionRequest> provisionReqs = new ArrayList<ProvisionRequest>();
		provisionReqs.add(pq);*/

		TEngine tEngine = new TEngine();
		long startTime = System.currentTimeMillis();
		//tEngine.provisionAll(tam.wholeTopology, userCredential, userDatabase);
		//tEngine.stopAll(tam.wholeTopology, userCredential, userDatabase);
		//tEngine.startAll(tam.wholeTopology, userCredential, userDatabase);
		//tEngine.provision(tam.wholeTopology, userCredential, userDatabase, provisionReqs);
		tEngine.deleteAll(tam.wholeTopology, userCredential, userDatabase);
		long endTime = System.currentTimeMillis();
		System.out.println("Total overhead: "+(endTime-startTime));
	}

}
