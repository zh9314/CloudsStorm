package topologyAnalysis.dataStructure;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class Eth {
	public String name;
	public String connectionName;
	public String subnetName;
	public String address;
	
	//During the format checking, these information is updated.
	//This information is only valid, if the eth belongs to a subnet.
	@JsonIgnore
	public Subnet subnet;
	
	@JsonIgnore
	public SubConnectionPoint scp;
}
