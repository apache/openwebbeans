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
package org.apache.webbeans.inject;

import java.lang.reflect.Field;

import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.enterprise.inject.spi.InjectionTarget;

import org.apache.webbeans.context.creational.CreationalContextImpl;
import org.apache.webbeans.exception.WebBeansException;

/**
 * Field type injection.
 * 
 * @author <a href="mailto:gurkanerdogdu@yahoo.com">Gurkan Erdogdu</a>
 * @since 1.0
 */
public class InjectableField<T> extends AbstractInjectable<T>
{
    protected Field field;
    protected Object instance;

    public InjectableField(Field field, Object instance, InjectionTarget<T> owner, CreationalContextImpl<T> creationalContext)
    {
        super(owner,creationalContext);
        this.field = field;
        this.instance = instance;
    }

    public T doInjection()
    {
        try
        {
            InjectionPoint injectedField = getInjectionPoints(field).get(0);
            
            if (!field.isAccessible())
            {
                getWebBeansContext().getSecurityService().doPrivilegedSetAccessible(field, true);
            }

            Object object;
            if (injectedField.isDelegate())
            {
                object = creationalContext.getDelegate();
            }
            else
            {
                object = inject(injectedField);
            }
            
            field.set(instance, object);

        }
        catch (IllegalAccessException e)
        {
            throw new WebBeansException(e);
        }
        catch (IllegalArgumentException e)
        {
            throw e;
        }

        return null;
    }
}
