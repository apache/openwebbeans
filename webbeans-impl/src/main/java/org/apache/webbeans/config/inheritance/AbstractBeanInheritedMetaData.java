/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.webbeans.config.inheritance;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Set;

/**
 * Defines the bean inherited meta-datas.
 */
abstract class AbstractBeanInheritedMetaData<T> implements IBeanInheritedMetaData
{
    /**Inherited class*/
    protected Class<?> inheritedClazz;
    
    /**Inherited qualifiers*/
    protected Set<Annotation> inheritedQualifiers = new HashSet<Annotation>();
    
    /**Inherited stereotypes*/
    protected Set<Annotation> inheritedStereoTypes = new HashSet<Annotation>();
    
    /**Inherited interceptor bindings*/
    protected Set<Annotation> inheritedInterceptorBindings = new HashSet<Annotation>();
    
    /**Inherited scope type*/
    protected Annotation inheritedScopeType = null;
    
    /**
     * Create a new bean inherited data.
     * 
     * @param inheritedClazz inherited class
     */
    protected AbstractBeanInheritedMetaData(Class<?> inheritedClazz)
    {
        this.inheritedClazz = inheritedClazz;
        
        setInheritedQualifiers();
        setInheritedInterceptorBindings();
        setInheritedScopeType();
        setInheritedStereoTypes();
    }     
     
    
    public Set<Annotation> getInheritedQualifiers()
    {
        return inheritedQualifiers;
    }

    public Set<Annotation> getInheritedStereoTypes()
    {
        return inheritedStereoTypes;
    }
    
    public Set<Annotation> getInheritedInterceptorBindings()
    {
        return inheritedInterceptorBindings;
    }
    
    public Annotation getInheritedScopeType()
    {
        return inheritedScopeType;
    }

    /**
     * Internally set the inheritedBindingTypes
     */
    protected abstract void setInheritedQualifiers();

    /**
     * Internally set the inheritedStereoTypes
     */
    protected abstract void setInheritedStereoTypes();

    /**
     * Internally set the inheritedInterceptorBindingTypes
     */
    protected abstract void setInheritedInterceptorBindings();

    /**
     * Internally set the inheritedScopeType.
     */
    protected abstract void setInheritedScopeType();
}
