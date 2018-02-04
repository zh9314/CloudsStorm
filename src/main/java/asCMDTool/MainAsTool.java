package asCMDTool;

import infrastructureCode.interpreter.ICInterpreter;
import infrastructureCode.main.ICYAML;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.log4j.Logger;

import provisioning.credential.UserCredential;
import provisioning.database.UserDatabase;
import topologyAnalysis.TopologyAnalysisMain;
import commonTool.CommonTool;
import commonTool.Log4JUtils;
import commonTool.TARGZ;

public class MainAsTool {
	
	private static final Logger logger = Logger.getLogger(MainAsTool.class);
	
	private static final String ctrlInf = "Infs" +File.separator+ "Topology" +File.separator+ "_ctrl.yml";

	/**
	 * This is the entry of the whole program when it is used as a command line tool by the end user.
	 * Usage:
	 *    1. java -jar lambdai_I.jar partition src dstDir 
	 *    -> src is the file path of the abstract topology description.
	 *    -> dstDir is the directory of concrete topology description files. 
	 *    2. java -jar lambdai_I.jar execute srcDir
	 *    -> srcDir is the directory of the concrete description. It is used to locate the description of the controller.
	 *    The controller is always the first to be provisioned.
	 * @param args
	 */
	public static void main(String[] args) {
		if(args.length == 0){
			System.out.println("ERROR! There should be arguments! Exit!");
			return ;
		}
		if(args[0].trim().toLowerCase().equals("partition")){
			if(args.length != 3){
				System.out.println("ERROR! There should be three arguments for 'partition'!");
				return ;
			}
			
		}else if(args[0].trim().toLowerCase().equals("execute")){
			if(args.length != 2){
				System.out.println("ERROR! There should be two arguments for 'execute'!");
				return ;
			}
			String ctrlFPath = CommonTool.formatDirWithSep(args[1]) + ctrlInf;
			File ctrlF = new File(ctrlFPath);
			if(!ctrlF.exists()){
				System.out.println("ERROR! No controller is defined!");
				return ;
			}
		}else if(args[0].trim().toLowerCase().equals("run")){
			if(args.length != 3){
				System.out.println("ERROR! There should be three arguments for 'run'!");
				return ;
			}
			String appFilePath = args[1];
			String appUpDir = CommonTool.getPathDir(appFilePath);
			String appDirName = args[2];
			String appRootDir = appUpDir + appDirName + File.separator;
			
			
			try {
				TARGZ.decompress(appFilePath, new File(appUpDir));
			} catch (IOException e) {
				e.printStackTrace();
				logger.error("Cannot decompress "+appFilePath);
				return ;
			}
			
			String ctrlFPath = appRootDir + ctrlInf;
			File ctrlF = new File(ctrlFPath);
			if(!ctrlF.exists()){
				logger.error("ERROR! No controller is defined!");
				return ;
			}
			
			String topTopologyLoadingPath = appRootDir + "Infs" +File.separator+ "Topology" +File.separator+ "_top.yml";
			String sshKeysDir = appRootDir + "Infs" +File.separator+ "Topology" +File.separator;
			String credentialsPath = appRootDir + "Infs" +File.separator+ "UC" +File.separator+ "cred.yml";
			String dbsPath = appRootDir + "Infs" +File.separator+ "UD" +File.separator+ "db.yml";
			String ICPath = appRootDir + "App" +File.separator+ "infrasCode.yml";
			String logsDir = appRootDir + "Logs" + File.separator;
			
			File logsDirF = new File(logsDir);
			if(!logsDirF.exists())
				logsDirF.mkdir();
			Log4JUtils.setWarnLogFile(logsDir + "CloudsStorm.log");
			
			TopologyAnalysisMain tam = new TopologyAnalysisMain(topTopologyLoadingPath);
			if(!tam.fullLoadWholeTopology())
			{
				logger.error("sth wrong!");
				return;
			}
			
			UserCredential userCredential = new UserCredential();
			userCredential.loadCloudAccessCreds(credentialsPath);
			UserDatabase userDatabase = new UserDatabase();
			userDatabase.loadCloudDBs(dbsPath);
			
			userCredential.initalSSHKeys(sshKeysDir, tam.wholeTopology);
			
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
			ICYAML ic = new ICYAML(tam.wholeTopology, userCredential, userDatabase, 
					icLogger);
			if(!ic.loadInfrasCodes(ICPath)){
				return ;
			}
			ICInterpreter icInterpreter = new ICInterpreter();
			icInterpreter.execute(ic);
			
			logger.warn("Finished!");
			
		}
		else
			System.out.println("ERROR! Invalid arguments!");

	}

}
