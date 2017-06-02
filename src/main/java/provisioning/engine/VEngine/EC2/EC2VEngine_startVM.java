package provisioning.engine.VEngine.EC2;

import org.apache.log4j.Logger;

import topologyAnalysis.dataStructure.EC2.EC2VM;

import com.jcabi.ssh.SSH;
import com.jcabi.ssh.Shell;

public class EC2VEngine_startVM extends EC2VEngine implements Runnable{
	
	private static final Logger logger = Logger.getLogger(EC2VEngine_startVM.class);
	
	
	public EC2VEngine_startVM(EC2Agent ec2agent, EC2VM curVM, 
			String privateKeyString){
		this.ec2agent = ec2agent;
		this.curVM = curVM;
		this.privateKeyString = privateKeyString;
	}
	
	/** 
	 * Test if a host is alive
	 * @param host ip and private key content
	 * @return true if it's alive
	 */
	private boolean isAlive(String host, String privateKeyString, String sshAccount){
	  boolean alive=false;
	  try {
		    String cmd="echo " + host;
		    Shell shell=new SSH(host, 22, sshAccount, privateKeyString);
		    new Shell.Plain(shell).exec(cmd);
		    alive=true;
	  }
	 catch ( Exception e) {
		 
	  }
	  
	  return alive;
	}
		
	@Override
	public void run() {
		String instanceId = curVM.instanceId;
		ec2agent.startInstance(instanceId);
		
		String publicAddress = ec2agent.getPublicAddress(instanceId);
		if(publicAddress == null){
			logger.error("Cannot get public address from '"+instanceId+"' of '"+curVM.name+"'");
			return ;
		}
		logger.info("Get '"+instanceId+"' <-> "+publicAddress);
		
		//Wait for 5min (300s) for maximum.
		long sshStartTime = System.currentTimeMillis();
		long sshEndTime = System.currentTimeMillis();
		while((sshEndTime - sshStartTime) < 300000){
			if(isAlive(publicAddress, privateKeyString, curVM.defaultSSHAccount)){
				curVM.publicAddress = publicAddress;
				logger.info(curVM.name+" ("+publicAddress+") is activated!");
				
				return ;
			}
		}
		
		curVM.publicAddress = null;
		
	}
	
	

}
