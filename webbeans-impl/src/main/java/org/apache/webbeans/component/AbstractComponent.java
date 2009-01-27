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
package org.apache.webbeans.component;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import javax.context.CreationalContext;
import javax.context.Dependent;
import javax.inject.CreationException;
import javax.inject.manager.Bean;
import javax.inject.manager.InjectionPoint;

import org.apache.webbeans.container.ManagerImpl;
import org.apache.webbeans.context.DependentContext;
import org.apache.webbeans.context.creational.CreationalContextImpl;
import org.apache.webbeans.deployment.DeploymentTypeManager;
import org.apache.webbeans.exception.WebBeansException;
import org.apache.webbeans.intercept.InterceptorData;

/**
 * Abstract implementation of the {@link Component} contract. There are several
 * different implementation of this abtract class, including
 * <ul>
 * <li>Bean Implementation Class Component,</li>
 * <li>Producer Method Component</li>
 * </ul>
 * <p>
 * Each subclass is responsible for overriding
 * {@link AbstractComponent#createInstance()} and
 * {@link AbstractComponent#destroyInstance(Object)} methods.
 * </p>
 * 
 * @since 1.0
 */
public abstract class AbstractComponent<T> extends Component<T>
{
    /** Name of the component */
    protected String name;

    /** Deployment type of the component */
    protected Annotation type;

    /** Scope type of the component */
    protected Annotation implScopeType;

    /** Binding types of the component */
    protected Set<Annotation> implBindingTypes = new HashSet<Annotation>();

    /** Api types of the component */
    protected Set<Class<?>> apiTypes = new HashSet<Class<?>>();

    /** Web Beans type */
    protected WebBeansType webBeansType;

    /** Return type of the component */
    protected Class<T> returnType;

    /** Dependent object map of the component */
    protected Map<Object, Bean<?>> dependentObjects = new WeakHashMap<Object, Bean<?>>();

    /** Stereotypes of the component */
    protected Set<Annotation> stereoTypes = new HashSet<Annotation>();

    /**
     * Holds the all of the interceptor related data, contains around-invoke,
     * post-construct and pre-destroy
     */
    protected List<InterceptorData> interceptorStack = new ArrayList<InterceptorData>();

    /** Holds decorator stack */
    protected List<Object> decoratorStack = new ArrayList<Object>();

    /** The bean is serializable or not */
    protected boolean serializable;

    /** The bean allows nullable object */
    protected boolean nullable = true;
    
    protected Set<InjectionPoint> injectionPoints = new HashSet<InjectionPoint>();

    /**
     * Constructor definiton. Each subclass redefines its own constructor with
     * calling this.
     * 
     * @param name name of the component
     * @param webBeansType web beans type
     */
    protected AbstractComponent(WebBeansType webBeansType, Class<T> returnType)
    {
        super(ManagerImpl.getManager());
        this.webBeansType = webBeansType;
        this.returnType = returnType;
    }

    /*
     * (non-Javadoc)
     * @see javax.webbeans.component.Component#create()
     */
    public T create(CreationalContext<T> creationalContext)
    {
        DependentContext context = (DependentContext) getManager().getContext(Dependent.class);
        boolean isActiveSet = false;
        T instance = null;
        try
        {
            if (!context.isActive())
            {
                context.setActive(true);
                isActiveSet = true;
            }

            instance = createInstance(creationalContext);

        }
        catch (RuntimeException re)
        {
            throw new CreationException(re);
        }
        catch (Exception e)
        {
            throw new WebBeansException(e);
        }
        finally
        {
            if (isActiveSet)
            {
                context.setActive(false);
            }
        }

        return instance;
    }

    /**
     * Creates the instance of the component that has a specific implementation
     * type. Each subclass must define its own create mechanism.
     * 
     * @return instance of the component
     */
    protected abstract T createInstance(CreationalContext<T> creationalContext);

    /*
     * (non-Javadoc)
     * @see javax.webbeans.component.Component#destroy(java.lang.Object)
     */
    public void destroy(T instance)
    {
        DependentContext context = (DependentContext) getManager().getContext(Dependent.class);
        boolean isActiveSet = false;

        try
        {
            if (!context.isActive())
            {
                context.setActive(true);
                isActiveSet = true;
            }

            destroyInstance(instance);
            destroyDependents();

        }
        finally
        {
            if (isActiveSet)
            {
                context.setActive(false);
            }
        }

    }

    /**
     * Destroy the instance of the component. Each subclass must define its own
     * destroy mechanism.
     * 
     * @param instance instance that is being destroyed
     */
    protected abstract void destroyInstance(T instance);

    /**
     * Get component type.
     * 
     * @return component type
     */
    public Annotation getType()
    {
        return type;
    }

    /**
     * Set component type.
     * 
     * @param type component type
     */
    public void setType(Annotation type)
    {
        this.type = type;
    }

    /**
     * Get scope type.
     * 
     * @return scope type
     */
    public Annotation getImplScopeType()
    {
        return implScopeType;
    }

    /**
     * Set scope type.
     * 
     * @param scopeType scope type
     */
    public void setImplScopeType(Annotation scopeType)
    {
        this.implScopeType = scopeType;
    }

    /**
     * Name of the component.
     * 
     * @return name of the component
     */
    public String getName()
    {
        return name;
    }

    /**
     * Get web bean type of the component.
     * 
     * @return web beans type
     */
    public WebBeansType getWebBeansType()
    {
        return webBeansType;
    }

    /**
     * Add new binding type.
     * 
     * @param bindingType new binding type
     */
    public void addBindingType(Annotation bindingType)
    {
        this.implBindingTypes.add(bindingType);
    }

    /**
     * Add new stereotype.
     * 
     * @param stereoType new stereotype annotation
     */
    public void addStereoType(Annotation stereoType)
    {
        this.stereoTypes.add(stereoType);
    }

    /**
     * Add new api type.
     * 
     * @param apiType new api type
     */
    public void addApiType(Class<?> apiType)
    {
        this.apiTypes.add(apiType);
    }

    /**
     * Get binding types.
     * 
     * @return binding types
     */
    public Set<Annotation> getImplBindingTypes()
    {
        return implBindingTypes;
    }

    /**
     * Gets the stereotypes.
     * 
     * @return stereotypes of the component
     */
    public Set<Annotation> getStereoTypes()
    {
        return this.stereoTypes;
    }

    /**
     * Set name.
     * 
     * @param name new name
     */
    public void setName(String name)
    {
        if (this.name == null)
            this.name = name;
        else
            throw new UnsupportedOperationException("Component name is not null, is " + this.name);
    }

    /**
     * Gets predecence of the component.
     * 
     * @return precedence
     */
    public int getPrecedence()
    {
        return DeploymentTypeManager.getInstance().getPrecedence(getDeploymentType());
    }

    /*
     * (non-Javadoc)
     * @see javax.webbeans.manager.Bean#getBindingTypes()
     */
    @Override
    public Set<Annotation> getBindings()
    {
        return this.implBindingTypes;
    }

    /*
     * (non-Javadoc)
     * @see javax.webbeans.manager.Bean#getDeploymentType()
     */
    @Override
    public Class<? extends Annotation> getDeploymentType()
    {
        return this.type.annotationType();
    }

    /*
     * (non-Javadoc)
     * @see javax.webbeans.manager.Bean#getScopeType()
     */
    @Override
    public Class<? extends Annotation> getScopeType()
    {
        return this.implScopeType.annotationType();
    }

    /*
     * (non-Javadoc)
     * @see javax.webbeans.manager.Bean#getTypes()
     */
    @Override
    public Set<Class<?>> getTypes()
    {
        return this.apiTypes;
    }

    /**
     * Gets type of the producer method.
     * 
     * @return type of the producer method
     */
    public Class<T> getReturnType()
    {
        return returnType;
    }

    /**
     * Gets the dependent component instance.
     * 
     * @param dependentComponent dependent web beans component
     * @return the dependent component instance
     */
    @SuppressWarnings("unchecked")
    public Object getDependent(Component<?> dependentComponent)
    {
        Object object = null;
        DependentContext context = (DependentContext) getManager().getContext(Dependent.class);

        object = context.get(dependentComponent,new CreationalContextImpl());

        this.dependentObjects.put(object, dependentComponent);

        return object;
    }

    /**
     * Gets the interceptor stack.
     * 
     * @return the interceptor stack
     */
    public List<InterceptorData> getInterceptorStack()
    {
        return this.interceptorStack;
    }

    public List<Object> getDecoratorStack()
    {
        return this.decoratorStack;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.webbeans.component.Component#setNullable()
     */
    @Override
    public void setNullable(boolean nullable)
    {
        this.nullable = nullable;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.webbeans.component.Component#setSerializable()
     */
    @Override
    public void setSerializable(boolean serializable)
    {
        this.serializable = serializable;

    }

    /*
     * (non-Javadoc)
     * @see javax.webbeans.manager.Bean#isNullable()
     */
    @Override
    public boolean isNullable()
    {

        return this.nullable;
    }

    /*
     * (non-Javadoc)
     * @see javax.webbeans.manager.Bean#isSerializable()
     */
    @Override
    public boolean isSerializable()
    {
        return this.serializable;
    }

    @SuppressWarnings("unchecked")
    protected <K> void destroyDependents()
    {
        Set<Object> keySet = this.dependentObjects.keySet();
        Iterator<Object> it = keySet.iterator();

        K instance = null;

        while (it.hasNext())
        {
            instance = (K) it.next();
            Bean<K> bean = (Bean<K>) this.dependentObjects.get(instance);
            bean.destroy(instance);
        }
    }
    
    public void addInjectionPoint(InjectionPoint injectionPoint)
    {
        this.injectionPoints.add(injectionPoint);
    }
    
    public Set<InjectionPoint> getInjectionPoints()
    {
        return this.injectionPoints;
    }

}
