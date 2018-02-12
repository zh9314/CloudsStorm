package topology.dataStructure;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * This is a member definition of the VM in one subnet.	
 * @author huan
 *
 */
public class Member {
	/**
	 * The name in the definition of SubTopology, such as "hadoop_3nodes_1.Node1".
	 */
	public String vmName;
	
	/**
	 * This is the real VM name
	 */
	@JsonIgnore
	public String absVMName;
	
	/**
	 * The IP address of this VM in this subnet.
	 */
	public String address;
	
	/**
	 * Indicate the adjacent VMs that connects with this VM.
	 * In order to create the non-logic connection according to the 
	 * subnet.
	 */
	@JsonIgnore
	public Map<String, Member> adjacentNodes = new HashMap<String, Member>();
}
