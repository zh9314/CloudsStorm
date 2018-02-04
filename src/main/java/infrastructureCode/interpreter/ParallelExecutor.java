package infrastructureCode.interpreter;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.output.NullOutputStream;

import com.jcabi.ssh.SSH;
import com.jcabi.ssh.Shell;

import commonTool.CommonTool;
import commonTool.TARGZ;

public class ParallelExecutor implements Runnable {
	
	public String exeResult;
	
	private String privateKeyString;
	private String pubIP;
	private String user;
	private String exeCMD;
	private String operation;
	
	ParallelExecutor(String user, String pubIP, String priKey, String operation, String exeCMD){
		this.user = user;
		this.pubIP = pubIP;
		this.privateKeyString = priKey;
		this.operation = operation;
		this.exeCMD = exeCMD;
		exeResult = " ";
	}
	
	@Override
	public void run() {
		if(operation == null){
			return ;
		}
		try {
			if(operation.trim().equalsIgnoreCase("execute")){
				ByteArrayOutputStream cmdOutputBytes = new ByteArrayOutputStream();
				String cmd = "sudo " + exeCMD;
				Shell shell = new SSH(pubIP, 22, user, privateKeyString);
				new Shell.Safe(shell).exec(cmd,
						  null, cmdOutputBytes, cmdOutputBytes);
				exeResult = cmdOutputBytes.toString("UTF-8");
			}else if(operation.trim().equalsIgnoreCase("put")
					|| operation.trim().equalsIgnoreCase("get")){
				String [] srcdst = exeCMD.split("::");
				if(srcdst.length != 2){
					exeResult = "ERROR: Invalid path for operation '"+operation.trim()+"'!";
					return ;
				}
				int firstLen = srcdst[0].split(":=").length;
				int secondLen = srcdst[1].split(":=").length;
				if(firstLen != 2 || secondLen != 2){
					exeResult = "ERROR: Invalid paths for operation '"+operation.trim()+"'!";
					return ;
				}
	
				String firstName = srcdst[0].split(":=")[0];
				String secondName = srcdst[1].split(":=")[0];
				String srcPath, dstPath;
				if(firstName.trim().equalsIgnoreCase("src") && secondName.trim().equalsIgnoreCase("dst")){
					srcPath = srcdst[0].split(":=")[1];
					dstPath = srcdst[1].split(":=")[1];
				}else if(firstName.trim().equalsIgnoreCase("dst") && secondName.trim().equalsIgnoreCase("src")){
					srcPath = srcdst[1].split(":=")[1];
					dstPath = srcdst[0].split(":=")[1];
				}else{
					exeResult = "ERROR: Invalid paths for operation '"+operation.trim()+"' missing 'src' or 'dst'!";
					return ;
				}
				String srcPathType = CommonTool.getFilePathType(srcPath);
				String dstPathType = CommonTool.getFilePathType(dstPath);
				if(operation.trim().equalsIgnoreCase("put")){
					if(dstPathType == null 
						||!dstPathType.trim().equalsIgnoreCase("file")){
						exeResult = "ERROR: Invalid 'dst' path type for operation '"+operation.trim()+"', must be 'file'!";
						return ;
					}
					String srcRealPath = CommonTool.getFilePath(srcPath);
					String dstRealPath = CommonTool.getFilePath(dstPath);
					String formatDstPath = CommonTool.formatDirWithoutSep(dstRealPath);
					ByteArrayOutputStream cmdOutputBytes = new ByteArrayOutputStream();
					if(srcPathType != null
							&& srcPathType.trim().equalsIgnoreCase("file")){
						File srcFile = new File(srcRealPath);
						if(srcFile.isDirectory()){
							String tmpFilePath = System.getProperty("java.io.tmpdir") + File.separator + "Upload.tar.gz";
							TARGZ.compress(tmpFilePath, new File(srcRealPath));
							String srcDirName = CommonTool.getDirName(srcRealPath);
							srcFile = new File(tmpFilePath);
							Shell shell = new SSH(pubIP, 22, user, privateKeyString);
							new Shell.Safe(shell).exec("sudo cat > " + formatDstPath + "/Upload.tar.gz",
									new FileInputStream(srcFile), cmdOutputBytes, cmdOutputBytes);
							String ret = cmdOutputBytes.toString("UTF-8");
							if(ret.trim().equals("")){
								new Shell.Safe(shell).exec("sudo tar -xzf " + formatDstPath + "/Upload.tar.gz -C "+formatDstPath,
										null, new NullOutputStream(), new NullOutputStream());
								new Shell.Safe(shell).exec("sudo rm " + formatDstPath + "/Upload.tar.gz",
										null, new NullOutputStream(), new NullOutputStream());
								exeResult = "Directory "+srcDirName+" is uploaded!";
							}
							else
								exeResult = cmdOutputBytes.toString("UTF-8");
							
							FileUtils.deleteQuietly(srcFile);
						}else{
							String srcFileName = srcFile.getName();
							Shell shell = new SSH(pubIP, 22, user, privateKeyString);
							new Shell.Safe(shell).exec("sudo cat > " + formatDstPath + "/" + srcFileName,
									new FileInputStream(srcFile), cmdOutputBytes, cmdOutputBytes);
							String ret = cmdOutputBytes.toString("UTF-8");
							if(ret.trim().equals(""))
								exeResult = "File "+srcFileName+" is uploaded!";
							else
								exeResult = cmdOutputBytes.toString("UTF-8");
						}
						
					}else if(srcPathType != null 
							&& srcPathType.trim().equalsIgnoreCase("url")){
						Shell shell = new SSH(pubIP, 22, user, privateKeyString);
						new Shell.Safe(shell).exec("sudo wget " + srcRealPath + " -P " + formatDstPath,
								null, cmdOutputBytes, cmdOutputBytes);
						exeResult = "ERROR: Some unknown error!";
						String [] resultLines = cmdOutputBytes.toString("UTF-8").split("\n");
						for(int li = 0 ; li < resultLines.length ; li++){
							if(resultLines[li].contains("saved")
								|| resultLines[li].contains("ERROR")){
								exeResult = resultLines[li];
								break;
							}
						}
					}else{
						exeResult = "ERROR: Invalid 'src' path type for operation '"+operation.trim()+"', must be 'file' or 'url'!";
						return ;
					}
				}
				if(operation.trim().equalsIgnoreCase("get")){
					if(srcPathType == null 
							|| !srcPathType.trim().equalsIgnoreCase("file")){
						exeResult = "ERROR: Invalid 'src' path type for operation '"+operation.trim()+"', must be 'file'!";
						return ;
					}
					if(dstPathType == null
							|| !dstPathType.trim().equalsIgnoreCase("file")){
						exeResult = "ERROR: Invalid 'dst' path type for operation '"+operation.trim()+"', must be 'file'!";
						return ;
					}
					String srcRealPath = CommonTool.getFilePath(srcPath);   /// this is the remote path on the operation subject
					String dstRealPath = CommonTool.getFilePath(dstPath);   /// this is the local path
					File dstFile = new File(dstRealPath);
					if(!dstFile.isDirectory()){
						exeResult = "ERROR: 'dst' path "+dstPath+" must be a local directory for operation '"+operation.trim()+"'!";
						return ;
					}
					
					////No matter the remote file is a directory or a file, we first package it into tar.gz file
					ByteArrayOutputStream cmdOutputBytes = new ByteArrayOutputStream();
					
					/////currently we only support the resource on the Cloud only with Unix based system.
					String srcParentDir = CommonTool.getParentDirInUnix(srcRealPath);
					String srcDirName = CommonTool.getDirNameInUnix(srcRealPath);
					
					Shell shell = new SSH(pubIP, 22, user, privateKeyString);
					new Shell.Safe(shell).exec("sudo tar -czf /tmp/D.tar.gz "+srcDirName+" -C " + srcParentDir,
							null, new NullOutputStream(), new NullOutputStream());
					new Shell.Safe(shell).exec("sudo cat /tmp/D.tar.gz",
							null, cmdOutputBytes, cmdOutputBytes);
					new Shell.Safe(shell).exec("sudo rm /tmp/D.tar.gz ",
							null, new NullOutputStream(), new NullOutputStream());
					
					String tmpFilePath = System.getProperty("java.io.tmpdir") + File.separator + "D.tar.gz";
					
					FileUtils.writeByteArrayToFile(new File(tmpFilePath), cmdOutputBytes.toByteArray());
					
					TARGZ.decompress(tmpFilePath, dstFile);

					exeResult = "File(s) is saved at "+dstRealPath+"!";
					FileUtils.deleteQuietly(new File(tmpFilePath));
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			exeResult = "ERROR: "+e.getMessage();
			return ;
		}
	}

}
