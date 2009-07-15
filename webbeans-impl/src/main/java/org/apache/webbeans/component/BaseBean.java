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
import java.lang.reflect.Member;
import java.util.List;
import java.util.Set;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.BeanManager;

import org.apache.webbeans.config.inheritance.IBeanInheritedMetaData;
import org.apache.webbeans.intercept.InterceptorData;

/**
 * OWB specific extension of the {@link Bean} interface.
 * It is used internally. Do not use it.
 * 
 * @version $Rev$ $Date$
 * <T> bean class
 */
public abstract class BaseBean<T> implements Bean<T>
{
	/**Bean Manager*/
    private final BeanManager manager;

    /**
     * Creates a new bean instance with given manager.
     * 
     * @param manager bean manager
     */
    protected BaseBean(BeanManager manager)
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
    
    /**
     * Returns bean's inherited meta data.
     * 
     * @return inherited meta data.
     */
    public abstract IBeanInheritedMetaData getInheritedMetaData();
    
    /**
     * Returna deployment type as annotation.
     * 
     * @return deployment type as annotation
     */
    public abstract Annotation getType();

    /**
     * Sets bean deployment type annotation.
     * 
     * @param type bean deployment type annotation
     */
    public abstract void setType(Annotation type);

    /**
     * Returns scope type annotation.
     * 
     * @return scope type annotation
     */
    public abstract Annotation getImplScopeType();

    /**
     * Sets bean scope type annotation.
     * 
     * @param scopeType bean scope type annotation
     */
    public abstract void setImplScopeType(Annotation scopeType);

    /**
     * Returns bean type.
     * 
     * @return webbeans type
     * @see WebBeansType
     */
    public abstract WebBeansType getWebBeansType();

    /**
     * Adds binding type.
     * 
     * @param bindingType bean binding type
     */
    public abstract void addBindingType(Annotation bindingType);

    /**
     * Adds new stereotype annotation.
     * 
     * @param stereoType stereotype annotation
     */
    public abstract void addStereoType(Annotation stereoType);

    /**
     * Adds new api type.
     * 
     * @param apiType api type
     */
    public abstract void addApiType(Class<?> apiType);
    
    /**
     * Adds new injection point.
     * 
     * @param injectionPoint injection point
     */
    public abstract void addInjectionPoint(InjectionPoint injectionPoint);

    /**
     * Returns set of binding type annotations.
     * 
     * @return set of binding type annotations
     */
    public abstract Set<Annotation> getImplBindingTypes();

    /**
     * Gets stereotypes annotations.
     */
    public abstract Set<Annotation> getOwbStereotypes();

    /**
     * Sets name of the bean.
     * 
     * @param name bean name
     */
    public abstract void setName(String name);
    
    public abstract List<InjectionPoint> getInjectionPoint(Member member);

    public abstract int getPrecedence();

    public abstract Class<T> getReturnType();

    public abstract Object getDependent(Bean<?> dependentComponent,InjectionPoint injectionPoint);

    public abstract List<InterceptorData> getInterceptorStack();

    public abstract List<Object> getDecoratorStack();

    public abstract void setSerializable(boolean serializable);

    public abstract void setNullable(boolean nullable);
    
    public abstract void setSpecializedBean(boolean specialized);
    
    public abstract boolean isSpecializedBean();
    
    public abstract CreationalContext<T> getCreationalContext();
}