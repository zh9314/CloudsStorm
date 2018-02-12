package provisioning.engine.VEngine.ExoGENI;

import java.util.ArrayList;

import org.apache.log4j.Logger;

import provisioning.credential.ExoGENICredential;
import provisioning.database.BasicDCMetaInfo;
import provisioning.database.ExoGENI.ExoGENIDatabase;
import topology.dataStructure.ExoGENI.ExoGENISubTopology;
import topology.dataStructure.ExoGENI.ExoGENIVM;
import topology.description.actual.VM;

public class ExoGENIAgent {
	
	private static final Logger logger = Logger.getLogger(ExoGENIAgent.class);
	
	public String cloudEntry;
	public String userKeyPath, keyAlias, keyPass;
	
	/**
	 * The ExoGENI agent will first choose the local entry for the target
	 * data center(domain). This is to reduce the workload of the global entry.
	 * @param exogeniDatabase
	 * @param exogeniCredential
	 * @param targetDC
	 */
	public ExoGENIAgent(ExoGENIDatabase exogeniDatabase,
			ExoGENICredential exogeniCredential, String targetDC){
		BasicDCMetaInfo dcMetaInfo = (BasicDCMetaInfo)exogeniDatabase
												.getDCMetaInfo(targetDC);
		if(dcMetaInfo.extraInfo != null
				&& dcMetaInfo.extraInfo.containsKey("LocalEntry"))
			this.cloudEntry = dcMetaInfo.extraInfo.get("LocalEntry");
		else
			this.cloudEntry = exogeniDatabase.GlobalEntry;
		this.userKeyPath = exogeniCredential.userKeyPath;
		this.keyAlias = exogeniCredential.keyAlias;
		this.keyPass = exogeniCredential.keyPassword;
	}
	
	public boolean createSlice(ExoGENISubTopology exoGENISubTopology){
		INDLGenerator indlGenerator = new INDLGenerator();
		ArrayList<ExoGENIVM> nodeSet = new ArrayList<ExoGENIVM>();
		for(int ni = 0 ; ni<exoGENISubTopology.VMs.size() ; ni++)
			nodeSet.add((ExoGENIVM)exoGENISubTopology.VMs.get(ni));
		String indl_s = indlGenerator.generateINDL(nodeSet,
								Integer.valueOf(exoGENISubTopology.duration));
		ExoGENIRPCConnector rpc = new ExoGENIRPCConnector(cloudEntry, userKeyPath,
			keyAlias, keyPass);
		
		String sliceName = exoGENISubTopology.sliceName;
		try {
			rpc.createSlice(sliceName, indl_s, 
					exoGENISubTopology.accessKeyPair.publicKeyString);
			
			int QuerryCount = 0;
			while(QuerryCount < 300)
			{
				Thread.sleep(1000);

				String status = rpc.sliceStatus(sliceName);
				
				int statusNow = analysisStatus(status);
				
				Thread.sleep(7000);
				
				if(statusNow == 1)
				{
					boolean addresses = true;
					int begin = status.indexOf('<');
					String xmlStatus = status.substring(begin);
					ArrayList<String> publicIPs = indlGenerator.getPublicIPs(xmlStatus);
					for(int i = 0 ; i<publicIPs.size() ; i++){
						String [] node_ip = publicIPs.get(i).split("::");
						logger.info("Address info -> "+publicIPs.get(i));
						VM curVM = exoGENISubTopology.getVMinSubClassbyName(node_ip[0]);
						if(curVM == null){
							logger.warn("These is no VM called '"+node_ip+"' in "+exoGENISubTopology.topologyName);
							addresses = false;
						}else
							curVM.publicAddress = node_ip[1];
					}
					
					ArrayList<VM> vms = exoGENISubTopology.getVMsinSubClass();
					for(int vi = 0 ; vi < vms.size() ; vi++){
						if(vms.get(vi).publicAddress == null){
							logger.warn("VM '"+vms.get(vi).name+"' in '"
										+exoGENISubTopology.topologyName
										+"' cannot get valid ip address! Querry Again!");
							addresses = false;
						}
					}
					
					if(addresses)
						return addresses;
				}
				if(statusNow == -1)
				{
					logger.error("Sth wrong during creating slices '"+sliceName+"'!");
					break;
				}
				QuerryCount++;
			}

			} catch (Exception e) {
				logger.error("An exception has occurred in creating slice "+sliceName+" : " + e);
				return false;
			}
		return false;
	}
	
	public boolean deleteSlice(ExoGENISubTopology exoGENISubTopology){
		String sliceName = exoGENISubTopology.sliceName;
		ExoGENIRPCConnector rpc = new ExoGENIRPCConnector(cloudEntry, 
				userKeyPath, keyAlias, keyPass);
		try {
			if(rpc.deleteSlice(sliceName)){
				logger.info("Slice "+sliceName+" has been deleted successfully!");
				return true;
			}
			else{
				logger.error("Slice "+sliceName+"does not exist!");
				return false;
			}
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("Error happens during deleting slice '"+sliceName+"' : "+e.getMessage());
			return false;
		}
		
	}
	
	
	////return 0 represents not setup yet. 1 represents setup. -1 represents failed.
	private int analysisStatus(String status)
	{
		String ss = status;
		int seq = ss.indexOf("Status: ");
		if(seq == -1)
			return 0;
		boolean allActive = true;
		while(seq != -1)
		{
			String statusw = ss.substring(seq+8,seq+14);
			if(!statusw.equals("Active"))
				allActive = false;
			if(statusw.equals("Failed"))
				return -1;
			ss = ss.substring(seq+14);
			seq = ss.indexOf("Status: ");
		}
		if(!allActive){
			return 0;
		}
		return 1;
		
	}

}
