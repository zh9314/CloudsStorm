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
package lambdaInfrs.credential;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import commonTool.CommonTool;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashSet;
import java.util.Set;

/**
 * This is a class to store the ssh keys.
 *
 */
public class SSHKeyPair {

    //This is a GUID. In some case, the public and key files are store in the directory of this GUID.
    //And this directory is in the same folder of the description files.
    //The public key file name is always 'id_rsa.pub' and the private key file name is always 'id_rsa'.
    //If the public key content cannot be got, there will  be a file called 'name.pub' to store the public key name.
    public String SSHKeyPairId;

    public String publicKeyString;

    //In some case, the public key string is unknown. You can just get the key id, EC2 for instance.
    public String publicKeyId;

    public String privateKeyString;

    public boolean loadSSHKeyPair(String sshKeyPairId, String keyDir) throws IOException {
        String sshKeyDir = CommonTool.formatDirWithSep(keyDir);
        String privateKeyPath = sshKeyDir + "id_rsa";
        File privateKeyFile = new File(privateKeyPath);
        Set<PosixFilePermission> perms = new HashSet<>();
        perms.add(PosixFilePermission.OWNER_READ);
        Files.setPosixFilePermissions(Paths.get(privateKeyFile.getAbsolutePath()), perms);

        String publicKeyPath = sshKeyDir + "id_rsa.pub";
        File publicKeyFile = new File(publicKeyPath);
        String publicKeyIdPath = sshKeyDir + "name.pub";
        File publicKeyIdFile = new File(publicKeyIdPath);
        String privateKeyString = null, publicKeyString = null, publicKeyIdString = null;
        boolean success = false;
        int count = 0;    ////try some times, in case the key is generating.
        while (!success && count < 5) {
            success = true;
            try {
                if (privateKeyFile.exists()) {
                    privateKeyString = FileUtils.readFileToString(privateKeyFile, "UTF-8");
                } else {
                    success = false;
                }
                if (success) {
                    boolean atLeastOne = false;
                    if (publicKeyFile.exists()) {
                        publicKeyString = FileUtils.readFileToString(publicKeyFile, "UTF-8");
                        atLeastOne = true;
                    }
                    if (publicKeyIdFile.exists()) {
                        publicKeyIdString = FileUtils.readFileToString(publicKeyIdFile, "UTF-8");
                        atLeastOne = true;
                    }
                    if (!atLeastOne) {
                        success = false;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                success = false;
            }

            if (success) {
                this.publicKeyString = publicKeyString;
                this.privateKeyString = privateKeyString;
                this.publicKeyId = publicKeyIdString;
                this.SSHKeyPairId = sshKeyPairId;
            } else {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            count++;
        }

        return success;
    }

}
