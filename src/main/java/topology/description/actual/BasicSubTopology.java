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
package topology.description.actual;

import java.util.ArrayList;

import com.fasterxml.jackson.annotation.JsonIgnore;


public class BasicSubTopology extends SubTopology{
	//Indicate different VMs.
	public ArrayList<BasicVM> VMs;

	@Override @JsonIgnore
	public VM getVMinSubClassbyName(String vmName) {
		if(this.VMs == null)
			return null;
		for(int i = 0 ; i<VMs.size() ; i++){
			if(VMs.get(i).name.equals(vmName)){
				return VMs.get(i);
			}
		}
		return null;
	}

	@Override   @JsonIgnore
	public ArrayList<VM> getVMsinSubClass() {
		if(this.VMs == null || VMs.size() == 0)
			return null;
		ArrayList<VM> vms = new ArrayList<VM>();
		for(int i = 0 ; i<VMs.size() ; i++)
			vms.add(VMs.get(i));
		return vms;
	}

	
	
	
}
