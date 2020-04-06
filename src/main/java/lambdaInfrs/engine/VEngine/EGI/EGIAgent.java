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
package lambdaInfrs.engine.VEngine.EGI;


import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import cz.cesnet.cloud.occi.Model;
import cz.cesnet.cloud.occi.api.Client;
import cz.cesnet.cloud.occi.api.EntityBuilder;
import cz.cesnet.cloud.occi.api.exception.CommunicationException;
import cz.cesnet.cloud.occi.api.exception.EntityBuildingException;
import cz.cesnet.cloud.occi.api.http.HTTPClient;
import cz.cesnet.cloud.occi.api.http.auth.HTTPAuthentication;
import cz.cesnet.cloud.occi.api.http.auth.VOMSAuthentication;
import cz.cesnet.cloud.occi.core.ActionInstance;
import cz.cesnet.cloud.occi.core.Attribute;
import cz.cesnet.cloud.occi.core.Entity;
import cz.cesnet.cloud.occi.core.Link;
import cz.cesnet.cloud.occi.core.Mixin;
import cz.cesnet.cloud.occi.core.Resource;
import cz.cesnet.cloud.occi.exception.AmbiguousIdentifierException;
import cz.cesnet.cloud.occi.exception.InvalidAttributeValueException;
import cz.cesnet.cloud.occi.infrastructure.IPNetworkInterface;
import cz.cesnet.cloud.occi.infrastructure.NetworkInterface;
import cz.cesnet.cloud.occi.infrastructure.Storage;
import cz.cesnet.cloud.occi.parser.MediaType;

public class EGIAgent {
	
	private Client client;
	private Model model ;
	private HTTPAuthentication authentication;
	private EntityBuilder eb;
	
	public int retryTimes = 2;   ////Used for retrying when sometimes failed
	
	public EGIAgent(String ProxyPath, String TrustedCertPath){
		authentication = new VOMSAuthentication(ProxyPath);
		authentication.setCAPath(TrustedCertPath);
	}
	
	////Set the endpoint of the EGI client and make the client connected
	public boolean initClient(String OcciEndPoint){
		try {
			client = new HTTPClient(URI.create(OcciEndPoint),
			        authentication, MediaType.TEXT_PLAIN, false);
			client.connect();
			model = client.getModel();
			eb = new EntityBuilder(model);
		} catch (CommunicationException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	public URI createComputeVM(String vmName, String pubKeyString, String pubKeyId, 
			String OSTpl, String resourceTpl){
		 Properties properties = new Properties();
		 properties.setProperty("RESOURCE", "compute");
		 properties.setProperty("OCCI_OS_TPL", OSTpl);
		 properties.setProperty("OCCI_RESOURCE_TPL", resourceTpl);
		 properties.setProperty("PUBLIC_KEY_STRING", pubKeyString);
		 properties.setProperty("OCCI_PUBLICKEY_NAME", pubKeyId);
		 properties.setProperty("OCCI_CORE_TITLE", vmName);
		 URI info = doCreate(properties);
		 return info;
	}
	
	///return the state of the vm, it should be "active". 
	public String getVMStatus(String vmURI){
		Properties properties = new Properties();
		properties.setProperty("OCCI_RESOURCE_ID", vmURI);
		List<Entity> infos = doDescribe(properties);
		if(infos == null)
			return "inactive";
		Entity info = infos.get(0);
		String state = "";
		Map<Attribute, String> map = info.getAttributes();
		state = map.get(new Attribute("occi.compute.state"));
		return state;
	}
	
	////Get the public address of a VM. If it cannot have a public 
	////address, then it returns null. If it can have a public but not 
	////active yet, then it returns "".
	public String getPubAddress(String vmURI){
		Properties properties = new Properties();
		properties.setProperty("OCCI_RESOURCE_ID", vmURI);
		List<Entity> infos = doDescribe(properties);
		Entity info = infos.get(0);
		Resource resource = (Resource) info;
		Set<Link> links = resource.getLinks(NetworkInterface.TERM_DEFAULT);
		boolean pubTitleExist = false;
		for (Link link : links) {
			String titleAttri = link.getValue(IPNetworkInterface.TITLE_ATTRIBUTE_NAME);
			if(titleAttri != null && titleAttri.toLowerCase().contains("public")){
				pubTitleExist = true;
				String nicState = link.getValue(IPNetworkInterface.STATE_ATTRIBUTE_NAME);
				if(nicState == null)
					return "";
				if(!nicState.toLowerCase().equals("active"))
					return "";
				else{
					String pubAddress = link.getValue(IPNetworkInterface.ADDRESS_ATTRIBUTE_NAME);
					if(pubAddress == null)
						return "";
					else
						return pubAddress;
				}
			}
		}
		
		if(!pubTitleExist)
			return null;
		
		return "";
		
	}
	
	///attach the public network
	public boolean attachPublicNetwork(String vmResource, String pubNetwork){
        try {
        		NetworkInterface ni = eb.getNetworkInterface();
        		List<Entity> infos = client.describe(URI.create(vmResource));
        		Entity info = infos.get(0);
        		Resource vr = (Resource)info;
            ni.setSource(vr);
            ni.setTarget(pubNetwork);
			URI location = client.create(ni);
			if(location != null)
				return true;
			else
				return false;
		} catch (CommunicationException | EntityBuildingException | InvalidAttributeValueException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	////Return the information of creating instances.
    private URI doCreate(Properties properties)
    {
        URI createdLocation = null;

        try {
		if (properties.getProperty("RESOURCE").equals("compute")) {

			String [] segments = properties.getProperty("OCCI_OS_TPL").split("#");
			String OCCI_OS_TPL = segments[segments.length - 1];

			String [] segments2 = properties.getProperty("OCCI_RESOURCE_TPL").split("#");
			String OCCI_RESOURCE_TPL = segments2[segments2.length - 1];
	
			// Creating a compute instance
			Resource compute = eb.getResource("compute");
            Mixin mixin1 = model.findMixin(OCCI_OS_TPL, "os_tpl");
            Mixin mixin2 = model.findMixin(OCCI_RESOURCE_TPL, "resource_tpl");
	        if(mixin1 == null && mixin2 == null){
	        		return null;
	        }
	        if(mixin1 != null)
	        		compute.addMixin(mixin1);
	        if(mixin2 != null)
	        		compute.addMixin(mixin2);
	                
			// Checking the context
			if (properties.getProperty("PUBLIC_KEY_STRING") != null && 
                   !properties.getProperty("PUBLIC_KEY_STRING").isEmpty()) 
			{				
				String _public_key_string = 
                        properties.getProperty("PUBLIC_KEY_STRING").trim();

				// Add SSH public key
	        	    compute
				.addMixin(model.findMixin(URI.create("http://schemas.openstack.org/instance/credentials#public_key")));

		        compute
				.addAttribute("org.openstack.credentials.publickey.data", _public_key_string);
		
				// Add the name for the public key	 
		        compute.addAttribute("org.openstack.credentials.publickey.name",
						properties.getProperty("OCCI_PUBLICKEY_NAME"));
			} 

        	    // Set VM title or name
			compute.setTitle(properties.getProperty("OCCI_CORE_TITLE"));
			createdLocation = client.create(compute);

		} 
		
		if (properties.getProperty("RESOURCE").equals("storage")) {
            // Creating a storage instance
			Storage storage = eb.getStorage();
 			storage.setTitle(properties.getProperty("OCCI_CORE_TITLE"));
			storage.setSize(properties.getProperty("OCCI_STORAGE_SIZE"));
		       	
			createdLocation = client.create(storage);
			
		} // end 'storage'

		} catch (InvalidAttributeValueException | CommunicationException | EntityBuildingException | AmbiguousIdentifierException ex) {
			return null;
		} 
        return createdLocation;
    }
    
    
    ////Get the public network URI in this domain. It will return null, if there is no explicit public network.
    public URI getPublicNetworkURI(){
    		Properties properties = new Properties();
    		properties.setProperty("RESOURCE", "network");
    		List<URI> results = doList(properties);
    		if(results == null)
    			return null;
    		for(URI result : results){
    			if(result.toString().toLowerCase().contains("public"))
    				return result;
    		}
    		return null;
    	
    }
    
    public boolean startVM(String VMURI){
	    	ActionInstance actionInstance;
		try {
			actionInstance = eb.getActionInstance(URI.create("http://schemas.ogf.org/occi/infrastructure/compute/action#start"));
			boolean status = client.trigger(URI
					.create(VMURI), actionInstance);

			return status;
		} catch (EntityBuildingException | CommunicationException e) {
			e.printStackTrace();
			return false;
		}
    }
    
    public boolean deleteVM(String VMURI){
		try {
			boolean status = client.delete(URI.create(VMURI));
			return status;
		} catch (CommunicationException e) {
			e.printStackTrace();
		}
		return false;
}
    
    public boolean stopVM(String VMURI){
    		ActionInstance actionInstance;
		try {
			actionInstance = eb.getActionInstance(URI.create("http://schemas.ogf.org/occi/infrastructure/compute/action#stop"));
			boolean status = client.trigger(URI
					.create(VMURI), actionInstance);

			return status;
		} catch (EntityBuildingException | CommunicationException e) {
			e.printStackTrace();
			return false;
		}
    }
	
	// Listing cloud resources from provider.
    // Available resources that can be listed via API are the following:
    // - compute = computing resources, 
    // - storage = storage resources,
    // - network = network resources.
    private List<URI> doList (Properties properties)
    {
    		List<URI> list = null;
    		try {
		if (properties.getProperty("RESOURCE").equals("compute")) 
			list = client.list("compute");

		if (properties.getProperty("RESOURCE").equals("storage"))
			list = client.list("storage");

		if (properties.getProperty("RESOURCE").equals("network"))
			list = client.list("network");
		
		} catch (CommunicationException ex) {
			return null;
		}
		return list;
    }
    
    private List<Entity> doDescribe(Properties properties)
    {
    	
    		List<Entity> infos = null;
		try {
	
		if (properties.getProperty("OCCI_RESOURCE_ID").contains("compute")) 
		    infos = client.describe(URI.create(properties.getProperty("OCCI_RESOURCE_ID")));
        		
		
		if (properties.getProperty("OCCI_RESOURCE_ID").contains("storage")) 
			infos = client.describe(URI.create(properties.getProperty("OCCI_RESOURCE_ID")));


		if (properties.getProperty("OCCI_RESOURCE_ID").contains("network")) 
			infos = client.describe(URI.create(properties.getProperty("OCCI_RESOURCE_ID")));

		} catch (CommunicationException ex) {
			return null;
		}
		
		return infos;
    }

}
