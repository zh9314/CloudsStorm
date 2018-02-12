package topology.dataStructure.ExoGENI;

import com.fasterxml.jackson.annotation.JsonIgnore;

import topology.description.actual.BasicVM;

public class ExoGENIVM extends BasicVM{
	
	
	public String OS_URL;
	public String OS_GUID;
	
	
	@JsonIgnore
	public String endpoint;    ////get this from the sub-topology

}
