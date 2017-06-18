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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Member;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Set;

import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.Interceptor;

import org.apache.webbeans.component.third.ThirdpartyBeanImpl;
import org.apache.webbeans.context.creational.CreationalContextImpl;
import org.apache.webbeans.util.ClassUtil;
import org.apache.webbeans.util.OwbCustomObjectInputStream;
import org.apache.webbeans.util.WebBeansUtil;

public class InjectionPointProducer extends AbstractProducer<InjectionPoint>
{
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected InjectionPoint produce(Map<Interceptor<?>, ?> interceptors, CreationalContextImpl<InjectionPoint> creationalContextImpl)
    {
        if (creationalContextImpl == null)
        {
            return null;
        }

        // the first injection point on the stack is of type InjectionPoint, so we need the second one
        InjectionPoint first = creationalContextImpl.removeInjectionPoint();
        InjectionPoint injectionPoint;
        if (!InjectionPoint.class.isAssignableFrom(ClassUtil.getClass(first.getType())))
        {
            if (!ThirdpartyBeanImpl.class.isInstance(creationalContextImpl.getBean()))
            {
                throw new IllegalStateException("Inconsistent injection point stack");
            }
            injectionPoint = first;
        }
        else
        {
            injectionPoint = creationalContextImpl.getInjectionPoint();
        }

        if (injectionPoint == null)
        {
            return null;
        }

        try
        {
            Type type = injectionPoint.getType();
            if (ParameterizedType.class.isInstance(type))
            {
                ParameterizedType parameterizedType = ParameterizedType.class.cast(type);
                if (parameterizedType.getRawType() == Instance.class)
                {
                    Bean<InjectionPoint> bean = creationalContextImpl.getBean();
                    return new InjectionPointDelegate(
                            injectionPoint,
                            bean.getBeanClass() != null ? bean.getBeanClass() : parameterizedType.getActualTypeArguments()[0]);
                }
            }
            return injectionPoint;
        }
        finally
        {
            creationalContextImpl.putInjectionPoint(first);
        }
    }

    @Override
    public void dispose(InjectionPoint ip)
    {
        // nothing to do
    }

    private static class InjectionPointDelegate implements InjectionPoint, Serializable
    {
        private InjectionPoint ip;
        private Type type;

        public InjectionPointDelegate(InjectionPoint injectionPoint, Type type)
        {
            this.ip = injectionPoint;
            this.type = type;
        }

        @Override
        public Type getType()
        {
            return type;
        }

        @Override
        public Set<Annotation> getQualifiers()
        {
            return ip.getQualifiers();
        }

        @Override
        public Bean<?> getBean()
        {
            return ip.getBean();
        }

        @Override
        public Member getMember()
        {
            return ip.getMember();
        }

        @Override
        public Annotated getAnnotated()
        {
            return ip.getAnnotated();
        }

        @Override
        public boolean isDelegate()
        {
            return ip.isDelegate();
        }

        @Override
        public boolean isTransient()
        {
            return ip.isTransient();
        }

        private void readObject(ObjectInputStream inp) throws IOException, ClassNotFoundException
        {
            OwbCustomObjectInputStream owbCustomObjectInputStream = new OwbCustomObjectInputStream(inp, WebBeansUtil.getCurrentClassLoader());
            type = Type.class.cast(owbCustomObjectInputStream.readObject());
            ip = InjectionPoint.class.cast(owbCustomObjectInputStream.readObject());
        }

        private void writeObject(ObjectOutputStream op) throws IOException
        {
            ObjectOutputStream out = new ObjectOutputStream(op);
            out.writeObject(type);
            out.writeObject(ip);
        }
    }
}
