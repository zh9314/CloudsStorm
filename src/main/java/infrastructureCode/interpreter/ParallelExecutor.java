package infrastructureCode.interpreter;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.output.NullOutputStream;

import com.jcabi.ssh.SSH;
import com.jcabi.ssh.Shell;

import commonTool.CommonTool;
import commonTool.TARGZ;
import commonTool.Values;

public class ParallelExecutor implements Runnable {
	
	public String exeResult;
	
	private String privateKeyString;
	private String pubIP;
	private String user;
	private String exeCMD;
	private String operation;
	private Map<String, String> options;
	private boolean multiOp; 
	
	////tells whether the operation is executed correctly
	public boolean exeState;
	public String objectName;
	
	ParallelExecutor(String user, String pubIP, String priKey, 
						String operation, String exeCMD,
						Map<String, String> options, String objectName, boolean multiOp){
		this.user = user;
		this.pubIP = pubIP;
		this.privateKeyString = priKey;
		this.operation = operation;
		this.exeCMD = exeCMD;
		this.objectName = objectName;
		this.options = options;
		this.multiOp = multiOp;
		exeResult = " ";
		exeState = true;
	}
	
	@Override
	public void run() {
		if(operation == null){
			exeState = false;
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
				if(!options.containsKey(Values.Options.srcPath) 
						|| !options.containsKey(Values.Options.dstPath)){
					exeResult = "ERROR: Invalid options of path for operation '"
												+operation.trim()+"'!";
					exeState = false;
					return ;
				}
	
				String srcPath = options.get(Values.Options.srcPath);
				String dstPath = options.get(Values.Options.dstPath);
				
				String srcPathType = CommonTool.getFilePathType(srcPath);
				String dstPathType = CommonTool.getFilePathType(dstPath);
				if(operation.trim().equalsIgnoreCase("put")){
					if(dstPathType == null 
						||!dstPathType.trim().equalsIgnoreCase("file")){
						exeResult = "ERROR: Invalid 'dst' path type for operation '"
										+operation.trim()+"', must be 'file'!";
						exeState = false;
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
						exeResult = "ERROR: Invalid 'src' path type for operation '"
										+operation.trim()+"', must be 'file' or 'url'!";
						exeState = false;
						return ;
					}
				}
				if(operation.trim().equalsIgnoreCase("get")){
					if(srcPathType == null 
							|| !srcPathType.trim().equalsIgnoreCase("file")){
						exeResult = "ERROR: Invalid 'src' path type for operation '"
										+operation.trim()+"', must be 'file'!";
						exeState = false;
						return ;
					}
					if(dstPathType == null
							|| !dstPathType.trim().equalsIgnoreCase("file")){
						exeResult = "ERROR: Invalid 'dst' path type for operation '"
										+operation.trim()+"', must be 'file'!";
						exeState = false;
						return ;
					}
					String srcRealPath = CommonTool.getFilePath(srcPath);   /// this is the remote path on the operation subject
					String dstRealPath = CommonTool.getFilePath(dstPath);   /// this is the local path
					///if it needs to get file from multiple VMs, the sub directory of 
					///the object VM name is needed
					if(multiOp){
						String subDir = CommonTool.formatDirWithSep(dstRealPath);
						dstRealPath = subDir + this.objectName ;
					}
					File dstFile = new File(dstRealPath);
					if(!dstFile.exists())
						if(!dstFile.mkdirs()){
							exeResult = "ERROR: 'dst' path " + dstPath
									+ " cannot be created!";
							exeState = false;
							return ;
						}
					
					if(!dstFile.isDirectory()){
						exeResult = "ERROR: 'dst' path " + dstPath
										+ " must be a local directory for operation '"
										+ operation.trim()+"'!";
						exeState = false;
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
			exeState = false;
			return ;
		}
	}

}
