package provisioning.engine.VEngine;

import com.jcabi.ssh.SSH;
import com.jcabi.ssh.Shell;


public abstract class VEngine {

	 /** 
	 * Test if a host is alive
	 * @param host ip and private key content
	 * @return true if it's alive
	 */
	public static boolean isAlive(String host, int port, String privateKeyString, String sshAccount){
		if(host == null)
			return false;
		  boolean alive=false;
		  try {
			    String cmd="echo " + host;
			    Shell shell=new SSH(host, 22, sshAccount, privateKeyString);
			    new Shell.Plain(shell).exec(cmd);
			    alive=true;
		  }catch ( Exception e) {
			  
		  }
		 return alive;
	}
	
}
