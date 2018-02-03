import java.util.ArrayList;
import java.util.HashMap;

import org.apache.log4j.Logger;

import provisioning.credential.Credential;
import provisioning.credential.EGICredential;
import provisioning.credential.UserCredential;
import provisioning.database.Database;
import provisioning.database.UserDatabase;
import provisioning.database.EGI.EGIDatabase;
import provisioning.engine.TEngine.TEngine;
import provisioning.request.StartRequest;
import topologyAnalysis.TopologyAnalysisMain;
import commonTool.CommonTool;
import commonTool.Log4JUtils;


public class testStart {

	private static final Logger logger = Logger.getLogger(testStart.class);
	
	public static void main(String[] args) {
		Log4JUtils.setErrorLogFile("error.log");
		Log4JUtils.setInfoLogFile("info.log");
		
		String topTopologyLoadingPath = "ES/EGI/zh_all_test.yml";
		String currentDir = CommonTool.getPathDir(topTopologyLoadingPath);
	      
		TopologyAnalysisMain tam = new TopologyAnalysisMain(topTopologyLoadingPath);
		if(!tam.fullLoadWholeTopology())
		{
			logger.error("sth wrong!");
			return;
		}
		
		////Initial credentials and ssh key pairs
		UserCredential userCredential = new UserCredential();
		/*EC2Credential ec2Credential = new EC2Credential();
		ec2Credential.accessKey = "sdfsdf";
		ec2Credential.secretKey = "sdfsdfsdfadfsdf";*/
		EGICredential egiCredential = new EGICredential();
		egiCredential.proxyFilePath = "/tmp/x509up_u0";
		egiCredential.trustedCertPath = "/etc/grid-security/certificates";
		if(userCredential.cloudAccess == null)
			userCredential.cloudAccess = new HashMap<String, Credential>();
		//userCredential.cloudAccess.put("ec2", ec2Credential);
		userCredential.cloudAccess.put("egi", egiCredential);
		
		userCredential.initalSSHKeys(currentDir, tam.wholeTopology);
		
		///Initial Database
		UserDatabase userDatabase = new UserDatabase();
		EGIDatabase egiDatabase = new EGIDatabase();
		//egiDatabase.loadDomainInfoFromFile(currentDir+"EGI_Domain_Info");
		/*EC2Database ec2Database = new EC2Database();
		ec2Database.loadDomainFromFile(currentDir+"domains");
		ec2Database.loadAmiFromFile(currentDir+"OS_Domain_AMI");*/
		if(userDatabase.databases == null)
			userDatabase.databases = new HashMap<String, Database>();
		//userDatabase.databases.put("ec2", ec2Database);
		userDatabase.databases.put("egi", egiDatabase);

		TEngine tEngine = new TEngine();
		
		StartRequest sq = new StartRequest();
		sq.topologyName = "egi_zh_a";
		ArrayList<StartRequest> startReqs = new ArrayList<StartRequest>();
		startReqs.add(sq);
		
		tEngine.start(tam.wholeTopology, userCredential, userDatabase, startReqs);
		
	}

}
