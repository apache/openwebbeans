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
package org.apache.webbeans.portable;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collections;

import javax.enterprise.context.Dependent;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;

import org.apache.webbeans.component.AbstractOwbBean;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.exception.WebBeansException;
import org.apache.webbeans.util.Asserts;
import org.apache.webbeans.util.WebBeansUtil;

public class ProducerFieldProducer<T, P> extends AbstractProducer<T>
{

    private Bean<P> owner;
    private WebBeansContext webBeansContext;
    private AnnotatedField<? super P> producerField;

    public ProducerFieldProducer(Bean<P> owner, AnnotatedField<? super P> producerField, WebBeansContext context)
    {
        super(Collections.<InjectionPoint>emptySet());
        Asserts.assertNotNull(owner, "owner may not be null");
        Asserts.assertNotNull(producerField, "field may not be null");
        Asserts.assertNotNull(context, "WebBeansContext may not be null");
        this.owner = owner;
        webBeansContext = context;
        this.producerField = producerField;
    }

    @Override
    public T produce(CreationalContext<T> creationalContext)
    {
        T instance = null;
        P parentInstance = null;
        CreationalContext<P> parentCreational = null;
        try
        {
            parentCreational = webBeansContext.getBeanManagerImpl().createCreationalContext(owner);
            
            Field field = producerField.getJavaMember();
            if (!field.isAccessible())
            {
                webBeansContext.getSecurityService().doPrivilegedSetAccessible(field, true);
            }

            if (Modifier.isStatic(field.getModifiers()))
            {
                instance = (T) field.get(null);
            }
            else
            { 
                parentInstance = getParentInstanceFromContext(parentCreational);
                
                instance = (T) field.get(parentInstance);
            }
        }
        catch(Exception e)
        {
            throw new WebBeansException(e);
        }
        finally
        {
            if (owner.getScope().equals(Dependent.class))
            {
                owner.destroy(parentInstance, parentCreational);
            }
        }

        return instance;

    }
    
    @SuppressWarnings("unchecked")
    protected P getParentInstanceFromContext(CreationalContext<?> creationalContext)
    {
        P  parentInstance;

        Bean<?> specialize = WebBeansUtil.getMostSpecializedBean(webBeansContext.getBeanManagerImpl(), (AbstractOwbBean<T>) owner);

        if (specialize != null)
        {
            parentInstance = (P) webBeansContext.getBeanManagerImpl().getContext(specialize.getScope()).
                    get((Bean<Object>)specialize,(CreationalContext<Object>) creationalContext);
        }
        else
        {
            parentInstance = (P) webBeansContext.getBeanManagerImpl().getContext(
                    owner.getScope()).get((Bean<Object>)owner, (CreationalContext<Object>) creationalContext);
        }

        return parentInstance;

    }
}
