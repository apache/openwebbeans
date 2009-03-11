/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.apache.webbeans.component;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Set;

import javax.inject.manager.Bean;
import javax.inject.manager.InjectionPoint;
import javax.inject.manager.Manager;

import org.apache.webbeans.config.inheritance.IBeanInheritedMetaData;
import org.apache.webbeans.intercept.InterceptorData;

/**
 * Extends the unpublished {@link Bean} interface for backward capability with
 * EDR-1 of the specification.
 * <p>
 * <b>This class is not used by the client. It is used entirely as internal. It
 * exists only for compatibility problems.</b>
 * </p>
 * 
 * @author <a href="mailto:gurkanerdogdu@yahoo.com">Gurkan Erdogdu</a>
 * @since 1.0
 */
public abstract class Component<T> extends Bean<T>
{
    protected Component(Manager manager)
    {
        super(manager);
    }
    
    abstract public IBeanInheritedMetaData getInheritedMetaData();
    
    abstract public Annotation getType();

    abstract public void setType(Annotation type);

    abstract public Annotation getImplScopeType();

    abstract public void setImplScopeType(Annotation scopeType);

    abstract public WebBeansType getWebBeansType();

    abstract public void addBindingType(Annotation bindingType);

    abstract public void addStereoType(Annotation stereoType);

    abstract public void addApiType(Class<?> apiType);
    
    abstract public void addInjectionPoint(InjectionPoint injectionPoint);

    abstract public Set<Annotation> getImplBindingTypes();

    abstract public Set<Annotation> getStereotypes();

    abstract public void setName(String name);

    abstract public int getPrecedence();

    abstract public Class<T> getReturnType();

    abstract public Object getDependent(Bean<?> dependentComponent,InjectionPoint injectionPoint);

    abstract public List<InterceptorData> getInterceptorStack();

    abstract public List<Object> getDecoratorStack();

    abstract public void setSerializable(boolean serializable);

    abstract public void setNullable(boolean nullable);
}