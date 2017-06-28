package topologyAnalysis.dataStructure;

import java.util.ArrayList;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class SubTopologyInfo {

    /**
     * The name of the topology. It is also the file name of the low level
     * description.
     */
    public String topology;

    /**
     * Currently, only support "EC2" and "ExoGENI". This is not case sensitive.
     */
    public String cloudProvider;

    //Indicate the location of the sub-topology
    public String domain;

    //This field is valid only when the sub-toopology is 'failed'.
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
    public String tag;

    /**
     * This field is used for record some information of the sub-topology.  <br/>
     * <p/>
     * &nbsp; 1. When the status is fresh, it's nothing.  <br/>
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
    public String statusInfo = "";

    //Indicate where this sub-topology is copied from, if this is a scaled one. 
    public String copyOf;

    ////Identify the key pairs used to access this sub-topology. This is mainly used by provisioner itself.
    public String sshKeyPairId;

    //Point to the origin sub-topology, if this is a scaled one.
    @JsonIgnore
    public SubTopologyInfo fatherTopology;

    //Point to the detailed description of the sub-topology.
    @JsonIgnore
    public SubTopology subTopology;

    //This is completed from the field of 'connections'.
    //This is useful when this sub-topology is scaling part and configure the 
    //inter domain connection.
    @JsonIgnore
    public ArrayList<TopConnectionPoint> connectors;

    //This is the scaling address pool.
    //The key is a private IP address and the value is to identify whether this address is available. 
    @JsonIgnore
    public Map<String, Boolean> scalingAddressPool;

    //Used for ssh configuration. equal to the field of 'userName' and 'publicKeyString' in top-topology.
    @JsonIgnore
    public String userName;
    @JsonIgnore
    public String publicKeyString;

}
