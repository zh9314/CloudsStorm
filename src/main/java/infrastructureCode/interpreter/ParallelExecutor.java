package infrastructureCode.interpreter;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;


import com.jcabi.ssh.SSH;
import com.jcabi.ssh.Shell;

import commonTool.CommonTool;

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
				String [] srcdst = operation.split("::");
				if(srcdst.length != 2){
					exeResult = "Invalid path for operation '"+operation.trim()+"'!";
					return ;
				}
				int firstLen = srcdst[0].split(":=").length;
				int secondLen = srcdst[1].split(":=").length;
				if(firstLen != 2 || secondLen != 2){
					exeResult = "Invalid paths for operation '"+operation.trim()+"'!";
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
					exeResult = "Invalid paths for operation '"+operation.trim()+"' missing 'src' or 'dst'!";
					return ;
				}
				String srcPathType = CommonTool.getFilePath(srcPath);
				String dstPathType = CommonTool.getFilePath(dstPath);
				if(operation.trim().equalsIgnoreCase("put")){
					if(!dstPathType.trim().equalsIgnoreCase("file")){
						exeResult = "Invalid 'dst' path type for operation '"+operation.trim()+"', must be 'file'!";
						return ;
					}
					String srcRealPath = CommonTool.getFilePath(srcPath);
					String dstRealPath = CommonTool.getFilePath(dstPath);
					String formatDstPath = CommonTool.formatDir(dstRealPath);
					ByteArrayOutputStream cmdOutputBytes = new ByteArrayOutputStream();
					if(srcPathType.trim().equalsIgnoreCase("file")){
						File srcFile = new File(srcRealPath);
						String srcFileName = srcFile.getName();
						Shell shell = new SSH(pubIP, 22, user, privateKeyString);
						new Shell.Safe(shell).exec("sudo cat > " + formatDstPath + srcFileName,
								new FileInputStream(srcFile), cmdOutputBytes, cmdOutputBytes);
						exeResult = cmdOutputBytes.toString("UTF-8");
					}else if(srcPathType.trim().equalsIgnoreCase("url")){
						Shell shell = new SSH(pubIP, 22, user, privateKeyString);
						new Shell.Safe(shell).exec("sudo wget " + srcRealPath + " -P " + formatDstPath,
								null, cmdOutputBytes, cmdOutputBytes);
						exeResult = "Some unknown error!";
						String [] resultLines = cmdOutputBytes.toString("UTF-8").split("\n");
						for(int li = 0 ; li < resultLines.length ; li++){
							if(resultLines[li].contains("saved")
								|| resultLines[li].contains("ERROR")){
								exeResult = resultLines[li];
								break;
							}
						}
					}else{
						exeResult = "Invalid 'src' path type for operation '"+operation.trim()+"', must be 'file' or 'url'!";
						return ;
					}
				}
				if(operation.trim().equalsIgnoreCase("get")){
					if(!srcPathType.trim().equalsIgnoreCase("file")){
						exeResult = "Invalid 'src' path type for operation '"+operation.trim()+"', must be 'file'!";
						return ;
					}
					if(!dstPathType.trim().equalsIgnoreCase("file")){
						exeResult = "Invalid 'dst' path type for operation '"+operation.trim()+"', must be 'file'!";
						return ;
					}
					String srcRealPath = CommonTool.getFilePath(srcPath);   /// this is the remote path on the operation subject
					String dstRealPath = CommonTool.getFilePath(dstPath);   /// this is the local path
					FileWriter downloadFile = new FileWriter(dstRealPath, false);
					ByteArrayOutputStream cmdOutputBytes = new ByteArrayOutputStream();
					Shell shell = new SSH(pubIP, 22, user, privateKeyString);
					new Shell.Safe(shell).exec("sudo cat " + srcRealPath,
							null, cmdOutputBytes, cmdOutputBytes);
					String fileContent = cmdOutputBytes.toString("UTF-8");
					downloadFile.write(fileContent);
					downloadFile.close();
					exeResult = "File "+dstRealPath+" is saved!";
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			exeResult = "ERROR: "+e.getMessage();
			return ;
		}
	}

}
