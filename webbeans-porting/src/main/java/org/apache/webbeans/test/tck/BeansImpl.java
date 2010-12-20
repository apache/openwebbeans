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
package org.apache.webbeans.test.tck;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Method;

import javax.enterprise.inject.spi.Bean;
import javax.persistence.Entity;

import org.apache.webbeans.component.InjectionTargetBean;
import org.apache.webbeans.component.ManagedBean;
import org.apache.webbeans.component.WebBeansType;
import org.apache.webbeans.config.DefinitionUtil;
import org.apache.webbeans.config.ManagedBeanConfigurator;
import org.apache.webbeans.util.AnnotationUtil;
import org.jboss.jsr299.tck.spi.Beans;

public class BeansImpl implements Beans
{

    @SuppressWarnings("unchecked")
    public <T> Bean<T> createProducerMethodBean(Method method, Bean<?> declaringBean)
    {
        return DefinitionUtil.createProducerComponent((Class<T>)method.getReturnType(), method, (InjectionTargetBean<?>)declaringBean, false);
        
    }

    public <T> Bean<T> createSimpleBean(Class<T> clazz)
    {
        ManagedBean<T> bean = null;

        ManagedBeanConfigurator.checkManagedBeanCondition(clazz);
        
        bean = ManagedBeanConfigurator.define(clazz, WebBeansType.MANAGED);

        return bean;
    }

    public boolean isEnterpriseBean( Class<?> clazz )
    {
        return false;
    }

    public boolean isEntityBean( Class<?> clazz )
    {
        return (AnnotationUtil.hasClassAnnotation(clazz, Entity.class));
    }        

    public boolean isProxy( Object instance )
    {
        return instance.getClass().getName().contains("$$");
    }

    @Override
    public byte[] serialize(Object o) throws IOException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(o);
        return baos.toByteArray();
    }

    @Override
    public Object deserialize(byte[] bytes) throws IOException, ClassNotFoundException
    {
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        ObjectInputStream ois = new ObjectInputStream(bais);
        return ois.readObject();
    }

    public boolean isStatefulBean( Class<?> clazz )
    {
        return false;
    }

    public boolean isStatelessBean( Class<?> clazz )
    {
        return false;
    }

    public <T> T getEnterpriseBean(Class<? extends T> beanType, Class<T> localInterface)
    {
        return null;
    }
}