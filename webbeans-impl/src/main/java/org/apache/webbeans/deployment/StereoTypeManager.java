/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.webbeans.deployment;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.webbeans.config.WebBeansFinder;
import org.apache.webbeans.deployment.stereotype.IStereoTypeModel;
import org.apache.webbeans.util.Asserts;

public class StereoTypeManager
{
	private Map<String, IStereoTypeModel> stereoTypeMap = new ConcurrentHashMap<String, IStereoTypeModel>();
	
	public StereoTypeManager()
	{
		
	}
	
	public static StereoTypeManager getInstance()
	{
		StereoTypeManager instance = (StereoTypeManager)WebBeansFinder.getSingletonInstance(WebBeansFinder.SINGLETON_STEREOTYPE_MANAGER);
		return instance;
	}
	
	public void addStereoTypeModel(IStereoTypeModel model)
	{
		Asserts.assertNotNull(model, "model parameter can not be null");
		
		stereoTypeMap.put(model.getName(), model);
	}
	
	public IStereoTypeModel getStereoTypeModel(String modelName)
	{
		Asserts.assertNotNull(modelName, "modelName parameter can not be null");
		
		if(stereoTypeMap.containsKey(modelName))
		{
			return stereoTypeMap.get(modelName);
		}
		
		return null;
	}
}
