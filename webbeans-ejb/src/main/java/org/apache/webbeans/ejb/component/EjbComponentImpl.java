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
package org.apache.webbeans.ejb.component;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.enterprise.context.spi.CreationalContext;

import org.apache.webbeans.component.AbstractObservesComponent;
import org.apache.webbeans.component.WebBeansType;
import org.apache.webbeans.ejb.EjbType;

/**
 * @version $Rev$ $Date$
 */
public class EjbComponentImpl<T> extends AbstractObservesComponent<T>
{
    private EjbType ejbType;
    
    private Set<Method> businessMethods = new HashSet<Method>();
    
    public EjbComponentImpl(Class<T> ejbClassType)
    {
        super(WebBeansType.ENTERPRISE,ejbClassType);
    }

    @Override
    protected T createComponentInstance(CreationalContext<T> creationalContext)
    {
        return null;
    }

    @Override
    protected void destroyComponentInstance(T instance)
    {
        
    }
    
    public void setEjbType(EjbType type)
    {
        this.ejbType = type;
        
    }
    
    public EjbType getEjbType()
    {
        return this.ejbType;
    }

    public void addBusinessMethod(Method method)
    {
        this.businessMethods.add(method);
    }
    
    public Set<Method> getBusinessMethods()
    {
        return Collections.unmodifiableSet(this.businessMethods);
    }
}
