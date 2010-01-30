/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.webbeans.proxy;

public class DependentBeanProxy
{
    private Object proxyInstance;
    
    private Object actualInstance;
    
    public DependentBeanProxy()
    {
        
    }

    /**
     * @return the proxyInstance
     */
    public Object getProxyInstance()
    {
        return proxyInstance;
    }

    /**
     * @param proxyInstance the proxyInstance to set
     */
    public void setProxyInstance(Object proxyInstance)
    {
        this.proxyInstance = proxyInstance;
    }

    /**
     * @return the actualInstance
     */
    public Object getActualInstance()
    {
        return actualInstance;
    }

    /**
     * @param actualInstance the actualInstance to set
     */
    public void setActualInstance(Object actualInstance)
    {
        this.actualInstance = actualInstance;
    }
    
    
}
