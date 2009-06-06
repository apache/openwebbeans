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
package org.apache.webbeans.jms.plugin;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.enterprise.inject.spi.InjectionPoint;

import org.apache.webbeans.container.ManagerImpl;
import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.jms.JMSManager;
import org.apache.webbeans.jms.JMSModel;
import org.apache.webbeans.jms.JMSModel.JMSType;
import org.apache.webbeans.jms.component.JmsComponentFactory;
import org.apache.webbeans.jms.component.JmsComponentImpl;
import org.apache.webbeans.jms.util.JmsProxyHandler;
import org.apache.webbeans.jms.util.JmsUtil;
import org.apache.webbeans.plugins.AbstractOpenWebBeansPlugin;

/**
 * JMS Plugin for JMS related components.
 * @version $Rev$ $Date$
 */
public class OpenWebBeansJmsPlugin extends AbstractOpenWebBeansPlugin
{

    public OpenWebBeansJmsPlugin()
    {
        super();
    }

    
    
    @Override
    @SuppressWarnings("unchecked")
    public <T> boolean addJMSBean(InjectionPoint injectionPoint)
    {        
        Type injectionPointType = injectionPoint.getType();
        if(injectionPointType instanceof Class)
        {
            Class<T> injectionPointClazz = (Class<T>)injectionPointType;
            
            if(JmsUtil.isJmsResourceClass(injectionPointClazz))
            {
                JMSType type = null;
                
                if(JmsUtil.isJmsQueueTypeResource(injectionPointClazz))
                {
                    type = JMSType.QUEUE;
                }
                else
                {
                    type = JMSType.TOPIC;
                }
                
                Annotation[] bindings = injectionPoint.getBindings().toArray(new Annotation[0]);
                JMSModel jmsModel = JMSManager.getInstance().getModel(type, bindings);
                
                JmsComponentImpl<T> bean = JmsComponentFactory.getJmsComponentFactory().getJmsComponent(jmsModel,injectionPointClazz);
                
                ManagerImpl.getManager().addBean(bean);
                
                return true;
            }            
        }
             
        return false;
    }



    /* (non-Javadoc)
     * @see org.apache.webbeans.plugins.AbstractOpenWebBeansPlugin#shutDown()
     */
    @Override
    public void shutDown() throws WebBeansConfigurationException
    {
        JmsProxyHandler.clearConnections();
    }
    
    
    
}
