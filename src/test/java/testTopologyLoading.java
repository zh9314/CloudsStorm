
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;

import commonTool.Log4JUtils;

import topologyAnalysis.TopologyAnalysisMain;


public class testTopologyLoading {
	
	private static final Logger logger = Logger.getLogger(testTopologyLoading.class);
	

	public static void main(String[] args) {
		   
	    Log4JUtils.setErrorLogFile("test.log");
	      
		TopologyAnalysisMain tam = new TopologyAnalysisMain("ES/standard2/zh_all_test.yml");
		if(!tam.fullLoadWholeTopology())
		{
			logger.error("sth wrong!");
			return;
		}
		
		Map<String, String> test = tam.generateControlOutputs(); 
		for(int i = 0 ; i<tam.wholeTopology.topologies.size() ; i++){
			if(tam.wholeTopology.topologies.get(i).scalingAddressPool != null){
				for(Entry<String, Boolean> entry: tam.wholeTopology.topologies.get(i).scalingAddressPool.entrySet()){
					logger.info(entry.getKey());
					logger.info(entry.getValue());
				}
			}
		}
		//tam.overwiteControlFiles();
		for(Entry<String, String> entry: test.entrySet()){
			logger.info(entry.getKey());
			logger.info(entry.getValue());
		}
		
		//tam.overwiteControlFiles();
		
		
		logger.debug("Loaded!");
	}

}
