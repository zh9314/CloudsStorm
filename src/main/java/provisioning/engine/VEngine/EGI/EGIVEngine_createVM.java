package provisioning.engine.VEngine.EGI;

import java.net.URI;

import org.apache.log4j.Logger;

import com.jcabi.ssh.SSH;
import com.jcabi.ssh.Shell;

import topologyAnalysis.dataStructure.EGI.EGIVM;



public class EGIVEngine_createVM extends EGIVEngine implements Runnable{

	private static final Logger logger = Logger.getLogger(EGIVEngine_createVM.class);
	
	private String publicKeyString;
	private String publicKeyId;
	
	public EGIVEngine_createVM(EGIAgent egiAgent, EGIVM curVM, 
			String publicKeyId, String publicKeyString, String privateKeyString){
		this.egiAgent = egiAgent;
		this.curVM = curVM;
		this.publicKeyString = publicKeyString;
		this.publicKeyId = publicKeyId;
		this.privateKeyString = privateKeyString;
	}
	
	
	@Override
	public void run() {
		URI createURI = egiAgent.createComputeVM(curVM.name, publicKeyString, publicKeyId,
				curVM.OS_occi_ID, curVM.Res_occi_ID);
		if(createURI == null){
			logger.error("VM '"+curVM.name+"' cannot be created!");
			return ;
		}
		curVM.VMResourceID = createURI.toString();
		
		long stateStartTime = System.currentTimeMillis();
		long stateEndTime = System.currentTimeMillis();
		while((stateEndTime - stateStartTime) < 300000){
			String vmStatus = egiAgent.getVMStatus(curVM.VMResourceID);
			if(vmStatus.equals("active")){
                                logger.info("VM "+curVM.VMResourceID+" is active!");
				break;
			}
			stateEndTime = System.currentTimeMillis();
		}
		stateEndTime = System.currentTimeMillis();
		logger.info("Activation time for VM '"+curVM.name+"' is "+(stateEndTime-stateStartTime) + " ms");
		
		boolean addressGot = false;
		String publicAddress = egiAgent.getPubAddress(curVM.VMResourceID);
		if(publicAddress == null){
			logger.info("VM '"+curVM.name+"' cannot be assigned a public address automatically!");
			////attach a public address to this VM
			URI pubNetwork ;
			if((pubNetwork = egiAgent.getPublicNetworkURI()) == null){
				logger.warn("VM '"+curVM.name+"' cannot be assigned a public address!");
				return;
			}
			egiAgent.attachPublicNetwork(curVM.VMResourceID, pubNetwork.toString());
		}else if(!publicAddress.equals("")){
			addressGot = true;
		}else  {
		}
		
		if(!addressGot){
			//Wait for 2min (120s) for maximum.
			long getAddressStartTime = System.currentTimeMillis();
			long getAddressEndTime = System.currentTimeMillis();
			while((getAddressEndTime - getAddressStartTime) < 120000){
				publicAddress = egiAgent.getPubAddress(curVM.VMResourceID);
				if(publicAddress == null)
					continue;
				if(!publicAddress.equals("")){
					break;
				}
			}
			getAddressEndTime = System.currentTimeMillis();
		}
		if(publicAddress == null){
			logger.error("Public address of VM '"+curVM.name+"' cannot be assigned!");
			return;
		}
		
		//Wait for 5min (300s) for maximum.
		long sshStartTime = System.currentTimeMillis();
		long sshEndTime = System.currentTimeMillis();
		while((sshEndTime - sshStartTime) < 300000){
			if(isAlive(publicAddress, privateKeyString, curVM.defaultSSHAccount)){
				logger.info(curVM.name+" ("+publicAddress+") is activated!");
				logger.info("Total activation time for '"+curVM.name+"' is "+(sshEndTime - sshStartTime)+" ms");
				curVM.publicAddress = publicAddress;
				return ;
			}
			sshEndTime = System.currentTimeMillis();
		}
		logger.info(curVM.name+" ("+publicAddress+") is not activated!");
		
	}
	
	
	/** 
	 * Test if a host is alive
	 * @param host ip and private key content
	 * @return true if it's alive
	 */
	private boolean isAlive(String host, String privateKeyString, String SSHAccount){
	  boolean alive=false;
	  try {
		    String cmd="echo " + host;
		    Shell shell=new SSH(host, 22, SSHAccount, privateKeyString);
		    new Shell.Plain(shell).exec(cmd);
		    alive=true;
	  }
	 catch ( Exception e) {
	  }
	  return alive;
	}

}
