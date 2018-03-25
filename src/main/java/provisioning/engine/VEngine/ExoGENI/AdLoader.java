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
package provisioning.engine.VEngine.ExoGENI;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import orca.ndl.DomainResourceType;
import orca.ndl.INdlAbstractDelegationModelListener;
import orca.ndl.elements.LabelSet;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * Basic parser of Ads provided by an SM
 * @author ibaldin
 *
 */
public class AdLoader implements INdlAbstractDelegationModelListener {
	String domain = null;
	
	protected  Map<String, Integer> hardwareTypeSlots = new HashMap<String, Integer>();
	
	public static final String ORCA_HT_CLOUD = "VMs";
	public static final String ORCA_LOCAL_NET = "Local VLANs";
	public static final String ORCA_TRANSIT_NET = "Transit VLANs";
	public static final String ORCA_STATIC_NET = "Static VLANs";
	public static final String ORCA_XCAT_CLOUD = "Baremetal Nodes";
	public static final String ORCA_ISCSI_STORAGE = "iSCSI LUNs";
	
	@Override
	public void ndlNetworkDomain(Resource dom, OntModel m,
			List<Resource> netServices, List<Resource> interfaces,
			List<LabelSet> labelSets, Map<Resource, List<LabelSet>> netLabelSets) {
		domain = dom.toString();
                		
		String htName = "unknown";

		for(LabelSet ls: labelSets) {
			// process hardware types
			//HardwareTypeContents ht = fact.createHardwareTypeContents();
			if (DomainResourceType.VM_RESOURCE_TYPE.equals(ls.LabelType)) {
				htName = ORCA_HT_CLOUD;
				hardwareTypeSlots.put(htName, ls.getSetSize());
			} else if (DomainResourceType.BM_RESOURCE_TYPE.equals(ls.LabelType)) {
				htName = ORCA_XCAT_CLOUD;
				hardwareTypeSlots.put(htName, ls.getSetSize());
			} else if (DomainResourceType.VLAN_RESOURCE_TYPE.equals(ls.LabelType)) {
				if (interfaces.size() == 1) {
					// local vlans - should not happen - 
					// we filtered them out above
				}
				else {
					if (ls.getIsAllocatable()) {
						// FIXME: ugly hack 06/25/13 /ib
						if (!dom.toString().contains("vmsite")) 
							htName = ORCA_TRANSIT_NET;
						else {
							htName = ORCA_LOCAL_NET;
						}
						hardwareTypeSlots.put(htName, ls.getSetSize());
					}
					else 
						htName = ORCA_STATIC_NET;
				}
			} else if (DomainResourceType.LUN_RESOURCE_TYPE.equals(ls.LabelType)) {
				htName = ORCA_ISCSI_STORAGE;
				hardwareTypeSlots.put(htName, ls.getSetSize());
			} 
		}
	}

	@Override
	public void ndlInterface(Resource l, OntModel om, Resource conn,
			Resource node, String ip, String mask) {
		// TODO Auto-generated method stub

	}

	@Override
	public void ndlNetworkConnection(Resource l, OntModel om, long bandwidth,
			long latency, List<Resource> interfaces) {
		// TODO Auto-generated method stub

	}

	@Override
	public void ndlNode(Resource ce, OntModel om, Resource ceClass,
			List<Resource> interfaces) {
		// TODO Auto-generated method stub

	}

	@Override
	public void ndlParseComplete() {
		// TODO Auto-generated method stub

	}
	
	public String getDomain() {
		return domain;
	}

	public Map<String, Integer> getSlots() {
		return hardwareTypeSlots;
	}
	
}
