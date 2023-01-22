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

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.inject.spi.AnnotatedParameter;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.enterprise.inject.spi.InjectionTarget;

import org.apache.webbeans.context.creational.CreationalContextImpl;
import org.apache.webbeans.exception.WebBeansException;

/**
 * Injects the parameters of the {@link org.apache.webbeans.component.ManagedBean} constructor and returns
 * the created instance.
 * 
 * @author <a href="mailto:gurkanerdogdu@yahoo.com">Gurkan Erdogdu</a>
 * @since 1.0
 * @see AbstractInjectable
 */
public class InjectableConstructor<T> extends AbstractInjectable<T>
{
    /** Injectable constructor instance */
    protected Constructor<T> con;

    private Object[] params;
    private T instance;

    /**
     * Sets the constructor.
     * 
     * @param cons injectable constructor
     */
    public InjectableConstructor(Constructor<T> cons, InjectionTarget<T> owner, CreationalContextImpl<T> creationalContext)
    {
        super(owner,creationalContext);
        con = cons;
    }

    /**
     * Creates the instance from the constructor. Each constructor parameter
     * instance is resolved using the resolution algorithm.
     */
    public T doInjection()
    {
        try
        {
            if(!con.isAccessible())
            {
                getWebBeansContext().getSecurityService().doPrivilegedSetAccessible(con, true);
            }
            
            instance = con.newInstance(createParameters());
            transientCreationalContext.release();
            return instance;
        }
        catch (Exception e)
        {
            throw new WebBeansException(e);
        }
    }

    public T getInstance()
    {
        return instance;
    }

    public Object[] createParameters()
    {
        if (params != null)
        {
            return params;
        }

        List<Object> list = new ArrayList<>();
        List<InjectionPoint> injectedPoints = getInjectionPoints(con);

        for (int i=0; i<injectedPoints.size(); i++)
        {
            for (InjectionPoint point : injectedPoints)
            {
                AnnotatedParameter<?> parameter = (AnnotatedParameter<?>)point.getAnnotated();
                if (parameter.getPosition() == i)
                {
                    if (point.isDelegate())
                    {
                        list.add(creationalContext.getDelegate());
                        break;
                    }
                    list.add(inject(point));
                    break;
                }
            }
        }
        params = list.toArray(new Object[list.size()]);
        return params;
    }
}
