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
package org.apache.webbeans.component;

import java.lang.annotation.Annotation;
import java.lang.reflect.Member;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.context.Dependent;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.CreationException;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.enterprise.inject.spi.Producer;

import org.apache.webbeans.config.OWBLogConst;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.container.BeanManagerImpl;
import org.apache.webbeans.context.creational.CreationalContextImpl;
import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.logger.WebBeansLoggerFacade;

/**
 * Abstract implementation of the {@link OwbBean} contract. 
 * 
 * @version $Rev$ $Date$
 * 
 * @see javax.enterprise.inject.spi.Bean
 * 
 */
public abstract class AbstractOwbBean<T> extends AbstractBean<T> implements OwbBean<T>
{
    /**Logger instance*/
    protected Logger logger = null;
    
    /** Web Beans type */
    protected WebBeansType webBeansType;

    /**This bean is specialized or not*/
    protected boolean specializedBean;

    /**This bean is enabled or disabled*/
    protected boolean enabled = true;

    /** The bean allows nullable object */
    protected boolean nullable = true;

    /**Beans injection points*/
    protected Set<InjectionPoint> injectionPoints = new HashSet<InjectionPoint>();

    /** The producer */
    private Producer<T> producer;

    /**
     * This string will be used for passivating the Bean.
     * It will be created on the first use.
     * @see #getId()
     */
    protected String passivatingId = null;
    
    protected final WebBeansContext webBeansContext;

    protected AbstractOwbBean(WebBeansContext webBeansContext,
                              WebBeansType webBeansType,
                              Set<Type> types,
                              Set<Annotation> qualifiers,
                              Class<? extends Annotation> scope,
                              Class<?> beanClass,
                              Set<Class<? extends Annotation>> stereotypes)
    {
        this(webBeansContext, webBeansType, types, qualifiers, scope, null, false, beanClass, stereotypes, false);
    }
    
    /**
     * Constructor definiton. Each subclass redefines its own constructor with
     * calling this.
     * 
     * @param webBeansContext
     * @param webBeansType web beans type
     */
    protected AbstractOwbBean(WebBeansContext webBeansContext,
                              WebBeansType webBeansType,
                              Set<Type> types,
                              Set<Annotation> qualifiers,
                              Class<? extends Annotation> scope,
                              String name,
                              boolean nullable,
                              Class<?> beanClass,
                              Set<Class<? extends Annotation>> stereotypes,
                              boolean alternative)
    {
        super(types, qualifiers, scope, name, nullable, beanClass, stereotypes, alternative);
        this.webBeansType = webBeansType;
        this.webBeansContext = webBeansContext;
    }

    /**
     * Get the web beans context this bean is associated with
     *
     * @return WebBeansContext this bean is associated with
     */
    public WebBeansContext getWebBeansContext()
    {
        return webBeansContext;
    }
    
    /**
     * Gets manager instance
     * 
     * @return manager instance
     */
    protected BeanManagerImpl getManager()
    {
        return webBeansContext.getBeanManagerImpl();
    }
    
    public Producer<T> getProducer()
    {
        return producer;
    }
    
    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public T create(CreationalContext<T> creationalContext)
    {
        try
        {
            if(!(creationalContext instanceof CreationalContextImpl))
            {
                creationalContext = webBeansContext.getCreationalContextFactory().wrappedCreationalContext(creationalContext, this);
            }

            T instance = producer.produce(creationalContext);
            if (producer instanceof InjectionTarget)
            {
                InjectionTarget<T> injectionTarget = (InjectionTarget<T>)producer;
                injectionTarget.inject(instance, creationalContext);
                injectionTarget.postConstruct(instance);
            }
            return instance;
        }
        catch (Exception re)
        {
            Throwable throwable = getRootException(re);
            
            if(!(throwable instanceof RuntimeException))
            {
                throw new CreationException(throwable);
            }
            throw (RuntimeException) throwable;
        }

    }

    private Throwable getRootException(Throwable throwable)
    {
        if(throwable.getCause() == null || throwable.getCause() == throwable)
        {
            return throwable;
        }
        else
        {
            return getRootException(throwable.getCause());
        }
    }

    /*
     * (non-Javadoc)
     * @param creationalContext the contextual instance has been created in
     */
    public void destroy(T instance, CreationalContext<T> creationalContext)
    {
        try
        {
            if (producer instanceof InjectionTarget)
            {
                InjectionTarget<T> injectionTarget = (InjectionTarget<T>)producer;
                injectionTarget.preDestroy(instance);
            }
            producer.dispose(instance);
            //Destroy dependent instances
            creationalContext.release();
        }
        catch(Exception e)
        {
            getLogger().log(Level.SEVERE, WebBeansLoggerFacade.constructMessage(OWBLogConst.FATAL_0001, this), e);
        }
    }

    /**
     * get the unique Id of the bean. This will get used as reference on
     * passivation.
     *
     * {@inheritDoc}
     */
    public String getId()
    {
        if (!isEnabled() || getReturnType().equals(Object.class))
        {
            // if the Bean is disabled, either by rule, or by
            // annotating it @Typed() as Object, then it is not serializable
            return null;
        }
        if (passivatingId == null)
        {
            StringBuilder sb = new StringBuilder(webBeansType.toString()).append('#');
            sb.append(getReturnType()).append('#');
            for (Annotation qualifier : getQualifiers())
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

    public void setProducer(Producer<T> producer)
    {
        this.producer = producer;
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
     * Gets type of the producer method/field or the bean class if it's not a producer.
     * This basically determines the class which will get created.
     * 
     * @return type of the producer method
     * @see #getBeanClass()
     */
    public Class<T> getReturnType()
    {
        return (Class<T>) getBeanClass();
    }
    
    /**
     * {@inheritDoc}
     */
    public void setNullable(boolean nullable)
    {
        this.nullable = nullable;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isNullable()
    {
        return nullable;
    }

    /**
     * {@inheritDoc}
     */    
    public void addInjectionPoint(InjectionPoint injectionPoint)
    {
        injectionPoints.add(injectionPoint);
    }
    
    /**
     * {@inheritDoc}
     */    
    public Set<InjectionPoint> getInjectionPoints()
    {
        return injectionPoints;
    }
    
    /**
     * {@inheritDoc}
     */    
    public void setSpecializedBean(boolean specialized)
    {
        specializedBean = specialized;
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
        return specializedBean;
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
    public boolean isAlternative()
    {
        return webBeansContext.getAlternativesManager().isBeanHasAlternative(this);
    }
    
    /**
     * {@inheritDoc}
     */
    public boolean isEnabled()
    {
        return enabled;
    }
    
        
    /**
     * {@inheritDoc}
     */    
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        final String simpleName = getReturnType().getSimpleName();
        builder.append(simpleName).append(", ");
        builder.append("Name:").append(getName()).append(", WebBeans Type:").append(getWebBeansType());
        builder.append(", API Types:[");
        
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
        
        builder.append("], ");
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

    /**
     * The Logger should really only be used to log errors!
     */
    protected synchronized Logger getLogger()
    {
        if (logger == null)
        {
            logger = WebBeansLoggerFacade.getLogger(getClass());
        }
        return logger;
    }

    public boolean isDependent()
    {
        return getScope().equals(Dependent.class);
    }
    
    public void validatePassivationDependencies()
    {
        if(isPassivationCapable())
        {
            Set<InjectionPoint> beanInjectionPoints = getInjectionPoints();
            for(InjectionPoint injectionPoint : beanInjectionPoints)
            {
                if(!injectionPoint.isTransient())
                {
                    if(!getWebBeansContext().getWebBeansUtil().isPassivationCapableDependency(injectionPoint))
                    {
                        if(injectionPoint.getAnnotated().isAnnotationPresent(Disposes.class))
                        {
                            continue;
                        }
                        throw new WebBeansConfigurationException(
                                "Passivation capable beans must satisfy passivation capable dependencies. " +
                                "Bean : " + toString() + " does not satisfy. Details about the Injection-point: " +
                                        injectionPoint.toString());
                    }
                }
            }            
        }
    }
}
