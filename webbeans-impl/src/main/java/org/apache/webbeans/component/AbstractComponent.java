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
import java.lang.reflect.Member;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.CreationException;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;

import org.apache.webbeans.config.inheritance.BeanInheritedMetaData;
import org.apache.webbeans.config.inheritance.IBeanInheritedMetaData;
import org.apache.webbeans.container.ManagerImpl;
import org.apache.webbeans.context.creational.CreationalContextImpl;
import org.apache.webbeans.deployment.DeploymentTypeManager;
import org.apache.webbeans.intercept.InterceptorData;
import org.apache.webbeans.logger.WebBeansLogger;
import org.apache.webbeans.util.ClassUtil;

/**
 * Abstract implementation of the {@link Component} contract. 
 * 
 * @version $Rev$ $Date$
 * 
 * @see Component
 * @see Bean
 */
public abstract class AbstractComponent<T> extends Component<T>
{
    /**Logger instance*/
    private final WebBeansLogger logger = WebBeansLogger.getLogger(getClass());
    
    /** Name of the component */
    protected String name;

    /** Deployment type of the component */
    protected Annotation type;

    /** Scope type of the component */
    protected Annotation implScopeType;

    /** Binding types of the component */
    protected Set<Annotation> implBindingTypes = new HashSet<Annotation>();

    /** Api types of the component */
    protected Set<Type> apiTypes = new HashSet<Type>();

    /** Web Beans type */
    protected WebBeansType webBeansType;

    /** Return type of the component */
    protected Class<T> returnType;

    /** Stereotypes of the component */
    protected Set<Annotation> stereoTypes = new HashSet<Annotation>();
    
    /**This bean is specialized or not*/
    protected boolean specializedBean;

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
    
    /**Beans injection points*/
    protected Set<InjectionPoint> injectionPoints = new HashSet<InjectionPoint>();
    
    /**Bean inherited meta data*/
    protected IBeanInheritedMetaData inheritedMetaData;
    
    /**Tracks dependent injection point owner, can be null*/
    protected InjectionPoint dependentOwnerInjectionPoint;
    
    /**Creational context*/
    protected CreationalContext<T> creationalContext = null;

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
    
    /**
     * Creates a new instance.
     * 
     * @param webBeanType beans type
     */
    protected AbstractComponent(WebBeansType webBeanType)
    {
        super(ManagerImpl.getManager());
        this.webBeansType = webBeanType;
        
    }
    
    /**
     * {@inheritDoc}
     */
    public IBeanInheritedMetaData getInheritedMetaData()
    {
        return this.inheritedMetaData;
    }
    
    protected void setInheritedMetaData()
    {
        this.inheritedMetaData = new BeanInheritedMetaData<T>(this);
    }

    /*
     * (non-Javadoc)
     * @see javax.webbeans.component.Component#create()
     */
    public T create(CreationalContext<T> creationalContext)
    {
        T instance = null;
        try
        {
            this.creationalContext = creationalContext;
            instance = createInstance(this.creationalContext);

        }
        catch (Exception re)
        {
            Throwable throwable = ClassUtil.getRootException(re);
            
            if(throwable instanceof RuntimeException)
            {
                RuntimeException rt = (RuntimeException)throwable;
                
                throw rt;
            }
            else
            {
                throw new CreationException(throwable);
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
    public void destroy(T instance, CreationalContext<T> creationalContext)
    {
        try
        {
            //Destory dependent instances
            this.creationalContext.release();
            
            //Destroy instance, call @PreDestroy
            destroyInstance(instance);
                        
            //Clear Decorator and Interceptor Stack
            this.decoratorStack.clear();
            this.interceptorStack.clear();
            
            //Reset it
            this.dependentOwnerInjectionPoint = null;  
            
        }catch(Exception e)
        {
            logger.fatal("Exception is thrown while destroying bean instance : " + toString());
            e.printStackTrace();
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
     * Get return types of the bean.
     */
    public Class<?> getBeanClass()
    {
        if(IComponentHasParent.class.isAssignableFrom(getClass()))
        {
            IComponentHasParent comp = (IComponentHasParent)this;
            
            return comp.getParent().getBeanClass();
        }
        
        return getReturnType();
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
    public Set<Annotation> getOwbStereotypes()
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

    
    public Set<Type> getTypes()
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
    public Object getDependent(Bean<?> dependentComponent, InjectionPoint injectionPoint)
    {
        Object object = null;
        
        //Setting injection point owner
        AbstractComponent<?> dependent = (AbstractComponent<?>)dependentComponent;
        dependent.setDependentOwnerInjectionPoint(injectionPoint);        
        
        //Get dependent instance
        object = ManagerImpl.getManager().getInstance(dependentComponent);
        
        CreationalContextImpl<T> cc = (CreationalContextImpl<T>)this.creationalContext;

        //Put this into the dependent map
        cc.addDependent(dependentComponent, object);

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
    
    public void addInjectionPoint(InjectionPoint injectionPoint)
    {
        this.injectionPoints.add(injectionPoint);
    }
    
    public Set<InjectionPoint> getInjectionPoints()
    {
        return this.injectionPoints;
    }
    
    /**
     * @return the dependentOwnerInjectionPoint
     */
    public InjectionPoint getDependentOwnerInjectionPoint()
    {
        return dependentOwnerInjectionPoint;
    }

    /**
     * @param dependentOwnerInjectionPoint the dependentOwnerInjectionPoint to set
     */
    public void setDependentOwnerInjectionPoint(InjectionPoint dependentOwnerInjectionPoint)
    {
        this.dependentOwnerInjectionPoint = dependentOwnerInjectionPoint;
    }
    
    
    public void setSpecializedBean(boolean specialized)
    {
        this.specializedBean = specialized;
    }
    
    public boolean isSpecializedBean()
    {
        return this.specializedBean;
    }
    
    public List<InjectionPoint> getInjectionPoint(Member member)
    {
        List<InjectionPoint> points = new ArrayList<InjectionPoint>();
        
        for(InjectionPoint ip : injectionPoints)
        {
            if(ip.getMember().equals(member))
            {
                points.add(ip);
            }
        }
        
        return points;
    }
    
    public Set<Class<? extends Annotation>> getStereotypes()
    {
        Set<Class<? extends Annotation>> set = new HashSet<Class<? extends Annotation>>();
        
        for(Annotation ann : this.stereoTypes)
        {
            set.add(ann.annotationType());
        }
        
        return set;
    }
    
    //TODO Replaces @Deploymeny Types, no starting work for now!
    public boolean isPolicy()
    {
        return false;
    }
    
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append("{\n");        
        builder.append("\tName : "+ getName() +", WebBeans Type: "+ getWebBeansType() + "\n");
        builder.append("\tAPI Types:\n");
        builder.append("\t[\n");
        
        for(Type clazz : getTypes())
        {
            if(clazz instanceof Class)
            {
                builder.append("\t\t\t"+((Class<?>)clazz).getName()+ "\n");    
            }
            else
            {
                Class<?> rawType = (Class<?>)((ParameterizedType)clazz).getRawType();
                builder.append("\t\t\t"+rawType.getName()+ "\n");
            }
                        
        }
        
        builder.append("\t]\n");
        builder.append("\t,\n");
        builder.append("\tBinding Types:\n");
        builder.append("\t[\n");
        
        for(Annotation ann : getBindings())
        {
            builder.append("\t\t\t"+ann.annotationType().getName()+"\n");
        }
        
        builder.append("\t]\n");
        builder.append("}\n");
        
        return builder.toString();
    }
}
