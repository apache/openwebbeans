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
package org.apache.webbeans.jms;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.jms.JMSModel.JMSType;
import org.apache.webbeans.util.Asserts;

public class JMSManager
{
    private final Set<JMSModel> jmsModels = new CopyOnWriteArraySet<JMSModel>();

    public JMSManager()
    {
        
    }
    
    public void clear()
    {
        this.jmsModels.clear();
    }

    @Deprecated
    public static JMSManager getInstance()
    {
        return WebBeansContext.getInstance().getjMSManager();
    }

    public void addJmsModel(JMSModel model)
    {
        Asserts.assertNotNull(model,"model parameter can not be null");
        
        this.jmsModels.add(model);
    }
    
    public JMSModel getModel(JMSType type, Annotation...bindingTypes)
    {
        Iterator<JMSModel> models = this.jmsModels.iterator();
        
        while(models.hasNext())
        {
            
            JMSModel model = models.next();
            
            if(model.getJmsType().equals(type) && Arrays.equals(bindingTypes, model.getBindings()))
            {
                return model;
            }
        }
        
        return null;
    }
}
