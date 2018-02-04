

import infrastructureCode.interpreter.ICInterpreter;
import infrastructureCode.main.ICYAML;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.output.NullOutputStream;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.jcabi.ssh.SSH;
import com.jcabi.ssh.Shell;

import commonTool.CommonTool;
import commonTool.Log4JUtils;
import commonTool.TARGZ;
import provisioning.credential.UserCredential;
import provisioning.database.UserDatabase;
import provisioning.engine.TEngine.TEngine;
import provisioning.request.ProvisionRequest;
import topologyAnalysis.TopologyAnalysisMain;
import topologyAnalysis.dataStructure.SubTopologyInfo;
import topologyAnalysis.dataStructure.VM;




public class testLoading {
	
	private static final Logger logger = Logger.getLogger(testLoading.class);


	public static void main(String[] args) {
		String appRootDir = "examples/HadoopTest/";
		String topTopologyLoadingPath = appRootDir + "Infs/Topology/_top.yml";
		String sshKeysDir = appRootDir + "Infs/Topology/";
		String credentialsPath = appRootDir + "Infs/UC/cred.yml";
		String dbsPath = appRootDir + "Infs/UD/db.yml";
		
		TopologyAnalysisMain tam = new TopologyAnalysisMain(topTopologyLoadingPath);
		if(!tam.fullLoadWholeTopology())
			return;
		
		UserCredential userCredential = new UserCredential();
		userCredential.loadCloudAccessCreds(credentialsPath);
		UserDatabase userDatabase = new UserDatabase();
		userDatabase.loadCloudDBs(dbsPath);
		
		userCredential.initalSSHKeys(sshKeysDir, tam.wholeTopology);
		
		String ICPath = appRootDir + "App" +File.separator+ "infrasCode.yml";
		ICYAML ic = new ICYAML(tam.wholeTopology, userCredential, userDatabase, 
				null);
		if(!ic.loadInfrasCodes(ICPath))
			return ;
		
		//Log4JUtils.setSystemOutputLogFile(Level.INFO);
		
		///by default the mode is remote 
		if(ic.Mode != null && ic.Mode.trim().equalsIgnoreCase("LOCAL")){
			String logsDir = appRootDir + "Logs" + File.separator;
			File logsDirF = new File(logsDir);
			if(!logsDirF.exists())
				logsDirF.mkdir();
			Log4JUtils.setWarnLogFile(logsDir + "CloudsStorm.log");
			
			String icLogPath = logsDir + "InfrasCode.log";
			FileWriter icLogger = null;
			try {
				icLogger = new FileWriter(icLogPath, false);
				icLogger.write("LOGs:\n");
				icLogger.flush();
			} catch (IOException e1) {
				e1.printStackTrace();
				logger.error("Cannot build log file for "+icLogPath);
				return ;
			}
			ic.setICLogger(icLogger);
			
			ICInterpreter icInterpreter = new ICInterpreter();
			icInterpreter.execute(ic);
			
			logger.warn("Finished!");
			
		}else if(ic.Mode == null || ic.Mode.trim().equalsIgnoreCase("CTRL")){
			ProvisionRequest pq = new ProvisionRequest();
			pq.topologyName = "_ctrl";
			ArrayList<ProvisionRequest> provisionReqs = new ArrayList<ProvisionRequest>();
			provisionReqs.add(pq);
			
			TEngine tEngine = new TEngine();
			
			tEngine.provision(tam.wholeTopology, userCredential, userDatabase, provisionReqs);
			
			////Get the IP address of the controller if it is running
			SubTopologyInfo ctrlST = tam.wholeTopology.getSubtopology("_ctrl");
			if(ctrlST == null || !ctrlST.status.trim().equals("running")){
				logger.error("The controller is not provisioned! EXIT!");
				return ;
			}
			VM ctrlVM = ctrlST.subTopology.getVMinSubClassbyName("ctrl");
			String tmpFilePath = System.getProperty("java.io.tmpdir") + File.separator + "AppInfs.tar.gz";
			
			try {
				TARGZ.compress(tmpFilePath, new File(appRootDir));
			} catch (IOException e) {
				e.printStackTrace();
				return ;
			}
			
			String rootDirName = CommonTool.getDirName(appRootDir);
			
			Shell shell;
			try {
				shell = new SSH(ctrlVM.publicAddress, 22, ctrlVM.defaultSSHAccount, ctrlST.subTopology.accessKeyPair.privateKeyString);
				File appTARGZ = new File(tmpFilePath);
				String appDir = "/root/" + System.currentTimeMillis() ;
				new Shell.Safe(shell).exec(
						  "sudo mkdir "+ appDir + "/",
						  null,
						  new NullOutputStream(), new NullOutputStream()
						);
				new Shell.Safe(shell).exec(
						  "sudo cat > "+ appDir + "/AppInfs.tar.gz",
						  new FileInputStream(appTARGZ),
						  new NullOutputStream(), new NullOutputStream()
						);
				logger.warn("Now application is deploying and running on the remote! You can terminate this and check the info on remote controller!");
				new Shell.Safe(shell).exec(
						  "sudo nohup java -jar /root/CloudsStorm.jar run "+ appDir + "/AppInfs.tar.gz "+rootDirName+" &",
						  null,
						  new NullOutputStream(), new NullOutputStream()
						);
				FileUtils.deleteQuietly(appTARGZ);
			} catch (IOException e) {
				e.printStackTrace();
				return ;
			}
			
			//tEngine.deleteAll(tam.wholeTopology, userCredential, userDatabase);
		}else
			logger.error("Unrecognized 'Mode' for executing infrastructure code!");
		
		
		
		
		

	}

}
