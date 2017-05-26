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
package org.apache.webbeans.container;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Set;

import javax.el.ELResolver;
import javax.el.ExpressionFactory;
import javax.enterprise.context.spi.Context;
import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Event;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedMember;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanAttributes;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Decorator;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.enterprise.inject.spi.InjectionTargetFactory;
import javax.enterprise.inject.spi.InterceptionFactory;
import javax.enterprise.inject.spi.InterceptionType;
import javax.enterprise.inject.spi.Interceptor;
import javax.enterprise.inject.spi.ObserverMethod;
import javax.enterprise.inject.spi.ProducerFactory;

import org.apache.webbeans.config.WebBeansContext;

/**
 * <p>This implementation of the {@link BeanManager} will get used
 * for whenever a BeanManager gets injected into a bean:
 * <pre>
 *   private @Inject BeanManager beanManager;
 * </pre>
 * </p>
 * This class is Serializable and always resolves the current
 * instance of the central BeanManager automatically.
 */
public class InjectableBeanManager implements BeanManager, Serializable, Externalizable
{

    private static final long serialVersionUID = 1L;
    
    private transient BeanManagerImpl bm;

    /**
     * Used by serialization.
     */
    public InjectableBeanManager()
    {
        this(WebBeansContext.getInstance().getBeanManagerImpl());
    }

    public InjectableBeanManager(BeanManagerImpl beanManager)
    {
        bm = beanManager;
    }

    @Override
    public <T> AnnotatedType<T> createAnnotatedType(Class<T> type)
    {
        return bm.createAnnotatedType(type);
    }

    @Override
    public <T> CreationalContext<T> createCreationalContext(Contextual<T> contextual)
    {
        return bm.createCreationalContext(contextual);
    }

    @Override
    public <T> InjectionTarget<T> createInjectionTarget(AnnotatedType<T> type)
    {
        return bm.createInjectionTarget(type);
    }

    @Override
    public void fireEvent(Object event, Annotation... qualifiers)
    {
        bm.fireEvent(event, qualifiers);
    }

    //X TODO OWB-1182 CDI 2.0
    @Override
    public Event<Object> getEvent()
    {
        throw new UnsupportedOperationException("CDI 2.0 not yet imlemented");
    }

    @Override
    public Set<Bean<?>> getBeans(String name)
    {
        checkAfterBeanDiscoveryProcessed("It's not allowed to call getBeans(String) before AfterBeanDiscovery");

        return bm.getBeans(name);
    }

    @Override
    public Set<Bean<?>> getBeans(Type beanType, Annotation... qualifiers)
    {
        checkAfterBeanDiscoveryProcessed("It's not allowed to call getBeans(Type, Annotation...) before AfterBeanDiscovery");

        return bm.getBeans(beanType, qualifiers);
    }

    @Override
    public Context getContext(Class<? extends Annotation> scope)
    {
        return bm.getContext(scope);
    }

    //X TODO OWB-1182 CDI 2.0
    @Override
    public Instance<Object> createInstance()
    {
        throw new UnsupportedOperationException("CDI 2.0 not yet imlemented");
    }

    @Override
    public ELResolver getELResolver()
    {
        return bm.getELResolver();
    }

    @Override
    public Object getInjectableReference(InjectionPoint injectionPoint, CreationalContext<?> ctx)
    {
        checkAfterDeploymentValidationFired("It's not allowed to call getInjectableReference(InjectionPoin, CreationalContext) before AfterDeploymentValidation");

        return bm.getInjectableReference(injectionPoint, ctx);
    }

    @Override
    public Set<Annotation> getInterceptorBindingDefinition(Class<? extends Annotation> qualifier)
    {
        return bm.getInterceptorBindingDefinition(qualifier);
    }

    @Override
    public Bean<?> getPassivationCapableBean(String id)
    {
        checkAfterBeanDiscoveryProcessed("It's not allowed to call getPassivationCapableBean(String) before AfterBeanDiscovery");

        return bm.getPassivationCapableBean(id);
    }

    @Override
    public Object getReference(Bean<?> bean, Type beanType, CreationalContext<?> ctx)
    {
        checkAfterDeploymentValidationFired("It's not allowed to call getReference(Bean, Type, CreationalContext) before AfterDeploymentValidation");

        return bm.getReference(bean, beanType, ctx);
    }

    @Override
    public Set<Annotation> getStereotypeDefinition(Class<? extends Annotation> stereotype)
    {
        return bm.getStereotypeDefinition(stereotype);
    }

    @Override
    public boolean isInterceptorBinding(Class<? extends Annotation> annotationType)
    {
        return bm.isInterceptorBinding(annotationType);
    }

    @Override
    public boolean isNormalScope(Class<? extends Annotation> annotationType)
    {
        return bm.isNormalScope(annotationType);
    }

    @Override
    public boolean isPassivatingScope(Class<? extends Annotation> annotationType)
    {
        return bm.isPassivatingScope(annotationType);
    }

    @Override
    public boolean isQualifier(Class<? extends Annotation> annotationType)
    {
        return bm.isQualifier(annotationType);
    }

    @Override
    public boolean isScope(Class<? extends Annotation> annotationType)
    {
        return bm.isScope(annotationType);
    }

    @Override
    public boolean isStereotype(Class<? extends Annotation> annotationType)
    {
        return bm.isStereotype(annotationType);
    }

    @Override
    public <X> Bean<? extends X> resolve(Set<Bean<? extends X>> beans)
    {
        checkAfterBeanDiscoveryProcessed("It's not allowed to call resolve(Set<Bean>) before AfterBeanDiscovery");

        return bm.resolve(beans);
    }

    //X TODO OWB-1182 CDI 2.0
    @Override
    public <T> InterceptionFactory<T> createInterceptionFactory(CreationalContext<T> creationalContext, Class<T> aClass)
    {
        throw new UnsupportedOperationException("CDI 2.0 not yet imlemented");
    }

    @Override
    public List<Decorator<?>> resolveDecorators(Set<Type> types, Annotation... qualifiers)
    {
        checkAfterBeanDiscoveryProcessed("It's not allowed to call resolveDecorators(Set<Type>, Annotation...) before AfterBeanDiscovery");

        return bm.resolveDecorators(types, qualifiers);
    }

    @Override
    public List<Interceptor<?>> resolveInterceptors(InterceptionType type, Annotation... interceptorBindings)
    {
        checkAfterBeanDiscoveryProcessed("It's not allowed to call resolveInterceptors(InterceptionType, Annotation...) before AfterBeanDiscovery");

        return bm.resolveInterceptors(type, interceptorBindings);
    }

    @Override
    public <T> Set<ObserverMethod<? super T>> resolveObserverMethods(T event, Annotation... qualifiers)
    {
        checkAfterBeanDiscoveryProcessed("It's not allowed to call resolveObserverMethods(Object, Annotation...) before AfterBeanDiscovery");

        return bm.resolveObserverMethods(event, qualifiers);
    }

    @Override
    public void validate(InjectionPoint injectionPoint)
    {
        checkAfterBeanDiscoveryProcessed("It's not allowed to call validate(InjectionPoint) before AfterBeanDiscovery");

        bm.validate(injectionPoint);
    }

    @Override
    public ExpressionFactory wrapExpressionFactory(ExpressionFactory expressionFactory)
    {
        return bm.wrapExpressionFactory(expressionFactory);
    }

    @Override
    public boolean areQualifiersEquivalent(Annotation qualifier1, Annotation qualifier2)
    {
        return bm.areQualifiersEquivalent(qualifier1, qualifier2);
    }

    @Override
    public int getQualifierHashCode(Annotation qualifier)
    {
        return bm.getQualifierHashCode(qualifier);
    }

    @Override
    public boolean areInterceptorBindingsEquivalent(Annotation interceptorBinding1, Annotation interceptorBinding2)
    {
        return bm.areInterceptorBindingsEquivalent(interceptorBinding1, interceptorBinding2);
    }

    @Override
    public int getInterceptorBindingHashCode(Annotation interceptorBinding)
    {
        return bm.getInterceptorBindingHashCode(interceptorBinding);
    }

    @Override
    public InjectionPoint createInjectionPoint(AnnotatedField<?> field)
    {
        return bm.createInjectionPoint(field);
    }

    @Override
    public InjectionPoint createInjectionPoint(AnnotatedParameter<?> parameter)
    {
        return bm.createInjectionPoint(parameter);
    }

    @Override
    public <T> InjectionTargetFactory<T> getInjectionTargetFactory(AnnotatedType<T> type)
    {
        return bm.getInjectionTargetFactory(type);
    }

    @Override
    public <X> ProducerFactory<X> getProducerFactory(AnnotatedField<? super X> field, Bean<X> declaringBean)
    {
        return bm.getProducerFactory(field, declaringBean);
    }

    @Override
    public <X> ProducerFactory<X> getProducerFactory(AnnotatedMethod<? super X> method, Bean<X> declaringBean)
    {
        return bm.getProducerFactory(method, declaringBean);
    }

    @Override
    public <T> BeanAttributes<T> createBeanAttributes(AnnotatedType<T> type)
    {
        return bm.createBeanAttributes(type);
    }

    @Override
    public BeanAttributes<?> createBeanAttributes(AnnotatedMember<?> member)
    {
        return bm.createBeanAttributes(member);
    }

    @Override
    public <T> Bean<T> createBean(BeanAttributes<T> attributes, Class<T> beanClass, InjectionTargetFactory<T> injectionTargetFactory)
    {
        return bm.createBean(attributes, beanClass, injectionTargetFactory);
    }

    @Override
    public <T, X> Bean<T> createBean(BeanAttributes<T> attributes, Class<X> beanClass, ProducerFactory<X> producerFactory)
    {
        return bm.createBean(attributes, beanClass, producerFactory);
    }

    @Override
    public <T extends Extension> T getExtension(Class<T> extensionClass)
    {
        return bm.getExtension(extensionClass);
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException
    {    
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException,
            ClassNotFoundException 
    {
        //static lookup required for bean manager
        bm = WebBeansContext.currentInstance().getBeanManagerImpl();
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((bm == null) ? 0 : System.identityHashCode(bm));
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;   
        }
        if (obj == null)
        {
            return false;   
        }
        if (getClass() != obj.getClass())
        {
            return false;   
        }
        
        InjectableBeanManager other = (InjectableBeanManager) obj;
        if (bm == null)
        {
            if (other.bm != null)
            {
                return false;   
            }
        }
        else if (System.identityHashCode(bm) != (System.identityHashCode(other.bm)))
        {
            return false;   
        }
        
        return true;
    }


    /**
     * @throws IllegalStateException if {@link javax.enterprise.inject.spi.AfterBeanDiscovery}
     */
    private void checkAfterBeanDiscoveryProcessed(String message)
    {
        if (!bm.isAfterBeanDiscoveryDone() && !bm.isAfterBeanDiscovery())
        {
            throw new IllegalStateException(message);
        }
    }

    /**
     * @throws IllegalStateException if {@link javax.enterprise.inject.spi.AfterDeploymentValidation}
     */
    private void checkAfterDeploymentValidationFired(String message)
    {
        if (!bm.isAfterDeploymentValidationFired())
        {
            throw new IllegalStateException(message);
        }
    }

}
