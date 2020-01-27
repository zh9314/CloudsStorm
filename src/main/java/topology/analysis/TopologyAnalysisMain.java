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
package topology.analysis;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import lambdaInfrs.credential.SSHKeyPair;

import org.apache.log4j.Logger;

import commonTool.CommonTool;
import java.io.IOException;
import topology.dataStructure.Member;
import topology.dataStructure.Subnet;
import topology.description.actual.ActualConnection;
import topology.description.actual.ActualConnectionPoint;
import topology.description.actual.SubTopology;
import topology.description.actual.SubTopologyInfo;
import topology.description.actual.TopTopology;
import topology.description.actual.VM;

public class TopologyAnalysisMain {

    private static final Logger logger = Logger.getLogger(TopologyAnalysisMain.class);

    public TopTopology wholeTopology;

    // The absolute file path of the top topology.
    private String topTopologyFilePath;

    public TopologyAnalysisMain(String topTopologyPath) {
        topTopologyFilePath = topTopologyPath;
    }

    /**
     * True indicates the topology is loaded successfully.  <br/>
     * All the files should be in the same directory and ending up with '.yml'
     * <br/>
     * This method is only used for generate the user output.
     */
    public boolean simpleLoadWholeTopology() {
        wholeTopology = new TopTopology();
        if (!wholeTopology.loadTopology(topTopologyFilePath)) {
            return false;
        }

        String topologiesDir = CommonTool.getPathDir(topTopologyFilePath);
        for (int i = 0; i < wholeTopology.topologies.size(); i++) {
            SubTopologyInfo tmpInfo = wholeTopology.topologies.get(i);
            String topologyPath = topologiesDir + tmpInfo.topology + ".yml";
            if (!tmpInfo.loadSubTopology(topologyPath)) {
                logger.error("One of the sub-topology cannot be loaded!");
                return false;
            }
            tmpInfo.pointBack2TTI = wholeTopology;
            if (tmpInfo.sshKeyPairId != null) {
                try {
                    tmpInfo.subTopology.accessKeyPair = new SSHKeyPair();
                    String sshKeyDir = topologiesDir + tmpInfo.sshKeyPairId + File.separator;
                    if (!tmpInfo.subTopology.accessKeyPair.loadSSHKeyPair(tmpInfo.sshKeyPairId, sshKeyDir)) {
                        logger.error("Error when loading SSH key Pair from " + sshKeyDir);
                        return false;
                    }
                } catch (IOException ex) {
                    logger.error(ex);
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * True indicates the topology is loaded successfully.  <br/>
     * All the files should be in the same directory and ending up with '.yml'
     * <br/>
     * After loading from the topologies, the full information of the topology
     * will be completed and the format will be checked. This method is used for
     * doing real interacting with cloud and generating control output.
     * @return 
     */
    public boolean fullLoadWholeTopology() {
        if (!simpleLoadWholeTopology()) {
            return false;
        }

        if (!formatChecking()) {
            return false;
        }

        if (!completeFullInfo()) {
            return false;
        }

        return true;
    }

    /**
     * This method is used for completing all the information after loading a
     * topology and do format checking.
     *
     */
    private boolean completeFullInfo() {

        if (wholeTopology.topologies == null) {
            logger.error("At least one topology should be defined in the top level description!");
            return false;
        }

        ///complete the VM index table
        for (int si = 0; si < wholeTopology.topologies.size(); si++) {
            SubTopology curST = wholeTopology.topologies.get(si).subTopology;
            ArrayList<VM> vms = curST.getVMsinSubClass();
            if (vms == null) {
                logger.error("There must be one VM in a sub-topology at least! See " + curST.topologyName);
                return false;
            }
            for (int vi = 0; vi < vms.size(); vi++) {
                VM curVM = vms.get(vi);
                curVM.ponintBack2STI = wholeTopology.topologies.get(si);
                if (curVM.fake != null && curVM.fake.trim().equalsIgnoreCase("true")) {
                    continue;
                }
                curVM.fake = null;
                if (!wholeTopology.VMIndex.containsKey(curVM.name)) {
                    wholeTopology.VMIndex.put(curVM.name, curVM);
                } else {
                    logger.error("The VM name '" + curVM.name + "' is repeated!");
                    return false;
                }

            }
        }

        //Complete the info of connectors for every sub-topology and VM
        ///complete the self eth name list
        ///complete the subnet info according to the connections
        if (wholeTopology.connections != null) {
            for (int i = 0; i < wholeTopology.connections.size(); i++) {
                ActualConnectionPoint sourceTcp = wholeTopology.connections.get(i).source;
                ActualConnectionPoint targetTcp = wholeTopology.connections.get(i).target;

                //Operating on the source connection point.
                if (!sourceTcp.vmName.contains(".")) {
                    logger.error("The source name of connection " + sourceTcp.vmName + " is not valid!");
                    return false;
                }
                String[] t_VM = sourceTcp.vmName.split("\\.");
                if (t_VM[0].trim().equals("") || t_VM.length != 2) {
                    logger.error("The format of connection point " + sourceTcp.vmName + " is not correct!");
                    return false;
                }
                String VMName = t_VM[1];
                String sourceTopologyName = t_VM[0];
                SubTopologyInfo sti = wholeTopology.subTopologyIndex.get(sourceTopologyName);
                if (sti == null) {
                    logger.error("The sub-topology of connection point " + sourceTcp.vmName + " doesn't exist!");
                    return false;
                }

                //Get the VM in the sub-topology
                VM vmInfo = wholeTopology.VMIndex.get(VMName);
                if (vmInfo == null) {
                    logger.error("There is no VM called " + VMName + " in " + sourceTopologyName);
                    return false;
                }
                sourceTcp.belongingVM = vmInfo;
                sourceTcp.belongingSubT = vmInfo.ponintBack2STI.topology;
                sourceTcp.peerACP = targetTcp;
                if (sti.connectors == null) {
                    sti.connectors = new ArrayList<ActualConnectionPoint>();
                }
                sti.connectors.add(sourceTcp);
                if (vmInfo.vmConnectors == null) {
                    vmInfo.vmConnectors = new ArrayList<ActualConnectionPoint>();
                }
                vmInfo.vmConnectors.add(sourceTcp);

                //Operating on the target connection point.
                if (!targetTcp.vmName.contains(".")) {
                    logger.error("The target name of connection " + targetTcp.vmName + " is not valid!");
                    return false;
                }
                t_VM = targetTcp.vmName.split("\\.");
                if (t_VM[0].trim().equals("") || t_VM.length != 2) {
                    logger.error("The format of connection point " + targetTcp.vmName + " is not correct!");
                    return false;
                }
                VMName = t_VM[1];
                String targetTopologyName = t_VM[0];
                sti = wholeTopology.subTopologyIndex.get(targetTopologyName);
                if (sti == null) {
                    logger.error("The sub-topology of connection point " + targetTcp.vmName + " doesn't exist!");
                    return false;
                }

                //Get the VM in the sub-topology
                vmInfo = wholeTopology.VMIndex.get(VMName);
                if (vmInfo == null) {
                    logger.error("There is no VM called " + VMName + " in " + targetTopologyName);
                    return false;
                }
                targetTcp.belongingVM = vmInfo;
                targetTcp.belongingSubT = vmInfo.ponintBack2STI.topology;
                targetTcp.peerACP = sourceTcp;
                if (sti.connectors == null) {
                    sti.connectors = new ArrayList<ActualConnectionPoint>();
                }
                sti.connectors.add(targetTcp);
                if (vmInfo.vmConnectors == null) {
                    vmInfo.vmConnectors = new ArrayList<ActualConnectionPoint>();
                }
                vmInfo.vmConnectors.add(targetTcp);

                /*if(targetTopologyName.equals(sourceTopologyName)){
					logger.error("The two connection points of '"+wholeTopology.connections.get(i).name+"' must come from two different topologies!");
					return false;
				}*/
                ////complete the subnet info. put all the connections into a 
                ////subnet. if there is no such subnet, then create one.
                int netmaskNum = Integer.valueOf(targetTcp.netmask);
                String conSubnet = CommonTool.getSubnet(targetTcp.address, netmaskNum);
                ActualConnection curCon = wholeTopology.connections.get(i);
                if (curCon.logic == null) {
                    curCon.logic = "true";
                }

                if (wholeTopology.subnets == null) {
                    wholeTopology.subnets = new ArrayList<Subnet>();
                    Subnet newSubnet = new Subnet();
                    newSubnet.name = "s1";
                    newSubnet.subnet = conSubnet;
                    newSubnet.netmask = targetTcp.netmask;
                    newSubnet.members = new ArrayList<Member>();
                    Member nm1 = new Member();
                    nm1.vmName = sourceTcp.vmName;
                    nm1.address = sourceTcp.address;
                    Member nm2 = new Member();
                    nm2.vmName = targetTcp.vmName;
                    nm2.address = targetTcp.address;
                    newSubnet.members.add(nm1);
                    newSubnet.members.add(nm2);
                    //// to be the neighbor of each other
                    nm1.adjacentNodes.put(nm2.vmName, nm2);
                    nm2.adjacentNodes.put(nm1.vmName, nm1);

                    newSubnet.memberIndex.put(nm1.vmName, nm1);
                    newSubnet.memberIndex.put(nm2.vmName, nm2);
                    wholeTopology.subnets.add(newSubnet);

                    wholeTopology.subnetIndex.put(newSubnet.name, newSubnet);
                } else {
                    boolean find = false;
                    for (int si = 0; si < wholeTopology.subnets.size(); si++) {
                        Subnet curSubnet = wholeTopology.subnets.get(si);
                        for (int curSi = 0; curSi < curSubnet.members.size(); curSi++) {
                            Member curMb = curSubnet.members.get(curSi);
                            curSubnet.memberIndex.put(curMb.vmName, curMb);
                        }
                        if (curSubnet.subnet.equals(conSubnet)
                                && curSubnet.netmask.equals(targetTcp.netmask)) {
                            find = true;
                            Member ms = null;
                            Member mt = null;
                            if (curSubnet.memberIndex.containsKey(sourceTcp.vmName)) {
                                ////same node but have different address in one subnet
                                if (!curSubnet.memberIndex.get(sourceTcp.vmName).address.equals(sourceTcp.address)) {
                                    logger.error("Same VM '" + sourceTcp.vmName
                                            + "' but have different addresses in one subnet");
                                    return false;
                                }
                                ms = curSubnet.memberIndex.get(sourceTcp.vmName);
                            } else {
                                ms = new Member();
                                ms.vmName = sourceTcp.vmName;
                                ms.address = sourceTcp.address;
                                curSubnet.members.add(ms);
                                curSubnet.memberIndex.put(ms.vmName, ms);
                            }
                            if (curSubnet.memberIndex.containsKey(targetTcp.vmName)) {
                                if (!curSubnet.memberIndex.get(targetTcp.vmName).address.equals(targetTcp.address)) {
                                    logger.error("Same VM '" + targetTcp.vmName
                                            + "' but have different addresses in one subnet");
                                    return false;
                                }
                                mt = curSubnet.memberIndex.get(targetTcp.vmName);
                            } else {
                                mt = new Member();
                                mt.vmName = targetTcp.vmName;
                                mt.address = targetTcp.address;
                                curSubnet.members.add(mt);
                                curSubnet.memberIndex.put(mt.vmName, mt);
                            }

                            ////Now all the Members are set
                            ms.adjacentNodes.put(mt.vmName, mt);
                            mt.adjacentNodes.put(ms.vmName, ms);
                        }
                    }
                    ///if this subnet does not exist, create one
                    if (!find) {
                        Subnet newSubnet = new Subnet();
                        newSubnet.name = wholeTopology.generateSubnetName();
                        newSubnet.subnet = conSubnet;
                        newSubnet.netmask = targetTcp.netmask;
                        newSubnet.members = new ArrayList<Member>();
                        Member nm1 = new Member();
                        nm1.vmName = sourceTcp.vmName;
                        nm1.address = sourceTcp.address;
                        Member nm2 = new Member();
                        nm2.vmName = targetTcp.vmName;
                        nm2.address = targetTcp.address;

                        nm1.adjacentNodes.put(nm2.vmName, nm2);
                        nm2.adjacentNodes.put(nm1.vmName, nm1);

                        newSubnet.members.add(nm1);
                        newSubnet.members.add(nm2);
                        newSubnet.memberIndex.put(nm1.vmName, nm1);
                        newSubnet.memberIndex.put(nm2.vmName, nm2);
                        wholeTopology.subnets.add(newSubnet);

                        wholeTopology.subnetIndex.put(newSubnet.subnet, newSubnet);
                    }
                }

            }
        }

        ///check the subnet and complete the missing non-logic connections
        if (!wholeTopology.completeConInfoFromSubnet()) {
            return false;
        }

        return true;
    }

    /**
     * Generate the Strings for user response.
     *
     * @return The format of the output is a hash map. The element is a
     * key-value pair:  <br/>
     * - Top level description -> key: "topLevel"	value: description content
     * <br/>
     * - Low level description -> key: $subTopologyName value: description
     * content  <br/>
     * If some error happens, return null.
     */
    public Map<String, String> generateUserOutputs() {
        if (wholeTopology == null) {
            logger.warn("Load the topology first!");
            return null;
        }
        Map<String, String> outputs = new HashMap<String, String>();
        Map<String, String> tmpEle;
        if ((tmpEle = wholeTopology.generateUserOutput()) == null) {
            return null;
        }

        outputs.putAll(tmpEle);
        logger.debug("The output String for user is generated!");

        for (int i = 0; i < wholeTopology.topologies.size(); i++) {
            SubTopology subTopology = wholeTopology.topologies.get(i).subTopology;
            if (subTopology == null) {
                logger.error("Please load topologies first!");
                return null;
            }
            Map<String, String> curEle;
            if ((curEle = subTopology.generateUserOutput()) == null) {
                return null;
            }
            outputs.putAll(curEle);
        }
        return outputs;
    }

    /**
     * Generate the Strings for controlling usage.  <br/>
     *
     * @return The format of the output is a hash map. The element is a
     * key-value pair:  <br/>
     * - Top level description -> key: "topLevel"	value: description content
     * <br/>
     * - Low level description -> key: $subTopologyName value: description
     * content  <br/>
     * If some error happens, return null.
     */
    public Map<String, String> generateControlOutputs() {
        if (wholeTopology == null) {
            logger.warn("Load the topology first!");
            return null;
        }
        Map<String, String> outputs = new HashMap<String, String>();
        Map<String, String> tmpEle;
        if ((tmpEle = wholeTopology.generateControlOutput()) == null) {
            return null;
        }

        outputs.putAll(tmpEle);
        logger.debug("The output String of top level topology for controlling is generated!");

        for (int i = 0; i < wholeTopology.topologies.size(); i++) {
            SubTopology subTopology = wholeTopology.topologies.get(i).subTopology;
            if (subTopology == null) {
                logger.error("Please load topologies first!");
                return null;
            }
            Map<String, String> curEle;
            if ((curEle = subTopology.generateControlOutput()) == null) {
                return null;
            }
            outputs.putAll(curEle);
            logger.debug("The output String of sub-topology " + subTopology.topologyName + " for controlling is generated!");
        }
        return outputs;
    }

    /**
     * Generate the output yaml file containing all the information. The
     * generated files is used for controlling. The content will be written
     * directly back to the original files.
     *
     */
    public boolean overwiteControlFiles() {
        if (!wholeTopology.overwirteControlOutput()) {
            return false;
        }

        logger.debug("Controlling information for top level description is overwritten");

        for (int i = 0; i < wholeTopology.topologies.size(); i++) {
            SubTopology subTopology = wholeTopology.topologies.get(i).subTopology;
            if (subTopology == null) {
                logger.error("Please load topologies first!");
                return false;
            }

            if (!subTopology.overwirteControlOutput()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Check the format and validate the description files. The detailed
     * checking items are described as below: <br/>
     * {@link topology.description.actual.TopTopology#formatChecking(String) Top level description format}
     * <br/>
     * {@link topology.description.actual.SubTopology#formatChecking(String) Low level description format}
     * <br/>
     * Tips:  <br/>
     * - When some field is not needed, do not leave it as blank. One option is
     * to put the value as "null". Another option is to make the name of the
     * field not appearing in the description. <br/>
     * - All of the field names are case sensitive. <br/>
     * - Some value of the fields are not case sensitive. Detailed information
     * can be found through links above. Only the case sensitive ones are
     * specified explicitly.
     */
    private boolean formatChecking() {
        if (!wholeTopology.formatChecking(null)) {
            return false;
        }

        logger.debug("Top level concrete actual description is valid!");

        for (int si = 0; si < wholeTopology.topologies.size(); si++) {
            String status = wholeTopology.topologies.get(si).status.trim().toLowerCase();
            if (!wholeTopology.topologies.get(si).subTopology.commonFormatChecking(status)) {
                return false;
            }
            if (!wholeTopology.topologies.get(si).subTopology.formatChecking(status)) {
                return false;
            }
            logger.debug("The description of sub-topology '" + wholeTopology.topologies.get(si).topology + "' is valid!");
        }

        return true;
    }

}
