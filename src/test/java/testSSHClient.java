import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.output.NullOutputStream;
import org.apache.log4j.Logger;

import com.jcabi.ssh.SSH;
import com.jcabi.ssh.Shell;


public class testSSHClient {

	private static final Logger logger = Logger.getLogger(testSSHClient.class);
	
	public static void main(String[] args) {
		long start = System.currentTimeMillis();
		File inputFile = new File("tmpfedcloud");
		String privateKey = "";
		try {
			privateKey = FileUtils.readFileToString(inputFile, "UTF-8");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		while(!isAlive("147.228.242.153", privateKey)){
			long time = System.currentTimeMillis();
			System.out.println("time used: "+((time-start)/1000));
		}
		
		long time = System.currentTimeMillis();
		System.out.println("time used: "+((time-start)/1000));
		
			
			
		/*
		File inputFile = new File("test");
		File outputFile = new File("out.tmp");
		FileOutputStream fo = null;
		try {
			fo = new FileOutputStream(outputFile, false);
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		String privateKey;
		try {
			privateKey = FileUtils.readFileToString(inputFile, "UTF-8");
			Shell shell = new SSH("54.89.116.133", 22, "zh9314", privateKey);
			logger.error("tt");
			
			File file = new File("test2.sh");
			new Shell.Safe(shell).exec(
			  "cat > d.sh && bash d.sh ",
			  new FileInputStream(file),
			  fo,
			  new NullOutputStream()
			);
			
			logger.debug("test");
			
			long time = System.currentTimeMillis();
			System.out.println("time used: "+((time-start)/1000));
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("test");
		}
		*/
	}
	
	/** 
	 * Test if a host is alive
	 * @param host
	 * @return true if it's alive
	 */
	public static boolean isAlive(String host, String pk){
	  boolean alive=false;
	  try {
	    String cmd="echo " + host;
	    Shell shell=new SSH(host, 22, "ubuntu", pk);
	    new Shell.Plain(shell).exec(cmd);
	    alive=true;
	  }
	 catch (  Exception e) {
	  }
	  return alive;
	}

}
