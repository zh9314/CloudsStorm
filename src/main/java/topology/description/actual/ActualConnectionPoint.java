package topology.description.actual;

import topology.dataStructure.ConnectionPoint;

import com.fasterxml.jackson.annotation.JsonIgnore;

//This is the connection point definition in the top level description file.
public class ActualConnectionPoint extends ConnectionPoint{

	
	//Used for recording the eth name on the VM.
	//When the other part of this connection is failed. We need deleted this ethName.
	//Originally, it should be null. This field is just used for controlling.
	public String ethName;
	
	//Point to the VM that this connection point belongs to. 
	//Main goal is to get the public address after provisioning.
	@JsonIgnore
	public VM belongingVM;
	
	/**
	 * The name of the sub-topology which this VM
	 * belongs to.
	 */
	@JsonIgnore
	public String belongingSubT;
	
	
	//Record the address of the peer in this connection.
	//Used for generating the available scaling pool.
	//@JsonIgnore
	//public String peerAddress;
	
	//Record the address of the peer in this connection.
	//Used for generating the available scaling pool.
	@JsonIgnore
	public ActualConnectionPoint peerACP;
	
}