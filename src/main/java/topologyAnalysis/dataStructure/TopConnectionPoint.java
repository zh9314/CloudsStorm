package topologyAnalysis.dataStructure;

import com.fasterxml.jackson.annotation.JsonIgnore;

//This is the connection point definition in the top level description file.
public class TopConnectionPoint{
	public String vmName;
	//public String portName;
	public String netmask;
	public String address;
	
	//Used for recording the eth name on the VM.
	//When the other part of this connection is failed. We need deleted this ethName.
	//Originally, it should be null. This field is just used for controlling.
	public String ethName;
	
	
	//The format should be 'IP-IP'. The IP in the front is smaller.
	//Only the sub-topology marked as 'scaling' can set this field.
	public String scalingPool;
	
	//Point to the VM that this connection point belongs to. 
	//Main goal is to get the public address after provisioning.
	@JsonIgnore
	public VM belongingVM;
	
	
	//Record the address of the peer in this connection.
	//Used for generating the available scaling pool.
	//@JsonIgnore
	//public String peerAddress;
	
	//Record the address of the peer in this connection.
	//Used for generating the available scaling pool.
	@JsonIgnore
	public TopConnectionPoint peerTCP;
	
}