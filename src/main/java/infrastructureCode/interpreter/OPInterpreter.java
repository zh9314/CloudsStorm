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

import commonTool.ClassSet;
import commonTool.CommonTool;
import commonTool.Values;
import provisioning.credential.UserCredential;
import provisioning.database.UserDatabase;
import provisioning.engine.TEngine.TEngine;
import provisioning.request.DeleteRequest;
import provisioning.request.HScalingSTRequest;
import provisioning.request.HScalingSTRequest.STScalingReqEle;
import provisioning.request.HScalingVMRequest;
import provisioning.request.HScalingVMRequest.VMScalingReqEle;
import provisioning.request.ProvisionRequest;
import provisioning.request.VScalingVMRequest;
import provisioning.request.VScalingVMRequest.VMVScalingReqEle;
import topology.description.actual.SubTopologyInfo;
import topology.description.actual.TopTopology;
import topology.description.actual.VM;
import infrastructureCode.log.Log;
import infrastructureCode.log.Logs;
import infrastructureCode.main.ICYAML;
import infrastructureCode.main.Operation;

public class OPInterpreter {
	private static final Logger logger = Logger.getLogger(OPInterpreter.class);
	
	private Operation opInput;
	private TopTopology topTopology;
	private UserCredential userCredential;
	private UserDatabase userDatabase;
	private FileWriter opLogger;
	private ICYAML ic;
	
	public OPInterpreter(Operation input, ICYAML ic){
		this.opInput = input;
		this.topTopology = ic.topTopology;
		this.userCredential = ic.userCredential;
		this.userDatabase = ic.userDatabase;
		this.opLogger = ic.icLogger;
		this.ic = ic;
	}
	
	public boolean execute(){
		if(opInput == null || opInput.Operation == null){
			logger.error("The input operation cannot be null!");
			return false;
		}
		boolean success = false;
		if(opInput.Operation.trim().equalsIgnoreCase("provision"))
			success = provision();
		else if(opInput.Operation.trim().equalsIgnoreCase("hscale"))
			success = hscale();
		else if(opInput.Operation.trim().equalsIgnoreCase("vscale"))
			success = vscale();
		else if(opInput.Operation.trim().equalsIgnoreCase("delete"))
			success = delete();
		else if(opInput.Operation.trim().equalsIgnoreCase("execute")
				|| opInput.Operation.trim().equalsIgnoreCase("put")    ////upload some file from a VM
				|| opInput.Operation.trim().equalsIgnoreCase("get"))   ////download some file from a VM
			success = opOnVMs();
		else if(opInput.Operation.trim().equalsIgnoreCase("sleep"))
			success = opSys();

		return success;
	}
	
	/**
	 * These are some system operations.
	 * @return
	 */
	private boolean opSys(){
		
		if(opInput.Operation.equalsIgnoreCase("sleep")){
			if(opInput.Command == null){
				logger.error("Invalid operation for 'sleep'!");
				return false;
			}
			long sleepStart = System.currentTimeMillis();
			int timeDura = -1;
			if(opInput.Command.endsWith("s")){
				String duration = opInput.Command.substring(0, opInput.Command.length()-1).trim();
				timeDura = Integer.valueOf(duration);
			}else if(opInput.Command.endsWith("m")){
				String duration = opInput.Command.substring(0, opInput.Command.length()-1).trim();
				timeDura = Integer.valueOf(duration)*60;
			}else if(opInput.Command.endsWith("h")){
				String duration = opInput.Command.substring(0, opInput.Command.length()-1).trim();
				timeDura = Integer.valueOf(duration)*60*60;
			}else{
				logger.error("Invalid time duration for 'sleep'!");
				return false;
			}
			int sleepInterval = 500;
			if(timeDura > 100)
				sleepInterval = 1000;
			if(timeDura > 1000)
				sleepInterval = 10*1000;
			
			while(true){
				long curTime = System.currentTimeMillis();
				int timeSecs = (int)(curTime - sleepStart)/1000;
				if( timeSecs > timeDura)
					return true;
				try {
					Thread.sleep(sleepInterval);
				} catch (InterruptedException e) {
				}
			}
		}
		return false;
	}
	
	private boolean provision(){
		boolean opResult = true;
		long opStart = System.currentTimeMillis();
		if(opInput.ObjectType == null){
			logger.error("Invalid operation without 'ObjectType': "+opInput.toString());
			return false;
		}
		if(opInput.ObjectType.trim().equalsIgnoreCase("subtopology")){
			String opObjects = opInput.Objects;
			if(opObjects == null){
				logger.warn("Nothing to operate on!");
				return true;
			}
			TEngine tEngine = new TEngine();
			ProvisionRequest provisionReq = new ProvisionRequest();
			if(opObjects.trim().equalsIgnoreCase("_all")){
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
				String [] opObjectsList = opObjects.split("\\|\\|");
				for(int oi = 0 ; oi < opObjectsList.length ; oi++){
					provisionReq.content.put(opObjectsList[oi], false);
					logger.debug("Provision sub-topology "+opObjectsList[oi]);
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
			recordOpLog(logsInfo, (int)((opEnd-opStart)));
			if(!opResult)
				return false;
			return true;
		}else{
			logger.warn("Invalid 'ObjectType' for operation 'provision'!");
			return false;
		}
	}
	
	private boolean delete(){
		boolean opResult = true;
		long opStart = System.currentTimeMillis();
		if(opInput.ObjectType == null){
			logger.error("Invalid operation without 'ObjectType': "+opInput.toString());
			return false;
		}
		if(opInput.ObjectType.trim().equalsIgnoreCase("subtopology")){
			String opObjects = opInput.Objects;
			if(opObjects == null){
				logger.warn("Nothing to operate on!");
				return true;
			}
			if(opObjects.trim().equalsIgnoreCase("_all")){
				logger.debug("Delete all sub-topologies!");
				TEngine tEngine = new TEngine();
				opResult = tEngine.deleteAll(topTopology, userCredential, userDatabase);
			}else{
				String [] opObjectsList = opObjects.split("\\|\\|");
				DeleteRequest deleteReq = new DeleteRequest();
				for(int oi = 0 ; oi < opObjectsList.length ; oi++){
					deleteReq.content.put(opObjectsList[oi], false);
					logger.debug("Delete sub-topology "+opObjectsList[oi]);
				}
				
				TEngine tEngine = new TEngine();
				opResult = tEngine.delete(topTopology, userCredential, userDatabase, deleteReq);
			}
			long opEnd = System.currentTimeMillis();
			recordOpLog(null, (int)((opEnd-opStart)));
			if(!opResult)
				return false;
			return true;
		}else{
			logger.warn("Invalid 'ObjectType' for operation 'delete'!");
			return false;
		}
		
	}
	
	private boolean hscale(){
		long opStart = System.currentTimeMillis();
		Map<String,String> logsInfo = new HashMap<String,String>();
		if(opInput.ObjectType == null){
			logger.error("Invalid operation without 'ObjectType': "+opInput.toString());
			logsInfo.put("WARN", "Invalid operation without 'ObjectType'");
			recordOpLog(logsInfo, 0);
			return false;
		}
		Map<String, String> actualOptions = null;
		if(opInput.Options != null){
			actualOptions = new HashMap<String, String>();
			for(Map.Entry<String, String> entry: opInput.Options.entrySet()){
				String orgOptionString = entry.getValue();
				if(orgOptionString != null){
					String newOptionString = orgOptionString.replaceAll("\\$counter", 
							String.valueOf(opInput.loopCounter))
							.replaceAll("\\$time", String.valueOf(System.currentTimeMillis()));
					actualOptions.put(entry.getKey(), newOptionString);
				}
			}
		}
		
		String opObjects = opInput.Objects;
		if(opObjects == null){
			logger.warn("Nothing to operate on!");
			logsInfo.put("WARN", "Nothing to operate on!");
			recordOpLog(logsInfo, 0);
			return true;
		}
		
		if(opInput.ObjectType.trim().equalsIgnoreCase("subtopology")){
			String upDown = actualOptions.get(Values.Options.scalingUpDown);
			if(upDown == null){
				logger.error("Scaling up or down must be specified for "+opInput.Operation);
				logsInfo.put("ERROR", "Scaling up or down must be specified");
				recordOpLog(logsInfo, 0);
				return false;
			}
			boolean scalingUpDown = false;
			if(upDown.trim().equalsIgnoreCase("up"))
				scalingUpDown = true;
			else if(upDown.trim().equalsIgnoreCase("down"))
				scalingUpDown = false;
			else {
				logger.error("Scaling up or down must be specified for "+opInput.Operation);
				logsInfo.put("ERROR", "Scaling up or down must be specified");
				recordOpLog(logsInfo, 0);
				return false;
			}
			
			String [] opObjectsList = opObjects.split("\\|\\|");
			for(int oi = 0 ; oi < opObjectsList.length ; oi++){
				STScalingReqEle reqEle = ic.hscalSTReq.new STScalingReqEle();
				reqEle.reqID = actualOptions.get(Values.Options.requstID);
				if(reqEle.reqID == null){
					logger.error("'ReqID' must be set in 'options' of "+opInput.Operation);
					logsInfo.put("ERROR", "'ReqID' must be set in 'options'");
					recordOpLog(logsInfo, 0);
					return false;
				}else
					reqEle.reqID = reqEle.reqID.trim();
				reqEle.cloudProvider = actualOptions.get(Values.Options.cloudProivder);
				reqEle.domain = actualOptions.get(Values.Options.domain);
				reqEle.scaledTopology = actualOptions.get(Values.Options.scaledTopology);
				reqEle.scaledClasses = new ClassSet();
				reqEle.scaledClasses.SubTopologyClass = actualOptions.get(Values.Options.subTopologyClass);
				reqEle.scaledClasses.SEngineClass = actualOptions.get(Values.Options.sEngineClass);
				reqEle.scaledClasses.VEngineClass = actualOptions.get(Values.Options.vEngineClass);
				reqEle.scalingUpDown = scalingUpDown;
				
				reqEle.targetTopology = opObjectsList[oi].trim();
				ic.hscalSTReq.content.put(reqEle, false);
				logger.debug("Generate a scaling request for sub-topology "+opObjectsList[oi]);
			}
			
			return true;
		}else if(opInput.ObjectType.trim().equalsIgnoreCase("vm")){
			String [] opObjectsList = opObjects.split("\\|\\|");
			VMScalingReqEle reqEle = ic.hscalVMReq.new VMScalingReqEle();
			reqEle.reqID = actualOptions.get(Values.Options.requstID);
			if(reqEle.reqID == null){
				logger.error("'ReqID' must be set in 'options' of "+opInput.Operation);
				logsInfo.put("ERROR", "'ReqID' must be set in 'options'");
				recordOpLog(logsInfo, 0);
				return false;
			}else
				reqEle.reqID = reqEle.reqID.trim();
			reqEle.cloudProvider = actualOptions.get(Values.Options.cloudProivder);
			reqEle.domain = actualOptions.get(Values.Options.domain);
			reqEle.scaledTopology = actualOptions.get(Values.Options.scaledTopology);
			reqEle.scaledClasses = new ClassSet();
			reqEle.scaledClasses.SubTopologyClass = actualOptions.get(Values.Options.subTopologyClass);
			reqEle.scaledClasses.SEngineClass = actualOptions.get(Values.Options.sEngineClass);
			reqEle.scaledClasses.VEngineClass = actualOptions.get(Values.Options.vEngineClass);
			
			for(int oi = 0 ; oi < opObjectsList.length ; oi++){
				String objVMName = opObjectsList[oi];
				if(objVMName.trim().equals(""))
					continue;
				if(!objVMName.contains(".")){
					String thisLog = "Invalid 'Object' named " + objVMName;
					logger.error(thisLog);
					logsInfo.put("ERROR", thisLog);
					recordOpLog(logsInfo, 0);
					return false;
				}
				String [] names = objVMName.split("\\.");
				SubTopologyInfo curSubInfo = topTopology.getSubtopology(names[0]);
				if(curSubInfo == null){
					String thisLog = "There is no 'SubTopology' named "+names[0];
					logger.error(thisLog);
					logsInfo.put("ERROR", thisLog);
					recordOpLog(logsInfo, 0);
					return false;
				}
				String objVMabsName = names[1];
				VM curVM = topTopology.VMIndex.get(objVMabsName);
				if(curVM == null){
					String thisLog = "There is no 'VM' named "+names[1];
					logger.error(thisLog);
					logsInfo.put("ERROR", thisLog);
					recordOpLog(logsInfo, 0);
					return false;
				}
				
				reqEle.targetVMs.add(objVMabsName.trim());
				
				logger.debug("Generate a scaling request containing VM "+opObjectsList[oi]);
			}
			ic.hscalVMReq.content.put(reqEle, false);
			return true;
		}else if(opInput.ObjectType.trim().equalsIgnoreCase("req")){
			HScalingSTRequest tmpHscaleSTReqUP = new HScalingSTRequest();
			HScalingSTRequest tmpHscaleSTReqDOWN = new HScalingSTRequest();
			HScalingVMRequest tmpHscaleVMReq = new HScalingVMRequest();
			String [] opObjectsList = opObjects.split("\\|\\|");
			for(int oi = 0 ; oi < opObjectsList.length ; oi++){
				String reqID = opObjectsList[oi].trim();
				for(Map.Entry<STScalingReqEle, Boolean> entry: ic.hscalSTReq.content.entrySet()){
					String curReqID = entry.getKey().reqID;
					if(reqID.equals(curReqID)){
						if(entry.getKey().scalingUpDown)
							tmpHscaleSTReqUP.content.put(entry.getKey(), entry.getValue());
						else
							tmpHscaleSTReqDOWN.content.put(entry.getKey(), entry.getValue());
					}
				}
				for(Map.Entry<VMScalingReqEle, Boolean> entry: ic.hscalVMReq.content.entrySet()){
					String curReqID = entry.getKey().reqID;
					if(reqID.equals(curReqID))
						tmpHscaleVMReq.content.put(entry.getKey(), entry.getValue());
					
				}
			}
			TEngine tEngine = new TEngine();
			boolean validReq = false;
			long scalingStart = System.currentTimeMillis();
			if(tmpHscaleSTReqDOWN.content.size() != 0){
				validReq = true;
				if(!tEngine.horizontalScaleSLevel(topTopology, userCredential, 
											userDatabase, tmpHscaleSTReqDOWN, false)){
					logger.error("HScaling down in S-Level error!");
					logsInfo.put("ERROR", "HScaling down in S-Level error!");
					recordOpLog(logsInfo, 0);
					return false;
				}
			}
			if(tmpHscaleSTReqUP.content.size() != 0){
				validReq = true;
				if(!tEngine.horizontalScaleSLevel(topTopology, userCredential, 
											userDatabase, tmpHscaleSTReqUP, true)){
					logger.error("HScaling up in S-Level error!");
					logsInfo.put("ERROR", "HScaling up in S-Level error!");
					recordOpLog(logsInfo, 0);
					return false;
				}
			}
			if(tmpHscaleVMReq.content.size() != 0){
				validReq = true;
				if(!tEngine.horizontalScaleVLevel(topTopology, 
											userCredential, userDatabase, tmpHscaleVMReq)){
					logger.error("HScaling in V-Level error!");
					logsInfo.put("ERROR", "HScaling in V-Level error!");
					recordOpLog(logsInfo, 0);
					return false;
				}
			}
			
			if(!validReq){
				logsInfo.put("WARN", "No valid scaling request!");
				return false;
			}
			
			long opEnd = System.currentTimeMillis();
			logsInfo.put("Scaling", String.valueOf(opEnd - scalingStart));
			recordOpLog(logsInfo, (int)((opEnd-opStart)));
			
			////remove all the requests
			for(Map.Entry<VMScalingReqEle, Boolean> entry: tmpHscaleVMReq.content.entrySet()){
				if(!CommonTool.rmObjInMap(ic.hscalVMReq.content, entry.getKey())){
					logger.error("Unexpected! the request is not found!");
					return false;
				}
			}
			for(Map.Entry<STScalingReqEle, Boolean> entry: tmpHscaleSTReqUP.content.entrySet()){
				if(!CommonTool.rmObjInMap(ic.hscalSTReq.content, entry.getKey())){
					logger.error("Unexpected! the request is not found!");
					return false;
				}
			}
			for(Map.Entry<STScalingReqEle, Boolean> entry: tmpHscaleSTReqDOWN.content.entrySet()){
				if(!CommonTool.rmObjInMap(ic.hscalSTReq.content, entry.getKey())){
					logger.error("Unexpected! the request is not found!");
					return false;
				}
			}

			return true;
			
		}
		else{
			logger.warn("Invalid 'ObjectType' for operation 'hscale'!");
			return false;
		}
	}
	
	private boolean vscale(){
		long opStart = System.currentTimeMillis();
		Map<String,String> logsInfo = new HashMap<String,String>();
		if(opInput.ObjectType == null){
			logger.error("Invalid operation without 'ObjectType': "+opInput.toString());
			logsInfo.put("WARN", "Invalid operation without 'ObjectType'");
			recordOpLog(logsInfo, 0);
			return false;
		}
		Map<String, String> actualOptions = null;
		if(opInput.Options != null){
			actualOptions = new HashMap<String, String>();
			for(Map.Entry<String, String> entry: opInput.Options.entrySet()){
				String orgOptionString = entry.getValue();
				if(orgOptionString != null){
					String newOptionString = orgOptionString.replaceAll("\\$counter", 
							String.valueOf(opInput.loopCounter))
							.replaceAll("\\$time", String.valueOf(System.currentTimeMillis()));
					actualOptions.put(entry.getKey(), newOptionString);
				}
			}
		}
		
		String opObjects = opInput.Objects;
		if(opObjects == null){
			logger.warn("Nothing to operate on!");
			logsInfo.put("WARN", "Nothing to operate on!");
			recordOpLog(logsInfo, 0);
			return true;
		}
		
		if(opInput.ObjectType.trim().equalsIgnoreCase("vm")){
			String [] opObjectsList = opObjects.split("\\|\\|");
			for(int oi = 0 ; oi < opObjectsList.length ; oi++){
				String objVMName = opObjectsList[oi];
				if(objVMName.trim().equals(""))
					continue;
				if(!objVMName.contains(".")){
					String thisLog = "Invalid 'Object' named " + objVMName;
					logger.error(thisLog);
					logsInfo.put("ERROR", thisLog);
					recordOpLog(logsInfo, 0);
					return false;
				}
				String [] names = objVMName.split("\\.");
				SubTopologyInfo curSubInfo = topTopology.getSubtopology(names[0]);
				if(curSubInfo == null){
					String thisLog = "There is no 'SubTopology' named "+names[0];
					logger.error(thisLog);
					logsInfo.put("ERROR", thisLog);
					recordOpLog(logsInfo, 0);
					return false;
				}
				String objVMabsName = names[1];
				VM curVM = topTopology.VMIndex.get(objVMabsName);
				if(curVM == null){
					String thisLog = "There is no 'VM' named "+names[1];
					logger.error(thisLog);
					logsInfo.put("ERROR", thisLog);
					recordOpLog(logsInfo, 0);
					return false;
				}
				VMVScalingReqEle reqEle = ic.vscalVMReq.new VMVScalingReqEle();
				reqEle.reqID = actualOptions.get(Values.Options.requstID);
				if(reqEle.reqID == null){
					logger.error("'ReqID' must be set in 'options' of "+opInput.Operation);
					logsInfo.put("ERROR", "'ReqID' must be set in 'options'");
					recordOpLog(logsInfo, 0);
					return false;
				}else
					reqEle.reqID = reqEle.reqID.trim();
				reqEle.targetCPU = Double.valueOf(actualOptions.get(Values.Options.targetCPU));
				reqEle.targetMEM = Double.valueOf(actualOptions.get(Values.Options.targetMEM));
				reqEle.orgVMName = objVMabsName;
				reqEle.scaledClasses = new ClassSet();
				reqEle.scaledClasses.SubTopologyClass = actualOptions.get(Values.Options.subTopologyClass);
				reqEle.scaledClasses.SEngineClass = actualOptions.get(Values.Options.sEngineClass);
				reqEle.scaledClasses.VEngineClass = actualOptions.get(Values.Options.vEngineClass);
				
				ic.vscalVMReq.content.put(reqEle, false);
				logger.debug("Generate a vertical scaling request for VM "+opObjectsList[oi]);
			}
			
			return true;
		}else if(opInput.ObjectType.trim().equalsIgnoreCase("req")){
			VScalingVMRequest tmpVscaleVMReq = new VScalingVMRequest();
			String [] opObjectsList = opObjects.split("\\|\\|");
			for(int oi = 0 ; oi < opObjectsList.length ; oi++){
				String reqID = opObjectsList[oi].trim();
				
				for(Map.Entry<VMVScalingReqEle, Boolean> entry: ic.vscalVMReq.content.entrySet()){
					String curReqID = entry.getKey().reqID;
					if(reqID.equals(curReqID))
						tmpVscaleVMReq.content.put(entry.getKey(), entry.getValue());
						
				}
			}
			TEngine tEngine = new TEngine();
			boolean validReq = false;
			long scalingStart = System.currentTimeMillis();
			
			if(tmpVscaleVMReq.content.size() != 0){
				validReq = true;
				if(!tEngine.verticalScale(topTopology, userCredential, 
												userDatabase, tmpVscaleVMReq)){
					logger.error("VScaling in V-Level error!");
					logsInfo.put("ERROR", "VScaling in V-Level error!");
					recordOpLog(logsInfo, 0);
					return false;
				}
			}
			
			if(!validReq){
				logsInfo.put("WARN", "No valid scaling request!");
				return false;
			}
			
			long opEnd = System.currentTimeMillis();
			logsInfo.put("Scaling", String.valueOf(opEnd - scalingStart));
			recordOpLog(logsInfo, (int)((opEnd-opStart)));
			
			///remove the requests
			for(Map.Entry<VMVScalingReqEle, Boolean> entry: tmpVscaleVMReq.content.entrySet()){
				if(!CommonTool.rmObjInMap(ic.vscalVMReq.content, entry.getKey())){
					logger.error("Unexpected! the request is not found!");
					return false;
				}
			}

			return true;
			
		}
		else{
			logger.warn("Invalid 'ObjectType' for operation 'vscale'!");
			return false;
		}
		
	}
	
	
	private boolean opOnVMs(){
		String logString = "";
		long opStart = System.currentTimeMillis();
		if(opInput.ObjectType == null){
			logString = "Invalid operation without 'ObjectType': "+opInput.toString();
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
		
		////replace the command with some predefined synatx 
		////Do not replace the original string, in order to avoid 
		String curCMD = opInput.Command.replaceAll("\\$counter", 
							String.valueOf(opInput.loopCounter))
							.replaceAll("\\$time", String.valueOf(System.currentTimeMillis()));
		Map<String, String> actualOptions = null;
		if(opInput.Options != null){
			actualOptions = new HashMap<String, String>();
			for(Map.Entry<String, String> entry: opInput.Options.entrySet()){
				String orgOptionString = entry.getValue();
				if(orgOptionString != null){
					String newOptionString = orgOptionString.replaceAll("\\$counter", 
							String.valueOf(opInput.loopCounter))
							.replaceAll("\\$time", String.valueOf(System.currentTimeMillis()));
					actualOptions.put(entry.getKey(), newOptionString);
				}
			}
		}
		
		
		if(opInput.ObjectType.trim().equalsIgnoreCase("vm")){
			if(opInput.Objects == null){
				logString = "Invalid operation without 'Objects'!";
				logger.warn(logString);
				Map<String,String> logsInfo = new HashMap<String,String>();
				logsInfo.put("WARN", logString);
				recordOpLog(logsInfo, 0);
				return false;
			}
			String [] opObjectsList = opInput.Objects.split("\\|\\|");
			ExecutorService executor4CMD = Executors.newFixedThreadPool(opObjectsList.length);
			ArrayList<ParallelExecutor> PEs = new ArrayList<ParallelExecutor>();
			boolean multiOp = true;
			if(opObjectsList.length == 1)
				multiOp = false;
			boolean success = true;
			for(int oi = 0 ; oi < opObjectsList.length ; oi++){
				String objVMName = opObjectsList[oi];
				if(objVMName.trim().equals(""))
					continue;
				if(!objVMName.contains(".")){
					String thisLog = "Invalid 'Object' named " + objVMName;
					logString += ("WARN: "+ thisLog + "||");
					logger.warn(thisLog);
					success = false;
					continue;
				}
				String [] names = objVMName.split("\\.");
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
											curVM.publicAddress, defaultSSHPrivateKey, 
											opInput.Operation, curCMD, 
											actualOptions, objVMName, multiOp);
				PEs.add(PE);
				executor4CMD.execute(PE);
			}
			
			executor4CMD.shutdown();
			try {
				while (!executor4CMD.awaitTermination(2, TimeUnit.SECONDS)){
					Thread.sleep(1000);
				}
			} catch (InterruptedException e) {
				return false;
			}
			Map<String,String> logsInfo = new HashMap<String,String>();
			for(int pi = 0 ; pi<PEs.size() ; pi++){
				logsInfo.put(PEs.get(pi).objectName, PEs.get(pi).exeResult);
				if(!PEs.get(pi).exeState)
					success = false;
			}
			int tail = logString.lastIndexOf("||");
			if(tail != -1)
				logString = logString.substring(0, tail);
			logsInfo.put("MSG", logString);
			
			
			///if the 'Log' option is not set, then the output will be logged by default
			long opEnd = System.currentTimeMillis();
			if(opInput.Log != null && opInput.Log.trim().equalsIgnoreCase("OFF")){
				recordOpLog(null, (int)((opEnd-opStart)));
				return success;
			}
			else{
				recordOpLog(logsInfo, (int)((opEnd-opStart)));
				return success;
			}
		}else{
			logger.warn("Invalid 'ObjectType' for operation 'execute'!");
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
