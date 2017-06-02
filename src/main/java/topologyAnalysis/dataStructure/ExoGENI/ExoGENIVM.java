package topologyAnalysis.dataStructure.ExoGENI;

import com.fasterxml.jackson.annotation.JsonIgnore;

import topologyAnalysis.dataStructure.VM;

public class ExoGENIVM extends VM{
	
	
	public String OSurl;
	public String OSguid;
	
	
	@JsonIgnore
	public String endpoint;    ////get this from the sub-topology

}
