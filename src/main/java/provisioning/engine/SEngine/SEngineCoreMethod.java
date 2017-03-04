package provisioning.engine.SEngine;

import provisioning.credential.Credential;
import provisioning.database.Database;
import topologyAnalysis.dataStructure.SubTopology;

public interface SEngineCoreMethod {
	
	/**
	 * This is a method to provision the sub-topology specific to some Cloud.
	 * The status of the sub-topology will be from 'fresh' -> 'running' or 'fresh' -> 'failed'
	 * @param subTopology one of the sub-topology defined in the description files.
	 * credential contains the credentials information to operate the Cloud.
	 * database contains the information of the Cloud, AMI for instance.
	 * @return successful or not. If the provisioning is not successful, 
	 * the error log information can be found in the file, which is set by 
	 * {@link commonTool.Log4JUtils#setErrorLogFile(String) setErrorLogFile}. If the 
	 * provisioning is successful, the controlling information will be written back 
	 * to the original files, for example public address, instance id etc. 
	 */
	public boolean provision(SubTopology subTopology, Credential credential, Database database);
	
	/**
	 * This is a method to delete the topology. All the issues related with 
	 * the topology will also be deleted.
	 * @return 
	 */
	public boolean stop(SubTopology subTopology, Credential credential, Database database);

}
