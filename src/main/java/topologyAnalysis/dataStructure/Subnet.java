package topologyAnalysis.dataStructure;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class Subnet {
	//The name of the subnet. Two subnets cannot have same name in one subnet.
	public String name;
	
	//True denotes this eth belongs to connection, otherwise it belongs to subnet.
	@JsonIgnore
	public boolean connectionOrSubnet;   
	
	public String subnet;
	
	public String netmask;
	
}
