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
