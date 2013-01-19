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
package org.apache.webbeans.jms.component;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Set;

import javax.enterprise.context.Dependent;
import javax.jms.Destination;

import org.apache.webbeans.component.AbstractOwbBean;
import org.apache.webbeans.component.JmsBeanMarker;
import org.apache.webbeans.component.WebBeansType;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.jms.JMSModel;

public class JmsBean<T> extends AbstractOwbBean<T> implements JmsBeanMarker
{
    private JMSModel jmsModel = null;
    
    JmsBean(WebBeansContext webBeansContext, JMSModel jmsModel, Set<Type> types, Set<Annotation> qualifiers)
    {
        super(webBeansContext, WebBeansType.JMS, types, qualifiers, Dependent.class, Destination.class, Collections.<Class<? extends Annotation>>emptySet());
        this.jmsModel = jmsModel;
    }
        
    public JMSModel getJmsModel()
    {
        return this.jmsModel;
    }
}
