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
package org.apache.webbeans.spi.plugins;

import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanAttributes;
import java.lang.reflect.Method;

/**
 * Session bean related plugin contract.
 * 
 * @version $Rev$ $Date$
 */
public interface OpenWebBeansEjbPlugin extends OpenWebBeansPlugin
{
    /**
     * Returns true if class is an session bean class false otherwise
     * 
     * @param clazz class definition
     * @return true if class is an ejb class false otherwise
     */
    boolean isSessionBean(Class<?> clazz);

    /**
     * return true is clazz is an impl class or business interface of an EJB
     *
     * @param clazz class definition
     * @return true is clazz denotes an EJB
     */
    boolean isNewSessionBean(Class<?> clazz);

    /**
     * Configures session bean and adds it into the container.
     * 
     * @param clazz session bean class
     * @param annotatedType processed annotated type
     */
    <T> Bean<T> defineSessionBean(Class<T> clazz, BeanAttributes<T> attributes, AnnotatedType<T> annotatedType);

    <T> Bean<T> defineNewSessionBean(Class<T> clazz);

    /**
     * Returns true if given class is singleton session bean, false otherwise.
     * 
     * @param clazz session bean class
     * @return true if given class is singleton session bean, false otherwise
     */
    boolean isSingletonBean(Class<?> clazz);

    /**
     * Returns true if given class is stateless session bean, false otherwise.
     * 
     * @param clazz session bean class
     * @return true if given class is singleton session bean, false otherwise
     */
    boolean isStatelessBean(Class<?> clazz);

    /**
     * Returns true if given class is stateful session bean, false otherwise.
     * 
     * @param clazz session bean class
     * @return true if given class is singleton session bean, false otherwise
     */
    boolean isStatefulBean(Class<?> clazz);

    /**
     * Returns session bean proxy.
     * 
     * @param bean session bean
     * @param iface interface
     * @return session bean proxy
     */
    Object getSessionBeanProxy(Bean<?> bean, Class<?> iface, CreationalContext<?> creationalContext);

    Method resolveViewMethod(Bean<?> component, Method declaredMethod);

    <T> BeanAttributes<T> createBeanAttributes(AnnotatedType<T> type);
}