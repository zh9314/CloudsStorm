package provisioning.engine.SEngine;

import java.util.UUID;

import org.apache.log4j.Logger;

import provisioning.credential.Credential;
import provisioning.credential.SSHKeyPair;
import provisioning.database.BasicVMMetaInfo;
import provisioning.database.Database;
import provisioning.database.EC2.EC2Database;
import provisioning.engine.VEngine.EC2.EC2VEngine;
import topology.dataStructure.EC2.EC2SubTopology;
import topology.dataStructure.EC2.EC2VM;
import topology.description.actual.SubTopology;
import topology.description.actual.SubTopologyInfo;

public class EC2SEngine extends SEngine {
	
	private static final Logger logger = Logger.getLogger(EC2SEngine.class);
	
	/**
	 * 1. Update the AMI information.
	 * 2. Update the endpoint information.
	 * 3. To be completed, check the validity of nodeType.
	 */
	@Override
	public boolean runtimeCheckandUpdate(SubTopologyInfo subTopologyInfo,
			Database database) {
		
		///general update
		if(!super.runtimeCheckandUpdate(subTopologyInfo, database))
			return false;
		
		///Update the endpoint information
		EC2Database ec2Database = (EC2Database)database;
		EC2SubTopology ec2SubTopology = (EC2SubTopology)subTopologyInfo.subTopology;
		String domain = subTopologyInfo.domain.trim().toLowerCase();
		
		for(int vi = 0 ; vi < ec2SubTopology.VMs.size() ; vi++){
			EC2VM curVM = ec2SubTopology.VMs.get(vi);
			String vmType = curVM.nodeType.toLowerCase().trim();
            String OS = curVM.OStype.toLowerCase().trim();
            BasicVMMetaInfo ec2VMMetaInfo = null;
            if((ec2VMMetaInfo = ((BasicVMMetaInfo)ec2Database.getVMMetaInfo(domain, OS, vmType))) == null){
            	 	logger.error("The EC2 VM meta information for 'OStype' '" 
            	 			+ curVM.OStype + "' and 'nodeType' '" 
            	 			+ curVM.nodeType + "' in domain '" + domain 
            	 			+ "' is not known!");
                 return false;
            }
            if(ec2VMMetaInfo.extraInfo != null)
	        		curVM.AMI = ec2VMMetaInfo.extraInfo.get("AMI");
	        if(curVM.AMI == null){
	        		logger.error("There must be 'AMI' information in EC2Database!");
	        		return false;
	        }
		}
		
		return true;
	}
	
	@Override
	public boolean createAccessSSHKey(SubTopologyInfo subTopologyInfo, 
			Credential credential, Database database) {
		SubTopology ec2SubTopology = subTopologyInfo.subTopology;
		//create a key pair for this sub-topology, if there is none.
		if(ec2SubTopology.accessKeyPair == null){
			String keyPairId = UUID.randomUUID().toString();
			String publicKeyId = "publicKey-"+keyPairId;
			String privateKeyString = 
					EC2VEngine.createSSHKeyPair(subTopologyInfo, 
							credential, publicKeyId);
			if(privateKeyString == null){
				logger.error("Unexpected error for creating ssh key pair for sub-topology '"+ec2SubTopology.topologyName+"'!");
				return false;
			}
			subTopologyInfo.sshKeyPairId = keyPairId;
			ec2SubTopology.accessKeyPair = new SSHKeyPair();
			ec2SubTopology.accessKeyPair.publicKeyId = publicKeyId;
			ec2SubTopology.accessKeyPair.privateKeyString = privateKeyString;
			ec2SubTopology.accessKeyPair.SSHKeyPairId = keyPairId;
		}
		return true;
	}
	
	@Override
	public boolean provision(SubTopologyInfo subTopologyInfo,
			Credential credential, Database database) {
		/////Create a common Subnet first
		if(!EC2VEngine.createCommonSubnet(subTopologyInfo, credential)){
			logger.error("Cannot create common Subnet!");
			subTopologyInfo.logsInfo.put("ERROR", "Cannot create common Subnet!");
			return false;
		}
		if(super.provision(subTopologyInfo, credential, database))
			return true;
		else
			return false;
	}
	

	@Override
	public boolean supportStop(SubTopologyInfo subTopologyInfo) {
		return true;
	}


	@Override
	public boolean delete(SubTopologyInfo subTopologyInfo,
			Credential credential, Database database) {
		////leverage VEngine to delete all the VMs
		if(!super.delete(subTopologyInfo, credential, database))
			return false;
		
		if(!EC2VEngine.deleteVPC(subTopologyInfo, credential))
			return false;
		
		return true;
	}
	
	



}
