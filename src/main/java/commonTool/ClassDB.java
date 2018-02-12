package commonTool;

import java.util.HashMap;
import java.util.Map;

import provisioning.credential.BasicCredential;
import provisioning.credential.EC2Credential;
import provisioning.credential.EGICredential;
import provisioning.credential.ExoGENICredential;
import provisioning.database.BasicDatabase;
import provisioning.database.EC2.EC2Database;
import provisioning.database.EGI.EGIDatabase;
import provisioning.database.ExoGENI.ExoGENIDatabase;
import provisioning.engine.SEngine.EC2SEngine;
import provisioning.engine.SEngine.EGISEngine;
import provisioning.engine.SEngine.ExoGENISEngine;
import provisioning.engine.SEngine.SEngine;
import provisioning.engine.VEngine.EC2.EC2VEngineUbuntu;
import provisioning.engine.VEngine.EGI.EGIVEngineUbuntu;
import provisioning.engine.VEngine.ExoGENI.ExoGENIVEngineUbuntu;
import provisioning.engine.VEngine.OS.ubuntu.BasicVEngineUbuntu;
import topology.dataStructure.EC2.EC2SubTopology;
import topology.dataStructure.EC2.EC2VM;
import topology.dataStructure.EGI.EGISubTopology;
import topology.dataStructure.EGI.EGIVM;
import topology.dataStructure.ExoGENI.ExoGENISubTopology;
import topology.dataStructure.ExoGENI.ExoGENIVM;
import topology.description.actual.BasicSubTopology;
import topology.description.actual.BasicVM;

/**
 * A class to load all the related classes. All the input Cloud provider
 * name must be without leading or tailing spaces and all in lower cases.
 * @author huan
 *
 */
public class ClassDB {
	
	
	private static Map<String, Class<?>> SubTopologyMap;
	static {
		SubTopologyMap = new HashMap<String, Class<?>>();
		SubTopologyMap.put("ec2", EC2SubTopology.class);
		SubTopologyMap.put("exogeni", ExoGENISubTopology.class);
		SubTopologyMap.put("egi", EGISubTopology.class);
		SubTopologyMap.put("_DEFAULT", BasicSubTopology.class);
	}
	
	private static Map<String, Class<?>> VMMap;
	static {
		VMMap = new HashMap<String, Class<?>>();
		VMMap.put("ec2", EC2VM.class);
		VMMap.put("exogeni", ExoGENIVM.class);
		VMMap.put("egi", EGIVM.class);
		VMMap.put("_DEFAULT", BasicVM.class);
	}
	
	private static Map<String, Class<?>> SEngineMap;
	static {
		SEngineMap = new HashMap<String, Class<?>>();
		SEngineMap.put("ec2", EC2SEngine.class);
		SEngineMap.put("exogeni", ExoGENISEngine.class);
		SEngineMap.put("egi", EGISEngine.class);
		SEngineMap.put("_DEFAULT", SEngine.class);
	}
	
	/**
	 * The first key is the Cloud provider, the second key is the OS type.
	 */
	private static Map<String, Map<String, Class<?>>> VEngineMap;
	static {
		VEngineMap = new HashMap<String, Map<String, Class<?>>>();
		
		Map<String, Class<?>> ec2OSMap = new HashMap<String, Class<?>>();
		ec2OSMap.put("ubuntu", EC2VEngineUbuntu.class);
		ec2OSMap.put("_DEFAULT", EC2VEngineUbuntu.class);
		
		Map<String, Class<?>> egiOSMap = new HashMap<String, Class<?>>();
		egiOSMap.put("ubuntu", EGIVEngineUbuntu.class);
		egiOSMap.put("_DEFAULT", EGIVEngineUbuntu.class);
		
		Map<String, Class<?>> geniOSMap = new HashMap<String, Class<?>>();
		geniOSMap.put("ubuntu", ExoGENIVEngineUbuntu.class);
		geniOSMap.put("_DEFAULT", ExoGENIVEngineUbuntu.class);
		
		VEngineMap.put("ec2", ec2OSMap);
		VEngineMap.put("egi", egiOSMap);
		VEngineMap.put("exogeni", geniOSMap);
		
		////these are kinds of VEngines that are not be able to create, stop, start, delete 
		////a VM. But they are useful for just configuration on a specific OS type VM.
		////Assume a scenario is that user A provisions an Infrastructure and generate the output control file.
		////There are some application-defined cloud in the infrastructure. User B do not implement this kind of
		////specific VEngine. Then user B can still do all the configuration related work on these VMs.
		Map<String, Class<?>> defaultOSMap = new HashMap<String, Class<?>>();
		defaultOSMap.put("ubuntu", BasicVEngineUbuntu.class);
		VEngineMap.put("_DEFAULT", defaultOSMap);
	}
	
	private static Map<String, Class<?>> DatabaseMap;
	static {
		DatabaseMap = new HashMap<String, Class<?>>();
		DatabaseMap.put("ec2", EC2Database.class);
		DatabaseMap.put("exogeni", ExoGENIDatabase.class);
		DatabaseMap.put("egi", EGIDatabase.class);
		DatabaseMap.put("_DEFAULT", BasicDatabase.class);
	}
	
	private static Map<String, Class<?>> CredentialMap;
	static {
		CredentialMap = new HashMap<String, Class<?>>();
		CredentialMap.put("ec2", EC2Credential.class);
		CredentialMap.put("exogeni", ExoGENICredential.class);
		CredentialMap.put("egi", EGICredential.class);
		CredentialMap.put("_DEFAULT", BasicCredential.class);
	}
	
	
	public static Class<?> getSubTopology(String cloudProvider, String className){
		if(cloudProvider != null)
			cloudProvider = cloudProvider.trim().toLowerCase();
		if(className == null){
			if(cloudProvider == null)
				return SubTopologyMap.get("_DEFAULT");
			else{
				if(SubTopologyMap.containsKey(cloudProvider))
					return SubTopologyMap.get(cloudProvider);
				else
					return SubTopologyMap.get("_DEFAULT");
			}
		}else{
			try {
				Class<?> XSubTopology = Class.forName(className);
				///if it is loaded, then it becomes a default class for this cloud provider
				if(cloudProvider != null 
						&& !SubTopologyMap.containsKey(cloudProvider))
					SubTopologyMap.put(cloudProvider, XSubTopology);
				return XSubTopology;
			} catch (ClassNotFoundException e) {
				e.getMessage();
				if(cloudProvider == null)
					return SubTopologyMap.get("_DEFAULT");
				///using the default classes, in order to avoid wrong configuration
				///this is good for extension
				else
					return SubTopologyMap.get(cloudProvider);
			}
		}
	}
	
	public static Class<?> getVM(String cloudProvider, String className){
		if(cloudProvider != null)
			cloudProvider = cloudProvider.trim().toLowerCase();
		if(className == null){
			if(cloudProvider == null)
				return VMMap.get("_DEFAULT");
			else{
				if(VMMap.containsKey(cloudProvider))
					return VMMap.get(cloudProvider);
				else
					return VMMap.get("_DEFAULT");
			}
		}else{
			try {
				Class<?> X = Class.forName(className);
				///if it is loaded, then it becomes a default class for this cloud provider
				if(cloudProvider != null 
						&& !VMMap.containsKey(cloudProvider))
					VMMap.put(cloudProvider, X);
				return X;
			} catch (ClassNotFoundException e) {
				e.getMessage();
				if(cloudProvider == null)
					return VMMap.get("_DEFAULT");
				///using the default classes, in order to avoid wrong configuration
				else
					return VMMap.get(cloudProvider);
			}
		}
	}
	
	public static Class<?> getSEngine(String cloudProvider, String className){
		if(cloudProvider != null)
			cloudProvider = cloudProvider.trim().toLowerCase();
		if(className == null){
			if(cloudProvider == null)
				return SEngineMap.get("_DEFAULT");
			else{
				if(SEngineMap.containsKey(cloudProvider))
					return SEngineMap.get(cloudProvider);
				else
					return SEngineMap.get("_DEFAULT");
			}
		}else{
			try {
				Class<?> X = Class.forName(className);
				///if it is loaded, then it becomes a default class for this cloud provider
				if(cloudProvider != null 
						&& !SEngineMap.containsKey(cloudProvider))
					SEngineMap.put(cloudProvider, X);
				return X;
			} catch (ClassNotFoundException e) {
				e.getMessage();
				if(cloudProvider == null)
					return SEngineMap.get("_DEFAULT");
				///using the default classes, in order to avoid wrong configuration
				else
					return SEngineMap.get(cloudProvider);
			}
		}
	}
	
	/**
	 * There is no default VEngine currently.
	 * @param cloudProvider
	 * @param className
	 * @param OSType
	 * @return
	 */
	public static Class<?> getVEngine(String cloudProvider, String className, String OSType){
		
		String queryOSType = OSType;
		if(OSType.toLowerCase().contains("ubuntu"))
			queryOSType = "ubuntu";
		if(OSType.toLowerCase().contains("centos"))
			queryOSType = "centos";
		if(OSType.toLowerCase().contains("redhat"))
			queryOSType = "redhat";
		
		
		if(cloudProvider != null)
			cloudProvider = cloudProvider.trim().toLowerCase();
		if(className == null){
			if(cloudProvider == null)
				return null;
			else{
				if(VEngineMap.containsKey(cloudProvider))
					return VEngineMap.get(cloudProvider).get(queryOSType);
				else
					////at least can configure on this VM, even the cloud provider is not supported currently.
					return VEngineMap.get("_DEFAULT").get(queryOSType);
			}
		}else{
			try {
				Class<?> X = Class.forName(className);
				///if it is loaded, then it becomes a default class for this cloud provider
				if(cloudProvider == null 
						&& !VEngineMap.get("_DEFAULT").containsKey(OSType)){
					Map<String, Class<?>> defaultOS = new HashMap<String, Class<?>>();
					defaultOS.put(OSType, X);
					VEngineMap.put("_DEFAULT", defaultOS);
				}
				if(cloudProvider != null) {
					Map<String, Class<?>> addOSMap = new HashMap<String, Class<?>>();
					if(VEngineMap.containsKey(cloudProvider)){
						if(!VEngineMap.get(cloudProvider).containsKey(OSType)){
							if(VEngineMap.get(cloudProvider).containsKey(queryOSType))
								addOSMap.put(OSType, X);
							else 
								addOSMap.put(queryOSType, X);
						}
					}else
						/////put queryOSType here in order to be more genenral
						addOSMap.put(queryOSType, X);
					
					if(addOSMap.keySet().size() == 1)
						VEngineMap.put(cloudProvider, addOSMap);
				}
					
				return X;
			} catch (ClassNotFoundException e) {
				e.getMessage();
				if(cloudProvider == null)
					return null;
				///using the default classes, in order to avoid wrong configuration
				else
					return VEngineMap.get("_DEFAULT").get(queryOSType);
			}
		}
	}
	
	public static Class<?> getDatabase(String cloudProvider, String className){
		if(cloudProvider != null)
			cloudProvider = cloudProvider.trim().toLowerCase();
		if(className == null){
			if(cloudProvider == null)
				return DatabaseMap.get("_DEFAULT");
			else{
				if(DatabaseMap.containsKey(cloudProvider))
					return DatabaseMap.get(cloudProvider);
				else
					return DatabaseMap.get("_DEFAULT");
			}
		}else{
			try {
				Class<?> X = Class.forName(className);
				///if it is loaded, then it becomes a default class for this cloud provider
				if(cloudProvider != null 
						&& !DatabaseMap.containsKey(cloudProvider))
					DatabaseMap.put(cloudProvider, X);
				return X;
			} catch (ClassNotFoundException e) {
				e.getMessage();
				if(cloudProvider == null)
					return DatabaseMap.get("_DEFAULT");
				///using the default classes, in order to avoid wrong configuration
				else
					return DatabaseMap.get(cloudProvider);
			}
		}
	}
	
	public static Class<?> getCredential(String cloudProvider, String className){
		if(cloudProvider != null)
			cloudProvider = cloudProvider.trim().toLowerCase();
		if(className == null){
			if(cloudProvider == null)
				return CredentialMap.get("_DEFAULT");
			else{
				if(CredentialMap.containsKey(cloudProvider))
					return CredentialMap.get(cloudProvider);
				else
					return CredentialMap.get("_DEFAULT");
			}
		}else{
			try {
				Class<?> X = Class.forName(className);
				///if it is loaded, then it becomes a default class for this cloud provider
				if(cloudProvider != null 
						&& !CredentialMap.containsKey(cloudProvider))
					CredentialMap.put(cloudProvider, X);
				return X;
			} catch (ClassNotFoundException e) {
				e.getMessage();
				if(cloudProvider == null)
					return CredentialMap.get("_DEFAULT");
				///using the default classes, in order to avoid wrong configuration
				else
					return CredentialMap.get(cloudProvider);
			}
		}
	}
	
	
	
	
	
}
