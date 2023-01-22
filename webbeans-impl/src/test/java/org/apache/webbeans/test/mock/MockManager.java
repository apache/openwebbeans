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
package org.apache.webbeans.test.mock;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import jakarta.el.ELResolver;
import jakarta.el.ExpressionFactory;
import jakarta.enterprise.context.spi.Context;
import jakarta.enterprise.context.spi.Contextual;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.AnnotatedField;
import jakarta.enterprise.inject.spi.AnnotatedMember;
import jakarta.enterprise.inject.spi.AnnotatedMethod;
import jakarta.enterprise.inject.spi.AnnotatedParameter;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanAttributes;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.Decorator;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.enterprise.inject.spi.InjectionTargetFactory;
import jakarta.enterprise.inject.spi.InterceptionFactory;
import jakarta.enterprise.inject.spi.InterceptionType;
import jakarta.enterprise.inject.spi.Interceptor;
import jakarta.enterprise.inject.spi.ObserverMethod;
import jakarta.enterprise.inject.spi.ProducerFactory;
import jakarta.enterprise.util.TypeLiteral;

import org.apache.webbeans.component.AbstractOwbBean;
import org.apache.webbeans.component.BeanAttributesImpl;
import org.apache.webbeans.component.creation.BeanAttributesBuilder;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.config.WebBeansFinder;
import org.apache.webbeans.container.BeanManagerImpl;
import org.apache.webbeans.util.WebBeansUtil;

public class MockManager implements BeanManager
{
    private BeanManagerImpl manager = null;

    private List<AbstractOwbBean<?>> componentList = new ArrayList<AbstractOwbBean<?>>();

    public MockManager()
    {
        WebBeansFinder.clearInstances(WebBeansUtil.getCurrentClassLoader());
        WebBeansContext webBeansContext = WebBeansContext.getInstance();
        this.manager = webBeansContext.getBeanManagerImpl();
        manager.addBean(webBeansContext.getWebBeansUtil().getManagerBean());
    }

    public WebBeansContext getWebBeansContext()
    {
        return manager.getWebBeansContext();
    }

    public void clear()
    {
        componentList.clear();        
        
        WebBeansFinder.clearInstances(WebBeansUtil.getCurrentClassLoader());

        this.manager = WebBeansContext.getInstance().getBeanManagerImpl();
    }

    public List<AbstractOwbBean<?>> getComponents()
    {
        return componentList;
    }

    public AbstractOwbBean<?> getComponent(int i)
    {
        return componentList.get(i);
    }

    public int getDeployedCompnents()
    {
        return manager.getBeans().size();
    }

    public BeanManager addBean(Bean<?> bean)
    {
        manager.addBean(bean);
        return this;
    }

    public BeanManager addInternalBean(Bean<?> bean)
    {
        manager.addInternalBean(bean);
        return this;
    }

    public BeanManager addContext(Context context)
    {
        return manager.addContext(context);
    }

    @Override
    public Event<Object> getEvent()
    {
        return manager.getEvent();
    }

    @Override
    public Context getContext(Class<? extends Annotation> scopeType)
    {
        return manager.getContext(scopeType);
    }

    @Override
    public Instance<Object> createInstance()
    {
        return manager.createInstance();
    }

    public <T> T getInstance(Bean<T> bean)
    {
        return (T) manager.getReference(bean,null, manager.createCreationalContext(bean));
    }

    public <T> T getInstanceByType(Type type, Annotation... bindingTypes)
    {
        Bean<?> bean = manager.resolve(manager.getBeans(type, bindingTypes));
        if (bean == null)
        {
            return null;
        }
        return (T) manager.getReference(bean, type, manager.createCreationalContext(bean));
    }

    public <T> T getInstanceByType(TypeLiteral<T> type, Annotation... bindingTypes)
    {
        return  getInstanceByType(type.getType(), bindingTypes);
    }

    public Set<Bean<?>> resolveByName(String name)
    {
        return manager.getBeans(name);
    }

    public Set<Bean<?>> resolveByType(Class<?> apiType, Annotation... bindingTypes)
    {
        return manager.getBeans(apiType, bindingTypes);
    }

    @Override
    public List<Decorator<?>> resolveDecorators(Set<Type> types, Annotation... bindingTypes)
    {
        return manager.resolveDecorators(types, bindingTypes);
    }

    @Override
    public List<Interceptor<?>> resolveInterceptors(InterceptionType type, Annotation... interceptorBindings)
    {
        return manager.resolveInterceptors(type, interceptorBindings);
    }

    @Override
    public <T> AnnotatedType<T> createAnnotatedType(Class<T> type)
    {
        return this.manager.createAnnotatedType(type);
    }

    @Override
    public <T> CreationalContext<T> createCreationalContext(Contextual<T> contextual)
    {
        return this.manager.createCreationalContext(contextual);
    }

    @Override
    public <T> BeanAttributesImpl<T> createBeanAttributes(AnnotatedType<T> type)
    {
        return BeanAttributesBuilder.forContext(manager.getWebBeansContext()).newBeanAttibutes(type).build();
    }

    @Override
    public Set<Bean<?>> getBeans(Type beanType, Annotation... bindings)
    {
        return this.manager.getBeans();
    }

    @Override
    public Set<Bean<?>> getBeans(String name)
    {
        return this.manager.getBeans(name);
    }

    @Override
    public ELResolver getELResolver()
    {
        return this.manager.getELResolver();
    }

    @Override
    public Object getInjectableReference(InjectionPoint injectionPoint, CreationalContext<?> ctx)
    {
        return this.manager.getInjectableReference(injectionPoint, ctx);
    }

    @Override
    public Set<Annotation> getInterceptorBindingDefinition(Class<? extends Annotation> binding)
    {
        return this.manager.getInterceptorBindingDefinition(binding);
    }

    @Override
    public Bean<?> getPassivationCapableBean(String id)
    {
        return this.manager.getPassivationCapableBean(id);
    }

    @Override
    public Object getReference(Bean<?> bean, Type beanType, CreationalContext<?> ctx)
    {
        return this.manager.getReference(bean, beanType, ctx);
    }

    @Override
    public Set<Annotation> getStereotypeDefinition(Class<? extends Annotation> stereotype)
    {
        return this.manager.getStereotypeDefinition(stereotype);
    }

    @Override
    public boolean isQualifier(Class<? extends Annotation> annotationType)
    {
        return this.manager.isQualifier(annotationType);
    }

    @Override
    public boolean isInterceptorBinding(Class<? extends Annotation> annotationType)
    {
        return this.manager.isInterceptorBinding(annotationType);
    }

    @Override
    public boolean isScope(Class<? extends Annotation> annotationType)
    {
        return this.manager.isScope(annotationType);
    }

    @Override
    public boolean isNormalScope(Class<? extends Annotation> annotationType)
    {
        return this.manager.isNormalScope(annotationType);
    }
    
    @Override
    public boolean isPassivatingScope(Class<? extends Annotation> annotationType)
    {
        return this.manager.isPassivatingScope(annotationType);
    }        

    @Override
    public boolean isStereotype(Class<? extends Annotation> annotationType)
    {
        return this.manager.isStereotype(annotationType);
    }

    @Override
    public <X> Bean<? extends X> resolve(Set<Bean<? extends X>> beans)
    {
        return this.manager.resolve(beans);
    }

    @Override
    public <T> InterceptionFactory<T> createInterceptionFactory(CreationalContext<T> creationalContext, Class<T> clazz)
    {
        return manager.createInterceptionFactory(creationalContext, clazz);
    }

    @Override
    public void validate(InjectionPoint injectionPoint)
    {
        this.manager.validate(injectionPoint);
        
    }

    @Override
    public <T> Set<ObserverMethod<? super T>> resolveObserverMethods(T event, Annotation... bindings)
    {
        return this.manager.resolveObserverMethods(event, bindings);
    }

    @Override
    public ExpressionFactory wrapExpressionFactory(ExpressionFactory expressionFactory)
    {
        return null;
    }

    @Override
    public boolean areQualifiersEquivalent(Annotation qualifier1, Annotation qualifier2)
    {
        return manager.areQualifiersEquivalent(qualifier1, qualifier2);
    }

    @Override
    public int getQualifierHashCode(Annotation qualifier)
    {
        return manager.getQualifierHashCode(qualifier);
    }

    @Override
    public boolean areInterceptorBindingsEquivalent(Annotation interceptorBinding1, Annotation interceptorBinding2)
    {
        return manager.areInterceptorBindingsEquivalent(interceptorBinding1, interceptorBinding2);
    }

    @Override
    public int getInterceptorBindingHashCode(Annotation interceptorBinding)
    {
        return manager.getInterceptorBindingHashCode(interceptorBinding);
    }

    @Override
    public InjectionPoint createInjectionPoint(AnnotatedField<?> field)
    {
        return manager.createInjectionPoint(field);
    }

    @Override
    public InjectionPoint createInjectionPoint(AnnotatedParameter<?> parameter)
    {
        return manager.createInjectionPoint(parameter);
    }

    @Override
    public <T> InjectionTargetFactory<T> getInjectionTargetFactory(AnnotatedType<T> type)
    {
        return manager.getInjectionTargetFactory(type);
    }

    @Override
    public <X> ProducerFactory<X> getProducerFactory(AnnotatedField<? super X> field, Bean<X> declaringBean)
    {
        return manager.getProducerFactory(field, declaringBean);
    }

    @Override
    public <X> ProducerFactory<X> getProducerFactory(AnnotatedMethod<? super X> method, Bean<X> declaringBean)
    {
        return manager.getProducerFactory(method, declaringBean);
    }

    @Override
    public BeanAttributes<?> createBeanAttributes(AnnotatedMember<?> member)
    {
        return manager.createBeanAttributes(member);
    }

    @Override
    public <T> Bean<T> createBean(BeanAttributes<T> attributes, Class<T> beanClass, InjectionTargetFactory<T> injectionTargetFactory)
    {
        return manager.createBean(attributes, beanClass, injectionTargetFactory);
    }

    @Override
    public <T, X> Bean<T> createBean(BeanAttributes<T> attributes, Class<X> beanClass, ProducerFactory<X> producerFactory)
    {
        return manager.createBean(attributes, beanClass, producerFactory);
    }

    @Override
    public <T extends Extension> T getExtension(Class<T> extensionClass)
    {
        return manager.getExtension(extensionClass);
    }
}
