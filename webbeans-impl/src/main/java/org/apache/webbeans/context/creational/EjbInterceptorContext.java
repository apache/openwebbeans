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
package org.apache.webbeans.context.creational;

import java.io.Serializable;

import org.apache.webbeans.inject.OWBInjector;

public class EjbInterceptorContext implements Serializable
{
    private static final long serialVersionUID = 1L;

    private Object interceptorInstance;
    
    private OWBInjector injectorInstance;
    
    public EjbInterceptorContext()
    {
        
    }

    /**
     * @return the interceptorInstance
     */
    public Object getInterceptorInstance()
    {
        return interceptorInstance;
    }

    /**
     * @param interceptorInstance the interceptorInstance to set
     */
    public void setInterceptorInstance(Object interceptorInstance)
    {
        this.interceptorInstance = interceptorInstance;
    }

    /**
     * @return the injectorInstance
     */
    public OWBInjector getInjectorInstance()
    {
        return injectorInstance;
    }

    /**
     * @param injectorInstance the injectorInstance to set
     */
    public void setInjectorInstance(OWBInjector injectorInstance)
    {
        this.injectorInstance = injectorInstance;
    }
    
    
}
