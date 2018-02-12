package topology.analysis.method;

import java.util.ArrayList;


import topology.description.actual.VM;


public interface SubTopologyMethod {
	
	/**
	 * This is used for getting a specified VM through the super class SubTopology. 
	 */
	public VM getVMinSubClassbyName(String vmName);
	
	
	/**
	 * This is used for getting all the VMs through the super class SubTopology. 
	 */
	public ArrayList<VM> getVMsinSubClass();
	
	
	
	
}
