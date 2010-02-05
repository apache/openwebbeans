package org.apache.webbeans.container;
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

import org.apache.webbeans.config.WebBeansFinder;
import org.apache.webbeans.util.WebBeansUtil;

import javax.enterprise.context.spi.Contextual;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.PassivationCapable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>This class acts as a storage for {@link SerializableBean}s.</p>
 *
 * <h3>The Background:</h3>
 * <p>Any Contextual&lt;T&gt; which holds it's information on a storage which may get serialized,
 * like e.g. the SessionContext, the ConversationContext or any 3-rd party Context
 * for a NormalScoped(passivating=true) and therefore PassivationCapable Scope needs to be Serializable.</p>
 * <p>Normal {@link Bean}s are not serializable because they contain non transportable information. But each
 * {@link PassivationCapable} {@link Bean} is uniquely identifyable via it's id and can be restored by
 * {@link javax.enterprise.inject.spi.BeanManager#getPassivationCapableBean(String)}.</p>
 * <p>Since a custom Context implementation doesn't know when it gets passivated (for a Http Session
 * this is pretty seldom), the Contextual<T> handed over to the Context implementation must be
 * Serializable.</p>
 */
public class SerializableBeanVault {

    private Map<String, SerializableBean<?>> serializableBeans = new ConcurrentHashMap<String, SerializableBean<?>>();

    public final static SerializableBeanVault getInstance()
    {
        return (SerializableBeanVault) WebBeansFinder.getSingletonInstance(SerializableBeanVault.class.getName());
    }

    @SuppressWarnings("unchecked")
    public <T> Contextual<T> getSerializableBean(Contextual<T> bean)
    {
        if (bean instanceof SerializableBean)
        {
            // we don't like to wrap SerializedBeans in itself!
            return bean;
        }

        String id = null;
        
        if((id=WebBeansUtil.isPassivationCapable(bean)) != null) 
        {
            SerializableBean<T> sb = (SerializableBean<T>) serializableBeans.get(id);
            if (sb == null)
            {
                sb = new SerializableBean<T>((Bean<T>) bean,id);
                serializableBeans.put(id, sb);
            }

            return sb;            
        }
        
        return null;
    }
}
