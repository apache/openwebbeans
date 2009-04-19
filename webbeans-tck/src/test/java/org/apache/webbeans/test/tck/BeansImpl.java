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
package org.apache.webbeans.test.tck;

import java.lang.reflect.Method;

import javax.inject.manager.Bean;
import javax.persistence.Entity;


import org.apache.webbeans.component.AbstractComponent;
import org.apache.webbeans.component.ComponentImpl;
import org.apache.webbeans.component.WebBeansType;
import org.apache.webbeans.config.DefinitionUtil;
import org.apache.webbeans.config.SimpleWebBeansConfigurator;
import org.apache.webbeans.ejb.EJBUtil;
import org.apache.webbeans.util.AnnotationUtil;
import org.jboss.jsr299.tck.spi.Beans;

public class BeansImpl implements Beans
{

    @SuppressWarnings("unchecked")
    public <T> Bean<T> createProducerMethodBean(Method method, Bean<?> declaringBean)
    {
        return DefinitionUtil.createProducerComponent((Class<T>)method.getReturnType(), method, (AbstractComponent<?>)declaringBean, false);
        
    }

    public <T> Bean<T> createSimpleBean(Class<T> clazz)
    {
        ComponentImpl<T> bean = null;

        SimpleWebBeansConfigurator.checkSimpleWebBeanCondition(clazz);
        
        bean = SimpleWebBeansConfigurator.define(clazz, WebBeansType.SIMPLE);

        return bean;
    }

    public boolean isEnterpriseBean( Class<?> clazz ) {
        return EJBUtil.isEJBMessageDrivenClass(clazz) || EJBUtil.isEJBSessionClass(clazz);
    }

    public boolean isEntityBean( Class<?> clazz ) 
    {
        return (AnnotationUtil.isAnnotationExistOnClass(clazz, Entity.class));
    }        

    public boolean isProxy( Object instance ) {
        return instance.getClass().getName().contains("$$");
    }

    public boolean isStatefulBean( Class<?> clazz ) {
        return EJBUtil.isEJBSessionStatefulClass(clazz);
    }

    public boolean isStatelessBean( Class<?> clazz ) {
        return EJBUtil.isEJBSessionStateless(clazz);
    }

    public <T> T getEnterpriseBean(Class<? extends T> beanType, Class<T> localInterface)
    {
        return null;
    }
}