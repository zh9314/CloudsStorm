/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * Copyright © Huan Zhou (SNE, University of Amsterdam) and contributors
 */
package lambdaInfrs.engine.VEngine.EGI;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import lambdaInfrs.credential.Credential;
import lambdaInfrs.database.Database;
import lambdaInfrs.engine.VEngine.OS.ubuntu.VEngineUbuntu;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.output.NullOutputStream;
import org.apache.log4j.Logger;

import com.jcabi.ssh.SSH;
import com.jcabi.ssh.Shell;

import commonTool.CommonTool;
import topology.description.actual.ActualConnectionPoint;
import topology.description.actual.VM;

public class EGIVEngineUbuntu extends VEngineUbuntu {
	private static final Logger logger = Logger.getLogger(EGIVEngineUbuntu.class);
	
	@Override
	public boolean provision(VM subjectVM, Credential credential,
			Database database) {
		if(EGIVEngine.provision(subjectVM, credential, database))
			return true;
		else
			return false;
	}

	@Override
	public boolean delete(VM subjectVM, Credential credential, Database database) {
		if(EGIVEngine.delete(subjectVM, credential, database))
			return true;
		else
			return false;
	}

	@Override
	public boolean stop(VM subjectVM, Credential credential, Database database) {
		if(EGIVEngine.stop(subjectVM, credential, database))
			return true;
		else
			return false;
	}

	@Override
	public boolean start(VM subjectVM, Credential credential, Database database) {
		if(EGIVEngine.start(subjectVM, credential, database))
			return true;
		else
			return false;
	}

	@Override
	public boolean supportStop() {
		return true;
	}
	
	@Override
	public boolean confVNF(VM curVM) {
		if(curVM.vmConnectors == null
				&& curVM.selfEthAddresses == null){
			logger.info("There is no connection need to be configured for "+curVM.name);
			return true;
		}
		if(curVM.publicAddress == null){
			logger.error("No valid public address for VM "+curVM.name);
			return false;
		}
		String confFilePath = System.getProperty("java.io.tmpdir") + File.separator 
				+ curVM.ponintBack2STI.cloudProvider + "_conf_" + curVM.name 
				+ UUID.randomUUID().toString() + System.nanoTime() + ".sh"; 
		logger.debug("confFilePath: "+confFilePath);
		
		try{
			FileWriter fw = new FileWriter(confFilePath, false);
			
			////do not need to check whether this is the first time to configure the network
			////always reconfigure the /etc/hosts
			
		    fw.write("rm /etc/hosts\ntouch /etc/hosts\n");
		    fw.write("echo \"127.0.0.1	localhost\" >> /etc/hosts\n");
		    
		    if(curVM.selfEthAddresses != null 
		    		&& curVM.selfEthAddresses.size() != 0){
		    		for(Map.Entry<String, String> entry : 
		    				curVM.selfEthAddresses.entrySet()){
		    			String selfIP = entry.getKey().split("/")[0];
		    			fw.write("echo \""+selfIP
		    						+"	"+curVM.name+"\" >> /etc/hosts\n");
		    			////only configure the first IP for its own host name
		    			////to avoid conflict
		    			break;
		    		}
		    }
		    
		    Map<String, String> repeatChecker = new HashMap<String, String>();
		    if(curVM.vmConnectors != null){
		    		for(int vi = 0 ; vi < curVM.vmConnectors.size() ; vi++){
		    			ActualConnectionPoint curACP = curVM.vmConnectors.get(vi);
		    			VM peerVM = curACP.peerACP.belongingVM;
		    			if(!repeatChecker.containsKey(peerVM.name)){
		    				fw.write("echo \""+curACP.peerACP.address
		    						+"	"+peerVM.name+"\" >> /etc/hosts\n");
		    				repeatChecker.put(peerVM.name, "");
		    				//needConf = true;
		    			}
		    		}
		    }
		    
			if(curVM.selfEthAddresses != null){
				int count = 0;
				for(Map.Entry<String, String> entry : curVM.selfEthAddresses.entrySet()){
					if(entry.getValue() == null){
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
						curVM.selfEthAddresses.put(entry.getKey(), linkName);
					}
				}
			}
			
			////Configure for connections
			if(curVM.vmConnectors != null){
				for(int tci = 0 ; tci<curVM.vmConnectors.size() ; tci++){
					ActualConnectionPoint curACP = curVM.vmConnectors.get(tci);
					
					///If this tunnel connection has already been configured, skipped it
					///If the peer VM has not been started, skipped it.
					if(curACP.ethName != null )
						continue;
					if(curACP.peerACP.belongingVM.publicAddress == null)
						continue;
					
					String linkName = "", remotePubAddress = "", remotePrivateAddress = "", 
							netmask = "", subnet = "", localPrivateAddress = "";
					
					boolean nameExists = true;
					int curIndex = 0;
					while(nameExists){
						nameExists = false;
						linkName = "acp_" + curIndex;
						for(int tcj = 0 ; tcj < curVM.vmConnectors.size() ; tcj++){
							if(linkName.equals(curVM.vmConnectors.get(tcj).ethName)){
								nameExists = true;
								break;
							}
						}
						curIndex++;
					}
					remotePubAddress = curACP.peerACP.belongingVM.publicAddress;
					
					remotePrivateAddress = curACP.peerACP.address;
					localPrivateAddress = curACP.address;
					netmask = CommonTool.netmaskIntToString(Integer.valueOf(curACP.netmask));
					subnet = CommonTool.getSubnet(localPrivateAddress, Integer.valueOf(curACP.netmask));
					
					
					///record the ethName
					curACP.ethName = linkName;
					logger.debug("Configure connection name "+linkName);
					
					fw.write("lp=`ifconfig ens3|grep 'inet addr'|awk -F'[ :]' '{print $13}'`\n");
					fw.write("ip tunnel add "+linkName+" mode ipip remote "+remotePubAddress+" local $lp\n");
					fw.write("ifconfig "+linkName+" "+localPrivateAddress+" netmask "+netmask+"\n");
					fw.write("route del -net "+subnet+" netmask "+netmask+" dev "+linkName+"\n");
					fw.write("route add -host "+remotePrivateAddress+" dev "+linkName+"\n");
					fw.flush();
				}
			}
			fw.close();
		}catch (IOException e1){
			logger.error("Cannot setup VNF configure file!");
			return false;
		}
		///if there is a ssh time out exception, try 100 times.
		int loopCount = 0;
		while(loopCount < 100){
			loopCount++;
			try{
			Shell shell = new SSH(curVM.publicAddress, 22, 
					curVM.defaultSSHAccount, 
					curVM.ponintBack2STI.subTopology.accessKeyPair.privateKeyString);
			File file = new File(confFilePath);
			if(file.exists()){
				new Shell.Safe(shell).exec(
						  "cat > connection.sh && sudo bash connection.sh ",
						  new FileInputStream(file),
						  new NullOutputStream(), new NullOutputStream()
				);
				FileUtils.deleteQuietly(file);
			}
			new Shell.Safe(shell).exec(
					  "rm connection.sh",
					  null,
					  new NullOutputStream(), new NullOutputStream()
			);

			}catch (IOException e) {
				////In this case, we give more chances to test.
				if(e.getMessage().contains("timeout: socket is not established")){   
					logger.warn(curVM.name +": "+ e.getMessage());
					try {
						Thread.sleep(2000);
					} catch (InterruptedException e1) {
					}
					continue;
				}else{
					logger.error(curVM.name +": "+ e.getMessage());
					curVM.ponintBack2STI.logsInfo.put(curVM.name, e.getMessage());
					return false;
				}
			}
			return true;
		}
		
		return false;
	}

}
