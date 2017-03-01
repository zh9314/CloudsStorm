
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

import topologyAnalysis.TopologyAnalysisMain;


public class testTopologyLoading {
	
	private static final Logger logger = Logger.getLogger(testTopologyLoading.class);
	

	public static void main(String[] args) {
		TopologyAnalysisMain tam = new TopologyAnalysisMain("zh_all_test.yml");
		if(!tam.fullLoadWholeTopology())
		{
			return;
		}
		
		Map<String, String> test = tam.generateControlOutputs(); 
		for(int i = 0 ; i<tam.wholeTopology.topologies.size() ; i++){
			if(tam.wholeTopology.topologies.get(i).scalingAddressPool != null){
				for(Entry<String, Boolean> entry: tam.wholeTopology.topologies.get(i).scalingAddressPool.entrySet()){
					System.out.println(entry.getKey());
					System.out.println(entry.getValue());
				}
			}
		}
		//tam.overwiteControlFiles();
		/*for(Entry<String, String> entry: test.entrySet()){
			System.out.println(entry.getKey());
			System.out.println(entry.getValue());
		}*/
		
		//tam.overwiteControlFiles();
		
		
		logger.debug("Loaded!");
	}

}
