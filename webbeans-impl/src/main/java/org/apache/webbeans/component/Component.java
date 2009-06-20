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

import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.BeanManager;

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
public abstract class Component<T> implements Bean<T>
{
	/**Manager for beans*/
    private final BeanManager manager;

    /**
     * Creates a new bean instance with given manager.
     * 
     * @param manager bean manager
     */
    protected Component(BeanManager manager)
    {
        this.manager = manager;
    }
    
    /**
     * Gets manager instance
     * 
     * @return manager instance
     */
    protected BeanManager getManager()
    {
        return manager;
    }
    
    public abstract IBeanInheritedMetaData getInheritedMetaData();
    
    public abstract Annotation getType();

    public abstract void setType(Annotation type);

    public abstract Annotation getImplScopeType();

    public abstract void setImplScopeType(Annotation scopeType);

    public abstract WebBeansType getWebBeansType();

    public abstract void addBindingType(Annotation bindingType);

    public abstract void addStereoType(Annotation stereoType);

    public abstract void addApiType(Class<?> apiType);
    
    public abstract void addInjectionPoint(InjectionPoint injectionPoint);

    public abstract Set<Annotation> getImplBindingTypes();

    public abstract Set<Annotation> getStereotypes();

    public abstract void setName(String name);

    public abstract int getPrecedence();

    public abstract Class<T> getReturnType();

    public abstract Object getDependent(Bean<?> dependentComponent,InjectionPoint injectionPoint);

    public abstract List<InterceptorData> getInterceptorStack();

    public abstract List<Object> getDecoratorStack();

    public abstract void setSerializable(boolean serializable);

    public abstract void setNullable(boolean nullable);
}