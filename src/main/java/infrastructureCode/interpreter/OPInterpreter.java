package infrastructureCode.interpreter;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
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
import topology.description.actual.SubTopologyInfo;
import topology.description.actual.TopTopology;
import topology.description.actual.VM;
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
		else if(opInput.Operation.trim().equalsIgnoreCase("execute")
				|| opInput.Operation.trim().equalsIgnoreCase("put")    ////upload some file from a VM
				|| opInput.Operation.trim().equalsIgnoreCase("get"))   ////download some file from a VM
			success = opOnVMs();


		return success;
	}
	
	private boolean provision(){
		boolean opResult = true;
		long opStart = System.currentTimeMillis();
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
			ProvisionRequest provisionReq = new ProvisionRequest();
			if(opSubjects.trim().equalsIgnoreCase("_all")){
				logger.debug("Provision all sub-topologies!");
				for(int si = 0 ; si<topTopology.topologies.size() ; si++){
					if(topTopology.topologies.get(si).topology.equalsIgnoreCase("_ctrl"))
						continue;
					if(topTopology.topologies.get(si).status.equals("fresh")
						||topTopology.topologies.get(si).status.equals("stopped")
						||topTopology.topologies.get(si).status.equals("deleted")){
						provisionReq.content.put(topTopology.topologies.get(si).topology, false);
					}
				}
				opResult = tEngine.provision(topTopology, userCredential, userDatabase, provisionReq);
			}else{
				String [] opSubjectsList = opSubjects.split("\\|\\|");
				for(int oi = 0 ; oi < opSubjectsList.length ; oi++){
					provisionReq.content.put(opSubjectsList[oi], false);
					logger.debug("Provision sub-topology "+opSubjectsList[oi]);
				}
				opResult = tEngine.provision(topTopology, userCredential, userDatabase, provisionReq);
			}
			
			Map<String, String> logsInfo = new HashMap<String, String>();
			for(Map.Entry<String, Boolean> entryReq: provisionReq.content.entrySet()){
				SubTopologyInfo curST = topTopology
											.getSubtopology(entryReq.getKey().trim());
				if(curST != null && curST.logsInfo != null){
					for(Map.Entry<String, String> entry: curST.logsInfo.entrySet())
						logsInfo.put(entry.getKey(), entry.getValue());
				
					for(int vi = 0 ; vi < curST.subTopology.getVMsinSubClass().size() ; vi++){
						VM curVM = curST.subTopology.getVMsinSubClass().get(vi);
						logsInfo.put(curVM.name+"#pubIP", curVM.publicAddress); 
					
					}
				}
			}
			long opEnd = System.currentTimeMillis();
			recordOpLog(logsInfo, (int)((opEnd-opStart)/1000));
			if(!opResult)
				return false;
			return true;
		}else{
			logger.warn("Invalid 'SubjectType' for operation 'delete'!");
			return false;
		}
	}
	
	private boolean delete(){
		boolean opResult = true;
		long opStart = System.currentTimeMillis();
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
				opResult = tEngine.deleteAll(topTopology, userCredential, userDatabase);
			}else{
				String [] opSubjectsList = opSubjects.split("\\|\\|");
				DeleteRequest deleteReq = new DeleteRequest();
				for(int oi = 0 ; oi < opSubjectsList.length ; oi++){
					deleteReq.content.put(opSubjectsList[oi], false);
					logger.debug("Delete sub-topology "+opSubjectsList[oi]);
				}
				
				TEngine tEngine = new TEngine();
				opResult = tEngine.delete(topTopology, userCredential, userDatabase, deleteReq);
			}
			long opEnd = System.currentTimeMillis();
			recordOpLog(null, (int)((opEnd-opStart)/1000));
			if(!opResult)
				return false;
			return true;
		}else{
			logger.warn("Invalid 'SubjectType' for operation 'delete'!");
			return false;
		}
		
	}
	
	
	private boolean opOnVMs(){
		String logString = "";
		long opStart = System.currentTimeMillis();
		if(opInput.SubjectType == null){
			logString = "Invalid operation without 'SubjectType': "+opInput.toString();
			logger.warn(logString);
			Map<String,String> logsInfo = new HashMap<String,String>();
			logsInfo.put("WARN", logString);
			recordOpLog(logsInfo, 0);
			return false;
		}
		if(opInput.Command == null || opInput.Command.trim().equals("")){
			logString = "Invalid operation without defining command";
			logger.warn(logString);
			Map<String,String> logsInfo = new HashMap<String,String>();
			logsInfo.put("WARN", logString);
			recordOpLog(logsInfo, 0);
			return false;
		}
		if(opInput.SubjectType.trim().equalsIgnoreCase("vm")){
			if(opInput.Subjects == null){
				logString = "Invalid operation without 'Subjects'!";
				logger.warn(logString);
				Map<String,String> logsInfo = new HashMap<String,String>();
				logsInfo.put("WARN", logString);
				recordOpLog(logsInfo, 0);
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
						curVM.publicAddress, defaultSSHPrivateKey, opInput.Operation, opInput.Command);
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
			long opEnd = System.currentTimeMillis();
			if(opInput.Log != null && opInput.Log.trim().equalsIgnoreCase("OFF")){
				recordOpLog(null, (int)((opEnd-opStart)/1000));
				return success;
			}
			else{
				Map<String,String> logsInfo = new HashMap<String,String>();
				logsInfo.put("WARN", logString);
				recordOpLog(logsInfo, (int)((opEnd-opStart)/1000));
				return success;
			}
		}else{
			logger.warn("Invalid 'SubjectType' for operation 'execute'!");
			return false;
		}
	}
	
	
	private void recordOpLog(Map<String, String> logsInfo, int opOverhead){
		Logs logs = new Logs();
		Log log = new Log();
		log.Event = this.opInput;
		
		log.LOG = logsInfo;
		log.Time = String.valueOf(System.currentTimeMillis());
		log.Overhead = String.valueOf(opOverhead);
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
