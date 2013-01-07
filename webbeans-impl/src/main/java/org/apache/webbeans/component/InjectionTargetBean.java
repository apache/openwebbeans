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
package org.apache.webbeans.component;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Decorator;

import org.apache.webbeans.config.inheritance.IBeanInheritedMetaData;
import org.apache.webbeans.intercept.InterceptorData;

/**
 * Defines contract for injection target beans.
 * 
 * @version $Rev$ $Date$
 *
 * @param <T> bean type
 */
public interface InjectionTargetBean<T> extends OwbBean<T>
{    
    /**
     * Inject JavaEE resources.
     * 
     * @param instance bean instance
     * @param creationalContext creational context
     */
    public void injectResources(T instance, CreationalContext<T> creationalContext);
    
    /**
     * Inject fields and methods of the bean instance.
     * 
     * @param instance bean instance
     * @param creationalContext creational context
     */
    public void injectFieldsAndMethods(T instance, CreationalContext<T> creationalContext);
        
    /**
     * Gets all injected fields of bean.
     * @return all injected fields
     */
    public Set<Field> getInjectedFields();

    /**
     * Adds new injected field.
     * @param field new injected field
     */
    public void addInjectedField(Field field);
    
    /**
     * Gets injected methods.
     * @return injected(initializer) methods
     */
    public Set<Method> getInjectedMethods();

    /**
     * Adds new injected method.
     * @param method new injected method
     */
    public void addInjectedMethod(Method method);

    /**
     * Gets inherited meta data.
     * @return inherited meta data
     */
    public IBeanInheritedMetaData getInheritedMetaData();
    
    /**
     * Gets interceptor stack of bean instance.
     * @return interceptor stack
     */
    public List<InterceptorData> getInterceptorStack();    
    
    /**
     * Gets decorator stack of bean instance.
     * @return decorator stack
     */
    public List<Decorator<?>> getDecoratorStack();

    /**
     * Calls post constrcut method.
     * 
     * @param instance bean instance
     */
    public void postConstruct(T instance, CreationalContext<T> creationalContext);
    
    /**
     * Calls predestroy method.
     * 
     * @param instance bean instance
     */
    public void preDestroy(T instance, CreationalContext<T> creationalContext);    
    
    /**
     * Gets annotated type.
     * @return annotated type
     */
    public AnnotatedType<T> getAnnotatedType();        
}
