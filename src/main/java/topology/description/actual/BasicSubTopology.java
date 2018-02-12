package topology.description.actual;

import java.util.ArrayList;

import com.fasterxml.jackson.annotation.JsonIgnore;


public class BasicSubTopology extends SubTopology{
	//Indicate different VMs.
	public ArrayList<BasicVM> VMs;

	@Override @JsonIgnore
	public VM getVMinSubClassbyName(String vmName) {
		if(this.VMs == null)
			return null;
		for(int i = 0 ; i<VMs.size() ; i++){
			if(VMs.get(i).name.equals(vmName)){
				return VMs.get(i);
			}
		}
		return null;
	}

	@Override   @JsonIgnore
	public ArrayList<VM> getVMsinSubClass() {
		if(this.VMs == null || VMs.size() == 0)
			return null;
		ArrayList<VM> vms = new ArrayList<VM>();
		for(int i = 0 ; i<VMs.size() ; i++)
			vms.add(VMs.get(i));
		return vms;
	}

	
	
	
}
