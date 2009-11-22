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
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Decorator;
import javax.enterprise.inject.spi.InjectionPoint;

import org.apache.webbeans.config.inheritance.IBeanInheritedMetaData;
import org.apache.webbeans.intercept.InterceptorData;

/**
 * OWB specific extension of the {@link Bean} interface.
 * It is used internally. Do not use it. Instead use {@link AbstractBean}
 * for extension.
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
     * Adds qualifier.
     * 
     * @param qualifier bean qualifier
     */
    public abstract void addQualifier(Annotation qualifier);
    
    /**
     * Returns true if bean is capable of
     * serializable, false otherwise.
     * 
     * @return true if bean is serializable
     */
    public abstract boolean isSerializable();    

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
     * Returns set of qualifier annotations.
     * 
     * @return set of qualifier annotations
     */
    public abstract Set<Annotation> getImplQualifiers();

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
    
    /**
     * Gets injection points for given member.
     * <p>
     * For example, if member is field, it gets all
     * injected field's injection points of bean.
     * </p>
     * @param member java member
     * @return injection points for given member
     */
    public abstract List<InjectionPoint> getInjectionPoint(Member member);

    /**
     * Returns bean class type
     * @return bean class type
     */
    public abstract Class<T> getReturnType();

    /**
     * Gets dependent bean at given injection point.
     * @param dependentBean dependent bean
     * @param injectionPoint injection point of dependent bean
     * @return dependent bean
     */
    public abstract Object getDependent(Bean<?> dependentBean,InjectionPoint injectionPoint, CreationalContext<?> creational);

    /**
     * Gets interceptor stack of bean instance.
     * @return interceptor stack
     */
    public abstract List<InterceptorData> getInterceptorStack();

    /**
     * Gets decorator stack of bean instance.
     * @return decorator stack
     */
    public abstract List<Decorator<?>> getDecorators();

    /**
     * Sets serializable flag.
     * @param serializable flag
     */
    public abstract void setSerializable(boolean serializable);

    /**
     * Set nullable flag.
     * @param nullable flag
     */
    public abstract void setNullable(boolean nullable);
    
    /**
     * Set specialized flag.
     * @param specialized flag
     */
    public abstract void setSpecializedBean(boolean specialized);
    
    /**
     * Returns true if bean is a specialized bean, false otherwise.
     * @return true if bean is a specialized bean
     */
    public abstract boolean isSpecializedBean();
    
    /**
     * Set enableed flag.
     * @param enabled flag
     */
    public abstract void setEnabled(boolean enabled);    
    
    /**
     * Bean is enabled or not.
     * @return true if enabled
     */    
    public abstract boolean isEnabled();
    
}