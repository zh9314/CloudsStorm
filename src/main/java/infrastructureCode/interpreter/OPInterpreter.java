package infrastructureCode.interpreter;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import commonTool.CommonTool;
import provisioning.credential.UserCredential;
import provisioning.database.UserDatabase;
import provisioning.engine.TEngine.TEngine;
import provisioning.request.DeleteRequest;
import provisioning.request.ProvisionRequest;
import topologyAnalysis.dataStructure.SubTopologyInfo;
import topologyAnalysis.dataStructure.TopTopology;
import topologyAnalysis.dataStructure.VM;
import infrastructureCode.log.Log;
import infrastructureCode.log.Logs;
import infrastructureCode.main.Operation;

public class OPInterpreter {
	private static final Logger logger = Logger.getLogger(OPInterpreter.class);
	
	private Operation opInput;
	private TopTopology topTopology;
	private UserCredential userCredential;
	private UserDatabase userDatabase;
	private FileWriter opLogger;
	
	public OPInterpreter(Operation input,
			TopTopology topTopology, UserCredential userCredential, UserDatabase userDatabase,
			FileWriter opLogger){
		this.opInput = input;
		this.topTopology = topTopology;
		this.userCredential = userCredential;
		this.userDatabase = userDatabase;
		this.opLogger = opLogger;
	}
	
	public boolean execute(){
		if(opInput == null || opInput.Operation == null){
			logger.error("The input operation cannot be null!");
			return false;
		}
		boolean success = false;
		if(opInput.Operation.trim().equalsIgnoreCase("provision"))
			success = provision();
		else if(opInput.Operation.trim().equalsIgnoreCase("delete"))
			success = delete();
		else if(opInput.Operation.trim().equalsIgnoreCase("execute"))
			success = executeCMD();

		return success;
	}
	
	private boolean provision(){
		if(opInput.SubjectType == null){
			logger.error("Invalid operation without 'SubjectType': "+opInput.toString());
			return false;
		}
		if(opInput.SubjectType.trim().equalsIgnoreCase("subtopology")){
			String opSubjects = opInput.Subjects;
			if(opSubjects == null){
				logger.warn("Nothing to operate on!");
				return true;
			}
			TEngine tEngine = new TEngine();
			ArrayList<ProvisionRequest> provisionReqs = new ArrayList<ProvisionRequest>();
			if(opSubjects.trim().equalsIgnoreCase("_all")){
				logger.debug("Provision all sub-topologies!");
				for(int si = 0 ; si<topTopology.topologies.size() ; si++){
					if(topTopology.topologies.get(si).topology.equalsIgnoreCase("_ctrl"))
						continue;
					if(topTopology.topologies.get(si).status.equals("fresh")
						||topTopology.topologies.get(si).status.equals("stopped")
						||topTopology.topologies.get(si).status.equals("deleted")){
						ProvisionRequest pq = new ProvisionRequest();
						pq.topologyName = topTopology.topologies.get(si).topology;
						provisionReqs.add(pq);
					}
				}
				tEngine.provision(topTopology, userCredential, userDatabase, provisionReqs);
			}else{
				String [] opSubjectsList = opSubjects.split("\\|\\|");
				for(int oi = 0 ; oi < opSubjectsList.length ; oi++){
					ProvisionRequest pq = new ProvisionRequest();
					pq.topologyName = opSubjectsList[oi];
					provisionReqs.add(pq);
					logger.debug("Provision sub-topology "+pq.topologyName);
				}
				tEngine.provision(topTopology, userCredential, userDatabase, provisionReqs);
			}
			
			String logString = "";
			for(int reqi = 0 ; reqi<provisionReqs.size() ; reqi++){
				SubTopologyInfo curST = topTopology.getSubtopology(provisionReqs.get(reqi).topologyName.trim());
				if(curST != null){
					logString += curST.topology + "::" + curST.status + "::" + curST.statusInfo + "::";
					for(int vi = 0 ; vi < curST.subTopology.getVMsinSubClass().size() ; vi++){
						VM curVM = curST.subTopology.getVMsinSubClass().get(vi);
						logString += curVM.name+"::"+curVM.publicAddress+"::";
					}
					logString += "||";
				}
			}
			recordOpLog(logString);
			return true;
		}else{
			logger.warn("Invalid 'SubjectType' for operation 'delete'!");
			return false;
		}
	}
	
	private boolean delete(){
		if(opInput.SubjectType == null){
			logger.error("Invalid operation without 'SubjectType': "+opInput.toString());
			return false;
		}
		if(opInput.SubjectType.trim().equalsIgnoreCase("subtopology")){
			String opSubjects = opInput.Subjects;
			if(opSubjects == null){
				logger.warn("Nothing to operate on!");
				return true;
			}
			if(opSubjects.trim().equalsIgnoreCase("_all")){
				logger.debug("Delete all sub-topologies!");
				TEngine tEngine = new TEngine();
				tEngine.deleteAll(topTopology, userCredential, userDatabase);
			}else{
				String [] opSubjectsList = opSubjects.split("\\|\\|");
				ArrayList<DeleteRequest> deleteReqs = new ArrayList<DeleteRequest>();
				for(int oi = 0 ; oi < opSubjectsList.length ; oi++){
					DeleteRequest dq = new DeleteRequest();
					dq.topologyName = opSubjectsList[oi];
					deleteReqs.add(dq);
					logger.debug("Delete sub-topology "+dq.topologyName);
				}
				
				TEngine tEngine = new TEngine();
				tEngine.delete(topTopology, userCredential, userDatabase, deleteReqs);
			}
			recordOpLog(null);
			return true;
		}else{
			logger.warn("Invalid 'SubjectType' for operation 'delete'!");
			return false;
		}
		
	}
	
	
	private boolean executeCMD(){
		String logString = "";
		if(opInput.SubjectType == null){
			logString = "Invalid operation without 'SubjectType': "+opInput.toString();
			logger.warn(logString);
			recordOpLog("WARN: "+logString);
			return false;
		}
		if(opInput.Command == null || opInput.Command.trim().equals("")){
			logString = "Invalid operation without defining command";
			logger.warn(logString);
			recordOpLog("WARN: "+logString);
			return false;
		}
		if(opInput.SubjectType.trim().equalsIgnoreCase("vm")){
			if(opInput.Subjects == null){
				logString = "Invalid operation without 'Subjects'!";
				logger.warn(logString);
				recordOpLog("WARN: "+logString);
				return false;
			}
			String [] opSubjectsList = opInput.Subjects.split("\\|\\|");
			ExecutorService executor4CMD = Executors.newFixedThreadPool(opSubjectsList.length);
			ArrayList<ParallelExecutor> PEs = new ArrayList<ParallelExecutor>();
			boolean success = true;
			for(int oi = 0 ; oi < opSubjectsList.length ; oi++){
				String subVMName = opSubjectsList[oi];
				if(subVMName.trim().equals(""))
					continue;
				if(!subVMName.contains(".")){
					String thisLog = "Invalid 'Subject' named " + subVMName;
					logString += ("WARN: "+ thisLog + "||");
					logger.warn(thisLog);
					success = false;
					continue;
				}
				String [] names = subVMName.split("\\.");
				SubTopologyInfo curSubInfo = topTopology.getSubtopology(names[0]);
				if(curSubInfo == null){
					String thisLog = "There is no 'SubTopology' named "+names[0];
					logString += ("WARN: "+ thisLog +"||");
					logger.warn(thisLog);
					success = false;
					continue;
				}
				if(!curSubInfo.status.trim().equalsIgnoreCase("running")){
					String thisLog = "The subject SubTopology "+names[0]+" must be running first!";
					logString += ("WARN: "+ thisLog +"||");
					logger.warn(thisLog);
					success = false;
					continue;
				}
				VM curVM = curSubInfo.subTopology.getVMinSubClassbyName(names[1]);
				if(curVM == null){
					String thisLog = "There is no 'VM' named "+names[1];
					logString += ("WARN: "+ thisLog +"||");
					logger.warn(thisLog);
					success = false;
					continue;
				}
				String defaultSSHPrivateKey = curSubInfo.subTopology.accessKeyPair.privateKeyString;
				ParallelExecutor PE = new ParallelExecutor(curVM.defaultSSHAccount, 
						curVM.publicAddress, defaultSSHPrivateKey, opInput.Command);
				PEs.add(PE);
				executor4CMD.execute(PE);
			}
			
			executor4CMD.shutdown();
			try {
				while (!executor4CMD.awaitTermination(2, TimeUnit.SECONDS)){
					Thread.sleep(1000);
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
				logger.error("Unexpected error for threads!");
				return false;
			}
			
			for(int pi = 0 ; pi<PEs.size() ; pi++)
				logString += (PEs.get(pi).exeResult+"||");
			
			
			///if the 'Log' option is not set, then the output will be logged by default
			if(opInput.Log != null && opInput.Log.trim().equalsIgnoreCase("OFF"))
				return success;
			else{
				recordOpLog(logString);
				return success;
			}
		}else{
			logger.warn("Invalid 'SubjectType' for operation 'execute'!");
			return false;
		}
	}
	
	private void recordOpLog(String logString){
		Logs logs = new Logs();
		Log log = new Log();
		log.Event = this.opInput;
		
		log.LOG = logString;
		log.Time = String.valueOf(System.currentTimeMillis());
		logs.LOGs = new ArrayList<Log>();
		logs.LOGs.add(log);
		
		ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
		try {
	    		String yamlString = mapper.writeValueAsString(logs);
	    		int startI = yamlString.indexOf("LOGs:");
	    		String trimString = yamlString.substring(startI);
	    		int headI = trimString.indexOf("\n");
	    		String finalString = trimString.substring(headI+1);
	    		String formatString = CommonTool.formatString(finalString);
	    		this.opLogger.write(formatString + "\n");
	    		this.opLogger.flush();
		
		} catch (IOException e) {
			e.printStackTrace();
			logger.error(e.getMessage());
		}
	}
}
