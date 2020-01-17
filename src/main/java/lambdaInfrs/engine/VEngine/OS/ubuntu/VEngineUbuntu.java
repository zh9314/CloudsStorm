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
package lambdaInfrs.engine.VEngine.OS.ubuntu;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import lambdaInfrs.engine.VEngine.VEngineOpMethod;
import lambdaInfrs.engine.VEngine.OS.VEngineOS;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import com.jcabi.ssh.SSH;
import com.jcabi.ssh.Shell;

import commonTool.CommonTool;
import java.io.ByteArrayOutputStream;
import java.util.logging.Level;
import topology.description.actual.ActualConnectionPoint;
import topology.description.actual.SubTopologyInfo;
import topology.description.actual.TopTopology;
import topology.description.actual.VM;

/**
 * This is a kind of VEngine is only to be leveraged to do configuration related
 * work. This is used to be derived by the specific VEngine for a Cloud. All
 * Ubuntu related VEngines should be derived from this Class, instead of
 * BasicVEngineUbuntu
 *
 * @author huan
 *
 */
public abstract class VEngineUbuntu extends VEngineOS implements VEngineOpMethod {

    private static final Logger logger = Logger.getLogger(VEngineUbuntu.class);

    @Override
    public boolean confVNF(VM curVM) {
        if (curVM.vmConnectors == null
                && curVM.selfEthAddresses == null) {
            logger.info("There is no connection need to be configured for " + curVM.name);
            return true;
        }
        if (curVM.publicAddress == null) {
            logger.error("No valid public address for VM " + curVM.name);
            return false;
        }
        String confFilePath = System.getProperty("java.io.tmpdir") + File.separator
                + curVM.ponintBack2STI.cloudProvider + "_conf_" + curVM.name
                + UUID.randomUUID().toString() + System.nanoTime() + ".sh";
        logger.debug("confFilePath: " + confFilePath);

        try {
            FileWriter fw = new FileWriter(confFilePath, false);

            ////do not need to check whether this is the first time to configure the network
            ////always reconfigure the /etc/hosts
            fw.write("rm /etc/hosts\ntouch /etc/hosts\n");
            fw.write("echo \"127.0.0.1	localhost\" >> /etc/hosts\n");

            if (curVM.selfEthAddresses != null
                    && curVM.selfEthAddresses.size() != 0) {
                for (Map.Entry<String, String> entry
                        : curVM.selfEthAddresses.entrySet()) {
                    String selfIP = entry.getKey().split("/")[0];
                    fw.write("echo \"" + selfIP
                            + "	" + curVM.name + "\" >> /etc/hosts\n");
                    ////only configure the first IP for its own host name
                    ////to avoid conflict
                    break;
                }
            }

            Map<String, String> repeatChecker = new HashMap<String, String>();
            if (curVM.vmConnectors != null) {
                for (int vi = 0; vi < curVM.vmConnectors.size(); vi++) {
                    ActualConnectionPoint curACP = curVM.vmConnectors.get(vi);
                    VM peerVM = curACP.peerACP.belongingVM;
                    if (!repeatChecker.containsKey(peerVM.name)) {
                        fw.write("echo \"" + curACP.peerACP.address
                                + "	" + peerVM.name + "\" >> /etc/hosts\n");
                        repeatChecker.put(peerVM.name, "");
                        //needConf = true;
                    }
                }
            }

            if (curVM.selfEthAddresses != null) {
                int count = 0;
                for (Map.Entry<String, String> entry : curVM.selfEthAddresses.entrySet()) {
                    if (entry.getValue() == null) {
                        String linkName = "self_" + count;
                        String remotePubAddress = curVM.publicAddress;
                        String[] addrNm = entry.getKey().split("/");
                        String localPrivateAddress = addrNm[0];
                        String netmask = addrNm[1];
                        int netmaskNum = CommonTool.netmaskStringToInt(netmask);
                        String subnet = CommonTool.getSubnet(localPrivateAddress, netmaskNum);

                        fw.write("ethName=`ip r show|grep \"default \"|cut -d \" \" -f 5`\n");
                        fw.write("lp=`ifconfig $ethName|grep 'inet addr'|awk -F'[ :]' '{print $13}'`\n");
                        fw.write("ip tunnel add " + linkName + " mode ipip remote " + remotePubAddress + " local $lp\n");
                        fw.write("ifconfig " + linkName + " " + localPrivateAddress + " netmask " + netmask + "\n");
                        fw.write("route del -net " + subnet + " netmask " + netmask + " dev " + linkName + "\n");
                        fw.write("route add -host " + localPrivateAddress + " dev " + linkName + "\n");
                        fw.flush();
                        curVM.selfEthAddresses.put(entry.getKey(), linkName);
                        count++;
                    }
                }
            }

            ////Configure for connections
            if (curVM.vmConnectors != null) {
                for (int tci = 0; tci < curVM.vmConnectors.size(); tci++) {
                    ActualConnectionPoint curACP = curVM.vmConnectors.get(tci);

                    ///If this tunnel connection has already been configured, skipped it
                    ///If the peer VM has not been started, skipped it.
                    if (curACP.ethName != null) {
                        continue;
                    }
                    if (curACP.peerACP.belongingVM.publicAddress == null) {
                        continue;
                    }

                    String linkName = "", remotePubAddress = "", remotePrivateAddress = "",
                            netmask = "", subnet = "", localPrivateAddress = "";

                    boolean nameExists = true;
                    int curIndex = 0;
                    while (nameExists) {
                        nameExists = false;
                        linkName = "acp_" + curIndex;
                        for (int tcj = 0; tcj < curVM.vmConnectors.size(); tcj++) {
                            if (linkName.equals(curVM.vmConnectors.get(tcj).ethName)) {
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
                    logger.debug("Configure connection name " + linkName);

                    fw.write("ethName=`ip r show|grep \"default \"|cut -d \" \" -f 5`\n");
                    fw.write("lp=`ifconfig $ethName|grep 'inet addr'|awk -F'[ :]' '{print $13}'`\n");
                    fw.write("ip tunnel add " + linkName + " mode ipip remote " + remotePubAddress + " local $lp\n");
                    fw.write("ifconfig " + linkName + " " + localPrivateAddress + " netmask " + netmask + "\n");
                    fw.write("route del -net " + subnet + " netmask " + netmask + " dev " + linkName + "\n");
                    fw.write("route add -host " + remotePrivateAddress + " dev " + linkName + "\n");
                    fw.flush();
                }
            }
            fw.close();
        } catch (IOException e1) {
            logger.error("Cannot setup VNF configure file!");
            return false;
        }
        ///if there is a ssh time out exception, try 100 times.
        int loopCount = 0;
        while (loopCount < 100) {
            loopCount++;
            try {
                Shell shell = new SSH(curVM.publicAddress, 22,
                        curVM.defaultSSHAccount,
                        curVM.ponintBack2STI.subTopology.accessKeyPair.privateKeyString);
                File file = new File(confFilePath);
                if (file.exists()) {

                    String cmd = "cat > connection.sh && sudo bash connection.sh ";
                    int exitCode = execShell(cmd, new FileInputStream(file), shell);

                    FileUtils.deleteQuietly(file);
                }
                String cmd = "rm connection.sh";
                int exitCode = execShell(cmd, null, shell);

            } catch (IOException e) {
                ////In this case, we give more chances to test.
                if (e.getMessage().contains("timeout: socket is not established")) {
                    logger.warn(curVM.name + ": " + e.getMessage());
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e1) {
                    }
                    continue;
                } else {
                    logger.error(curVM.name + ": " + e.getMessage());
                    curVM.ponintBack2STI.logsInfo.put(curVM.name, e.getMessage());
                    return false;
                }
            }
            return true;
        }

        return false;
    }

    @Override
    public boolean confSSH(VM curVM) {
        if (curVM.publicAddress == null) {
            logger.error("No valid public address for VM " + curVM.name);
            return false;
        }
        SubTopologyInfo curSTI = curVM.ponintBack2STI;

        TopTopology topTopology = curSTI.pointBack2TTI;
        SubTopologyInfo ctrlSTI = topTopology.getSubtopology("_ctrl");

        String runFilePath = System.getProperty("java.io.tmpdir") + File.separator
                + "runSSH_" + curVM.name + System.nanoTime() + ".sh";
        String pubFilePath = System.getProperty("java.io.tmpdir") + File.separator
                + "pubFile_" + curVM.name + System.nanoTime() + ".pub";
        logger.debug("runFilePath: " + runFilePath);
        logger.debug("pubFilePath: " + pubFilePath);
        FileWriter fw;
        try {
            fw = new FileWriter(pubFilePath, false);
            fw.write(curSTI.publicKeyString);
            fw.close();

            fw = new FileWriter(runFilePath, false);

            ///configure the overall ssh key
            fw.write("chmod 400 id_rsa\n");
            fw.write("cat id_rsa.pub >> /root/.ssh/authorized_keys \n");
            fw.write("cp id_rsa /root/.ssh/id_rsa\n");

            if (curSTI.userName != null && curSTI.publicKeyString != null) {
                fw.write("useradd -d \"/home/" + curSTI.userName + "\" -m -s \"/bin/bash\" " + curSTI.userName + "\n");
                fw.write("mkdir /home/" + curSTI.userName + "/.ssh \n");
                fw.write("mv user.pub /home/" + curSTI.userName + "/.ssh/authorized_keys \n");
                fw.write("cat id_rsa.pub >> /home/" + curSTI.userName + "/.ssh/authorized_keys \n");
                fw.write("chmod 740 /etc/sudoers \n");
                fw.write("echo \"" + curSTI.userName + " ALL=(ALL)NOPASSWD: ALL\" >> /etc/sudoers \n");
                fw.write("chmod 440 /etc/sudoers \n");
                fw.write("chown -R " + curSTI.userName + ":" + curSTI.userName + " /home/" + curSTI.userName + "/.ssh/\n");
                fw.write("cp id_rsa /home/" + curSTI.userName + "/.ssh/id_rsa\n");
            } else {
                logger.warn("The username is not specified! The user account will not be configured!");
            }

            ////configure an system overall account "CloudsStorm"
            fw.write("useradd -d \"/home/CloudsStorm\" -m -s \"/bin/bash\" CloudsStorm\n");
            fw.write("mkdir /home/CloudsStorm/.ssh \n");
            fw.write("mv user.pub /home/CloudsStorm/.ssh/authorized_keys \n");
            fw.write("cat id_rsa.pub >> /home/CloudsStorm/.ssh/authorized_keys \n");
            fw.write("chmod 740 /etc/sudoers \n");
            fw.write("echo \"CloudsStorm ALL=(ALL)NOPASSWD: ALL\" >> /etc/sudoers \n");
            fw.write("chmod 440 /etc/sudoers \n");
            fw.write("chown -R CloudsStorm:CloudsStorm /home/CloudsStorm/.ssh/\n");
            fw.write("cp id_rsa /home/CloudsStorm/.ssh/id_rsa\n");

            fw.write("rm id_rsa.pub id_rsa\n");

            fw.write("hostname " + curVM.name + "\n");

            fw.write("echo \"StrictHostKeyChecking no\" >> /etc/ssh/ssh_config\n");
            fw.write("echo \"UserKnownHostsFile=/dev/null\" >> /etc/ssh/ssh_config\n");

            ///configure the control agent address
            if (ctrlSTI != null) {
                VM ctrlVM = ctrlSTI.subTopology.getVMinSubClassbyName("ctrl");
                if (ctrlVM != null) {
                    fw.write("echo \"" + ctrlVM.publicAddress + "\" >> /tmp/ctrl.info\n");
                }
            }

            fw.close();
        } catch (IOException e1) {
            logger.error("Cannot setup SSH configure file!");
            return false;
        }
        boolean op1 = false, op2 = false, op3 = false,
                op4 = false, op5 = false;
        int loopCount = 0;
        while (loopCount < 100) {
            loopCount++;
            try {
                Shell shell = new SSH(curVM.publicAddress, 22, curVM.defaultSSHAccount,
                        curSTI.subTopology.accessKeyPair.privateKeyString);
                File pubFile = new File(pubFilePath);
                if (!op1) {
                    ByteArrayOutputStream stdOut = new ByteArrayOutputStream();
                    ByteArrayOutputStream stdErr = new ByteArrayOutputStream();

                    int execShell = execShell("cat > user.pub", new FileInputStream(pubFile), shell);
                    FileUtils.deleteQuietly(pubFile);
                    op1 = true;
                }
                String currentDir = CommonTool.getPathDir(curSTI.subTopology.loadingPath);
                String clusterPubKeyPath = currentDir + "clusterKeyPair"
                        + File.separator + "id_rsa.pub";
                File clusterPubKey = new File(clusterPubKeyPath);
                if (!op2) {

                    execShell("cat > id_rsa.pub", new FileInputStream(clusterPubKey), shell);
                    op2 = true;
                }
                String clusterPriKeyPath = currentDir + "clusterKeyPair"
                        + File.separator + "id_rsa";
                File clusterPriKey = new File(clusterPriKeyPath);
                if (!op3) {
                    execShell("cat > id_rsa", new FileInputStream(clusterPriKey), shell);
                    op3 = true;
                }

                if (!op4) {
                    File sshFile = new File(runFilePath);
                    execShell("cat > sshconf.sh && sudo bash sshconf.sh ", new FileInputStream(sshFile), shell);
                    FileUtils.deleteQuietly(sshFile);
                    op4 = true;
                }
                if (!op5) {
                    execShell("rm sshconf.sh", null, shell);
                    op5 = true;
                }
            } catch (IOException e) {
                ////In this case, we give more chances to test.
                if (e.getMessage().contains("timeout: socket is not established")) {
                    logger.warn(curVM.name + ": " + e.getMessage());
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e1) {
                    }
                    continue;
                } else {
                    logger.error(curVM.name + ": " + e.getMessage());
                    curVM.ponintBack2STI.logsInfo.put(curVM.name, e.getMessage());
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean confENV(VM curVM) {
        if (curVM.script == null) {
            logger.info("There is no script needed for '" + curVM.name + "' to run!");
            return true;
        }
        if (curVM.publicAddress == null) {
            logger.error("No valid public address for VM " + curVM.name);
            return false;
        }
        SubTopologyInfo curSTI = curVM.ponintBack2STI;
        String scriptPath = System.getProperty("java.io.tmpdir") + File.separator
                + "script_" + curVM.name + System.nanoTime() + ".sh";
        try {
            FileWriter fw = new FileWriter(scriptPath, false);
            fw.write(curVM.v_scriptString);
            fw.close();
        } catch (IOException e) {
            logger.error("Cannot setup ENV configure file!");
            return false;
        }
        boolean op1 = false, op2 = false, op3 = false;
        int loopCount = 0;
        while (loopCount < 100) {
            loopCount++;
            try {
                Shell shell = new SSH(curVM.publicAddress, 22, curVM.defaultSSHAccount,
                        curSTI.subTopology.accessKeyPair.privateKeyString);
                if (!op1) {
                    File scriptFile = new File(scriptPath);
                    execShell("cat > script.sh", new FileInputStream(scriptFile), shell);
                    FileUtils.deleteQuietly(scriptFile);
                    op1 = true;
                }

                ////Logging files to log the output of executing the script
                if (!op2) {
                    String currentDir = CommonTool.getPathDir(curSTI.subTopology.loadingPath);
                    String logPath = currentDir + curVM.name + "_" + System.nanoTime() + "_script.log";
                    logger.debug("The log file of executing script on '" + curVM.name + "' is redirected to " + logPath);
                    File logFile = new File(logPath);
                    FileOutputStream logOutput = new FileOutputStream(logFile, false);
                    execShell("sudo bash script.sh", null, shell);
                    logOutput.close();
                    op2 = true;
                }
                if (curSTI.userName != null && curSTI.publicKeyString != null) {
                    if (!op3) {
                        execShell("sudo mv script.sh /home/" + curSTI.userName + "/", null, shell);
                        op3 = true;
                    }
                }

                logger.info("Script for '" + curVM.name + "' is done!");
            } catch (IOException e) {
                ////In this case, we give more chances to test.
                if (e.getMessage().contains("timeout: socket is not established")) {
                    logger.warn(curVM.name + ": " + e.getMessage());
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e1) {
                    }
                    continue;
                } else {
                    logger.error(curVM.name + ": " + e.getMessage());
                    curVM.ponintBack2STI.logsInfo.put(curVM.name, e.getMessage());
                    return false;
                }
            }
            return true;
        }

        return false;
    }

    @Override
    public boolean detach(VM curVM) {
        if (curVM.publicAddress == null) {
            logger.error("No valid public address for VM " + curVM.name);
            return false;
        }
        ////identify whether there exists that kind of connection.
        boolean deletedNeeded = false;
        String confFilePath = System.getProperty("java.io.tmpdir") + File.separator
                + curVM.ponintBack2STI.cloudProvider + "_conf_" + curVM.name
                + UUID.randomUUID().toString() + System.nanoTime() + ".sh";
        logger.debug("rmEthFilePath: " + confFilePath);
        try {
            ////Delete all the related connections
            FileWriter fw = new FileWriter(confFilePath, false);

            if (curVM.vmConnectors != null) {
                for (int aci = 0; aci < curVM.vmConnectors.size(); aci++) {
                    ActualConnectionPoint curACP = curVM.vmConnectors.get(aci);
                    if (curACP.peerACP.ethName != null) ////This means the peer sub-topology is not failed or deleted
                    {
                        continue;
                    }
                    deletedNeeded = true;
                    String tunnelName = curACP.ethName;
                    String remotePrivateAddress = curACP.peerACP.address;
                    if (tunnelName == null) {
                        logger.warn("TunnelName of '" + curVM.name
                                + "' has been deleted for some reason!");
                        continue;
                    }
                    fw.write("route del -host " + remotePrivateAddress
                            + " dev " + tunnelName + "\n");
                    fw.write("ip tunnel del " + tunnelName + "\n");
                    fw.flush();

                    ///Identify this tunnel is deleted in the control file.
                    curACP.ethName = null;
                }
            }
            fw.close();
        } catch (IOException e) {
            logger.error("Cannot setup Detach configure file!");
            return false;
        }
        boolean op1 = false, op2 = false;
        int loopCount = 0;
        while (loopCount < 100) {
            loopCount++;
            try {
                if (deletedNeeded) {
                    String rmConnectionName = UUID.randomUUID().toString() + ".sh";

                    Shell shell = new SSH(curVM.publicAddress, 22, curVM.defaultSSHAccount,
                            curVM.ponintBack2STI.subTopology.accessKeyPair.privateKeyString);
                    if (!op1) {
                        File file = new File(confFilePath);
                        execShell("cat > " + rmConnectionName + " && sudo bash " + rmConnectionName, new FileInputStream(file), shell);
                        FileUtils.deleteQuietly(file);
                        op1 = true;
                    }
                    if (!op2) {
                        execShell("rm " + rmConnectionName, null, shell);
                        op2 = true;
                    }
                }
            } catch (IOException e) {
                ////In this case, we give more chances to test.
                if (e.getMessage().contains("timeout: socket is not established")) {
                    logger.warn(curVM.name + ": " + e.getMessage());
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e1) {
                    }
                    continue;
                } else {
                    logger.error(curVM.name + ": " + e.getMessage());
                    curVM.ponintBack2STI.logsInfo.put(curVM.name, e.getMessage());
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    private int execShell(String cmd, FileInputStream input, Shell shell) throws IOException {
        int exitCode;
        exitCode = new Shell.Safe(shell).exec(
                cmd,
                input,
                com.jcabi.log.Logger.stream(Level.INFO, this), com.jcabi.log.Logger.stream(Level.WARNING, this)
        );
        logger.info("Command: " + cmd +" exitCode: " + exitCode);

        return exitCode;

    }

}
