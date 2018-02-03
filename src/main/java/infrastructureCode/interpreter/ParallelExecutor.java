package infrastructureCode.interpreter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import com.jcabi.ssh.SSH;
import com.jcabi.ssh.Shell;

public class ParallelExecutor implements Runnable {

	public String exeResult;
	
	private String privateKeyString;
	private String pubIP;
	private String user;
	private String exeCMD;
	
	ParallelExecutor(String user, String pubIP, String priKey, String exeCMD){
		this.user = user;
		this.pubIP = pubIP;
		this.privateKeyString = priKey;
		this.exeCMD = exeCMD;
		exeResult = " ";
	}
	
	@Override
	public void run() {
		try {
			ByteArrayOutputStream cmdOutputBytes = new ByteArrayOutputStream();
			String cmd = "sudo " + exeCMD;
			Shell shell = new SSH(pubIP, 22, user, privateKeyString);
			new Shell.Safe(shell).exec(cmd,
					  null, cmdOutputBytes, cmdOutputBytes);
			exeResult = cmdOutputBytes.toString("UTF-8");
		} catch (IOException e) {
			e.printStackTrace();
			exeResult = "Cannot connect VM "+user+"@"
						+pubIP+" for "+e.getMessage();
		}
	}

}
