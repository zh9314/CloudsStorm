import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;

import provisioning.database.EC2.AmiInfo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import commonTool.CommonTool;
import topologyAnalysis.dataStructure.SubTopology;
import topologyAnalysis.dataStructure.TopTopology;
import topologyAnalysis.dataStructure.EC2.EC2SubTopology;
import topologyAnalysis.dataStructure.EC2.EC2VM;


public class testYAMLLoading {
	

	private static final Logger logger = Logger.getLogger(testYAMLLoading.class);
	
	public static void main(String[] args) throws FileNotFoundException {
		// TODO Auto-generated method stub
		//TopTopology topTopology = Yaml.loadType(new File("zh_all.yml"), TopTopology.class);
		//logger.debug("YAML is successfully loaded!\n"+topTopology.publicKeyPath);
		/*ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        try {
        	TopTopology topTopology = mapper.readValue(new File("zh_all.yml"), TopTopology.class);
        	logger.debug("YAML: "+topTopology.publicKeyPath);
        	//File yamlFileOut = new File("zh_all_test.yml");
        	String yamlString = "";
        	topTopology.publicKeyPath = "test";
        	//mapper.writeValue(yamlString, topTopology);
        	yamlString = mapper.writeValueAsString(topTopology);
        	System.out.println(yamlString);
        	String content = "";
        	String [] lines = yamlString.split("\\\n");
        	for(int i = 0 ; i<lines.length ; i++){
        		if(lines[i].contains(":")){
					String [] contents = lines[i].split(":");
					if(!contents[0].trim().equals("statusInfo"))
						content += (lines[i]+"\n");
				}else
					content += (lines[i]+"\n"); 
        	}
        	System.out.println(content);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }*/
		/*try {
			Class cls = Class.forName("topologyAnalysis.dataStructure.EC2.EC2SubTopology");
			
			System.out.println("Class found = " + cls.getName());
		    System.out.println("Package = " + cls.getPackage());
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
		/*int x = 5;
		long startTime = System.currentTimeMillis();
		ExecutorService executor = Executors.newFixedThreadPool(x);
		ArrayList<EC2VM> array = new ArrayList<EC2VM>();
		for(int i = 0 ; i<5 ; i++){
			EC2VM curVM = new EC2VM();
			curVM.AMI = "init";
			array.add(curVM);
		}
		for(int i = 0 ; i<5 ; i++){
			Runnable worker = new runTest(array.get(i));
			executor.execute(worker);
		}
		executor.shutdown();
		while (!executor.isTerminated()){
			
		}
		long endTime = System.currentTimeMillis();
		System.out.println("Used: "+(endTime-startTime)/1000);
		for(int i = 0 ; i<array.size() ; i++){
			System.out.println(array.get(i).AMI);
		}*/

		System.out.println(System.getProperty("java.io.tmpdir"));

		
	}
	
}
