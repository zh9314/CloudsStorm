package provisioning.engine.VEngine.EGI;

import org.apache.log4j.Logger;

import topologyAnalysis.dataStructure.EGI.EGIVM;

import com.jcabi.ssh.SSH;
import com.jcabi.ssh.Shell;

public class EGIVEngine_startVM extends EGIVEngine implements Runnable{
	
private static final Logger logger = Logger.getLogger(EGIVEngine_startVM.class);
	
	
	public EGIVEngine_startVM(EGIAgent egiAgent, EGIVM curVM, 
			String privateKeyString){
		this.egiAgent = egiAgent;
		this.curVM = curVM;
		this.privateKeyString = privateKeyString;
	}
	
	/** 
	 * Test if a host is alive
	 * @param host ip and private key content
	 * @return true if it's alive
	 */
	private boolean isAlive(String host, String privateKeyString, String defaultSSHAccount){
	  boolean alive=false;
	  try {
		    String cmd="echo " + host;
		    Shell shell=new SSH(host, 22, defaultSSHAccount, privateKeyString);
		    new Shell.Plain(shell).exec(cmd);
		    alive=true;
	  }
	  
	 catch ( Exception e) {
	  }
	  
	  
	  return alive;
	}
		
	@Override
	public void run() {
		if(curVM.VMResourceID == null){
			logger.error("VM '"+curVM.name+"' doesn't have valid reource location!");
			return;
		}
		if(curVM.publicAddress == null){
			logger.error("There is no public address for "+curVM.name);
			return;
		}
		int rtnum = 0;
		boolean success = false;
        while(!success){
        		success = egiAgent.startVM(curVM.VMResourceID);
        		logger.info("Start the VM!");
        		if(rtnum > egiAgent.retryTimes)
        			break;
        		rtnum++;
        }
		if(!success){
			logger.error("VM '"+curVM.name+"' cannot be started!");
			return ;
		}
		
		logger.debug("VM '"+curVM.name+"' with address ("+curVM.publicAddress+") is started!");
		
		//Wait for 5min (300s) for maximum.
		long sshStartTime = System.currentTimeMillis();
		long sshEndTime = System.currentTimeMillis();
		while((sshEndTime - sshStartTime) < 300000){
			if(isAlive(curVM.publicAddress, privateKeyString, curVM.defaultSSHAccount)){
				logger.info(curVM.name+" ("+curVM.publicAddress+") can be accessed!");
				return ;
			}
		}
		
		logger.error("VM '"+curVM.name+"' cannot be accessed!");
		
	}
	
	
	
}
