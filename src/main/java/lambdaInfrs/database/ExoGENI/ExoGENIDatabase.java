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
package lambdaInfrs.database.ExoGENI;

import lambdaInfrs.database.BasicDatabase;

public class ExoGENIDatabase extends BasicDatabase{

	
	public String GlobalEntry;
	
	
	/*private void initDomainMap(){
		domainMap.put("RENCI (Chapel Hill, NC USA) XO Rack", "rcivmsite.rdf#rcivmsite");
		domainMap.put("BBN/GPO (Boston, MA USA) XO Rack", "bbnvmsite.rdf#bbnvmsite");
		domainMap.put("Duke CS (Durham, NC USA) XO Rack", "dukevmsite.rdf#dukevmsite");
		domainMap.put("UNC BEN (Chapel Hill, NC USA)", "uncvmsite.rdf#uncvmsite");
		domainMap.put("RENCI BEN (Chapel Hill, NC USA)", "rencivmsite.rdf#rencivmsite");
		domainMap.put("NICTA (Sydney, Australia) XO Rack", "nictavmsite.rdf#nictavmsite");
		domainMap.put("FIU (Miami, FL USA) XO Rack", "fiuvmsite.rdf#fiuvmsite");
		domainMap.put("UH (Houston, TX USA) XO Rack", "uhvmsite.rdf#uhvmsite");
		domainMap.put("UvA (Amsterdam, The Netherlands) XO Rack", "uvanlvmsite.rdf#uvanlvmsite");
		domainMap.put("UFL (Gainesville, FL USA) XO Rack", "uflvmsite.rdf#uflvmsite");
		domainMap.put("UCD (Davis, CA USA) XO Rack", "ucdvmsite.rdf#ucdvmsite");
		domainMap.put("OSF (Oakland, CA USA) XO Rack", "osfvmsite.rdf#osfvmsite");
		domainMap.put("SL (Chicago, IL USA) XO Rack", "slvmsite.rdf#slvmsite");
		domainMap.put("WVN (UCS-B series rack in Morgantown, WV, USA)", "wvnvmsite.rdf#wvnvmsite");
		domainMap.put("NCSU (UCS-B series rack at NCSU)", "ncsuvmsite.rdf#ncsuvmsite");
		domainMap.put("NCSU2 (UCS-C series rack at NCSU)", "ncsu2vmsite.rdf#ncsu2vmsite");
		domainMap.put("TAMU (College Station, TX, USA) XO Rack", "tamuvmsite.rdf#tamuvmsite");
		domainMap.put("UMass (UMass Amherst, MA, USA) XO Rack", "umassvmsite.rdf#umassvmsite");
		domainMap.put("WSU (Detroit, MI, USA) XO Rack", "wsuvmsite.rdf#wsuvmsite");
		domainMap.put("UAF (Fairbanks, AK, USA) XO Rack", "uafvmsite.rdf#uafvmsite");
		domainMap.put("PSC (Pittsburgh, PA, USA) XO Rack", "pscvmsite.rdf#pscvmsite");
		domainMap.put("GWU (Washington DC,  USA) XO Rack", "gwuvmsite.rdf#gwuvmsite");
		domainMap.put("CIENA (Ottawa,  CA) XO Rack", "cienavmsite.rdf#cienavmsite");
	}
	
	private void initOS(){
		OSMap.put("Ubuntu 14.04", new OSdata("http://geni-images.renci.org/images/standard/ubuntu/ub1404-v1.0.4.xml", 
				"9394ca154aa35eb55e604503ae7943ddaecc6ca5"));
		OSMap.put("Centos 6.7 v1.0.0", new OSdata("http://geni-images.renci.org/images/standard/centos/centos6.7-v1.0.0/centos6.7-v1.0.0.xml", 
				"dceedc1e70bd4d8d95bb4e92197f64217d17eea0"));
		OSMap.put("Apache Storm", new OSdata("http://geni-images.renci.org/images/dcvan/storm/Storm-SiteAware-v0.2/Storm-SiteAware-v0.2.xml", 
				"047baee53ecb3455b8c527f064194cad9a67771a"));
		OSMap.put("Debian 6 (squeeze)", new OSdata("http://geni-images.renci.org/images/standard/debian/deb6-neuca-v1.0.8.xml", 
				"d1044d9162bd7851e3fc2c57a8251ad6b3641c0c"));
		OSMap.put("Debian 6 (squeeze) + Hadoop", new OSdata("http://geni-images.renci.org/images/hadoop/deb-sparse-hadoop-10G.v0.2.xml", 
				"e1948a8b67d01b93fd0fb1f78ba5c2b3ce0b41f1"));
		OSMap.put("Debian 6 (squeeze) v1.0.10", new OSdata("http://geni-images.renci.org/images/standard/debian/deb6.v1.0.10.xml", 
				"c120b9d79d3f3882114c0e59cce14f671ef9b0db"));
		OSMap.put("Debian 6 (squeeze) v1.0.9", new OSdata("http://geni-images.renci.org/images/standard/debian/deb6-neuca-v1.0.9.xml", 
				"e1972b5a5b30fa1adbd42f2df1effbd40084fb3e"));
		OSMap.put("Debian 6 (squeeze) with OVS", new OSdata("http://geni-images.renci.org/images/standard/debian/deb6-ovs-neuca-v1.0.3.xml", 
				"ef7e0b4883e23c218d19b0f22980436020c72b4d"));
		OSMap.put("UDebian-6-Standard-Multi-Size-Image-v.1.0.6", new OSdata("http://geni-images.renci.org/images/standard/debian/deb6-neuca-v1.0.7.xml", 
				"ba15fa6f56cc00d354e505259b9cb3804e1bcb73"));
		OSMap.put("Delay.v1", new OSdata("http://geni-images.renci.org/images/tqian/u64_32_delay.xml", 
				"37be17f937c259d2068ad59060a745c033aa3145"));
		OSMap.put("UDocker-v0.1", new OSdata("http://geni-images.renci.org/images/ibaldin/docker/centos6.6-docker-v0.1/centos6.6-docker-v0.1.xml", 
				"b2262a8858c9c200f9f43d767e7727a152a02248"));
		OSMap.put("Fedd-enabled Ubuntu 12.04", new OSdata("http://www.isi.edu/~faber/tmp/fedd.xml", 
				"05cf5d86906c11cdb35ece535d2539fe38481d17"));
		OSMap.put("Fedora 22", new OSdata("http://geni-images.renci.org/images/standard/fedora/fedora22-v1.0.xml", 
				"4fdd820d481f9afe8b9a48ec53dc54d50982d266"));
		OSMap.put("GIMI", new OSdata("http://emmy9.casa.umass.edu/Disk_Images/ExoGENI/exogeni-umass-1.2.xml", 
				"49f0c193cc91d7b2fc1a6f038427935f4c296a8a"));
		OSMap.put("Hadoop 2.7.1 (Centos7)", new OSdata("http://geni-images.renci.org/images/pruth/standard/hadoop/Hadoop-Centos7-v0.1/hadoop-centos7.v0.1.1.xml", 
				"af212901b35c96e1b2abed7a937882fcae81a513"));
		OSMap.put("OpenDaylight", new OSdata("http://geni-images.renci.org/images/cisco-demos/opendaylight-1.0.0.xml", 
				"9b1001c38f203522b1a3cec15b675243b567cc71"));
		OSMap.put("Ubuntu 12.04", new OSdata("http://emmy9.casa.umass.edu/Disk_Images/ExoGENI/Ubuntu12.04-1.0.2/ubuntu12.04-1.0.2.xml", 
				"8ee8735fa6a4f102313f7a29f6fd0918b8ed5fc4"));
		OSMap.put("Ubuntu 13.04 + OVS + OpenDaylight", new OSdata("http://geni-images.renci.org/images/standard/ubuntu/ub1304-ovs-opendaylight-v1.0.0.xml", 
				"608a5757ccb2bbe3b3bb5c85e8fa1f2c3e712258"));
		OSMap.put("perfSonar-v0.3", new OSdata("http://geni-images.renci.org/images/ibaldin/perfSonar/psImage-v0.3/psImage-v0.3.xml", 
				"e45a2c809729c1eb38cf58c4bff235510da7fde5"));
	}*/

	

}
