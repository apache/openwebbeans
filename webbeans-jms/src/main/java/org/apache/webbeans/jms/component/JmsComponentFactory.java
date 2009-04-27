/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.webbeans.jms.component;

import java.lang.annotation.Annotation;

import org.apache.webbeans.annotation.DependentScopeLiteral;
import org.apache.webbeans.jms.JMSModel;
import org.apache.webbeans.util.Asserts;

public final class JmsComponentFactory
{
    private static JmsComponentFactory instance = new JmsComponentFactory();

    private JmsComponentFactory()
    {
        
    }
    
    public static JmsComponentFactory getJmsComponentFactory()
    {
        return instance;
    }
    
    public <T> JmsComponentImpl<T> getJmsComponent(JMSModel model, Class<T> clazz)
    {
        Asserts.assertNotNull(model,"model parameter can not be null");
        Asserts.assertNotNull(clazz, "clazz parameter can not be null");
        
        JmsComponentImpl<T> component = new JmsComponentImpl<T>(model,clazz);
        
        component.addApiType(clazz);
        component.setImplScopeType(new DependentScopeLiteral());
        
        Annotation[] anns = model.getBindings();
        
        for(Annotation a : anns)
        {
            component.addBindingType(a);   
        }
        
        return component;
    }
}
