package provisioning.engine.VEngine.EC2;


import org.apache.log4j.Logger;

import com.jcabi.ssh.SSH;
import com.jcabi.ssh.Shell;

import topologyAnalysis.dataStructure.EC2.EC2VM;

public class EC2VEngine_createVM extends EC2VEngine implements Runnable{
	
	private static final Logger logger = Logger.getLogger(EC2VEngine_createVM.class);
	
	private String publicKeyId;
	//private String privateKeyString;
	
	public EC2VEngine_createVM(EC2Agent ec2agent, EC2VM curVM, 
			String publicKeyId, String privateKeyString){
		this.ec2agent = ec2agent;
		this.curVM = curVM;
		this.publicKeyId = publicKeyId;
		this.privateKeyString = privateKeyString;
	}
		
	@Override
	public void run() {
		String instanceId = ec2agent.runInstance(curVM.subnetId, curVM.securityGroupId, curVM.AMI,
				curVM.actualPrivateAddress, curVM.nodeType.trim().toLowerCase(), publicKeyId);
		if(instanceId == null){
			logger.error("Cannot run instance for "+curVM.name);
			return ;
		}
		curVM.instanceId = instanceId;
		boolean attachNeeded = false;
		if((Integer.valueOf(curVM.diskSize) - 8) > 0){
			String volumeId = ec2agent.createVolume(
					Integer.valueOf(curVM.diskSize), 
					Integer.valueOf(curVM.IOPS),
					curVM.subnetId);
			if(volumeId == null){
				logger.error("Cannot create volume for "+curVM.name);
				return ;
			}
			curVM.volumeId = volumeId;
			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			attachNeeded = true;
		}
		
		
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
			if(isAlive(publicAddress, privateKeyString)){
				curVM.publicAddress = publicAddress;
				logger.info(curVM.name+" ("+publicAddress+") is activated!");
				
				if(attachNeeded){
					ec2agent.attachVolume(curVM.volumeId, instanceId);
					logger.debug("Volume '"+curVM.volumeId+"' is attached!");
				}
				return ;
			}
		}
		
		curVM.publicAddress = null;
		
	}
	
	/** 
	 * Test if a host is alive
	 * @param host ip and private key content
	 * @return true if it's alive
	 */
	private boolean isAlive(String host, String privateKeyString){
	  boolean alive=false;
	  try {
		    String cmd="echo " + host;
		    Shell shell=new SSH(host, 22, "root", privateKeyString);
		    new Shell.Plain(shell).exec(cmd);
		    alive=true;
	  }
	 catch ( Exception e) {
	  }
	  return alive;
	}

}
