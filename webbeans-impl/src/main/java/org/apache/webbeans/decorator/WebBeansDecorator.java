/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.apache.webbeans.decorator;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import javax.webbeans.Decorates;
import javax.webbeans.manager.Decorator;

import org.apache.webbeans.component.AbstractComponent;
import org.apache.webbeans.component.ComponentImpl;
import org.apache.webbeans.container.ManagerImpl;
import org.apache.webbeans.exception.WebBeansException;
import org.apache.webbeans.inject.InjectableField;
import org.apache.webbeans.inject.InjectableMethods;
import org.apache.webbeans.logger.WebBeansLogger;
import org.apache.webbeans.proxy.JavassistProxyFactory;
import org.apache.webbeans.util.AnnotationUtil;
import org.apache.webbeans.util.ClassUtil;

public class WebBeansDecorator extends Decorator
{
    private static WebBeansLogger logger = WebBeansLogger.getLogger(WebBeansDecorator.class);

    /** Decorator class */
    private Class<?> clazz;

    /** Decorates api types */
    private Set<Class<?>> decoratedTypes = new HashSet<Class<?>>();

    /** Delegate field class type */
    protected Class<?> delegateType;

    /** Delegate field binding types */
    protected Set<Annotation> delegateBindingTypes = new HashSet<Annotation>();

    /** Delegated component */
    private AbstractComponent<Object> delegateComponent;

    public WebBeansDecorator(AbstractComponent<Object> delegateComponent)
    {
        super(ManagerImpl.getManager());
        this.delegateComponent = delegateComponent;
        this.clazz = delegateComponent.getReturnType();

        init();
    }

    protected void init()
    {
        ClassUtil.setInterfaceTypeHierarchy(this.decoratedTypes, this.clazz);

        if (this.decoratedTypes.contains(Serializable.class))
        {
            this.decoratedTypes.remove(Serializable.class);
        }

        initDelegate();
    }

    protected void initDelegate()
    {
        Field field = ClassUtil.getFieldWithAnnotation(this.clazz, Decorates.class);
        this.delegateType = field.getType();

        Annotation[] anns = field.getAnnotations();

        for (Annotation ann : anns)
        {
            if (AnnotationUtil.isBindingAnnotation(ann.annotationType()))
            {
                this.delegateBindingTypes.add(ann);
            }
        }

    }

    public boolean isDecoratorMatch(Set<Class<?>> apiType, Set<Annotation> annotation)
    {
        boolean foundApi = false;
        for (Class<?> clazz : apiType)
        {
            if (this.delegateType.equals(clazz))
            {
                foundApi = true;
                break;
            }
        }

        boolean allBindingsOk = false;
        if (foundApi)
        {
            for (Annotation annot : annotation)
            {
                boolean bindingOk = false;
                for (Annotation bindingType : delegateBindingTypes)
                {
                    if (AnnotationUtil.isAnnotationMemberExist(bindingType.annotationType(), annot, bindingType))
                    {
                        bindingOk = true;
                        break;
                    }
                }

                if (bindingOk)
                {
                    allBindingsOk = true;
                } else
                {
                    allBindingsOk = false;
                    break;
                }
            }
        }

        if (!allBindingsOk)
        {
            return false;
        }

        return true;
    }

    @Override
    public Set<Annotation> getDelegateBindingTypes()
    {
        return delegateBindingTypes;
    }

    @Override
    public Class<?> getDelegateType()
    {
        return delegateType;
    }

    @Override
    public void setDelegate(Object instance, Object delegate)
    {
        Field field = ClassUtil.getFieldWithAnnotation(getClazz(), Decorates.class);
        if (!field.isAccessible())
        {
            field.setAccessible(true);
        }

        try
        {
            field.set(instance, delegate);

        } catch (IllegalArgumentException e)
        {
            logger.error("Delegate field is not found on the given decorator class : " + instance.getClass().getName(), e);
            throw new WebBeansException(e);

        } catch (IllegalAccessException e)
        {
            logger.error("Illegal access exception for field " + field.getName() + " in decorator class : " + instance.getClass().getName(), e);
        }

    }

    @Override
    public Object create()
    {
        Object proxy = JavassistProxyFactory.createNewProxyInstance(this);

        return proxy;
    }

    public void setInjections(Object proxy)
    {
        // Set injected fields
        ComponentImpl<Object> delegate = (ComponentImpl<Object>) this.delegateComponent;

        Set<Field> injectedFields = delegate.getInjectedFields();
        for (Field injectedField : injectedFields)
        {
            boolean isDecorates = injectedField.isAnnotationPresent(Decorates.class);

            if (!isDecorates)
            {
                InjectableField ife = new InjectableField(injectedField, proxy, this.delegateComponent);
                ife.doInjection();
            }
        }

        Set<Method> injectedMethods = delegate.getInjectedMethods();
        for (Method injectedMethod : injectedMethods)
        {
            @SuppressWarnings("unchecked")
            InjectableMethods<?> ife = new InjectableMethods(injectedMethod, proxy, this.delegateComponent);
            ife.doInjection();
        }
    }

    @Override
    public void destroy(Object instance)
    {
        delegateComponent.destroy(instance);
    }

    @Override
    public Set<Annotation> getBindingTypes()
    {
        return delegateComponent.getBindingTypes();
    }

    @Override
    public Class<? extends Annotation> getDeploymentType()
    {
        return delegateComponent.getDeploymentType();
    }

    @Override
    public String getName()
    {
        return delegateComponent.getName();
    }

    @Override
    public Class<? extends Annotation> getScopeType()
    {
        return delegateComponent.getScopeType();
    }

    @Override
    public Set<Class<?>> getTypes()
    {
        return delegateComponent.getTypes();
    }

    @Override
    public boolean isNullable()
    {
        return delegateComponent.isNullable();
    }

    @Override
    public boolean isSerializable()
    {
        return delegateComponent.isSerializable();
    }

    /**
     * @return the delegateComponent
     */
    public AbstractComponent<Object> getDelegateComponent()
    {
        return delegateComponent;
    }

    /**
     * @return the clazz
     */
    public Class<?> getClazz()
    {
        return clazz;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((clazz == null) ? 0 : clazz.hashCode());
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final WebBeansDecorator other = (WebBeansDecorator) obj;
        if (clazz == null)
        {
            if (other.clazz != null)
                return false;
        } else if (!clazz.equals(other.clazz))
            return false;
        return true;
    }

}
