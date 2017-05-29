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
package org.apache.webbeans.inject.instance;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Member;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.enterprise.context.spi.AlterableContext;
import javax.enterprise.context.spi.Context;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.util.TypeLiteral;
import javax.inject.Provider;

import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.container.BeanManagerImpl;
import org.apache.webbeans.container.InjectionResolver;
import org.apache.webbeans.context.creational.CreationalContextImpl;
import org.apache.webbeans.intercept.NormalScopedBeanInterceptorHandler;
import org.apache.webbeans.proxy.OwbNormalScopeProxy;
import org.apache.webbeans.util.ClassUtil;
import org.apache.webbeans.util.InjectionExceptionUtil;
import org.apache.webbeans.util.OwbCustomObjectInputStream;
import org.apache.webbeans.util.WebBeansUtil;

/**
 * Implements the {@link Instance} interface.
 * 
 * @param <T> specific instance type
 */
public class InstanceImpl<T> implements Instance<T>, Serializable
{
    private static final long serialVersionUID = -8401944412490389024L;

    /** Injected class type */
    private Type injectionClazz;

    /**
     * injection point, needed for injection into producer method and used to determine the BDA it was loaded from or null.
     */
    private InjectionPoint injectionPoint;

    /** Qualifier annotations appeared on the injection point */
    private Set<Annotation> qualifierAnnotations = new HashSet<Annotation>();

    private WebBeansContext webBeansContext;

    private Map<Object, CreationalContextImpl<?>> creationalContexts;
    private CreationalContextImpl<?> parentCreationalContext;

    private boolean strictValidation;

    /**
     * Creates new instance.
     * 
     * @param injectionClazz injection class type
     * @param injectionPoint null or injection point
     * @param webBeansContext
     * @param creationalContext will get used for creating &#064;Dependent beans
     * @param qualifiers qualifier annotations
     */
    public InstanceImpl(Type injectionClazz, InjectionPoint injectionPoint, WebBeansContext webBeansContext,
                 CreationalContextImpl<?> creationalContext, Annotation... qualifiers)
    {
        this.injectionClazz = injectionClazz;
        this.injectionPoint = injectionPoint;
        parentCreationalContext = creationalContext;

        this.webBeansContext = webBeansContext;
        strictValidation = webBeansContext.getOpenWebBeansConfiguration().strictDynamicValidation();

        if (strictValidation)
        {
            webBeansContext.getAnnotationManager().checkQualifierConditions(qualifiers);
        }

        for (Annotation ann : qualifiers)
        {
            qualifierAnnotations.add(ann);
        }
    }

    /**
     * Returns the bean instance with given qualifier annotations.
     * 
     * @return bean instance
     */
    @Override
    public T get()
    {

        Set<Bean<?>> beans = resolveBeans();

        Bean<?> bean = webBeansContext.getBeanManagerImpl().resolve(beans);

        if (bean == null)
        {
            Annotation[] anns = new Annotation[qualifierAnnotations.size()];
            anns = qualifierAnnotations.toArray(anns);
            InjectionExceptionUtil.throwUnsatisfiedResolutionException(ClassUtil.getClazz(injectionClazz), injectionPoint, anns);
        }

        return create(bean);
    }


    /**
     * Returns set of resolved beans.
     * 
     * @return set of resolved beans
     */
    private Set<Bean<?>> resolveBeans()
    {
        Annotation[] anns = new Annotation[qualifierAnnotations.size()];
        anns = qualifierAnnotations.toArray(anns);

        InjectionResolver injectionResolver = webBeansContext.getBeanManagerImpl().getInjectionResolver();

        Bean<?> injectionPointBean = injectionPoint != null ? injectionPoint.getBean() : null;
        Class<?> injectionPointClass = null;
        if (injectionPointBean != null)
        {
            injectionPointClass = injectionPointBean.getBeanClass();
        }
        Set<Bean<?>> beans = injectionResolver.implResolveByType(false, injectionClazz, injectionPointClass, anns);
        return injectionResolver.resolveAll(beans);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isAmbiguous()
    {
        Set<Bean<?>> beans = resolveBeans();
        
        return beans.size() > 1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isUnsatisfied()
    {
        Set<Bean<?>> beans = resolveBeans();
        
        return beans.size() == 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Instance<T> select(Annotation... qualifiers)
    {
        if (strictValidation)
        {
            webBeansContext.getAnnotationManager().checkQualifierConditions(qualifiers);
        }

        Annotation[] newQualifiersArray = qualifiers;
        return new InstanceImpl<T>(
                injectionClazz, injectionPoint == null ? null : new InstanceInjectionPoint(injectionPoint, newQualifiersArray),
                webBeansContext, parentCreationalContext, newQualifiersArray);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <U extends T> Instance<U> select(Class<U> subtype, Annotation... qualifiers)
    {
        if (strictValidation)
        {
            webBeansContext.getAnnotationManager().checkQualifierConditions(qualifiers);
        }

        Type sub = subtype;
        
        if(sub == null)
        {
            sub = injectionClazz;
        }
        Set<Annotation> mergedQualifiers = new HashSet<>(qualifierAnnotations);
        for (Annotation qualifier : qualifiers)
        {
            mergedQualifiers.add(qualifier);
        }
        return new InstanceImpl<U>(sub, injectionPoint, webBeansContext, parentCreationalContext,
                                   mergedQualifiers.toArray(new Annotation[mergedQualifiers.size()]));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <U extends T> Instance<U> select(TypeLiteral<U> subtype, Annotation... qualifiers)
    {        
        return select(subtype.getRawType(), qualifiers);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator<T> iterator()
    {
        Set<Bean<?>> beans = resolveBeans();
        List<T> instances = new ArrayList<T>();
        parentCreationalContext.putInjectionPoint(injectionPoint);
        try
        {
            for(Bean<?> bean : beans)
            {
                T instance = create(bean);
                instances.add(instance);
            }
        }
        finally
        {
            parentCreationalContext.removeInjectionPoint();
        }
        
        return instances.iterator();
    }

    public void destroy(T instance)
    {
        if (instance == null)
        {
            throw new NullPointerException("instance is null, can't be destroyed");
        }
        if (instance instanceof OwbNormalScopeProxy)
        {
            OwbNormalScopeProxy proxy = (OwbNormalScopeProxy) instance;
            Provider<T> provider = webBeansContext.getNormalScopeProxyFactory().getInstanceProvider(proxy);
            NormalScopedBeanInterceptorHandler handler = (NormalScopedBeanInterceptorHandler)provider;
            Bean<T> bean = (Bean<T>)handler.getBean();
            Class<? extends Annotation> beanScope = bean.getScope();
            Context currentContext = webBeansContext.getBeanManagerImpl().getContext(beanScope);
            if (currentContext instanceof AlterableContext)
            {
                AlterableContext alterableContext = (AlterableContext)currentContext;
                alterableContext.destroy(bean);
            }
            else
            {
                throw new UnsupportedOperationException("Not AlterableContext so you can't call destroy youself");
            }
        }
        else
        {
            final CreationalContextImpl<?> creationalContext = creationalContexts.remove(instance);
            if (creationalContext == null)
            {
                throw new IllegalArgumentException("instance " + instance + " not produced with this Instance<?>");
            }
            creationalContext.destroyDependent(instance);
        }
    }

    @SuppressWarnings("unchecked")
    private T create(Bean<?> bean)
    {
        BeanManagerImpl beanManager = webBeansContext.getBeanManagerImpl();
        CreationalContextImpl<?> creationalContext = beanManager.createCreationalContext(bean);

        creationalContext.putInjectionPoint(injectionPoint);
        try
        {
            final T reference = (T) beanManager.getReference(bean, injectionClazz, creationalContext);
            if (creationalContexts == null)
            {
                creationalContexts = new IdentityHashMap<Object, CreationalContextImpl<?>>();
            }
            creationalContexts.put(reference, creationalContext);
            if (WebBeansUtil.isDependent(bean))
            {
                parentCreationalContext.addDependent(bean, reference);
            }
            return reference;
        }
        finally
        {
            creationalContext.removeInjectionPoint();
        }
    }
    
    private void writeObject(java.io.ObjectOutputStream op) throws IOException
    {
        ObjectOutputStream oos = new ObjectOutputStream(op);
        oos.writeObject(injectionClazz);
        oos.writeObject(qualifierAnnotations);
        oos.writeObject(injectionPoint);
        
        oos.flush();
    }
    
    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException
    {
        webBeansContext = WebBeansContext.currentInstance();
        final ObjectInputStream inputStream = new OwbCustomObjectInputStream(in, WebBeansUtil.getCurrentClassLoader());
        injectionClazz = (Type)inputStream.readObject();
        qualifierAnnotations = (Set<Annotation>)inputStream.readObject();
        injectionPoint = (InjectionPoint) inputStream.readObject();
        parentCreationalContext = webBeansContext.getBeanManagerImpl().createCreationalContext(null); // TODO: check what we can do
    }
    
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append("Instance<");
        builder.append(ClassUtil.getClazz(injectionClazz).getName());
        builder.append("> injectionPointClazz=").append(injectionPoint);
        
        builder.append(",with qualifier annotations {");
        int i = 0;
        for (Annotation qualifier : qualifierAnnotations)
        {
            if (i != 0)
            {
                builder.append(",");
            }

            builder.append(qualifier.toString());
            i++;
        }

        builder.append("}");

        return builder.toString();
    }

    private static class InstanceInjectionPoint implements InjectionPoint, Serializable
    {
        private InjectionPoint delegate;
        private Set<Annotation> qualifiers;

        protected InstanceInjectionPoint(final InjectionPoint injectionPoint, final Annotation[] newQualifiersArray)
        {
            this.delegate = injectionPoint;
            this.qualifiers = Collections.unmodifiableSet(new HashSet<Annotation>(Arrays.asList(newQualifiersArray)));
        }

        @Override
        public Type getType()
        {
            return delegate.getType();
        }

        @Override
        public Set<Annotation> getQualifiers()
        {
            return qualifiers;
        }

        @Override
        public Bean<?> getBean()
        {
            return delegate.getBean();
        }

        @Override
        public Member getMember()
        {
            return delegate.getMember();
        }

        @Override
        public Annotated getAnnotated()
        {
            return delegate.getAnnotated();
        }

        @Override
        public boolean isDelegate()
        {
            return delegate.isDelegate();
        }

        @Override
        public boolean isTransient()
        {
            return delegate.isTransient();
        }

        private void readObject(final ObjectInputStream inp) throws IOException, ClassNotFoundException
        {
            final OwbCustomObjectInputStream owbCustomObjectInputStream = new OwbCustomObjectInputStream(inp, WebBeansUtil.getCurrentClassLoader());
            qualifiers = Set.class.cast(owbCustomObjectInputStream.readObject());
            delegate = InjectionPoint.class.cast(owbCustomObjectInputStream.readObject());
        }

        private void writeObject(final ObjectOutputStream op) throws IOException
        {
            final ObjectOutputStream out = new ObjectOutputStream(op);
            out.writeObject(qualifiers);
            out.writeObject(delegate);
        }
    }
}
