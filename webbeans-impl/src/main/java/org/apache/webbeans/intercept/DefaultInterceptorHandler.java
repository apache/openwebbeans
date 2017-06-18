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

import org.apache.webbeans.component.InjectionTargetBean;
import org.apache.webbeans.component.SelfInterceptorBean;
import org.apache.webbeans.component.WebBeansType;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.portable.InjectionTargetImpl;
import org.apache.webbeans.proxy.InterceptorHandler;
import org.apache.webbeans.util.ExceptionUtil;
import org.apache.webbeans.util.WebBeansUtil;

import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.enterprise.inject.spi.InterceptionType;
import javax.enterprise.inject.spi.Interceptor;
import javax.inject.Provider;
import java.io.Externalizable;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.ObjectStreamException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DefaultInterceptorHandler<T> implements InterceptorHandler, Externalizable
{
    private static final String SELF_KEY = "SELF_INTERCEPTOR";

    /**
     * The native contextual instance target instance.
     * This is the unproxies and undecorated instance.
     * It e.g. get's used for direct event delivery to private Observer methods.
     */
    private T target;

    /**
     * The instance the Interceptors get applied on.
     * If there is no Decorator involved, then this is the same like {@link #target}.
     * For decorated beans this will point to the outermost Decorator instance.
     */
    private T delegate;

    /**
     * The passivation if in case this is a
     * {@link javax.enterprise.inject.spi.PassivationCapable} bean.
     * we just keep this field for serializing it away
     */
    private String beanPassivationId;


    private Map<Method, List<Interceptor<?>>> interceptors;
    private Map<Interceptor<?>, ?> instances;

    /**
     * InterceptorHandler wich gets used in our InjectionTargets which
     * support interceptors and decorators
     * @param target the decorated and intercepted instance. Needed for delivering Events to private methods, etc.
     * @param delegate the outermost Decorator or the intercepted instance
     * @param interceptors Map with all active interceptors for each method.
     * @param instances the Interceptor instances
     * @param beanPassivationId passivationId if a Bean is {@link javax.enterprise.inject.spi.PassivationCapable}
     */
    public DefaultInterceptorHandler(T target,
                                     T delegate,
                                     Map<Method, List<Interceptor<?>>> interceptors,
                                     Map<Interceptor<?>, ?> instances,
                                     String beanPassivationId)
    {
        this.target = target;
        this.delegate = delegate;
        this.instances = instances;
        this.interceptors = interceptors;
        this.beanPassivationId = beanPassivationId;
    }

    public DefaultInterceptorHandler()
    {
        // no-op: for serialization
    }

    public T getTarget()
    {
        return target;
    }

    public T getDelegate()
    {
        return delegate;
    }

    public Map<Interceptor<?>, ?> getInstances()
    {
        return instances;
    }

    public Map<Method, List<Interceptor<?>>> getInterceptors()
    {
        return interceptors;
    }

    @Override
    public Object invoke(Method method, Object[] parameters)
    {
        try
        {
            List<Interceptor<?>> methodInterceptors = interceptors.get(method);
            if (methodInterceptors == null)
            {
                methodInterceptors = Collections.emptyList();
            }

            InterceptorInvocationContext<T> ctx
                = new InterceptorInvocationContext<T>(new InstanceProvider(delegate), InterceptionType.AROUND_INVOKE, methodInterceptors, instances, method, parameters);

            return ctx.proceed();
        }
        catch (Exception e)
        {
            return ExceptionUtil.throwAsRuntimeException(e);
        }
    }

    /**
     * The following code gets generated into the proxy:
     *
     * <pre>
     * Object writeReplace() throws ObjectStreamException
     * {
     *     return provider;
     * }
     * </pre>
     *
     * The trick is to replace the generated proxy class with this handler
     * and on deserialisation we use readResolve to create/resolve
     * the proxy class again.
     */
    @SuppressWarnings("unused")
    Object readResolve() throws ObjectStreamException
    {
        WebBeansContext webBeansContext = WebBeansContext.getInstance();
        BeanManager beanManager = webBeansContext.getBeanManagerImpl();
        Bean<T> bean = (Bean<T>) beanManager.getPassivationCapableBean(beanPassivationId);

        return webBeansContext.getInterceptorDecoratorProxyFactory().createProxyInstance(
            webBeansContext.getInterceptorDecoratorProxyFactory().getCachedProxyClass(bean),
            target,
            this
        );
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException
    {
        out.writeObject(target);

        boolean noDecorator = target == delegate;
        out.writeBoolean(noDecorator);
        if (!noDecorator)
        {
            out.writeObject(delegate);
        }

        out.writeInt(instances.size());
        for (Map.Entry<Interceptor<?>, ?> entry : instances.entrySet())
        {
            Interceptor<?> key = entry.getKey();
            if (serializeInterceptor(out, key))
            {
                out.writeObject(entry.getValue());
            }
        }

        out.writeInt(interceptors.size());
        for (Map.Entry<Method, List<Interceptor<?>>> entry : interceptors.entrySet())
        {
            Method key = entry.getKey();
            out.writeObject(key.getDeclaringClass());
            out.writeUTF(key.getName());
            out.writeObject(key.getParameterTypes());

            List<Interceptor<?>> value = entry.getValue();
            out.writeInt(value.size());
            for (Interceptor<?> i : value)
            {
                serializeInterceptor(out, i);
            }
        }

        out.writeUTF(beanPassivationId);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
    {
        target = (T) in.readObject();
        if (in.readBoolean())
        {
            delegate = target;
        }
        else
        {
            delegate = (T) in.readObject();
        }

        int instancesSize = in.readInt();
        WebBeansContext webBeansContext = WebBeansContext.getInstance();
        BeanManager beanManager = webBeansContext.getBeanManagerImpl();

        Map<Interceptor<?>, Object> tmpInstances = new HashMap<Interceptor<?>, Object>();
        for (int i = 0; i < instancesSize; i++)
        {
            Interceptor<?> interceptor = readInterceptor(in.readUTF(), beanManager);
            if (!SelfInterceptorBean.class.isInstance(interceptor))
            {
                Object value = in.readObject();
                tmpInstances.put(interceptor, value);
            }
            else
            {
                tmpInstances.put(interceptor, target);
            }
        }
        instances = tmpInstances;

        int interceptorsSize = in.readInt();
        interceptors = new HashMap<Method, List<Interceptor<?>>>(interceptorsSize);
        for (int i = 0; i < interceptorsSize; i++)
        {
            Class<?> declaringClass = (Class<?>) in.readObject();
            String name = in.readUTF();
            Class<?>[] parameters = (Class<?>[]) in.readObject();
            Method method;
            try
            {
                method = declaringClass.getDeclaredMethod(name, parameters);
            }
            catch (NoSuchMethodException e)
            {
                throw new NotSerializableException(target.getClass().getName());
            }

            int interceptorListSize = in.readInt();
            List<Interceptor<?>> interceptorList = new ArrayList<Interceptor<?>>(interceptorListSize);
            for (int j = 0; j < interceptorListSize; j++)
            {
                interceptorList.add(readInterceptor(in.readUTF(), beanManager));
            }
            interceptors.put(method, interceptorList);
        }

        beanPassivationId = in.readUTF();
    }

    /**
     * @return false if the interceptor value can be ignored
     */
    private static boolean serializeInterceptor(ObjectOutput out, Interceptor<?> key) throws IOException
    {
        if (SelfInterceptorBean.class.isInstance(key))
        {
            String beanName = WebBeansUtil.getPassivationId(key)
                .replace(WebBeansType.INTERCEPTOR.name(), WebBeansType.MANAGED.name());
            out.writeUTF(SELF_KEY + beanName);
            return false;
        }

        String id = WebBeansUtil.getPassivationId(key);
        if (id == null)
        {
            throw new NotSerializableException(key + " is not serializable");
        }
        out.writeUTF(id);
        return true;
    }

    private static Interceptor<?> readInterceptor(String id, BeanManager beanManager) throws IOException
    {
        if (id.startsWith(SELF_KEY))
        {
            Bean<?> bean = beanManager.getPassivationCapableBean(id.substring(SELF_KEY.length()));
            if (InjectionTargetBean.class.isInstance(bean))
            {
                InjectionTarget<?> it = InjectionTargetBean.class.cast(bean).getInjectionTarget();
                if (InjectionTargetImpl.class.isInstance(it))
                {
                    InterceptorResolutionService.BeanInterceptorInfo info = InjectionTargetImpl.class.cast(it)
                                                                                                .getInterceptorInfo();
                    return info.getSelfInterceptorBean();
                }
                else
                {
                    throw new NotSerializableException("Can't find self interceptor");
                }
            }
            else
            {
                throw new NotSerializableException("Can't find self interceptor");
            }
        }
        return (Interceptor<?>) beanManager.getPassivationCapableBean(id);
    }

    private static class InstanceProvider<T> implements Provider<T>
    {
        private final T value;

        public InstanceProvider(T delegate)
        {
            this.value = delegate;
        }

        @Override
        public T get()
        {
            return value;
        }
    }
}
