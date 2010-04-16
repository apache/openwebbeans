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

import javax.enterprise.context.Dependent;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.CreationException;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;

import org.apache.webbeans.config.OWBLogConst;
import org.apache.webbeans.container.BeanManagerImpl;
import org.apache.webbeans.context.creational.CreationalContextFactory;
import org.apache.webbeans.context.creational.CreationalContextImpl;
import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.inject.AlternativesManager;
import org.apache.webbeans.logger.WebBeansLogger;
import org.apache.webbeans.util.ClassUtil;
import org.apache.webbeans.util.WebBeansUtil;

/**
 * Abstract implementation of the {@link OwbBean} contract. 
 * 
 * @version $Rev$ $Date$
 * 
 * @see OwbBean
 * @see Bean
 */
public abstract class AbstractOwbBean<T> implements OwbBean<T>
{
    /**Logger instance*/
    private final WebBeansLogger logger = WebBeansLogger.getLogger(getClass());
    
    /** Name of the bean */
    protected String name;

    /** Scope type of the bean */
    protected Annotation implScopeType;

    /** Qualifiers of the bean */
    protected Set<Annotation> implQualifiers = new HashSet<Annotation>();

    /** Api types of the bean */
    protected Set<Type> apiTypes = new HashSet<Type>();

    /** Web Beans type */
    protected WebBeansType webBeansType;

    /** Return type of the bean */
    protected Class<T> returnType;

    /** Stereotypes of the bean */
    protected Set<Annotation> stereoTypes = new HashSet<Annotation>();
    
    /**This bean is specialized or not*/
    protected boolean specializedBean;

    /**This bean is enabled or disabled*/
    protected boolean enabled = true;
    
    /** The bean is serializable or not */
    protected boolean serializable;

    /** The bean allows nullable object */
    protected boolean nullable = true;
    
    /**Beans injection points*/
    protected Set<InjectionPoint> injectionPoints = new HashSet<InjectionPoint>();
    
    /**
     * This string will be used for passivating the Bean.
     * It will be created on the first use.
     * @see #getId()
     */
    protected String passivatingId = null;
    
    /**Bean Manager*/
    private final BeanManagerImpl manager;

    
            
    /**
     * Constructor definiton. Each subclass redefines its own constructor with
     * calling this.
     * 
     * @param returnType of the bean
     * @param webBeansType web beans type
     */
    protected AbstractOwbBean(WebBeansType webBeansType, Class<T> returnType)
    {
        this.webBeansType = webBeansType;
        this.returnType = returnType;
        this.manager = BeanManagerImpl.getManager();
    }
    
    /**
     * Creates a new instance.
     * 
     * @param webBeanType beans type
     */
    protected AbstractOwbBean(WebBeansType webBeanType)
    {
        this(webBeanType, null);
    }
    
    /**
     * Gets manager instance
     * 
     * @return manager instance
     */
    protected BeanManagerImpl getManager()
    {
        return manager;
    }
    
    
    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public T create(CreationalContext<T> creationalContext)
    {
        T instance;
        try
        {  
            if(!(creationalContext instanceof CreationalContextImpl))
            {                
                creationalContext = CreationalContextFactory.getInstance().wrappedCreationalContext(creationalContext, this); 
            }
           
            InjectionTargetWrapper<T> wrapper = getManager().getInjectionTargetWrapper(this);
            //If wrapper not null
            if(wrapper != null)
            {
                instance = wrapper.produce(creationalContext);
                wrapper.inject(instance, creationalContext);
                wrapper.postConstruct(instance);
            }
            else
            {
                instance = createInstance(creationalContext); 
                if(this instanceof AbstractInjectionTargetBean)
                {
                    ((AbstractInjectionTargetBean<T>)this).afterConstructor(instance, creationalContext);
                }
            }                                    
        }
        catch (Exception re)
        {
            Throwable throwable = ClassUtil.getRootException(re);
            
            if(!(throwable instanceof RuntimeException))
            {
                throw new CreationException(throwable);
            }
            throw (RuntimeException) throwable;
        }

        return instance;
    }

    /**
     * Creates the instance of the bean that has a specific implementation
     * type. Each subclass must define its own create mechanism.
     *
     * @param creationalContext the contextual instance shall be created in
     * @return instance of the bean
     */
    protected abstract T createInstance(CreationalContext<T> creationalContext);
    
    /**
     * {@inheritDoc}
     */
    @Override
    public T createNewInstance(CreationalContext<T> creationalContext)
    {
        return createInstance(creationalContext);
    }

    /*
     * (non-Javadoc)
     * @param creationalContext the contextual instance has been created in
     * @see javax.webbeans.bean.Component#destroy(java.lang.Object)
     */
    public void destroy(T instance, CreationalContext<T> creationalContext)
    {
        try
        {  
            InjectionTargetWrapper<T> wrapper = getManager().getInjectionTargetWrapper(this);
            if(wrapper != null)
            {
                wrapper.preDestroy(instance);
                wrapper.dispose(instance);
            }
            else
            {
                //Destroy instance, call @PreDestroy
                destroyInstance(instance,creationalContext);
            }
            
            //Setting destroying instance
            CreationalContextImpl.currentRemoveObject.set(instance);
            
            //Destory dependent instances
            creationalContext.release();                
                                                
        }catch(Exception e)
        {
            logger.fatal(OWBLogConst.FATAL_0001, new Object[]{toString()});
            e.printStackTrace();
        }finally
        {
            CreationalContextImpl.currentRemoveObject.remove();
        }
    }

    /**
     * Destroy the instance of the bean. Each subclass must define its own
     * destroy mechanism.
     * 
     * @param instance instance of the bean that is being destroyed
     * @param creationalContext the contextual instance has been created in
     */
    protected void destroyInstance(T instance, CreationalContext<T> creationalContext)
    {
        
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void destroyCreatedInstance(T instance, CreationalContext<T> creationalContext)
    {
        destroyInstance(instance, creationalContext);
    }
    
    /**
     * TODO there are probably other infos which must get added to make the id unique!
     * If not, it will crash in {@link BeanManagerImpl#addPassivationInfo(javax.enterprise.inject.spi.Bean)}
     * anyway. 
     *
     * {@inheritDoc}
     */
    public String getId()
    {
        if (!isEnabled() || returnType.equals(Object.class)) {
            // if the Bean is disabled, either by rule, or by
            // annotating it @Typed() as Object, then it is not serializable
            return null;
        }
        if (passivatingId == null)
        {
            StringBuilder sb = new StringBuilder(webBeansType.toString()).append('#');
            sb.append(returnType).append('#');
            for (Annotation qualifier : implQualifiers)
            {
                sb.append(qualifier.toString()).append(',');
            }
            
            passivatingId = sb.toString();
        }

        return passivatingId;
    }
    
    public boolean isPassivationCapable()
    {
        return false;
    }

    /**
     * Get return types of the bean.
     */
    public Class<?> getBeanClass()
    {
        if(IBeanHasParent.class.isAssignableFrom(getClass()))
        {
            @SuppressWarnings("unchecked")
            IBeanHasParent<T> comp = (IBeanHasParent<T>)this;
            
            return comp.getParent().getBeanClass();
        }
        
        return getReturnType();
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
     * Name of the bean.
     * 
     * @return name of the bean
     */
    public String getName()
    {
        return name;
    }

    /**
     * Get web bean type of the bean.
     * 
     * @return web beans type
     */
    public WebBeansType getWebBeansType()
    {
        return webBeansType;
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
     * Get qualifiers.
     *
     * @return qualifiers
     */
    public Set<Annotation> getImplQualifiers()
    {
        return implQualifiers;
    }

    /**
     * Gets the stereotypes.
     *
     * @return stereotypes of the bean
     */
    public Set<Annotation> getOwbStereotypes()
    {
        return this.stereoTypes;
    }

    /**
     * Add new qualifier.
     *
     * @param qualifier new qualifier
     */
    public void addQualifier(Annotation qualifier)
    {
        this.implQualifiers.add(qualifier);
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

    /*
     * (non-Javadoc)
     * @see javax.webbeans.manager.Bean#getQualifiers()
     */
    @Override
    public Set<Annotation> getQualifiers()
    {
        return this.implQualifiers;
    }

    /*
     * (non-Javadoc)
     * @see javax.webbeans.manager.Bean#getScope()
     */
    @Override
    public Class<? extends Annotation> getScope()
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
     * {@inheritDoc}
     */
    @Override
    public void setNullable(boolean nullable)
    {
        this.nullable = nullable;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setSerializable(boolean serializable)
    {
        this.serializable = serializable;

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isNullable()
    {

        return this.nullable;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSerializable()
    {
        return this.serializable;
    }

    /**
     * {@inheritDoc}
     */    
    public void addInjectionPoint(InjectionPoint injectionPoint)
    {
        this.injectionPoints.add(injectionPoint);
    }
    
    /**
     * {@inheritDoc}
     */    
    public Set<InjectionPoint> getInjectionPoints()
    {
        return this.injectionPoints;
    }
    
    /**
     * {@inheritDoc}
     */    
    public void setSpecializedBean(boolean specialized)
    {
        this.specializedBean = specialized;
    }
    
    /**
     * {@inheritDoc}
     */
    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }
    
    /**
     * {@inheritDoc}
     */    
    public boolean isSpecializedBean()
    {
        return this.specializedBean;
    }
    
    /**
     * {@inheritDoc}
     */    
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
    
    /**
     * {@inheritDoc}
     */    
    public Set<Class<? extends Annotation>> getStereotypes()
    {
        Set<Class<? extends Annotation>> set = new HashSet<Class<? extends Annotation>>();
        
        for(Annotation ann : this.stereoTypes)
        {
            set.add(ann.annotationType());
        }
        
        return set;
    }
    
     /**
     * {@inheritDoc}
     */    
    public boolean isAlternative()
    {
        return AlternativesManager.getInstance().isBeanHasAlternative(this);
    }
    
    /**
     * {@inheritDoc}
     */
    public boolean isEnabled()
    {
        return this.enabled;
    }
    
        
    /**
     * {@inheritDoc}
     */    
    public String toString()
    {
        StringBuilder builder = new StringBuilder();        
        builder.append("Name:").append(getName()).append(",WebBeans Type:").append(getWebBeansType());
        builder.append(",API Types:[");
        
        int size = getTypes().size();
        int index = 1;
        for(Type clazz : getTypes())
        {
            if(clazz instanceof Class)
            {
                builder.append(((Class<?>)clazz).getName());    
            }
            else
            {
                Class<?> rawType = (Class<?>)((ParameterizedType)clazz).getRawType();
                builder.append(rawType.getName());
            }
            
            if(index < size)
            {
                builder.append(",");
            }
            
            index++;                        
        }
        
        builder.append("],");
        builder.append("Qualifiers:[");
        
        size = getQualifiers().size();
        index = 1;
        for(Annotation ann : getQualifiers())
        {
            builder.append(ann.annotationType().getName());
            
            if(index < size)
            {
                builder.append(",");
            }
            
            index++;
        }
        
        builder.append("]");
        
        return builder.toString();
    }
    
    protected WebBeansLogger getLogger()
    {
        return this.logger;
    }

    @Override
    public boolean isDependent() 
    {
        return getScope().equals(Dependent.class);
    }
    
    @Override
    public void validatePassivationDependencies()
    {
        if(isPassivationCapable() || (this instanceof AbstractProducerBean))
        {
            Set<InjectionPoint> injectionPoints = getInjectionPoints();
            for(InjectionPoint injectionPoint : injectionPoints)
            {
                if(!injectionPoint.isTransient())
                {
                    if(!WebBeansUtil.isPassivationCapableDependency(injectionPoint))
                    {
                        if(injectionPoint.getAnnotated().isAnnotationPresent(Disposes.class))
                        {
                            continue;
                        }
                        throw new WebBeansConfigurationException("Passivation capable beans must satisfy passivation capable dependencies. " +
                                "Bean : " + toString() + " does not satisfy.");
                    }
                }
            }            
        }
    }
    
}
