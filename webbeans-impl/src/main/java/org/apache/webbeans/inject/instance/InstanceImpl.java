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
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.enterprise.context.spi.AlterableContext;
import javax.enterprise.context.spi.Context;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.Instance;
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

    private CreationalContextImpl<?> parentCreationalContext;
    
    /**
     * Creates new instance.
     * 
     * @param injectionClazz injection class type
     * @param injectionPoint null or injection point
     * @param webBeansContext
     * @param creationalContext will get used for creating &#064;Dependent beans
     * @param annotations qualifier annotations
     */
    public InstanceImpl(Type injectionClazz, InjectionPoint injectionPoint, WebBeansContext webBeansContext,
                 CreationalContextImpl<?> creationalContext, Annotation... annotations)
    {
        this.injectionClazz = injectionClazz;
        this.injectionPoint = injectionPoint;
        parentCreationalContext = creationalContext;

        for (Annotation ann : annotations)
        {
            qualifierAnnotations.add(ann);
        }
        this.webBeansContext = webBeansContext;
    }

    /**
     * Returns the bean instance with given qualifier annotations.
     * 
     * @return bean instance
     */
    @Override
    @SuppressWarnings("unchecked")
    public T get()
    {
        Annotation[] anns = new Annotation[qualifierAnnotations.size()];
        anns = qualifierAnnotations.toArray(anns);

        Set<Bean<?>> beans = resolveBeans();

        BeanManagerImpl beanManager = webBeansContext.getBeanManagerImpl();

        Bean<?> bean = beanManager.resolve(beans);

        if (bean == null)
        {
            InjectionExceptionUtil.throwUnsatisfiedResolutionException(ClassUtil.getClazz(injectionClazz), injectionPoint, anns);
        }

        // since Instance<T> is Dependent, we we gonna use the parent CreationalContext by default
        CreationalContext<?> creationalContext = parentCreationalContext;

        boolean isDependentBean = WebBeansUtil.isDependent(bean);

        if (!isDependentBean)
        {
            // but for all NormalScoped beans we will need to create a fresh CreationalContext
            creationalContext = beanManager.createCreationalContext(bean);
        }
        if (!(creationalContext instanceof CreationalContextImpl))
        {
            creationalContext = webBeansContext.getCreationalContextFactory().wrappedCreationalContext(creationalContext, bean);
        }

        ((CreationalContextImpl<?>)creationalContext).putInjectionPoint(injectionPoint);
        try
        {
            return (T) beanManager.getReference(bean, injectionClazz, creationalContext);
        }
        finally
        {
            ((CreationalContextImpl<?>)creationalContext).removeInjectionPoint();
        }
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

        Bean<?> injectionPointBean = injectionPoint.getBean();
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
        Annotation[] newQualifiersArray = getAdditionalQualifiers(qualifiers);
        return new InstanceImpl<T>(injectionClazz, injectionPoint, webBeansContext, parentCreationalContext, newQualifiersArray);
    }

    /**
     * Returns total qualifiers array
     * 
     * @param qualifiers additional qualifiers
     * @return total qualifiers array
     */
    private Annotation[] getAdditionalQualifiers(Annotation[] qualifiers)
    {
        webBeansContext.getAnnotationManager().checkQualifierConditions(qualifiers);

        Set<Annotation> newQualifiers = new HashSet<Annotation>(qualifierAnnotations);

        if (qualifiers != null && qualifiers.length > 0)
        {
            for (int i = 0; i < qualifiers.length; i++)
            {
                newQualifiers.add(qualifiers[i]);
            }
        }

        Annotation[] newQualifiersArray = new Annotation[newQualifiers.size()];
        newQualifiersArray = newQualifiers.toArray(newQualifiersArray);
        
        return newQualifiersArray;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public <U extends T> Instance<U> select(Class<U> subtype, Annotation... qualifiers)
    {
        webBeansContext.getAnnotationManager().checkQualifierConditions(qualifiers);

        Type sub = subtype;
        
        if(sub == null)
        {
            sub = injectionClazz;
        }
        
        Annotation[] newQualifiers = getAdditionalQualifiers(qualifiers);
        
        return new InstanceImpl<U>(sub, injectionPoint, webBeansContext, parentCreationalContext, newQualifiers);
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
    @SuppressWarnings("unchecked")
    public Iterator<T> iterator()
    {
        Set<Bean<?>> beans = resolveBeans();
        List<T> instances = new ArrayList<T>();
        parentCreationalContext.putInjectionPoint(injectionPoint);
        try
        {
            for(Bean<?> bean : beans)
            {
                T instance = (T) webBeansContext.getBeanManagerImpl().getReference(bean,null, parentCreationalContext);
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
        if (instance instanceof OwbNormalScopeProxy)
        {
            OwbNormalScopeProxy proxy = (OwbNormalScopeProxy) instance;
            Provider<T> provider = webBeansContext.getNormalScopeProxyFactory().getInstanceProvider(proxy);
            NormalScopedBeanInterceptorHandler handler = (NormalScopedBeanInterceptorHandler)provider;
            Bean<T> bean = (Bean<T>)handler.getBean();
            CreationalContext<T> creationalContext = (CreationalContext<T>)parentCreationalContext;
            Context currentContext = webBeansContext.getContextsService().getCurrentContext(bean.getScope());
            if (currentContext instanceof AlterableContext)
            {
                AlterableContext alterableContext = (AlterableContext)currentContext;
                alterableContext.destroy(bean);
            }
            else
            {
                bean.destroy(instance, creationalContext);
            }
        }
        else
        {
            parentCreationalContext.destroyDependent(instance);
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
        }

        builder.append("}");

        return builder.toString();
    }
}
