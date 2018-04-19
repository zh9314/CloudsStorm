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
package topology.description.actual;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import commonTool.ClassDB;

public class SubTopologyInfo {
	
	private static final Logger logger = Logger.getLogger(SubTopologyInfo.class);

    /**
     * The name of the topology. It is also the file name of the low level
     * description.
     */
    public String topology;

    /**
     * Currently, support "EC2", "EGI" and "ExoGENI". This is not case sensitive.
     */
    public String cloudProvider;
    
    /**
     * If the sub-topology is from current supported Cloud providers,
     * this field needs to be set, which is the full class path to the 
     * user-defined sub-topology class. 
     */
    public String subTopologyClass;
    

    /**
     * Indicate the Data Center name of the sub-topology
     */
    public String domain;

    //This field is valid only when the sub-topology is 'failed'.
    //If it is null, then the value of 'optionalDomain' is the same with 'domain'.
    //public String optionalDomain;
    //This is used for real provisioning
    @JsonIgnore
    public String endpoint;

    /**
     * Indicate the status of the sub-topology. The string should all in lower
     * case. They can be, <br/>
     * &nbsp; fresh: have never been provisioned. <br/>
     * &nbsp; running: provisioned. <br/>
     * &nbsp; failed: the sub-topology cannot be accessed, because of some
     * errors happening. <br/>
     * &nbsp; stopped: the sub-topology is stopped, can be activated again very
     * fast. Currently can only be done by EC2. <br/>
     * &nbsp; deleted: the sub-topology is deleted, re-provisioning needs some
     * time. <br/>
     */
    public String status;

    //Currently, only 2 tags. The string should all in lower case.
    //fixed: this sub-topology is fixed part; 
    //scaling: define this sub-topology is a scaling part.
    //scaled: define this sub-topology is a scaled part.
    //public String tag;

    /**
     * This field is used for record some information of the sub-topology.  <br/>
     * <p/>
     * &nbsp; 1. When the status is fresh, it can be nothing.  <br/>
     * &nbsp; 2. When the status is running, it records the provisioning time
     * for last running. The unit is in millisecond. <br/>
     * &nbsp; 3. When the status is error, it records the error information.
     * <br/>
     * &nbsp; 4. When the status is failed, it records the system time of
     * failure. <br/>
     * &nbsp; 5. When the status is stopped, it records the system time that
     * this sub-topology is stopped.  <br/>
     * &nbsp; 6. When the status is deleted, it records the system time that
     * this sub-topology is deleted. <br/>
     * &nbsp; This field will not be written to the file which is responded to
     * the user.
     */
    public Map<String, String> logsInfo;

    //Indicate where this sub-topology is copied from, if this is a scaled one. 
    //public String copyOf;

    ////Identify the key pairs used to access this sub-topology. This is mainly used by provisioner itself.
    public String sshKeyPairId;

    //Point to the origin sub-topology, if this is a scaled one.
    //@JsonIgnore
    //public SubTopologyInfo fatherTopology;

    //Point to the detailed description of the sub-topology.
    @JsonIgnore
    public SubTopology subTopology;

    //This is completed from the field of 'connections'.
    //This is useful when this sub-topology is scaling part and configure the 
    @JsonIgnore
    public ArrayList<ActualConnectionPoint> connectors;

    //This is the scaling address pool.
    //The key is a private IP address and the value is to identify whether this address is available. 
    //@JsonIgnore
    //public Map<String, Boolean> scalingAddressPool;

    //Used for ssh configuration. equal to the field of 'userName' and 'publicKeyString' in top-topology.
    @JsonIgnore
    public String userName;
    @JsonIgnore
    public String publicKeyString;
    
    /**
     * Identify whether this sub-topology is a scaled copy.
     * When a scaled copy is deleted, it must be totally deleted from the entire topoloty. 
     */
    public String scaledFrom;
    
    
    public boolean loadSubTopology(String topologyPath) {
    	
    		File testExist = new File(topologyPath);
    		if(!testExist.exists()){
    			logger.error("Sub-topology file " + topologyPath + " does not exist!");
    			return false;
    		}
    		
    		///logs will be refreshed every time
    		this.logsInfo = new HashMap<String, String>();
    		String className = this.subTopologyClass;
    		String cp = this.cloudProvider.trim().toLowerCase();
		ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
		Object xSubTopology = null;
		try{
			Class<?> XSubTopology = ClassDB.getSubTopology(
								cp, className);
			if(XSubTopology == null){
				logger.error("Cannot load class for Cloud '"
						+cloudProvider+"' with class name of '"+className+"'");
				return false;
			}
			xSubTopology = XSubTopology.newInstance();
			xSubTopology = mapper.readValue(new File(topologyPath), XSubTopology);
	        	if(xSubTopology == null){
	        		logger.error("Sub-topology from "+topologyPath+" is invalid!");
	            	return false;
	        	}
	        ((SubTopology)xSubTopology).loadingPath = topologyPath;
	        ((SubTopology)xSubTopology).topologyName = this.topology;
	        ((SubTopology)xSubTopology).cloudProvider = this.cloudProvider;
	        ((SubTopology)xSubTopology).subTopologyClass = this.subTopologyClass;
	        this.subTopology = ((SubTopology)xSubTopology);
	        	logger.info("Sub-topology of Cloud '"+cloudProvider
	        			+"' from "+topologyPath+" is loaded without validation successfully!");
    		}catch (Exception e) {
    			logger.error(e.getMessage());
            e.printStackTrace();
            return false;
		}
		return true;
	}


}
