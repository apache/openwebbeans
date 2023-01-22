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
package org.apache.webbeans.intercept;

import org.apache.webbeans.component.OwbBean;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.intercept.InterceptorResolutionService.BeanInterceptorInfo;
import org.apache.webbeans.intercept.InterceptorResolutionService.BusinessMethodInterceptorInfo;
import org.apache.webbeans.portable.AbstractProducer;
import org.apache.webbeans.proxy.InterceptorHandler;
import org.apache.webbeans.util.ExceptionUtil;
import org.apache.webbeans.util.WebBeansUtil;

import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.Decorator;
import jakarta.enterprise.inject.spi.Producer;
import java.io.Externalizable;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.ObjectStreamException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * InterceptorHandler which handles all the Decorators on the InjectionTarget.
 * This one always gets added at the last position in the interceptor chain.
 */
public class DecoratorHandler implements InterceptorHandler, Externalizable
{

    private BeanInterceptorInfo interceptorInfo;
    private List<Decorator<?>> decorators;
    private Map<Decorator<?>, ?> instances;
    private int index;
    private Object target;
    private String passivationId;

    public DecoratorHandler(BeanInterceptorInfo interceptorInfo, List<Decorator<?>> decorators, Map<Decorator<?>, ?> instances, int index, Object target, String passivationId)
    {
        this.interceptorInfo = interceptorInfo;
        this.instances = instances;
        this.decorators = decorators;
        this.index = index;
        this.target = target;
        this.passivationId = passivationId;
    }

    public DecoratorHandler()
    {
        // no-op: for serialization
    }

    @Override
    public Object invoke(Method method, Object[] args)
    {
        BusinessMethodInterceptorInfo methodInterceptorInfo = interceptorInfo.getBusinessMethodsInfo().get(method);
        LinkedHashMap<Decorator<?>, Method> methodDecorators = methodInterceptorInfo.getMethodDecorators();
        if (methodDecorators != null)
        {
            for (int i = index; i < decorators.size(); i++)
            {
                Decorator<?> decorator = decorators.get(i);
                Method decoratingMethod = methodDecorators.get(decorator);
                if (decoratingMethod != null)
                {
                    try
                    {
                        if (!decoratingMethod.isAccessible())
                        {
                            decoratingMethod.setAccessible(true);
                        }
                        return decoratingMethod.invoke(instances.get(decorator), args);
                    }
                    catch (InvocationTargetException e)
                    {
                        return ExceptionUtil.throwAsRuntimeException(e.getTargetException());
                    }
                    catch (Exception e)
                    {
                        return ExceptionUtil.throwAsRuntimeException(e);
                    }
                }
            }
        }
        try
        {
            if (!method.isAccessible())
            {
                method.setAccessible(true);
            }
            return method.invoke(target, args);
        }
        catch (InvocationTargetException e)
        {
            return ExceptionUtil.throwAsRuntimeException(e.getTargetException());
        }
        catch (Exception e)
        {
            return ExceptionUtil.throwAsRuntimeException(e);
        }
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException
    {
        out.writeInt(index);
        out.writeObject(target);

        out.writeInt(instances.size());
        for (Map.Entry<Decorator<?>, ?> entry : instances.entrySet())
        {
            Decorator<?> key = entry.getKey();
            serializeDecorator(out, key);
            out.writeObject(entry.getValue());
        }

        out.writeInt(decorators.size());
        for (Decorator<?> decorator : decorators)
        {
            serializeDecorator(out, decorator);
        }

        out.writeUTF(passivationId);
    }

    Object readResolve() throws ObjectStreamException
    {
        WebBeansContext webBeansContext = WebBeansContext.getInstance();
        BeanManager beanManager = webBeansContext.getBeanManagerImpl();
        Bean<?> bean = beanManager.getPassivationCapableBean(passivationId);

        return webBeansContext.getInterceptorDecoratorProxyFactory().createProxyInstance(
            webBeansContext.getInterceptorDecoratorProxyFactory().getCachedProxyClass(bean),
            target,
            this
        );
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
    {
        index = in.readInt();
        target = in.readObject();

        int instancesSize = in.readInt();
        WebBeansContext webBeansContext = WebBeansContext.getInstance();
        BeanManager beanManager = webBeansContext.getBeanManagerImpl();

        Map<Decorator<?>, Object> tmpInstances = new HashMap<>();
        for (int i = 0; i < instancesSize; i++)
        {
            Decorator<?> bean = (Decorator<?>) beanManager.getPassivationCapableBean(in.readUTF());
            Object value = in.readObject();
            tmpInstances.put(bean, value);
        }
        instances = tmpInstances;

        int decoratorsSize = in.readInt();
        decorators = new CopyOnWriteArrayList<>();
        for (int i = 0; i < decoratorsSize; i++)
        {
            decorators.add((Decorator<?>) beanManager.getPassivationCapableBean(in.readUTF()));
        }

        passivationId = in.readUTF();
        Bean<?> bean = beanManager.getPassivationCapableBean(passivationId);
        if (bean instanceof OwbBean)
        {
            Producer<?> producer = ((OwbBean<?>)bean).getProducer();
            if (producer instanceof AbstractProducer)
            {
                interceptorInfo = ((AbstractProducer<?>)producer).getInterceptorInfo();
            }
        }
    }

    private static void serializeDecorator(ObjectOutput out, Decorator<?> key) throws IOException
    {
        String id = WebBeansUtil.getPassivationId(key);
        if (id == null)
        {
            throw new NotSerializableException(key + " is not serializable");
        }
        out.writeUTF(id);
    }
}
