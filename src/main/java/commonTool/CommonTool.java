/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * Copyright © Huan Zhou (SNE, University of Amsterdam) and contributors
 */
package commonTool;

import java.io.File;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.KeyPair;

import topology.description.actual.ActualConnection;
import topology.description.actual.ActualConnectionPoint;

public class CommonTool {

    private static final Logger logger = Logger.getLogger(CommonTool.class);

    /**
     * Make sure the directory path end up with 'File.seperator' Only when the
     * input dir equals "", it does not end with 'File.seperator'
     *
     * @param inputDir
     * @return
     */
    public static String formatDirWithSep(String inputDir) {
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
     * Make sure the directory path does not end up with 'File.seperator'
     *
     * @param inputDir
     * @return
     */
    public static String formatDirWithoutSep(String inputDir) {
        String outputDir = inputDir;
        if (inputDir.equals("")) {
            return outputDir;
        }
        if (outputDir.lastIndexOf(File.separator) == outputDir.length() - 1) {
            outputDir = outputDir.substring(0, outputDir.length() - 1);
        }
        return outputDir;
    }

    /**
     * Get rid of all the '\n' and ' ' in the tail.
     *
     * @return
     */
    public static String formatString(String input) {
        int endIndex;
        for (endIndex = input.length() - 1; endIndex >= 0; endIndex--) {
            if (input.charAt(endIndex) != '\n'
                    && input.charAt(endIndex) != ' ') {
                break;
            }
        }
        String output = input.substring(0, endIndex + 1);
        return output;
    }

    /**
     * The directory name must not end up with file separator
     *
     * @param dirPath
     * @return
     */
    public static String getDirName(String dirPath) {
        String dirName = dirPath;
        if (dirPath.equals("")) {
            return "";
        }

        if (dirPath.lastIndexOf(File.separator) == dirPath.length() - 1) {
            dirName = dirPath.substring(0, dirPath.length() - 1);
        }
        int index = dirName.lastIndexOf(File.separator);
        if (index != -1) {
            return dirName.substring(index + 1);
        } else {
            return dirName;
        }
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
     * The input is a file or directory path of Unix based system. The output is
     * the parent directory path of the input. This output directory must not
     * end up with a "/", only when like "/1.txt"
     */
    public static String getParentDirInUnix(String inputPath) {
        String dir = "";
        if (inputPath.lastIndexOf("/") == inputPath.length() - 1) {
            inputPath = inputPath.substring(0, inputPath.length() - 1);
        }
        if (inputPath.contains("/")) {
            int index = inputPath.lastIndexOf("/");
            if (index == 0) {
                dir = "/";
            } else {
                dir = inputPath.substring(0, index);
            }
        }
        return dir;
    }

    /**
     * The directory name must not end up with "/"
     *
     * @param dirPath
     * @return
     */
    public static String getDirNameInUnix(String dirPath) {
        String dirName = dirPath;
        if (dirPath.equals("")) {
            return "";
        }

        if (dirPath.lastIndexOf("/") == dirPath.length() - 1) {
            dirName = dirPath.substring(0, dirPath.length() - 1);
        }
        int index = dirName.lastIndexOf("/");
        if (index != -1) {
            return dirName.substring(index + 1);
        } else {
            return dirName;
        }
    }

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
     * Convert netmask int to string (null returned if nm > 32 or nm < 1) @
     *
     *
     * param nm @return
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
        if (subPriAddress.length != 4) {
            return null;
        }
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
     * Combine the subnet and host number to be the full IP address. Return
     * 'null' when host number is less than 1 or exceeds the capacity of the
     * subnet, or it is a broadcast address
     */
    public static String getFullAddress(String subnet, int netmaskNum, int hostNum) {
        if (hostNum <= 0) {
            return null;
        }
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
        int fillingLen = (32 - netmaskNum) - binaryHostNum.length();
        ///Host number exceeds the capacity of the subnet
        if (fillingLen < 0) {
            return null;
        }
        //// This is the broadcast address, cannot be a host IP address
        if (fillingLen == 0 && !binaryHostNum.contains("0")) {
            return null;
        }
        for (int i = 0; i < fillingLen; i++) {
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
     *
     * @param topConnections
     * @param tcp
     * @return
     */
    public static ActualConnection getActualConnectionByPoint(ArrayList<ActualConnection> topConnections,
            ActualConnectionPoint tcp) {
        ActualConnection resultCon = null;
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
     * @param keyDirPath
     * @return
     */
    public static boolean rsaKeyGenerate(String keyDirPath) {
        File keyDir = new File(keyDirPath);
        if (keyDir.exists()) {
            logger.error("The key pair for " + keyDirPath + " has already exist!");
            return false;
        }
        if (!keyDir.mkdir()) {
            logger.warn("Cannot create directory " + keyDirPath);
            return false;
        }
        JSch jsch = new JSch();
        KeyPair kpair;
        try {
            kpair = KeyPair.genKeyPair(jsch, KeyPair.RSA);
            kpair.writePrivateKey(keyDirPath + "id_rsa");
            kpair.writePublicKey(keyDirPath + "id_rsa.pub", "keyPair-" + UUID.randomUUID().toString());
            kpair.dispose();

            File file = new File(keyDirPath + "id_rsa");
            file.setReadOnly();
// verify if file is made read-only
            if (file.canWrite()) {
                logger.warn("File: " + file.getAbsolutePath() + " is not read-only");
                return false;
            }

        } catch (JSchException | IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static boolean rmObjInMap(Map<?, ?> map, Object targetObj) {
        Iterator<?> it = map.entrySet().iterator();
        while (it.hasNext()) {
            Entry<?, ?> item = (Entry<?, ?>) it.next();
            if (item.getKey() == targetObj) {
                it.remove();
                return true;
            }
        }
        return false;
    }

    public static boolean rmKeyInMap(Map<?, ?> map, Object targetKey) {
        if (targetKey == null) {
            return false;
        }
        Iterator<?> it = map.entrySet().iterator();
        while (it.hasNext()) {
            Entry<?, ?> item = (Entry<?, ?>) it.next();
            if (targetKey.equals(item.getKey())) {
                it.remove();
            }
        }
        return false;
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
