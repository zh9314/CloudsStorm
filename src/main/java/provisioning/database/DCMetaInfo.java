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
package provisioning.database;

import java.util.Map;


public abstract class DCMetaInfo {
	public String domain;
	public String endpoint;
	
	///location info. It is the three digits code for country.
	///Satisfy the standard of ISO 3166.
	public String country;
	
	////double number with a direction number
	public String longitude;
	public String latitude;
	
	public String availability; 
	

	public Map<String, String> extraInfo;
	
}
