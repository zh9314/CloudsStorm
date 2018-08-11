/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * Copyright © Huan Zhou (SNE, University of Amsterdam) and contributors
 */
package standalone;

import infscall.CtrlAgent;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import lambdaExprs.infrasCode.main.ICYAML;
import lambdaInfrs.credential.UserCredential;
import lambdaInfrs.database.UserDatabase;
import lambdaInfrs.engine.TEngine.TEngine;
import lambdaInfrs.request.DeleteRequest;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import topology.analysis.TopologyAnalysisMain;
import topology.description.actual.SubTopologyInfo;
import topology.description.actual.VM;
import commonTool.CommonTool;
import commonTool.Log4JUtils;
import commonTool.TARGZ;

public class MainAsTool {
	
	private static final Logger logger = Logger.getLogger(MainAsTool.class);
	
	private static final String ctrlInf = "Infs" +File.separator+ "Topology" +File.separator+ "_ctrl.yml";
	private static final String topologyInf = "Infs" +File.separator+ "Topology" +File.separator+ "_top.yml";
	private static final String credInf = "Infs" +File.separator+ "UC" +File.separator+ "cred.yml";
	private static final String dbInf = "Infs" +File.separator+ "UD" +File.separator+ "db.yml";
	private static final String icInf = "App" +File.separator+ "infrasCode.yml";
	private static final String logsInf = "Logs" + File.separator;

	/**
	 * This is the entry of the whole program when used as a command line tool by the end user. It is also used in 'CTRL' mode.
	 * Usage:<br>
	 * 	  1. java -jar CloudsStorm.jar execute srcDir <br>
	 *    -> This command is used to execute the infrastructure code in this directory.    <br>
	 *    	 srcDir is the directory of the concrete description.   <br>
	 *       In this directory, the file structure is organized specifically. <br>
	 *    2. java -jar CloudsStorm.jar run srcDir <br>
	 *    -> This command is used to execute the infrastructure code in this directory but only in 'LOCAL' mode.   <br>
	 *    3. java -jar CloudsStorm.jar delete srcDir all/ctrl <br>
	 *    -> This command is used to delete some extra resources. <br>
	 *       When the option is 'all', this command deletes all the resources except the controlling agent. <br>
	 *       'ctrl' deletes all the resources including the controlling agent. <br>
	 *    4. java -jar CloudsStorm.jar ctrl icPath icLogPath <br>
	 *    -> This is used to execute the infrastructure code directly and genenrate the log at the icLogPath. <br>
	 *    5. java -jar CloudsStorm.jar partition srcDir dstDir (TBD) <br>
	 *    -> src is the file path of the abstract topology description. <br>
	 *    -> dstDir is the directory of concrete topology description files.  <br>
	 * @param args
	 */
	public static void main(String[] args) {
		Log4JUtils.setSystemOutputLogFile(Level.INFO);
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
			
			String appRootDir = CommonTool.formatDirWithSep(args[1]);
			
			String topTopologyLoadingPath = appRootDir + topologyInf;
			String credentialsPath = appRootDir + credInf;
			String dbsPath = appRootDir + dbInf;
			String ctrlFPath = appRootDir + ctrlInf;
			String logsDir = appRootDir + logsInf;
			String ICPath = appRootDir + icInf;
			
			
			TopologyAnalysisMain tam = new TopologyAnalysisMain(topTopologyLoadingPath);
			if(!tam.fullLoadWholeTopology())
				return;
			
			UserCredential userCredential = new UserCredential();
			userCredential.loadCloudAccessCreds(credentialsPath);
			UserDatabase userDatabase = new UserDatabase();
			userDatabase.loadCloudDBs(dbsPath);
			
			File logsDirF = new File(logsDir);
			if(!logsDirF.exists())
				logsDirF.mkdir();
			Log4JUtils.setInfoLogFile(logsDir + "CloudsStorm.log");
			
			
			ICYAML ic = new ICYAML(tam.wholeTopology, userCredential, userDatabase);
			if(!ic.loadInfrasCodes(ICPath, appRootDir))
				return ;
			
			ic.execute(appRootDir, ctrlFPath, logsDir);
			
			logger.warn("Finished!");
			
		}else if(args[0].trim().toLowerCase().equals("run")){
			if(args.length != 2){
				System.out.println("ERROR! There should be two arguments for 'run'!");
				return ;
			}
			String appRootDir = CommonTool.formatDirWithSep(args[1]);
			
			String topTopologyLoadingPath = appRootDir + topologyInf;
			String credentialsPath = appRootDir + credInf;
			String dbsPath = appRootDir + dbInf;
			String ICPath = appRootDir + icInf;
			String logsDir = appRootDir + logsInf;
			
			
			TopologyAnalysisMain tam = new TopologyAnalysisMain(topTopologyLoadingPath);
			if(!tam.fullLoadWholeTopology())
				return;
			
			UserCredential userCredential = new UserCredential();
			userCredential.loadCloudAccessCreds(credentialsPath);
			UserDatabase userDatabase = new UserDatabase();
			userDatabase.loadCloudDBs(dbsPath);
			
			File logsDirF = new File(logsDir);
			if(!logsDirF.exists())
				logsDirF.mkdir();
			Log4JUtils.setInfoLogFile(logsDir + "CloudsStorm.log");
			
			ICYAML ic = new ICYAML(tam.wholeTopology, userCredential, userDatabase);
			if(!ic.loadInfrasCodes(ICPath, appRootDir))
				return ;
			
			ic.run(logsDir + "InfrasCode.log");
			
			logger.warn("Finished!");
			
		}else if(args[0].trim().toLowerCase().equals("go")){
			if(args.length != 3){
				System.out.println("ERROR! There should be three arguments for 'go'!");
				return ;
			}
			String appFilePath = args[1];
			//String appUpDir = CommonTool.getPathDir(appFilePath);
			String appUpDir = "/tmp/";
			String appDirName = "AppInfs"; // args[2];
			String appRootDir = appUpDir + appDirName + File.separator;
			
			String topTopologyLoadingPath = appRootDir + topologyInf;
			String credentialsPath = appRootDir + credInf;
			String dbsPath = appRootDir + dbInf;
			String ICPath = appRootDir + icInf;
			String logsDir = appRootDir + logsInf;
			
			
			try {
				TARGZ.decompress(appFilePath, new File(appRootDir));
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
			
			
			ICYAML ic = new ICYAML(tam.wholeTopology, userCredential, userDatabase);
			if(!ic.loadInfrasCodes(ICPath, appRootDir)){
				return ;
			}
			
			String tmpInfoPath = System.getProperty("java.io.tmpdir") + File.separator + "info";
			try {
				FileWriter infoFW = new FileWriter(tmpInfoPath, false);
				infoFW.write(appRootDir);
				infoFW.close();
			} catch (IOException e) {
				e.printStackTrace();
				return ;
			}
			
			ic.run(logsDir + "InfrasCode.log");
			
			logger.warn("Finished!");
			
		}else if(args[0].trim().toLowerCase().equals("delete")){
			if(args.length != 3){
				System.out.println("ERROR! There should be three arguments for 'delete'!");
				return ;
			}
			String object = args[2];
			String appRootDir = CommonTool.formatDirWithSep(args[1]);
			
			String topTopologyLoadingPath = appRootDir + topologyInf;
			String credentialsPath = appRootDir + credInf;
			String dbsPath = appRootDir + dbInf;
			String logsDir = appRootDir + logsInf;
			
			TopologyAnalysisMain tam = new TopologyAnalysisMain(topTopologyLoadingPath);
			if(!tam.fullLoadWholeTopology())
				return;
			
			UserCredential userCredential = new UserCredential();
			userCredential.loadCloudAccessCreds(credentialsPath);
			UserDatabase userDatabase = new UserDatabase();
			userDatabase.loadCloudDBs(dbsPath);
			
			File logsDirF = new File(logsDir);
			if(!logsDirF.exists())
				logsDirF.mkdir();
			Log4JUtils.setInfoLogFile(logsDir + "CloudsStorm.log");
			
			if(object.trim().toLowerCase().contains("ctrl")){
				
				SubTopologyInfo ctrlST = tam.wholeTopology.getSubtopology("_ctrl");
				if(ctrlST == null){
					logger.error("There is no control agent! Use 'delete all' instead!");
					return ;
				}
				if( ctrlST.status.trim().equals("running") ){
					VM ctrlVM = ctrlST.subTopology.getVMinSubClassbyName("ctrl");
					CtrlAgent ctrlAgent = new CtrlAgent();
					///by default, 'AppID' is '123'.
					String AppID = "123";
					if( object.trim().contains(":") ){
						String ids [] = object.trim().split(":");
						AppID = ids[1];
					}
					if( ctrlAgent.init(ctrlVM.publicAddress).setAppID(AppID).deleteCtrl() == null ){
						logger.error("'AppID' is wrong!");
						return ;
					}
				}
				
				TEngine tEngine = new TEngine();
				tEngine.deleteAll(tam.wholeTopology, userCredential, userDatabase);
			}
			if(object.trim().toLowerCase().contains("all")){
				
				SubTopologyInfo ctrlST = tam.wholeTopology.getSubtopology("_ctrl");
				if(ctrlST == null || !ctrlST.status.trim().equals("running")){   ////in ctrl mode
					VM ctrlVM = ctrlST.subTopology.getVMinSubClassbyName("ctrl");
					CtrlAgent ctrlAgent = new CtrlAgent();
					///by default, 'AppID' is '123'
					String AppID = "123";
					if( object.trim().contains(":") ){
						String ids [] = object.trim().split(":");
						AppID = ids[1];
					}
					if( ctrlAgent.init(ctrlVM.publicAddress).setAppID(AppID).deleteAll() == null ){
						logger.error("'AppID' is wrong!");
						return ;
					}
					
					TEngine tEngine = new TEngine();
					DeleteRequest deleteReq = new DeleteRequest();
					for(int si = 0 ; si<tam.wholeTopology.topologies.size() ; si++){
						if(tam.wholeTopology.topologies.get(si).topology.equalsIgnoreCase("_ctrl"))
							continue;
						deleteReq.content.put(tam.wholeTopology.topologies.get(si).topology, false);
					}
					tEngine.delete(tam.wholeTopology, userCredential, userDatabase, deleteReq);
					
				}else{
					TEngine tEngine = new TEngine();
					DeleteRequest deleteReq = new DeleteRequest();
					for(int si = 0 ; si<tam.wholeTopology.topologies.size() ; si++){
						if(tam.wholeTopology.topologies.get(si).topology.equalsIgnoreCase("_ctrl"))
							continue;
						deleteReq.content.put(tam.wholeTopology.topologies.get(si).topology, false);
					}
					tEngine.delete(tam.wholeTopology, userCredential, userDatabase, deleteReq);
				}
				
				
			}
		}
		else
			System.out.println("ERROR! Invalid arguments!");

	}

}
