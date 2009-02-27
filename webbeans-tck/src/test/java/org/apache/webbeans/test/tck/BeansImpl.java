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
import java.util.List;

import javax.inject.manager.Bean;


import org.apache.webbeans.component.AbstractComponent;
import org.apache.webbeans.component.ComponentImpl;
import org.apache.webbeans.component.WebBeansType;
import org.apache.webbeans.config.SimpleWebBeansConfigurator;
import org.apache.webbeans.decorator.DecoratorUtil;
import org.apache.webbeans.test.mock.MockManager;
import org.jboss.jsr299.tck.spi.Beans;

public class BeansImpl implements Beans
{

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    public <T> Bean<T> createProducerMethodBean(Method method, Bean<?> declaringBean)
    {
        //X TODO plz review! If the declaringBean has been already parsed, then the producer must be also.
        MockManager manager = MockManager.getInstance();
        List<AbstractComponent<?>> components = manager.getComponents();
        
        for (AbstractComponent<?> component : components)
        {
            if (component.getClass().equals(method.getReturnType())) 
            {
                //X TODO plz review the cast!
                return (Bean<T>) component;
            }
        }
        
        return null;
    }

    /** {@inheritDoc} */
    public <T> Bean<T> createSimpleBean(Class<T> clazz)
    {
        ComponentImpl<T> bean = null;

        SimpleWebBeansConfigurator.checkSimpleWebBeanCondition(clazz);
        bean = SimpleWebBeansConfigurator.define(clazz, WebBeansType.SIMPLE);

        if (bean != null)
        {
            DecoratorUtil.checkSimpleWebBeanDecoratorConditions(bean);
            // DefinitionUtil.defineSimpleWebBeanInterceptorStack(bean);

            MockManager manager = MockManager.getInstance();
            manager.getComponents().add((AbstractComponent<?>) bean);
            manager.addBean(bean);
        }

        return bean;
    }

    public boolean isEnterpriseBean( Class<?> clazz ) {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean isEntityBean( Class<?> clazz ) {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean isProxy( Object instance ) {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean isStatefulBean( Class<?> clazz ) {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean isStatelessBean( Class<?> clazz ) {
        // TODO Auto-generated method stub
        return false;
    }

}
