package provisioning.engine.VEngine.ExoGENI;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.XMLOutputter;
import org.xml.sax.InputSource;

import topology.dataStructure.ExoGENI.ExoGENIVM;



public class INDLGenerator {
	
	     private static Namespace ns_ec2 = Namespace.getNamespace("ec2", "http://geni-orca.renci.org/owl/ec2.owl#");
	     private static Namespace ns_request;
	     private static Namespace ns_kansei = Namespace.getNamespace("kansei", "http://geni-orca.renci.org/owl/kansei.owl#");
	     private static Namespace ns_appcolor = Namespace.getNamespace("app-color", "http://geni-orca.renci.org/owl/app-color.owl#");
	     private static Namespace ns_geni = Namespace.getNamespace("geni", "http://geni-orca.renci.org/owl/geni.owl#");
	     private static Namespace ns_domain = Namespace.getNamespace("domain", "http://geni-orca.renci.org/owl/domain.owl#");
	     private static Namespace ns_eucalyptus = Namespace.getNamespace("eucalyptus", "http://geni-orca.renci.org/owl/eucalyptus.owl#");
	     private static Namespace ns_collections = Namespace.getNamespace("collections", "http://geni-orca.renci.org/owl/collections.owl#");
	     private static Namespace ns_openflow = Namespace.getNamespace("openflow", "http://geni-orca.renci.org/owl/openflow.owl#");
	     private static Namespace ns_xsd = Namespace.getNamespace("xsd", "http://www.w3.org/2001/XMLSchema#");
	     private static Namespace ns_rdf = Namespace.getNamespace("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
	     private static Namespace ns_exogeni = Namespace.getNamespace("exogeni", "http://geni-orca.renci.org/owl/exogeni.owl#");
	     private static Namespace ns_layer = Namespace.getNamespace("layer", "http://geni-orca.renci.org/owl/layer.owl#");
	     private static Namespace ns_rdfs = Namespace.getNamespace("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
	     private static Namespace ns_request_schema = Namespace.getNamespace("request-schema", "http://geni-orca.renci.org/owl/request.owl#");
	     private static Namespace ns_ip4 = Namespace.getNamespace("ip4", "http://geni-orca.renci.org/owl/ip4.owl#");
	     private static Namespace ns_planetlab = Namespace.getNamespace("planetlab", "http://geni-orca.renci.org/owl/planetlab.owl#");
	     private static Namespace ns_ethernet = Namespace.getNamespace("ethernet", "http://geni-orca.renci.org/owl/ethernet.owl#");
	     private static Namespace ns_dtn = Namespace.getNamespace("dtn", "http://geni-orca.renci.org/owl/dtn.owl#");
	     private static Namespace ns_time = Namespace.getNamespace("time", "http://www.w3.org/2006/time#");
	     private static Namespace ns_owl = Namespace.getNamespace("owl", "http://www.w3.org/2002/07/owl#");
	     private static Namespace ns_modify_schema = Namespace.getNamespace("modify-schema", "http://geni-orca.renci.org/owl/modify.owl#");
	     private static Namespace ns_compute = Namespace.getNamespace("compute", "http://geni-orca.renci.org/owl/compute.owl#");
	     private static Namespace ns_topology = Namespace.getNamespace("topology", "http://geni-orca.renci.org/owl/topology.owl#");
	     private static Namespace ns_orca = Namespace.getNamespace("orca", "http://geni-orca.renci.org/owl/orca.rdf#");
	     private static Namespace ns_j16 = Namespace.getNamespace("j.16", "http://geni-orca.renci.org/owl/topology.owl#");
	     
	
	public static Element getTermDuration(int days, String guid){
		Element e = new Element("Description");
		e.setNamespace(ns_rdf);
		Attribute atr_about = new Attribute("about", "http://geni-orca.renci.org/owl/"+guid+"#TermDuration");
		atr_about.setNamespace(ns_rdf);
        e.setAttribute(atr_about);
        Element time = new Element("days");
        time.setNamespace(ns_time);
        Attribute atr_datatype = new Attribute("datatype", "http://www.w3.org/2001/XMLSchema#decimal");
		atr_datatype.setNamespace(ns_rdf);
		time.setAttribute(atr_datatype);
        time.setText(Integer.toString(days));
        Element type = new Element("type");
        type.setNamespace(ns_rdf);
        Attribute atr_type = new Attribute("resource", "http://www.w3.org/2006/time#DurationDescription");
		atr_type.setNamespace(ns_rdf);
        type.setAttribute(atr_type);

        e.addContent(time);
        e.addContent(type);
        
        return e;
	}
	
	public static Element getTerm(String guid){
		Element e = new Element("Description");
		e.setNamespace(ns_rdf);
		Attribute atr_about = new Attribute("about", "http://geni-orca.renci.org/owl/"+guid+"#Term");
		atr_about.setNamespace(ns_rdf);
        e.setAttribute(atr_about);
        Element has = new Element("hasDurationDescription");
        has.setNamespace(ns_time);
        Attribute atr_resource = new Attribute("resource", "http://geni-orca.renci.org/owl/"+guid+"#TermDuration");
		atr_resource.setNamespace(ns_rdf);
        has.setAttribute(atr_resource);
        Element type = new Element("type");
        type.setNamespace(ns_rdf);
        Attribute atr_type = new Attribute("resource", "http://www.w3.org/2006/time#Interval");
		atr_type.setNamespace(ns_rdf);
        type.setAttribute(atr_type);
        
        e.addContent(has);
        e.addContent(type);
        
        return e;
	}
	
	////get the description element. It describe the ip of the node, which interface connects to link
	public static Element getIP(String node, String link, String ip, String netmask, String guid){
		String dashIP = ip.replace('.', '-');
		Element e = new Element("Description");
		e.setNamespace(ns_rdf);
		Attribute atr_about = new Attribute("about", "http://geni-orca.renci.org/owl/"+guid+"#"+link+"-"+node+"-ip-"+dashIP);
		atr_about.setNamespace(ns_rdf);
        e.setAttribute(atr_about);
        Element nm = new Element("netmask");
        nm.setNamespace(ns_ip4);
        nm.setText(netmask);
        Element layer = new Element("label_ID");
        layer.setNamespace(ns_layer);
        layer.setText(ip);
        Element type = new Element("type");
        type.setNamespace(ns_rdf);
        Attribute atr_resource = new Attribute("resource", "http://geni-orca.renci.org/owl/ip4.owl#IPAddress");
		atr_resource.setNamespace(ns_rdf);
        type.setAttribute(atr_resource);
        
        e.addContent(nm);
        e.addContent(layer);
        e.addContent(type);
        
        return e;
	}
	
	public static Element getOS(String OS, String guid, String OSurl, String OSguid){
		String dashOS = OS.replace("+", "%2B").replace("(", "%28").replace(")", "%29").replace(" ", "+");
		Element e = new Element("Description");
		e.setNamespace(ns_rdf);
		Attribute atr_about = new Attribute("about", "http://geni-orca.renci.org/owl/"+guid+"#"+dashOS);
		atr_about.setNamespace(ns_rdf);
		e.setAttribute(atr_about);
	    
		Element name = new Element("hasName");
		name.setNamespace(ns_topology);
		Attribute atr_datatype = new Attribute("datatype", "http://www.w3.org/2001/XMLSchema#string");
		atr_datatype.setNamespace(ns_rdf);
	    name.setAttribute(atr_datatype);
	    name.setText(OS);
	    Element imageUrl = new Element("hasURL");
	    imageUrl.setNamespace(ns_topology);
	    imageUrl.setText(OSurl);
	    Element imageGuid = new Element("hasGUID");
	    imageGuid.setNamespace(ns_topology);
	    imageGuid.setText(OSguid);
	    Element type = new Element("type");
	    type.setNamespace(ns_rdf);
	    Attribute atr_resource = new Attribute("resource", "http://geni-orca.renci.org/owl/compute.owl#DiskImage");
	    atr_resource.setNamespace(ns_rdf);
	    type.setAttribute(atr_resource);
	    
	    e.addContent(name);
	    e.addContent(imageUrl);
	    e.addContent(imageGuid);
	    e.addContent(type);
	    
		return e;
	}
	
	public static Element getInterface(String link, String node, String ip, String guid){
		Element e = new Element("Description");
		e.setNamespace(ns_rdf);
		Attribute atr_about = new Attribute("about", "http://geni-orca.renci.org/owl/"+guid+"#"+link+"-"+node);
		atr_about.setNamespace(ns_rdf);
		e.setAttribute(atr_about);
		
		Element address = new Element("localIPAddress");
		address.setNamespace(ns_ip4);
		String dashIP = ip.replace('.', '-');
		Attribute atr_resource = new Attribute("resource", "http://geni-orca.renci.org/owl/"+guid+"#"+link+"-"+node+"-ip-"+dashIP);
		atr_resource.setNamespace(ns_rdf);
		address.setAttribute(atr_resource);
		Element type = new Element("type");
		type.setNamespace(ns_rdf);
		Attribute atr_resource2 = new Attribute("resource", "http://geni-orca.renci.org/owl/topology.owl#Interface");
		atr_resource2.setNamespace(ns_rdf);
		type.setAttribute(atr_resource2);
		
		e.addContent(address);
		e.addContent(type);
		
		return e;
	}
	
	public static Element getNode(String [] link, String node, String OS, String endpoint, String vmType, String guid){
		Element e = new Element("Description");
		e.setNamespace(ns_rdf);
		Attribute atr_about = new Attribute("about", "http://geni-orca.renci.org/owl/"+guid+"#"+node);
		atr_about.setNamespace(ns_rdf);
		e.setAttribute(atr_about);
		
		if(link != null){
			for(int i = 0 ; i<link.length ; i++){
				Element interFace = new Element("hasInterface");
				interFace.setNamespace(ns_topology);
				Attribute atr_resource = new Attribute("resource", "http://geni-orca.renci.org/owl/"+guid+"#"+link[i]+"-"+node);
				atr_resource.setNamespace(ns_rdf);
				interFace.setAttribute(atr_resource);
				e.addContent(interFace);
			}
		}
		Element nodeGuid = new Element("hasGUID");
		nodeGuid.setNamespace(ns_topology);
		UUID uuid = UUID.randomUUID();
		nodeGuid.setText(uuid.toString());
		Element edm = new Element("inDomain");
		edm.setNamespace(ns_request_schema);
		Attribute atr_resource = new Attribute("resource", "http://geni-orca.renci.org/owl/"+endpoint+"/Domain");
		atr_resource.setNamespace(ns_rdf);
		edm.setAttribute(atr_resource);
		
		Element diskImage = new Element("diskImage");
		diskImage.setNamespace(ns_compute);
		String dashOS = OS.replace("+", "%2B").replace("(", "%28").replace(")", "%29").replace(" ", "+");
		Attribute atr_resource2 = new Attribute("resource", "http://geni-orca.renci.org/owl/"+guid+"#"+dashOS);
		atr_resource2.setNamespace(ns_rdf);
		diskImage.setAttribute(atr_resource2);
		Element vm = new Element("specificCE");
		vm.setNamespace(ns_compute);
		Attribute atr_resource3 = new Attribute("resource", "http://geni-orca.renci.org/owl/exogeni.owl#"+vmType);
		atr_resource3.setNamespace(ns_rdf);
		vm.setAttribute(atr_resource3);
		Element hasType = new Element("hasResourceType");
		hasType.setNamespace(ns_domain);
		Attribute atr_resource4 = new Attribute("resource", "http://geni-orca.renci.org/owl/compute.owl#VM");
		atr_resource4.setNamespace(ns_rdf);
		hasType.setAttribute(atr_resource4);
		Element type = new Element("type");
		type.setNamespace(ns_rdf);
		Attribute atr_resource5 = new Attribute("resource", "http://geni-orca.renci.org/owl/compute.owl#ComputeElement");
		atr_resource5.setNamespace(ns_rdf);
		type.setAttribute(atr_resource5);
		
		e.addContent(nodeGuid);
		e.addContent(edm);
		e.addContent(diskImage);
		e.addContent(vm);
		e.addContent(hasType);
		e.addContent(type);
		
		return e;
	}
	
	public static Element getLink(String link, String []node, int bw, String guid){
		Element e = new Element("Description");
		e.setNamespace(ns_rdf);
		Attribute atr_about = new Attribute("about", "http://geni-orca.renci.org/owl/"+guid+"#"+link);
		atr_about.setNamespace(ns_rdf);
		e.setAttribute(atr_about);
		
		for(int i = 0 ; i<node.length ; i++){
			Element interFace = new Element("hasInterface");
			interFace.setNamespace(ns_topology);
			Attribute atr_resource = new Attribute("resource", "http://geni-orca.renci.org/owl/"+guid+"#"+link+"-"+node[i]);
			atr_resource.setNamespace(ns_rdf);
			interFace.setAttribute(atr_resource);
			e.addContent(interFace);
		}
		Element layer = new Element("atLayer");
		layer.setNamespace(ns_layer);
		Attribute atr_resource2 = new Attribute("resource", "http://geni-orca.renci.org/owl/ethernet.owl#EthernetNetworkElement");
		atr_resource2.setNamespace(ns_rdf);
		layer.setAttribute(atr_resource2);
		Element bandwidth = new Element("bandwidth");
		bandwidth.setNamespace(ns_layer);
		Attribute atr_datatype = new Attribute("datatype", "http://www.w3.org/2001/XMLSchema#integer");
		atr_datatype.setNamespace(ns_rdf);
		bandwidth.setAttribute(atr_datatype);
		bandwidth.setText(Integer.toString(bw));
		Element linkGuid = new Element("hasGUID");
		linkGuid.setNamespace(ns_topology);
		UUID uuid = UUID.randomUUID();
	    linkGuid.setText(uuid.toString());
	    Element type = new Element("type");
	    type.setNamespace(ns_rdf);
	    Attribute atr_resource3 = new Attribute("resource", "http://geni-orca.renci.org/owl/topology.owl#NetworkConnection");
		atr_resource3.setNamespace(ns_rdf);
	    type.setAttribute(atr_resource3);
		
	    e.addContent(layer);
	    e.addContent(bandwidth);
	    e.addContent(linkGuid);
	    e.addContent(type);
	    
		return e;
	}
	
	public static Element getDomain(String endpoint, String guid){
		Element e = new Element("Description");
		e.setNamespace(ns_rdf);
		Attribute atr_about = new Attribute("about", "http://geni-orca.renci.org/owl/"+endpoint+"/Domain");
		atr_about.setNamespace(ns_rdf);
		e.setAttribute(atr_about);
		
		Element type = new Element("type");
		type.setNamespace(ns_rdf);
		Attribute atr_resource = new Attribute("resource", "http://geni-orca.renci.org/owl/topology.owl#NetworkDomain");
		atr_resource.setNamespace(ns_rdf);
		type.setAttribute(atr_resource);
		
		e.addContent(type);
		
		return e;
	    	
	}
	
	public static Element getTopology(String [] nodes, String guid){
		Element e = new Element("Description");
		e.setNamespace(ns_rdf);
		Attribute atr_about = new Attribute("about", "http://geni-orca.renci.org/owl/"+guid+"#");
		atr_about.setNamespace(ns_rdf);
		e.setAttribute(atr_about);
		
		
		/*for(int i = 0 ; i<links.length ; i++){
			Element interFace = new Element("element");
			interFace.setNamespace(ns_collections);
			Attribute atr_resource = new Attribute("resource", "http://geni-orca.renci.org/owl/"+guid+"#"+links[i]);
			atr_resource.setNamespace(ns_rdf);
			interFace.setAttribute(atr_resource);
			e.addContent(interFace);
		}*/
		for(int i = 0 ; i<nodes.length ; i++){
			Element interFace = new Element("element");
			interFace.setNamespace(ns_collections);
			Attribute atr_resource = new Attribute("resource", "http://geni-orca.renci.org/owl/"+guid+"#"+nodes[i]);
			atr_resource.setNamespace(ns_rdf);
			interFace.setAttribute(atr_resource);
			e.addContent(interFace);
		}
		
		Element term = new Element("hasTerm");
		term.setNamespace(ns_request_schema);
		Attribute atr_resource = new Attribute("resource", "http://geni-orca.renci.org/owl/"+guid+"#Term");
		atr_resource.setNamespace(ns_rdf);
		term.setAttribute(atr_resource);
		Element reservation = new Element("type");
		reservation.setNamespace(ns_rdf);
		Attribute atr_resource2 = new Attribute("resource", "http://geni-orca.renci.org/owl/request.owl#Reservation");
		atr_resource2.setNamespace(ns_rdf);
		reservation.setAttribute(atr_resource2);
		
		e.addContent(term);
		e.addContent(reservation);
		
		return e;
	    	
	}
	
	
	public String generateINDL(ArrayList<ExoGENIVM> nodeSet, 
			int validDay){
		
		
		
		Element root = new Element("RDF");
		UUID uuid = UUID.randomUUID();
		String guid = uuid.toString();
		
		ns_request = Namespace.getNamespace("request", "http://geni-orca.renci.org/owl/"+guid+"#");
		root.setNamespace(ns_rdf);
		root.addNamespaceDeclaration(ns_rdf);
		root.addNamespaceDeclaration(ns_appcolor);
		root.addNamespaceDeclaration(ns_collections);
		root.addNamespaceDeclaration(ns_compute);
		root.addNamespaceDeclaration(ns_domain);
		root.addNamespaceDeclaration(ns_dtn);
		root.addNamespaceDeclaration(ns_ec2);
		root.addNamespaceDeclaration(ns_ethernet);
		root.addNamespaceDeclaration(ns_eucalyptus);
		root.addNamespaceDeclaration(ns_exogeni);
		root.addNamespaceDeclaration(ns_geni);
		root.addNamespaceDeclaration(ns_ip4);
		root.addNamespaceDeclaration(ns_kansei);
		root.addNamespaceDeclaration(ns_layer);
		root.addNamespaceDeclaration(ns_modify_schema);
		root.addNamespaceDeclaration(ns_openflow);
		root.addNamespaceDeclaration(ns_orca);
		root.addNamespaceDeclaration(ns_owl);
		root.addNamespaceDeclaration(ns_planetlab);
		root.addNamespaceDeclaration(ns_rdfs);
		root.addNamespaceDeclaration(ns_request);
		root.addNamespaceDeclaration(ns_request_schema);
		root.addNamespaceDeclaration(ns_time);
		root.addNamespaceDeclaration(ns_topology);
		root.addNamespaceDeclaration(ns_xsd);
		
		Element termDuration = getTermDuration(validDay, guid);
		Element term = getTerm(guid);
		root.addContent(term);
		root.addContent(termDuration);
		for(int i = 0 ; i<nodeSet.size() ; i++){
			ExoGENIVM tmpVM = nodeSet.get(i);
			String tmpLinks [] = null;
			/*if(tmpVM.ethernetPort != null){
				tmpLinks = new String[tmpVM.ethernetPort.size()];
				for(int j = 0 ; j<tmpVM.ethernetPort.size() ; j++){
					Eth tmpEth = tmpVM.ethernetPort.get(j);
					if(tmpEth.subnetName == null)
					{
						int pointIndex = tmpEth.connectionName.lastIndexOf('.');
						String linkName = tmpEth.connectionName.substring(0, pointIndex);
						Element ip = getIP(tmpVM.name, linkName, tmpEth.scp.address, tmpEth.scp.netmask, guid);
						Element interFace = getInterface(linkName, tmpVM.name, tmpEth.scp.address, guid);
						root.addContent(ip);
						root.addContent(interFace);
						tmpLinks[j] = linkName;
					}
				}
			}*/
			Element nodeInfo = getNode(tmpLinks, tmpVM.name, tmpVM.OStype, tmpVM.endpoint, tmpVM.nodeType, guid);
			root.addContent(nodeInfo);
		}
		
		ArrayList<String> OSList = new ArrayList<String>();
		ArrayList<String> endpointList = new ArrayList<String>();
		for(int i = 0 ; i<nodeSet.size() ; i++){
			ExoGENIVM curVM = nodeSet.get(i);
			String tmpOS = curVM.OStype;
			String tmpEd = curVM.endpoint;
			boolean findOS = false;
			boolean findDomain = false;
			
			String OSguid = curVM.OS_GUID, OSurl = curVM.OS_URL;
			for(int j = 0; j<OSList.size() ; j++){
				if(tmpOS.equals(OSList.get(j))){
					findOS = true;
					break;
				}
			}
			if(!findOS){
				OSList.add(tmpOS);
				Element os = getOS(OSList.get(i), guid, OSurl, OSguid);
				root.addContent(os);
			}
			for(int j = 0 ; j<endpointList.size() ; j++){
				if(tmpEd.equals(endpointList.get(j))){
					findDomain = true;
					break;
				}
			}
			if(!findDomain){
				endpointList.add(tmpEd);
				Element domain = getDomain(tmpEd, guid);
				root.addContent(domain);
			}
		}
		
		////get Links
		/*for(int i = 0 ; i<linkSet.size() ; i++){
			String nodes [] = new String[2];
			SubConnection tmpLink =linkSet.get(i);
			nodes[0] = tmpLink.source.componentName;
			nodes[1] = tmpLink.target.componentName;
			Element link = getLink(tmpLink.name, nodes, Integer.valueOf(tmpLink.bandwidth), guid);
			root.addContent(link);
		}*/
		
		//String allLink [] = new String[linkSet.size()];
		String allNode [] = new String[nodeSet.size()];
 		for(int i = 0 ; i<nodeSet.size() ; i++)
 			allNode[i] = nodeSet.get(i).name;
 		/*for(int i = 0 ; i<linkSet.size() ; i++)
 			allLink[i] = linkSet.get(i).name;*/
		Element topology = getTopology(allNode, guid);
		root.addContent(topology);
		
        Document Doc = new Document(root);
        
        XMLOutputter XMLOut = new XMLOutputter();
        return XMLOut.outputString(Doc);
	}
	
	
	///return a set of public address pair. nodeName::PublicIP
	public ArrayList<String> getPublicIPs(String status){
		ArrayList<String> publicIPs = new ArrayList<String>();
		SAXBuilder saxBuilder = new SAXBuilder();  
		try {
			StringReader read = new StringReader(status);
			InputSource source = new InputSource(read);
			Document doc = saxBuilder.build(source);
			Element root = doc.getRootElement();
			List<Element> children = root.getChildren("Description", ns_rdf);
			for(int i = 0 ; i<children.size() ; i++){
				Element cur = children.get(i);
				Attribute test = cur.getAttribute("about", ns_rdf);
				if(test != null){
					String atrS = test.toString();
					if(atrS.contains("/Service")){
						int begin = atrS.indexOf("#");
						int end = atrS.lastIndexOf("/S");
						String nodeName = atrS.substring(begin+1, end);
						Element son = cur.getChild("managementIP", ns_j16);
						String publicIP = son.getText();
						publicIPs.add(nodeName+"::"+publicIP);
					}
				}
			}
		} catch (JDOMException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		 return publicIPs;
	}

}
