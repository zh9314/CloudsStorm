package infrastructureCode.interpreter;

import infrastructureCode.main.ICYAML;
import infrastructureCode.main.LOOPCode;
import infrastructureCode.main.Operation;
import infrastructureCode.main.SEQCode;


import org.apache.log4j.Logger;

public class ICInterpreter {
	private static final Logger logger = Logger.getLogger(ICInterpreter.class);
	
	
	public void execute(ICYAML ic){
		if(ic == null || ic.InfrasCodes == null || ic.topTopology == null){
			logger.error("There is no infrastructure codes!");
			return;
		}
		if(ic.userCredential == null){
			logger.error("There is no available credential for infrastructure codes!");
			return;
		}
		if(ic.userDatabase == null){
			logger.error("There is no available database information for infrastructure codes!");
			return;
		}
		int codeLine = 0;
		for(int ici = 0 ; ici < ic.InfrasCodes.size() ; ici++){
			////Sequence code
			if(ic.InfrasCodes.get(ici).CodeType.equals("SEQ")){
				SEQCode curLine = ((SEQCode)ic.InfrasCodes.get(ici));
				OPInterpreter opInterpreter = new OPInterpreter(curLine.OpCode,
						ic.topTopology, ic.userCredential, ic.userDatabase,
						ic.icLogger);
				codeLine++;
				if(!opInterpreter.execute()){
					logger.error("Error of Operation "+curLine.OpCode.Operation+" at Code "+codeLine+ "! Exit!");
					return ;
				}
			}
			///Codes in loop
			if(ic.InfrasCodes.get(ici).CodeType.equals("LOOP")){
				LOOPCode curLine = ((LOOPCode)ic.InfrasCodes.get(ici));
				int loopCount = Integer.MAX_VALUE;
				if(curLine.Count != null)
					loopCount = Integer.valueOf(curLine.Count);
				int duration = Integer.MAX_VALUE; ////maximum duration time for the loop. The unit is second.
				if(curLine.Duration != null)
					duration = Integer.valueOf(curLine.Duration);
				long deadline = Long.MAX_VALUE;
				if(curLine.Deadline != null)
					deadline = Long.valueOf(curLine.Deadline);
				
				int loopCounter = 0;
				long curTime = System.currentTimeMillis();
				int opCounter = 0;
				int opCodeNum = curLine.OpCodes.size();
				while(true){
					if(loopCounter >= loopCount)
						break;
					long curLoopTime = System.currentTimeMillis();
					long curDuration = (curLoopTime - curTime)/1000;
					if(curLoopTime >= deadline)
						break;
					if(curDuration >= duration)
						break;
					
					if(loopCounter == 0)
						codeLine++;
					Operation curOp = curLine.OpCodes.get(opCounter);
					curOp.loopCounter = loopCounter;
					OPInterpreter opInterpreter = new OPInterpreter(curOp,
							ic.topTopology, ic.userCredential, ic.userDatabase,
							ic.icLogger);
					if(!opInterpreter.execute()){
						logger.error("Error of Operation "+curOp.Operation
										+" at Code "+codeLine+ "! Exit!");
						return ;
					}
					
					opCounter = (opCounter+1)%opCodeNum;
					if(opCounter == 0)
						loopCounter++;
				}
					
				
			}
		}
		
	}
}
