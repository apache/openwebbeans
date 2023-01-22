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
package org.apache.webbeans.context;

import jakarta.enterprise.context.spi.Contextual;
import java.io.Externalizable;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;

import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.context.creational.BeanInstanceBag;
import org.apache.webbeans.util.WebBeansUtil;

/**
 * Base class for passivating contexts.
 * It basically provides serialisation support
 */
public abstract class PassivatingContext extends AbstractContext implements Externalizable
{

    public PassivatingContext(Class<? extends Annotation> scopeType)
    {
        super(scopeType);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
    {
        WebBeansContext webBeansContext = WebBeansContext.currentInstance();

        scopeType = (Class<? extends Annotation>) in.readObject();
        Map<String, BeanInstanceBag<?>> map = (Map<String, BeanInstanceBag<?>>)in.readObject();
        setComponentInstanceMap();
        for (Map.Entry<String, BeanInstanceBag<?>> beanBagEntry : map.entrySet())
        {
            String id = beanBagEntry.getKey();
            if (id != null)
            {
                Contextual<?> contextual = webBeansContext.getBeanManagerImpl().getPassivationCapableBean(id);
                if (contextual != null)
                {
                    componentInstanceMap.put(contextual, beanBagEntry.getValue());
                }
            }
        }
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException
    {
        out.writeObject(scopeType);
        Map<String, BeanInstanceBag<?>> map = new HashMap<>(componentInstanceMap.size());

        for (Map.Entry<Contextual<?>, BeanInstanceBag<?>> beanBagEntry : componentInstanceMap.entrySet())
        {
            Contextual<?> contextual = beanBagEntry.getKey();

            String id = WebBeansUtil.getPassivationId(contextual);
            if (id == null)
            {
                throw new NotSerializableException("cannot serialize " + contextual.toString());
            }
            map.put(id, beanBagEntry.getValue());
        }

        out.writeObject(map);
    }

}
