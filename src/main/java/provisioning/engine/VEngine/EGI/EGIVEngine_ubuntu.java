package provisioning.engine.VEngine.EGI;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
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

public class EGIVEngine_ubuntu extends EGIVEngine implements VEngineCoreMethod, Runnable{
	
	private static final Logger logger = Logger.getLogger(EGIVEngine_ubuntu.class);

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
					logger.debug("Get topconnection name "+linkName);
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
				
				fw.write("lp=`ifconfig eth0|grep 'inet addr'|awk -F'[ :]' '{print $13}'`\n");
				fw.write("ip tunnel add "+linkName+" mode ipip remote "+remotePubAddress+" local $lp\n");
				fw.write("ifconfig "+linkName+" "+localPrivateAddress+" netmask "+netmask+"\n");
				fw.write("route del -net "+subnet+" netmask "+netmask+" dev "+linkName+"\n");
				fw.write("route add -host "+remotePrivateAddress+" dev "+linkName+"\n");
				fw.flush();
			}
		}
		fw.close();
		

		Thread.sleep(2000);
		Shell shell = new SSH(curVM.publicAddress, 22, "ubuntu", this.privateKeyString);
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
		
		}catch (IOException | InterruptedException e) {
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
						Shell shell = new SSH(curVM.publicAddress, 22, "ubuntu", this.privateKeyString);
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
					Shell shell = new SSH(curVM.publicAddress, 22, "ubuntu", this.privateKeyString);
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
	
	

	
	

}
