package topology.dataStructure;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import commonTool.CommonTool;



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
	 * Key value pairs to store the name and the member. Tha name is the 
	 * vm full name, containing the sub-topology name.
	 */
	@JsonIgnore
	public Map<String, Member> memberIndex = new HashMap<String, Member>();
	
	@JsonIgnore
	public void rmMember(String vmName){
		if(vmName == null)
			return ;
		String vmFullName = null;
		for(int mi = 0 ; mi<members.size() ; mi++){
			Member curMember = members.get(mi);
			if(curMember.absVMName.equals(vmName.trim())){
				///delete it according to the VM full name
				vmFullName = curMember.vmName;
				CommonTool.rmKeyInMap(this.memberIndex, vmFullName);
				members.remove(mi--);
				break;
			}
		}
		if(vmFullName == null)
			return ;
		////update the adj members
		for(int mi = 0 ; mi<members.size() ; mi++){
			Member curMember = members.get(mi);
			CommonTool.rmKeyInMap(curMember.adjacentNodes, vmFullName);
		}
	}
}
