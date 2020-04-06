package data;
import java.util.ArrayList;

public class InfoData {
	
	public ArrayList<STInfo> SubTopologies;
	
	public ArrayList<SBInfo> Subnets;
	
	public class STInfo {
		
		public String Name;
		
		public String CloudProvider;
		
		public String DataCentre;
		
		public ArrayList<VMInfo> VMs; 
		
		public class VMInfo {
			
			public String Name;
			
			public String CPU;
			
			public String MEM;

			public String OSType;
			
			public String PublicIP;
			
			public String Status;
			
			////used for visualization
			public String Color;
		}
	}
	
	public class SBInfo {
		
		public String Name;
		
		public String Subnet;
		
		public String Netmask;
		
		public ArrayList<MBInfo> Memebers;
		
		public class MBInfo {
			
			public String VMName;
			
			public String PrivateIP;
		}
	}
	
}
