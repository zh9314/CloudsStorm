package topology.dataStructure.EGI;


import com.fasterxml.jackson.annotation.JsonIgnore;

import topology.description.actual.BasicVM;

public class EGIVM extends BasicVM {
	
	///Identify the URI link of the VM resource
	public String VMResourceID;
	
	
	////Denote the occi id of the os and type of the vm
	@JsonIgnore
	public String OS_occi_ID;
	
	@JsonIgnore
	public String Res_occi_ID;
		
	

}
