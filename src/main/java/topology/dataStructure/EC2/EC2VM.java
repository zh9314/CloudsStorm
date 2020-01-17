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

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import topology.description.actual.BasicVM;

public class EC2VM extends BasicVM {

    @JsonProperty("CPU")
    @JsonAlias({"cpu"})
    public String cpu;

    @JsonProperty("mem")
    @JsonAlias({"MEM"})
    public String mem;

    //The unit is GigaByte and must be a positive integer. 
    //This field is only valid when this sub-topology is from EC2. 
    //The default size is 8.
    //The disk size of node in the ExoGENI is fixed.
    public String diskSize;

    //if IOPS < 1000, the disk type will be 'gp2'. if IOPS > 1000, the disk type will be 'io1'
    //default amount is 0.
    public String IOPS;

    //The following fields are used for deleting the sub-topology. 
    //Only the field of instanceId can be written to the response file.
    //All the fields should be written to the control file, which is not returned to user.
    public String vpcId;

    public String subnetId;

    public String securityGroupId;

    public String instanceId;

    public String volumeId;

    public String routeTableId;

    public String internetGatewayId;

    //Used for updating all the information above.
    //@JsonIgnore
    //public EC2Subnet subnetAllInfo;
    //This is the actual public address in EC2 subnet, 
    @JsonIgnore
    public String actualPrivateAddress;

    //This is only valid during provisioning. 
    //This will be loaded depending on the domain, OStype and VMtype.
    @JsonIgnore
    public String AMI;

    @JsonProperty("Price")
    public String price;

    @JsonProperty("availability")
    public String availability;

    @JsonProperty("VEngineClass")
    public String vEngineClass;

    @JsonProperty("VNFType")
    public String vNFType;

    @JsonProperty("OS_URL")
    public String OS_URL;

    @JsonProperty("OS_GUID")
    public String OS_GUID;
           

}
