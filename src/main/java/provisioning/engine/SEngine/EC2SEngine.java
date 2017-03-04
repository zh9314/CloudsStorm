package provisioning.engine.SEngine;

import provisioning.credential.Credential;
import provisioning.database.Database;
import topologyAnalysis.dataStructure.SubTopology;
import topologyAnalysis.dataStructure.EC2.EC2SubTopology;

public class EC2SEngine implements SEngineCoreMethod{

	@Override
	public boolean provision(SubTopology subTopology, Credential credential, Database database) {
		EC2SubTopology ec2SubTopology = (EC2SubTopology)subTopology;
		return false;
	}

	@Override
	public boolean stop(SubTopology subTopology, Credential credential, Database database) {
		
		return false;
	}

}
