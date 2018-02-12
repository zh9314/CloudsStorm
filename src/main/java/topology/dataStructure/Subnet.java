package topology.dataStructure;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;



public class Subnet {
	/**
	 * The name of the subnet.
	 */
	public String name;
	
	/**
	 * The actual address of the subnet, such as '192.168.10.0'
	 */
	public String subnet;
	
	/**
	 * The netmask of the subnet. '24' and '255.255.255.0' are equal.
	 */
	public String netmask;
	
	
	public ArrayList<Member> members;
	
	/**
	 * This is a list to record all the connections which belong to 
	 * this subnet.
	 */
	//@JsonIgnore
	//public ArrayList<String> conNames = new ArrayList<String>();
	
	@JsonIgnore
	public Map<String, Member> memberIndex = new HashMap<String, Member>();
}
