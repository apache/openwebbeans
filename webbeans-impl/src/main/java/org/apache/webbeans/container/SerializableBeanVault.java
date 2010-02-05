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

import org.apache.webbeans.component.AbstractBean;
import org.apache.webbeans.config.WebBeansFinder;

import javax.enterprise.context.spi.Contextual;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.PassivationCapable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class acts as a storage for {@link SerializableBean}s
 */
public class SerializableBeanVault {

    private Map<String, SerializableBean<?>> serializableBeans = new ConcurrentHashMap<String, SerializableBean<?>>();

    public final static SerializableBeanVault getInstance()
    {
        return (SerializableBeanVault) WebBeansFinder.getSingletonInstance(SerializableBeanVault.class.getName());
    }

    public <T> Contextual<T> getSerializableBean(Contextual<T> bean)
    {
        if (bean instanceof SerializableBean)
        {
            return bean;
        }

        if (bean instanceof SerializableBean && bean instanceof AbstractBean)
        {
            AbstractBean ab = (AbstractBean)bean;
            if (ab.isSerializable())
            {
                String id = ab.getId();
                SerializableBean sb = serializableBeans.get(id);
                if (sb == null)
                {
                    sb = new SerializableBean((Bean<?>) bean);
                    serializableBeans.put(id, sb);
                }

                return sb;
            }
        }

        return null;
    }
}
