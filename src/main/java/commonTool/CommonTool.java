package commonTool;

import java.io.File;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.KeyPair;

import topologyAnalysis.dataStructure.TopConnection;
import topologyAnalysis.dataStructure.TopConnectionPoint;

public class CommonTool {

    private static final Logger logger = Logger.getLogger(CommonTool.class);

    /**
     * Make sure the directory path end up with 'File.seperator'
     * Only when the input dir equals "", it does not end with 'File.seperator'
     * @param inputDir
     * @return
     */
    public static String formatDir(String inputDir) {
        String outputDir = inputDir;
        if (inputDir.equals("")) {
            return outputDir;
        }
        if (inputDir.lastIndexOf(File.separator) != inputDir.length() - 1) {
            outputDir = inputDir + File.separator;
        }
        return outputDir;
    }
    
    /**
     * Get rid of all the '\n' and ' ' in the tail.
     * @return
     */
    public static String formatString(String input){
    		int endIndex;
    		for(endIndex = input.length()-1 ; endIndex >= 0 ; endIndex--){
    			if(input.charAt(endIndex) != '\n'
    				&& input.charAt(endIndex) != ' ')
    				break;
    		}
    		String output = input.substring(0, endIndex+1);
    		return output;
    }
    
    public static String getDirName(String dirPath){
	    	String dirName = dirPath;
	    	// this dir path ends up with file separator
	    	if (dirPath.lastIndexOf(File.separator) == dirPath.length() - 1) 
	    		dirName = dirPath.substring(0, dirPath.length() - 1);
	    	int index = dirName.lastIndexOf(File.separator);
	    	if(index != -1)
	    		return dirName.substring(index+1);
	    	else
	    		return dirName;
    		
    }

    /**
     * The input is a file path. The output is the directory path of the file.
     * This output directory always ends up with a File.separator, only when
     * it's "".
     */
    public static String getPathDir(String filePath) {
        String dir = "";
        if (filePath.contains(File.separator)) {
            int index = filePath.lastIndexOf(File.separator);
            dir = filePath.substring(0, index + 1);
        }
        return dir;
    }

    /**
     * This function is used to modify the value of the field 'publicKeyPath' in
     * the top level description file. This function is needed before loading
     * the topology, when user needs to upload his public key.
     */
    /*public static void setPublicKeyPath(String newPbKeyPath, String topologyPath){
		File topologyFile = new File(topologyPath);
		String fileContent = "";
		try {
			BufferedReader in = new BufferedReader(new FileReader(topologyFile));
			String line = "";
			while((line = in.readLine()) != null){
				if(line.contains("publicKeyPath"))
					fileContent += ("publicKeyPath: "+newPbKeyPath+"\n");
				else
					fileContent += (line+"\n");
			}
			in.close();
			
			FileWriter fw = new FileWriter(topologyPath, false);
			fw.write(fileContent);
			fw.close();
			logger.debug("The 'publicKeyPath' topology file ["+topologyPath+"] is modifyed to "
					+newPbKeyPath);
		} catch (IOException e) {
			e.printStackTrace();
			logger.error("Error happens during modifying the field 'publicKeyPath'");
		}
	}*/
    // converting to netmask
    private static final String[] netmaskConverter = {
        "128.0.0.0", "192.0.0.0", "224.0.0.0", "240.0.0.0", "248.0.0.0", "252.0.0.0", "254.0.0.0", "255.0.0.0",
        "255.128.0.0", "255.192.0.0", "255.224.0.0", "255.240.0.0", "255.248.0.0", "255.252.0.0", "255.254.0.0", "255.255.0.0",
        "255.255.128.0", "255.255.192.0", "255.255.224.0", "255.255.240.0", "255.255.248.0", "255.255.252.0", "255.255.254.0", "255.255.255.0",
        "255.255.255.128", "255.255.255.192", "255.255.255.224", "255.255.255.240", "255.255.255.248", "255.255.255.252", "255.255.255.254", "255.255.255.255"
    };

    /**
     * Convert netmask string to an integer (-1 returned if no match)
     *
     * @param nm
     * @return normally, returned value should be 1-32. -1 means the input
     * string of netmask is not valid.
     */
    public static int netmaskStringToInt(String nm) {
        int i = 1;
        for (String s : netmaskConverter) {
            if (s.equals(nm)) {
                return i;
            }
            i++;
        }
        return -1;
    }

    /**
     * Convert netmask int to string (null returned if nm > 32 or nm <
     * 1)
	 * @
     *
     * param nm
     * @return
     */
    public static String netmaskIntToString(int nm) {
        if ((nm > 32) || (nm < 1)) {
            return null;
        } else {
            return netmaskConverter[nm - 1];
        }
    }

    //The input string is just the IP address without netmask.
    //It will also return false, if the IP address is not valid.
    public static boolean checkPrivateIPaddress(String ip) {
        try {
            Inet4Address address = (Inet4Address) InetAddress.getByName(ip);
            if (address.isSiteLocalAddress()) {
                return true;
            }
            return false;
        } catch (UnknownHostException e) {
            e.printStackTrace();
            logger.warn("Not valid ip address!");
            return false;
        }
    }

    /**
     * Given a IP address and netmask number. Generate the subnet.
     *
     * @return subnet of the IP address.
     */
    public static String getSubnet(String ip, int netmaskNum) {
        String subnet = "";
        String[] subPriAddress = ip.split("\\.");
        String combineAddress = "";
        for (int i = 0; i < subPriAddress.length; i++) {
            int subAddNum = Integer.valueOf(subPriAddress[i]);
            String bString = Integer.toBinaryString(subAddNum);
            int len = 8 - bString.length();
            for (int j = 0; j < len; j++) {
                bString = "0" + bString;
            }
            combineAddress += bString;
        }
        String binarySubnet = combineAddress.substring(0, netmaskNum);
        for (int i = 0; i < (32 - netmaskNum); i++) {
            binarySubnet += "0";
        }

        for (int i = 0; i < 4; i++) {
            String nums = binarySubnet.substring(i * 8, i * 8 + 8);
            int num = Integer.parseInt(nums, 2);
            if (i == 0) {
                subnet = num + "";
            } else {
                subnet += "." + num;
            }
        }

        return subnet;
    }

    /**
     * Given a IP address and netmask number. Generate the host specific address
     * number.
     *
     * @return host specific address number.
     */
    public static int getHostInfo(String ip, int netmaskNum) {
        String[] subPriAddress = ip.split("\\.");
        String combineAddress = "";
        for (int i = 0; i < subPriAddress.length; i++) {
            int subAddNum = Integer.valueOf(subPriAddress[i]);
            String bString = Integer.toBinaryString(subAddNum);
            int len = 8 - bString.length();
            for (int j = 0; j < len; j++) {
                bString = "0" + bString;
            }
            combineAddress += bString;
        }
        String binaryHost = combineAddress.substring(netmaskNum, 32);
        int addressNum = Integer.parseInt(binaryHost, 2);

        return addressNum;
    }

    /**
     * Combine the subnet and host number to be the full IP address.
     */
    public static String getFullAddress(String subnet, int netmaskNum, int hostNum) {
        String[] subPriAddress = subnet.split("\\.");
        String combineAddress = "";
        for (int i = 0; i < subPriAddress.length; i++) {
            int subAddNum = Integer.valueOf(subPriAddress[i]);
            String bString = Integer.toBinaryString(subAddNum);
            int len = 8 - bString.length();
            for (int j = 0; j < len; j++) {
                bString = "0" + bString;
            }
            combineAddress += bString;
        }
        String binarySubnet = combineAddress.substring(0, netmaskNum);
        String binaryHostNum = Integer.toBinaryString(hostNum);
        String fillingS = "";
        for (int i = 0; i < ((32 - netmaskNum) - binaryHostNum.length()); i++) {
            fillingS += "0";
        }
        String binaryFullAddress = binarySubnet + fillingS + binaryHostNum;

        String fullAddress = "";
        for (int i = 0; i < 4; i++) {
            String nums = binaryFullAddress.substring(i * 8, i * 8 + 8);
            int num = Integer.parseInt(nums, 2);
            if (i == 0) {
                fullAddress = num + "";
            } else {
                fullAddress += "." + num;
            }
        }

        return fullAddress;

    }

    /**
     * The input parameter is a path of the file. It can be the path of the
     * system or an url or a file name. <br/>
     * Examples: url@http://www.mydomain.com/pathToFile/myId_dsa or
     * file@/home/id_dsa name@id_rsa.pub. <br/>
     * The input parameter of 'currentDir' is useful, when the 'filePath' is
     * just a file name. The 'currentDir' must end up with a file separator.
     *
     * @return the content of the file. If the file cannot be opened, then
     * return null.
     */
    public static String getFileContent(String filePath, String currentDir) {
        String outputString = "";
        int firstIndex = filePath.indexOf("@");
        if (firstIndex == -1) {
            return null;
        }
        String prefix = filePath.substring(0, firstIndex);
        String realPath = filePath.substring(firstIndex + 1, filePath.length());
        if (prefix.trim().equalsIgnoreCase("file")) {
            File inputFile = new File(realPath);
            try {
                outputString = FileUtils.readFileToString(inputFile, "UTF-8");
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }

        } else if (prefix.trim().equalsIgnoreCase("url")) {
            URL url;
            try {
                url = new URL(realPath);
                //Copying the url to a temporary file, set the connection timeout and reading timeout as 10s.
                String tmpPath = System.getProperty("java.io.tmpdir") + File.separator + "URL-" + Long.toString(System.nanoTime()) + "-tmp.txt";
                File inputFile = new File(tmpPath);
                FileUtils.copyURLToFile(url, inputFile, 10000, 10000);
                outputString = FileUtils.readFileToString(inputFile, "UTF-8");
                FileUtils.deleteQuietly(inputFile);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        } else if (prefix.trim().equalsIgnoreCase("name")) {
            //In this case, 'realPath' is the file name.
            File inputFile = new File(currentDir + realPath);
            try {
                outputString = FileUtils.readFileToString(inputFile, "UTF-8");
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        } else {
            logger.error("Unsupported file path of (" + filePath + ")!");
            return null;
        }
        return outputString;

    }
    
    public static String getFilePathType(String filePath) {
    		int firstIndex = filePath.indexOf("@");
        if (firstIndex == -1) {
            return null;
        }
        String prefix = filePath.substring(0, firstIndex);
        return prefix;
    }
    
    public static String getFilePath(String filePath) {
		int firstIndex = filePath.indexOf("@");
	    if (firstIndex == -1) {
	        return null;
	    }
	    String realPath = filePath.substring(firstIndex + 1, filePath.length());
	    return realPath;
    }

    /**
     * Given a source or target connection point and a set of top connections.
     * This method is to find whether there is a top connection contains this
     * point.
     */
    public static TopConnection getTopConnectionByPoint(ArrayList<TopConnection> topConnections,
            TopConnectionPoint tcp) {
        TopConnection resultCon = null;
        for (int ti = 0; ti < topConnections.size(); ti++) {
            if (topConnections.get(ti).source.address.equals(tcp.address)
                    && topConnections.get(ti).source.vmName.equals(tcp.vmName)) {
                resultCon = topConnections.get(ti);
                break;
            }
            if (topConnections.get(ti).target.address.equals(tcp.address)
                    && topConnections.get(ti).target.vmName.equals(tcp.vmName)) {
                resultCon = topConnections.get(ti);
                break;
            }
        }
        return resultCon;
    }

    /**
     * This method is used to generate a pair of ssh key. The input value is the
     * folder to store these keys. The value of 'keyDirPath' must end up with
     * the file separator. The private key is always named as id_rsa and the
     * public key is always named as id_rsa.pub. The return value is boolean.
     *
     * @return
     */
    public static boolean rsaKeyGenerate(String keyDirPath) {
        File keyDir = new File(keyDirPath);
        if (keyDir.exists()) {
            logger.error("The key pair for " + keyDirPath + " has already exist!");
            return false;
        }
        if (!keyDir.mkdir()) {
            logger.error("Cannot create directory " + keyDirPath);
            return false;
        }
        JSch jsch = new JSch();
        KeyPair kpair;
        try {
            kpair = KeyPair.genKeyPair(jsch, KeyPair.RSA);
            kpair.writePrivateKey(keyDirPath + "id_rsa");
            kpair.writePublicKey(keyDirPath + "id_rsa.pub", "clusterKeyPair-" + UUID.randomUUID().toString());
            kpair.dispose();
        } catch (JSchException | IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;

    }

    //This is used for converting the ip integer into binary string.
    //As the number of bits between '.' in IP is 8. 
    //The output string needs to be filled with leading 0 to be 8 bits.
    /*private static String convertBinary(int sum) {
        StringBuffer binary = new StringBuffer();
        while (sum != 0 && sum != 1) {
            binary.insert(0, sum % 2);
            sum = sum / 2;
            if (sum == 0 || sum == 1) {
                binary.insert(0, sum % 2);
            }
        }
        String ipBinary = binary.toString();
        String fillingS = "";
        if(ipBinary.length() < 8){
        	int fillingLen = 8-ipBinary.length();
        	for(int i = 0 ; i<fillingLen ; i++){
        		fillingS += "0";
        	}
        }
        return fillingS + ipBinary;
    }*/
}
