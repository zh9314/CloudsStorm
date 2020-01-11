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
package lambdaInfrs.engine.SEngine;

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import lambdaInfrs.credential.Credential;
import lambdaInfrs.credential.ExoGENICredential;
import lambdaInfrs.database.BasicVMMetaInfo;
import lambdaInfrs.database.Database;
import lambdaInfrs.database.ExoGENI.ExoGENIDatabase;
import lambdaInfrs.engine.VEngine.ExoGENI.ExoGENIAgent;
import lambdaInfrs.engine.VEngine.ExoGENI.adapter.ExoGENIVEngine_provision;
import lambdaInfrs.engine.VEngine.adapter.VEngineAdapter;

import org.apache.log4j.Logger;

import topology.dataStructure.ExoGENI.ExoGENISubTopology;
import topology.dataStructure.ExoGENI.ExoGENIVM;
import topology.description.actual.SubTopologyInfo;
import topology.description.actual.VM;

public class ExoGENISEngine extends SEngine {

    private static final Logger logger = Logger.getLogger(ExoGENISEngine.class);

    /**
     * 1. Update the endpoint 2. Update the OSurl and OSguid for each VM 3.
     * Every VM creates the corresponding VEngine. 4. Generate the sliceName.
     * @param subTopologyInfo
     * @param database
     * @return 
     */
    @Override
    public boolean runtimeCheckandUpdate(SubTopologyInfo subTopologyInfo,
            Database database) {
        ///general update
        if (!super.runtimeCheckandUpdate(subTopologyInfo, database)) {
            return false;
        }

        ExoGENIDatabase exoGENIDatabase = (ExoGENIDatabase) database;
        ExoGENISubTopology exoGENISubTopology = (ExoGENISubTopology) subTopologyInfo.subTopology;

        String domain = subTopologyInfo.domain.trim().toLowerCase();
        for (int vi = 0; vi < exoGENISubTopology.VMs.size(); vi++) {
            ExoGENIVM curVM = (ExoGENIVM) exoGENISubTopology.VMs.get(vi);
            String OS = curVM.OStype.trim().toLowerCase();
            String vmType = curVM.nodeType.toLowerCase().trim();
            curVM.endpoint = subTopologyInfo.endpoint;
            BasicVMMetaInfo exoGENIVMMetaInfo = null;
            if ((exoGENIVMMetaInfo = ((BasicVMMetaInfo) exoGENIDatabase.getVMMetaInfo(domain, OS, vmType))) == null) {
                logger.error("The ExoGENI VM meta information for 'OStype' '"
                        + curVM.OStype + "' and 'nodeType' '" + curVM.nodeType + "' in domain '"
                        + domain + "' is not known!");
                return false;
            }
            if (exoGENIVMMetaInfo.extraInfo != null) {
                curVM.OS_GUID = exoGENIVMMetaInfo.extraInfo.get("OS_GUID");
                curVM.OS_URL = exoGENIVMMetaInfo.extraInfo.get("OS_URL");
            }
            if (curVM.OS_GUID == null) {
                logger.error("There must be 'OS_GUID' information in EC2Database!");
                return false;
            }
            if (curVM.OS_URL == null) {
                logger.error("There must be 'OS_URL' information in EC2Database!");
                return false;
            }
        }

        ////only when there is no slice name, we need to generate one.
        ////or when it is failed, we also need to generate one.
        if (exoGENISubTopology.sliceName == null || subTopologyInfo.status.equals("failed")) {
            exoGENISubTopology.sliceName = exoGENISubTopology.topologyName + "-" + UUID.randomUUID().toString();
        }

        return true;
    }

    @Override
    public boolean provision(SubTopologyInfo subTopologyInfo,
            Credential credential, Database database) {
        if (!subTopologyInfo.status.trim().toLowerCase().equals("fresh")
                && !subTopologyInfo.status.trim().toLowerCase().equals("deleted")) {
            logger.warn("The sub-topology '" + subTopologyInfo.topology + "' is not in the status of 'fresh' or 'deleted'!");
            return false;
        }
        if (createSubTopology(subTopologyInfo, credential, database)) {
            return true;
        } else {
            return false;
        }
    }

    private boolean createSubTopology(SubTopologyInfo subTopologyInfo, Credential credential, Database database) {
        ExoGENISubTopology exoGENISubTopology = (ExoGENISubTopology) subTopologyInfo.subTopology;
        ExoGENICredential exoGENICredential = (ExoGENICredential) credential;
        ExoGENIDatabase exoGENIDatabase = (ExoGENIDatabase) database;

        ExoGENIAgent exoGENIAgent = new ExoGENIAgent(exoGENIDatabase,
                exoGENICredential, subTopologyInfo.domain);
        logger.debug("Endpoint for '" + subTopologyInfo.topology
                + "' is " + subTopologyInfo.endpoint);

        boolean result = exoGENIAgent.createSlice(exoGENISubTopology);
        if (!result) {
            logger.error("Error happens during provisioning for " + subTopologyInfo.topology + "!");
            return false;
        }

        ArrayList<VM> xVMs = exoGENISubTopology.getVMsinSubClass();
        try {
            ///configure the ssh account 
            ////deploy the environment for the VM if there is
            ArrayList<VEngineAdapter> vEAs = new ArrayList<>();
            ExecutorService executor4vm = Executors.newFixedThreadPool(xVMs.size());
            for (int vi = 0; vi < xVMs.size(); vi++) {
                VM curVM = xVMs.get(vi);
                ExoGENIVEngine_provision v_confAfterProvision = new ExoGENIVEngine_provision(
                        curVM, credential, database);
//				vEAs.add(v_confAfterProvision);
                v_confAfterProvision.run();
                executor4vm.execute(v_confAfterProvision);
            }
            executor4vm.shutdown();

            int count = 0;
            while (!executor4vm.awaitTermination(2, TimeUnit.SECONDS)) {
                count++;
                if (count > 2000 * xVMs.size()) {
                    logger.error("Some VM cannot be configured after provisioning!");
                    return false;
                }
            }

            if (!checkVEnginesResults(vEAs)) {
                return false;
            }

        } catch (InterruptedException e) {
            e.printStackTrace();
            logger.error("Unexpected error!");
            return false;
        }

        return true;
    }

    @Override
    public boolean supportStop(SubTopologyInfo subTopologyInfo) {
        return false;
    }

    @Override
    public boolean supportSeparate() {
        return false;
    }

    @Override
    public boolean delete(SubTopologyInfo subTopologyInfo,
            Credential credential, Database database) {
        ExoGENISubTopology exoGENISubTopology = (ExoGENISubTopology) subTopologyInfo.subTopology;
        ExoGENICredential exoGENICredential = (ExoGENICredential) credential;
        ExoGENIDatabase exoGENIDatabase = (ExoGENIDatabase) database;

        ExoGENIAgent exoGENIAgent = new ExoGENIAgent(exoGENIDatabase,
                exoGENICredential, subTopologyInfo.domain);
        logger.debug("Endpoint for '" + subTopologyInfo.topology + "' is " + subTopologyInfo.endpoint);

        boolean result = exoGENIAgent.deleteSlice(exoGENISubTopology);
        ////clear all the information
        for (int vi = 0; vi < exoGENISubTopology.VMs.size(); vi++) {
            ExoGENIVM curVM = (ExoGENIVM) exoGENISubTopology.VMs.get(vi);
            curVM.publicAddress = null;
            if (curVM.vmConnectors != null) {
                for (int vapi = 0; vapi < curVM.vmConnectors.size(); vapi++) {
                    curVM.vmConnectors.get(vapi).ethName = null;
                }
            }
            if (curVM.selfEthAddresses != null) {
                for (Map.Entry<String, String> entry : curVM.selfEthAddresses.entrySet()) {
                    curVM.selfEthAddresses.put(entry.getKey(), null);
                }
            }
            curVM.fake = null;
        }
        exoGENISubTopology.sliceName = null;

        return result;
    }

}
