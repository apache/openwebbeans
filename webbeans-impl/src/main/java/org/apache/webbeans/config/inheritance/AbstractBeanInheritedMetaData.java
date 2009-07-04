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
package org.apache.webbeans.config.inheritance;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Set;


import org.apache.webbeans.component.AbstractBean;

/**
 * Defines the bean inherited meta-datas.
 */
abstract class AbstractBeanInheritedMetaData<T> implements IBeanInheritedMetaData
{
    /**Component that inherits the meta-datas*/
    protected AbstractBean<T> component;
    
    /**Inherited class*/
    protected Class<?> inheritedClazz;
    
    /**Inherited binding types*/
    protected Set<Annotation> inheritedBindingTypes = new HashSet<Annotation>();
    
    /**Inherited stereotypes*/
    protected Set<Annotation> inheritedStereoTypes = new HashSet<Annotation>();
    
    /**Inherited interceptor binding types*/
    protected Set<Annotation> inheritedInterceptorBindingTypes = new HashSet<Annotation>();
    
    /**Inherited scope type*/
    protected Annotation inheritedScopeType = null;
    
    /**Inherited deployment type*/
    protected Annotation inheritedDeploymentType = null;
    
    /**
     * Create a new bean inherited data.
     * 
     * @param component webbeans component
     * @param inheritedClazz inherited class
     */
    protected AbstractBeanInheritedMetaData(AbstractBean<T> component, Class<?> inheritedClazz)
    {
        this.component = component;
        this.inheritedClazz = inheritedClazz;
        
        setInheritedBindingTypes();
        setInheritedDeploymentType();
        setInheritedInterceptorBindingTypes();
        setInheritedScopeType();
        setInheritedStereoTypes();
    }     
     
    
    public Set<Annotation> getInheritedBindingTypes()
    {
        return this.inheritedBindingTypes;
    }

    public Set<Annotation> getInheritedStereoTypes()
    {
        return this.inheritedStereoTypes;
    }
    
    public Set<Annotation> getInheritedInterceptorBindingTypes()
    {
        return this.inheritedInterceptorBindingTypes;
    }
    
    public Annotation getInheritedScopeType()
    {
        return this.inheritedScopeType;
    }
    
    public Annotation getInheritedDeploymentType()
    {
        return this.inheritedDeploymentType;
    }
    
    protected AbstractBean<T> getComponent()
    {
        return this.component;
    }
    
    protected Class<?> getInheritedClazz()
    {
        return this.inheritedClazz;
    }
     
    
    /**
     * @param inheritedBindingTypes the inheritedBindingTypes to set
     */
    abstract protected void setInheritedBindingTypes();

    /**
     * @param inheritedStereoTypes the inheritedStereoTypes to set
     */
    abstract protected void setInheritedStereoTypes();

    /**
     * @param inheritedInterceptorBindingTypes the inheritedInterceptorBindingTypes to set
     */
    abstract protected void setInheritedInterceptorBindingTypes();

    /**
     * @param inheritedScopeType the inheritedScopeType to set
     */
    abstract protected void setInheritedScopeType();

    /**
     * @param inheritedDeploymentType the inheritedDeploymentType to set
     */
    abstract protected void setInheritedDeploymentType();
    
    
}
