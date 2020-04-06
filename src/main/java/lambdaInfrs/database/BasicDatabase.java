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
package lambdaInfrs.database;

import java.util.ArrayList;


public class BasicDatabase extends Database{
	
	public ArrayList<BasicDCMetaInfo> DCMetaInfo;
	
	@Override
	public String getEndpoint(String domain) {
		if(domain == null)
			return null;
		for(int di = 0 ; di < this.DCMetaInfo.size(); di++)
			if(DCMetaInfo.get(di).domain != null
			 && domain.trim().equalsIgnoreCase(DCMetaInfo.get(di).domain.trim()))
				return DCMetaInfo.get(di).endpoint;
		return null;
	}
	
	@Override
	public DCMetaInfo getDCMetaInfo(String domain) {
		if(domain == null)
			return null;
		for(int di = 0 ; di < this.DCMetaInfo.size(); di++)
			if(DCMetaInfo.get(di).domain != null
			 && domain.trim().equalsIgnoreCase(DCMetaInfo.get(di).domain.trim()))
				return DCMetaInfo.get(di);
		return null;
	}

	@Override
	public VMMetaInfo getVMMetaInfo(String domain, String OS, String vmType) {
		if(domain == null || OS == null || vmType == null)
			return null;
		for(int di = 0 ; di < this.DCMetaInfo.size(); di++)
			if(this.DCMetaInfo.get(di).domain != null
			 && domain.trim().equalsIgnoreCase(DCMetaInfo.get(di).domain.trim())){
				for(int vi = 0 ; vi < DCMetaInfo.get(di).VMMetaInfo.size() ; vi++){
					BasicVMMetaInfo curInfo = DCMetaInfo.get(di).VMMetaInfo.get(vi);
					if(curInfo.OS != null
				      && curInfo.VMType != null
				      && OS.trim().equalsIgnoreCase(curInfo.OS.trim())
				      && vmType.trim().equalsIgnoreCase(curInfo.VMType.trim()))
						return (VMMetaInfo)curInfo;
				}
			}
		return null;
	}

	@Override
	public String getVMType(String domain, String OS, double vCPUNum, double mem) {
		if(domain == null || OS == null || vCPUNum <= 0 || mem <= 0)
			return null;
		String VMType = null;
		double closeRatio = 10000;
		for(int di = 0 ; di < this.DCMetaInfo.size(); di++)
			if(this.DCMetaInfo.get(di).domain != null
			 && domain.trim().equalsIgnoreCase(DCMetaInfo.get(di).domain.trim())){
				for(int vi = 0 ; vi < DCMetaInfo.get(di).VMMetaInfo.size() ; vi++){
					BasicVMMetaInfo curInfo = DCMetaInfo.get(di).VMMetaInfo.get(vi);
					if(OS.trim().equalsIgnoreCase(curInfo.OS.trim()))
					{
						double curCloseRatio = (vCPUNum - Double.valueOf(curInfo.CPU))
												+ (mem - Double.valueOf(curInfo.MEM));
						if(curCloseRatio < 0)
							curCloseRatio = 0 - curCloseRatio;
						if(curCloseRatio < closeRatio){
							closeRatio = curCloseRatio;
							VMType = curInfo.VMType;
						}
					}
				}
			}
		
		return VMType;
	}
	
}
