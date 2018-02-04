package provisioning.engine.VEngine.EC2;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.output.NullOutputStream;
import org.apache.log4j.Logger;

import com.jcabi.ssh.SSH;
import com.jcabi.ssh.Shell;

import commonTool.CommonTool;
import provisioning.engine.VEngine.VEngineCoreMethod;
import topologyAnalysis.dataStructure.SubConnection;
import topologyAnalysis.dataStructure.TopConnectionPoint;

/**
 * This is a specific EC2 VM engine which is responsible for connection and run 
 * some predefined scripts. This class is specially designed for the VM of ubuntu 
 *
 */
public class EC2VEngine_ubuntu extends EC2VEngine implements VEngineCoreMethod, Runnable{
	
	private static final Logger logger = Logger.getLogger(EC2VEngine_ubuntu.class);
	
	
	
	public EC2VEngine_ubuntu(){
		
	}
	
	/*public void setParameters(EC2Agent ec2agent, 
			EC2VM curVM, ArrayList<SubConnection> subConnections, 
			ArrayList<TopConnection>topConnections, 
			String privateKeyString, String cmd,
			String userName, String publicKeyString){
		this.ec2agent = ec2agent;
		this.curVM = curVM;
		this.cmd = cmd;
		this.subConnections = subConnections;
		this.topConnections = topConnections;
		this.privateKeyString = privateKeyString;
		this.userName = userName;
		this.publicKeyString = publicKeyString;
	}*/
	
	/**
	 * Configuration on the connection to configure the VM to be connected  
	 */
	public void connectionConf(){
		if(this.subConnections == null && this.topConnectors == null){
			logger.info("There is no connection need to be configured for "+this.curVM.name);
			return ;
		}
		String confFilePath = System.getProperty("java.io.tmpdir") + File.separator 
				+ "ec2_conf_" + curVM.name + UUID.randomUUID().toString() + System.nanoTime() + ".sh"; 
		logger.debug("confFilePath: "+confFilePath);
		try{
		FileWriter fw = new FileWriter(confFilePath, false);
		
		boolean needConf = false;
		////Configure for all top self connections
		if(this.curVM.selfEthAddresses != null){
			int count = 0;
			for(Map.Entry<String, Boolean> entry : curVM.selfEthAddresses.entrySet()){
				if(!entry.getValue()){
					String linkName = "self_"+count;
					String remotePubAddress = curVM.publicAddress;
					String [] addrNm = entry.getKey().split("/");
					String localPrivateAddress = addrNm[0];
					String netmask = addrNm[1];
					int netmaskNum = CommonTool.netmaskStringToInt(netmask);
					String subnet = CommonTool.getSubnet(localPrivateAddress, netmaskNum);
					
					fw.write("lp=`ifconfig eth0|grep 'inet addr'|awk -F'[ :]' '{print $13}'`\n");
					fw.write("ip tunnel add "+linkName+" mode ipip remote "+remotePubAddress+" local $lp\n");
					fw.write("ifconfig "+linkName+" "+localPrivateAddress+" netmask "+netmask+"\n");
					fw.write("route del -net "+subnet+" netmask "+netmask+" dev "+linkName+"\n");
					fw.write("route add -host "+localPrivateAddress+" dev "+linkName+"\n");
					fw.flush();
					entry.setValue(true);
					needConf = true;
				}
			}
		}
		
		////Configure for subconnections
		if(this.subConnections != null){
			for(int sci = 0 ; sci<this.subConnections.size() ; sci++){
				SubConnection curSubCon = this.subConnections.get(sci);
				String linkName = curSubCon.name+".sub";
				String remotePubAddress = "", remotePrivateAddress = "", 
						netmask = "", subnet = "", localPrivateAddress = "";
				boolean findVM = false;
				if(curSubCon.source.belongingVM.name.equals(curVM.name)){
					remotePubAddress = curSubCon.target.belongingVM.publicAddress;
					remotePrivateAddress = curSubCon.target.address;
					localPrivateAddress = curSubCon.source.address;
					netmask = CommonTool.netmaskIntToString(Integer.valueOf(curSubCon.source.netmask));
					subnet = CommonTool.getSubnet(localPrivateAddress, Integer.valueOf(curSubCon.source.netmask));
					findVM = true;
				}
				
				if(curSubCon.target.belongingVM.name.equals(curVM.name)){
					remotePubAddress = curSubCon.source.belongingVM.publicAddress;
					remotePrivateAddress = curSubCon.source.address;
					localPrivateAddress = curSubCon.target.address;
					netmask = CommonTool.netmaskIntToString(Integer.valueOf(curSubCon.target.netmask));
					subnet = CommonTool.getSubnet(localPrivateAddress, Integer.valueOf(curSubCon.target.netmask));
					findVM = true;
				}
				if(!findVM)
					continue;
				fw.write("lp=`ifconfig eth0|grep 'inet addr'|awk -F'[ :]' '{print $13}'`\n");
				fw.write("ip tunnel add "+linkName+" mode ipip remote "+remotePubAddress+" local $lp\n");
				fw.write("ifconfig "+linkName+" "+localPrivateAddress+" netmask "+netmask+"\n");
				fw.write("route del -net "+subnet+" netmask "+netmask+" dev "+linkName+"\n");
				fw.write("route add -host "+remotePrivateAddress+" dev "+linkName+"\n");
				fw.flush();
				needConf = true;
			}
		}
		
		////Configure for topconnections
		if(this.topConnectors != null){
			for(int tci = 0 ; tci<this.topConnectors.size() ; tci++){
				TopConnectionPoint curTCP = this.topConnectors.get(tci);
				
				///If this tunnel connection has already been configured, skipped it
				if(curTCP.ethName != null)
					continue;
				
				String linkName = "", remotePubAddress = "", remotePrivateAddress = "", 
						netmask = "", subnet = "", localPrivateAddress = "";
				
				if(curTCP.belongingVM.name.equals(curVM.name)){
					boolean nameExists = true;
					int curIndex = 0;
					while(nameExists){
						nameExists = false;
						linkName = "top_" + curIndex;
						for(int tcj = 0 ; tcj < this.topConnectors.size() ; tcj++){
							if(linkName.equals(topConnectors.get(tcj).ethName)){
								nameExists = true;
								break;
							}
						}
						curIndex++;
					}
					remotePubAddress = curTCP.peerTCP.belongingVM.publicAddress;
					if(remotePubAddress == null){
						curTCP.ethName = null;
						continue;
					}
					remotePrivateAddress = curTCP.peerTCP.address;
					localPrivateAddress = curTCP.address;
					netmask = CommonTool.netmaskIntToString(Integer.valueOf(curTCP.netmask));
					subnet = CommonTool.getSubnet(localPrivateAddress, Integer.valueOf(curTCP.netmask));
				}else
					continue;
				
				///record the ethName
				curTCP.ethName = linkName;
				logger.debug("Get topconnection name "+linkName);
				
				fw.write("lp=`ifconfig eth0|grep 'inet addr'|awk -F'[ :]' '{print $13}'`\n");
				fw.write("ip tunnel add "+linkName+" mode ipip remote "+remotePubAddress+" local $lp\n");
				fw.write("ifconfig "+linkName+" "+localPrivateAddress+" netmask "+netmask+"\n");
				fw.write("route del -net "+subnet+" netmask "+netmask+" dev "+linkName+"\n");
				fw.write("route add -host "+remotePrivateAddress+" dev "+linkName+"\n");
				fw.flush();
				needConf = true;
			}
		}
		fw.close();
		
		if(needConf){
			Shell shell = new SSH(curVM.publicAddress, 22, curVM.defaultSSHAccount, this.privateKeyString);
			File file = new File(confFilePath);
			new Shell.Safe(shell).exec(
			  "cat > connection.sh && sudo bash connection.sh ",
			  new FileInputStream(file),
			  new NullOutputStream(), new NullOutputStream()
			);
			FileUtils.deleteQuietly(file);
			new Shell.Safe(shell).exec(
					  "rm connection.sh",
					  null,
					  new NullOutputStream(), new NullOutputStream()
			);
		}
		
		
		}catch (IOException  e) {
			e.printStackTrace();
			logger.error(curVM.name +": "+ e.getMessage());
			if(e.getMessage().contains("timeout: socket is not established")){   ////In this case, we give another chance to test.
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e1) {
				}
				File file = new File(confFilePath);
				if(file.exists()){
					try {
						Shell shell = new SSH(curVM.publicAddress, 22, curVM.defaultSSHAccount, this.privateKeyString);
						new Shell.Safe(shell).exec(
								  "cat > connection.sh && sudo bash connection.sh ",
								  new FileInputStream(file),
								  new NullOutputStream(), new NullOutputStream()
								);
						FileUtils.deleteQuietly(file);
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				}
				try {
					Shell shell = new SSH(curVM.publicAddress, 22, curVM.defaultSSHAccount, this.privateKeyString);
					new Shell.Safe(shell).exec(
							  "rm connection.sh",
							  null,
							  new NullOutputStream(), new NullOutputStream()
					);
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		}
	}

	@Override
	public void run() {
		if(cmd.equals("all")){
			connectionConf();
			sshConf();
			runScript();
		}else if(cmd.equals("connection")){
			connectionConf();
		}else if(cmd.equals("ssh")){
			sshConf();
		}else if(cmd.equals("script")){
			runScript();
		}else if(cmd.equals("remove")){
			removeEth();
		}else{
			logger.error("The command for thread of '"+curVM.name+"' is wrong!");
			return;
		}
	}
	
	

	@Override
	public void sshConf() {
		if(userName == null || publicKeyString == null){
			logger.warn("The username is not specified! Unified ssh account will not be configured!");
			return;
		}
		String runFilePath = System.getProperty("java.io.tmpdir") + File.separator 
				+ "runSSH_" + curVM.name + System.nanoTime() + ".sh";
		String pubFilePath = System.getProperty("java.io.tmpdir") + File.separator 
				+ "pubFile_" + curVM.name + System.nanoTime() + ".pub";
		logger.debug("runFilePath: "+runFilePath);
		logger.debug("pubFilePath: "+pubFilePath);
		FileWriter fw;
		try {
			fw = new FileWriter(pubFilePath, false);
			fw.write(this.publicKeyString);
			fw.close();
			
			fw = new FileWriter(runFilePath, false);
			fw.write("useradd -d \"/home/"+userName+"\" -m -s \"/bin/bash\" "+userName+"\n");
			fw.write("mkdir /home/"+userName+"/.ssh \n");
			fw.write("mv user.pub /home/"+userName+"/.ssh/authorized_keys \n");
			fw.write("cat id_rsa.pub >> /home/"+userName+"/.ssh/authorized_keys \n");
			fw.write("cat id_rsa.pub >> /root/.ssh/authorized_keys \n");
			fw.write("chmod 400 id_rsa\n");
			fw.write("cp id_rsa /home/"+userName+"/.ssh/id_rsa\n");
			fw.write("cp id_rsa /root/.ssh/id_rsa\n");
			fw.write("rm id_rsa.pub id_rsa\n");
		    fw.write("chmod 740 /etc/sudoers \n");
		    fw.write("echo \""+userName+" ALL=(ALL)NOPASSWD: ALL\" >> /etc/sudoers \n");
		    fw.write("chmod 440 /etc/sudoers \n");
		    fw.write("chown -R "+userName+":"+userName+" /home/"+userName+"/.ssh/\n");
		    fw.close();
		    
		    Shell shell = new SSH(curVM.publicAddress, 22, curVM.defaultSSHAccount, this.privateKeyString);
			File pubFile = new File(pubFilePath);
			new Shell.Safe(shell).exec(
					  "cat > user.pub",
					  new FileInputStream(pubFile),
					  new NullOutputStream(), new NullOutputStream()
					);
			String clusterPubKeyPath = this.currentDir + "clusterKeyPair" + File.separator + "id_rsa.pub";
			File clusterPubKey = new File(clusterPubKeyPath);
			new Shell.Safe(shell).exec(
					  "cat > id_rsa.pub",
					  new FileInputStream(clusterPubKey),
					  new NullOutputStream(), new NullOutputStream()
					);
			String clusterPriKeyPath = this.currentDir + "clusterKeyPair" + File.separator + "id_rsa";
			File clusterPriKey = new File(clusterPriKeyPath);
			new Shell.Safe(shell).exec(
					  "cat > id_rsa",
					  new FileInputStream(clusterPriKey),
					  new NullOutputStream(), new NullOutputStream()
					);
			
		    File sshFile = new File(runFilePath);
			new Shell.Safe(shell).exec(
			  "cat > sshconf.sh && sudo bash sshconf.sh ",
			  new FileInputStream(sshFile),
			  new NullOutputStream(), new NullOutputStream()
			);
			Thread.sleep(1000);
			new Shell.Safe(shell).exec(
					  "rm sshconf.sh",
					  null,
					  new NullOutputStream(), new NullOutputStream()
			);
			FileUtils.deleteQuietly(pubFile);
			FileUtils.deleteQuietly(sshFile);
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
			logger.error(curVM.name+": "+e.getMessage());
		}
		
	}

	@Override
	public void runScript() {
		if(curVM.script == null){
			logger.info("There is no script needed for '"+curVM.name+"' to run!");
			return ;
		}
		String scriptPath = System.getProperty("java.io.tmpdir") + File.separator 
				+ "script_" + curVM.name + System.nanoTime() + ".sh";
		try {
			FileWriter fw = new FileWriter(scriptPath, false);
			fw.write(curVM.v_scriptString);
			fw.close();
			Shell shell = new SSH(curVM.publicAddress, 22, curVM.defaultSSHAccount, this.privateKeyString);
			File scriptFile = new File(scriptPath);
			new Shell.Safe(shell).exec(
					  "cat > script.sh",
					  new FileInputStream(scriptFile),
					  new NullOutputStream(), new NullOutputStream()
					);
			
			////Logging files to log the output of executing the script
			String logPath = this.currentDir + curVM.name + "_" + System.nanoTime() + "_script.log";
			logger.debug("The log file of executing script on '"+curVM.name+"' is redirected to "+logPath);
			File logFile = new File(logPath);
			FileOutputStream logOutput = new FileOutputStream(logFile, false);
			if(userName != null && publicKeyString != null){
				new Shell.Safe(shell).exec(
						  "sudo mv script.sh /home/"+userName+"/",
						  null,
						  new NullOutputStream(), new NullOutputStream()
				);
				new Shell.Safe(shell).exec(
						  "sudo bash /home/"+userName+"/script.sh",
						  null,
						  logOutput,
						  logOutput
				);
			}else{
				new Shell.Safe(shell).exec(
						  "sudo bash script.sh",
						  null,
						  logOutput,
						  logOutput
				);
			}
			logOutput.close();
			FileUtils.deleteQuietly(scriptFile);
			logger.info("Script for '"+curVM.name+"' is executed!");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void removeEth() {
		try{
		
		///always one element in this topConnnectors
		TopConnectionPoint curTCP = this.topConnectors.get(0);
		String tunnelName = curTCP.ethName;
		String remotePrivateAddress = curTCP.peerTCP.address; 
		if(tunnelName == null){
			logger.warn("TunnelName of '"+curVM.name+"' has been deleted for unknown reason!");
			return ;
		}
		String confFilePath = System.getProperty("java.io.tmpdir") + File.separator 
				+ "ec2_conf_" + curVM.name + UUID.randomUUID().toString() + System.nanoTime() + ".sh"; 
		logger.debug("rmEthFilePath: "+confFilePath);
		FileWriter fw = new FileWriter(confFilePath, false);
		
		fw.write("route del -host "+remotePrivateAddress+" dev "+tunnelName+"\n");
		fw.write("ip tunnel del "+tunnelName+"\n");
		fw.flush();
		fw.close();
		
		///Identify this tunnel is deleted in the control file.
		curTCP.ethName = null;

		String rmConnectionName = UUID.randomUUID().toString()+".sh";
		Thread.sleep(2000);
		Shell shell = new SSH(curVM.publicAddress, 22, curVM.defaultSSHAccount, this.privateKeyString);
		File file = new File(confFilePath);
		new Shell.Safe(shell).exec(
		  "cat > "+rmConnectionName+" && sudo bash "+rmConnectionName ,
		  new FileInputStream(file),
		  new NullOutputStream(), new NullOutputStream()
		);
		FileUtils.deleteQuietly(file);
		new Shell.Safe(shell).exec(
				  "rm "+rmConnectionName,
				  null,
				  new NullOutputStream(), new NullOutputStream()
		);
		
		}catch (IOException | InterruptedException e) {
			e.printStackTrace();
			logger.error(curVM.name +": "+ e.getMessage());
		}
	}

}
