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
package topology.dataStructure.EC2;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import topology.description.actual.SubTopology;

public class EC2SubTopology extends SubTopology{
	
	private static final Logger logger = Logger.getLogger(EC2SubTopology.class);
	
	///to define whether the VPC is created. It determines whether the VPC should be deleted
	public String WhetherCreateVPC;
	
	//Indicate different VMs.
	public ArrayList<EC2VM> VMs;
	

	@Override
	public Map<String, String> generateUserOutput() {
		Map<String, String> output = new HashMap<String, String>();
		ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    	try {
    		String yamlString = mapper.writeValueAsString(this);
		String content = "";
        	String [] lines = yamlString.split("\\\n");
        	for(int i = 0 ; i<lines.length ; i++){
        		if(lines[i].trim().equals("subnetName: null")
        			|| lines[i].trim().equals("address: null")
        			|| lines[i].trim().equals("connectionName: null"))
        			continue;
        		if(lines[i].contains(":")){
					String [] contents = lines[i].split(":");
					if(contents[0].trim().equals("vcpId") ||
						contents[0].trim().equals("subnetId") ||
						  contents[0].trim().equals("securityGroupId"))
						continue;
				}
				content += (lines[i]+"\n"); 
        	}
        	output.put("_"+this.topologyName, content);
		} catch (IOException e) {
			e.printStackTrace();
			logger.error(e.getMessage());
			return null;
		}
		return output;
	}

	

	/**
	 * Specific checking for EC2 sub-topology.
	 * The checking items includes: <br/>
	 * 1. The 'instanceId', 'vpcId', 'subnetId' and 'securityGroupId' should be null, 
	 * if the topology status is 'fresh'. <br/>
	 * 2. The 'instanceId', 'vpcId', 'subnetId' and 'securityGroupId' must not be null, 
	 * if the topology status is 'running'. <br/>
	 * 3. If the 'diskSize' is null, then the default value is 8. <br/>
	 * 4. The value of 'diskSize', whose unit is GigaBytes, must be positive. 
	 * For EC2, the 'diskSize' cannot be smaller than 8.  <br/>
	 * 
	 * 
	 */
	@Override
	public boolean formatChecking(String topologyStatus) {
		
		if(WhetherCreateVPC == null)
			WhetherCreateVPC = "false";
		for(int vmi = 0 ; vmi < this.VMs.size() ; vmi++){
			EC2VM curVM = VMs.get(vmi);
			if(topologyStatus.equals("fresh")){
				if(curVM.instanceId != null){
					logger.error("Some information in VM '"+curVM.name+"' cannot be defined in a 'fresh' sub-topology!");
					return false;
				}
			}
			
			if(topologyStatus.equals("running")){
				if(curVM.instanceId == null
					|| curVM.securityGroupId == null
					|| curVM.vpcId == null
					|| curVM.subnetId == null){
					logger.error("Some information in VM '"+curVM.name+"' must be defined in a 'running' sub-topology!");
					return false;
				}
			}
			
			if(curVM.diskSize == null){
				curVM.diskSize = "8";
			}else{
				try {
					int diskSize = Integer.parseInt(curVM.diskSize);
					if(diskSize<1){
						logger.error("The minimum number for field 'diskSize' of EC2VM '"+curVM.name+"' is 1!");
						return false;
					}
					if(diskSize > 16000){   ///Maxium is 16TB.
						logger.error("Field 'diskSize' of EC2VM '"+curVM.name+"' out of the range according to current capability!");
						return false;
					}
					} catch (NumberFormatException e) {
						logger.error("Field 'diskSize' of EC2VM '"+curVM.name+"' must be a positive number!");
						return false;
					}
			}
			
			//check the IOPS
			if(curVM.IOPS == null){
				curVM.IOPS = "0";
			}else{
				try {
					int IOPS = Integer.parseInt(curVM.IOPS);
					if(IOPS<0){
						logger.error("Field 'IOPS' of EC2VM '"+curVM.name+"' must be positive!");
						return false;
					}
					if(IOPS>20000){
						logger.error("Field 'IOPS' of EC2VM '"+curVM.name+"' out of the range according to current capability!");
						return false;
					}
					} catch (NumberFormatException e) {
						logger.error("Field 'IOPS' of EC2VM '"+curVM.name+"' must be a positive number!");
						return false;
					}
			}
		}
		
		return true;
	}




}
