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

import jakarta.enterprise.inject.spi.BeanAttributes;
import org.apache.webbeans.config.OWBLogConst;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.container.SerializableBean;
import org.apache.webbeans.context.creational.CreationalContextImpl;
import org.apache.webbeans.exception.WebBeansException;
import org.apache.webbeans.logger.WebBeansLoggerFacade;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.CreationException;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.enterprise.inject.spi.InjectionTarget;
import jakarta.enterprise.inject.spi.PassivationCapable;
import jakarta.enterprise.inject.spi.Producer;
import org.apache.webbeans.util.WebBeansUtil;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Abstract implementation of the {@link OwbBean} contract. 
 * 
 * @version $Rev$ $Date$
 * 
 * @see jakarta.enterprise.inject.spi.Bean
 * 
 */
public abstract class AbstractOwbBean<T>
        extends BeanAttributesImpl<T>
        implements OwbBean<T>, PassivationCapable
{
    /**Logger instance*/
    protected Logger logger;
    
    /** Web Beans type */
    protected WebBeansType webBeansType;

    /** the bean class */
    private final Class<?> beanClass;

    /**This bean is specialized or not*/
    protected boolean specializedBean;

    /**This bean is enabled or disabled*/
    protected boolean enabled = true;

    /**
     * This string will be used for passivating the Bean.
     * It will be created on the first use.
     * @see #getId()
     */
    protected String passivatingId;
    
    protected final WebBeansContext webBeansContext;

    protected final Class<T> returnType;

    protected AbstractOwbBean(WebBeansContext webBeansContext,
                              WebBeansType webBeansType,
                              BeanAttributes<T> beanAttributes,
                              Class<?> beanClass)
    {
        super(beanAttributes);
        this.webBeansType = webBeansType;
        this.beanClass = beanClass;
        this.webBeansContext = webBeansContext;

        returnType = (Class<T>) WebBeansUtil.resolveReturnType(beanAttributes.getTypes());
    }

    /**
     * Get the web beans context this bean is associated with
     *
     * @return WebBeansContext this bean is associated with
     */
    @Override
    public WebBeansContext getWebBeansContext()
    {
        return webBeansContext;
    }
    
    @Override
    public Class<?> getBeanClass()
    {
        return beanClass;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public T create(CreationalContext<T> creationalContext)
    {
        try
        {
            if(!(creationalContext instanceof CreationalContextImpl))
            {
                creationalContext = webBeansContext.getCreationalContextFactory().wrappedCreationalContext(creationalContext, this);
            }

            Producer<T> producer = getProducer();
            T instance = producer.produce(creationalContext);
            if (producer instanceof InjectionTarget && instance != null) // @AroundConstruct can skip proceed and then it returns null
            {
                InjectionTarget<T> injectionTarget = (InjectionTarget<T>)producer;
                injectionTarget.inject(instance, creationalContext);
                injectionTarget.postConstruct(instance);
            }
            if (getScope().equals(Dependent.class) && instance != null)
            {
                ((CreationalContextImpl<T>)creationalContext).addDependent(this, instance);
            }
            return instance;
        }
        catch (Exception re)
        {
            Throwable current = re;
            Set<Throwable> visited = new HashSet<>();
            while (current instanceof InvocationTargetException || current instanceof WebBeansException)
            {
                Throwable cause = current.getCause();
                if (visited.add(cause))
                {
                    current = cause;
                }
                else
                {
                    break;
                }
            }
            if(current != null && !(current instanceof RuntimeException))
            {
                throw new CreationException(current);
            }
            if (current == null) // just a guard but highly unlikely
            {
                throw new CreationException(re);
            }
            throw (RuntimeException) current;
        }
    }

    /*
     * (non-Javadoc)
     * @param creationalContext the contextual instance has been created in
     */
    @Override
    public void destroy(T instance, CreationalContext<T> creationalContext)
    {
        if (getScope().equals(Dependent.class)
            && creationalContext instanceof CreationalContextImpl
            && ((CreationalContextImpl<T>)creationalContext).containsDependent(this, instance))
        {
            // we just have to call release, because release will destroy us since we are @Dependent
            creationalContext.release();
            return;
        }
        try
        {
            Producer<T> producer = getProducer();
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
    @Override
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

            String providedId = providedId();
            if (providedId != null)
            {
                sb.append(providedId).append('#');
            }

            sb.append(getReturnType()).append('#');
            for (Annotation qualifier : getQualifiers())
            {
                sb.append(qualifier.toString()).append(',');
            }

            passivatingId = sb.toString();
        }

        return passivatingId;
    }

    protected String providedId()
    {
        return null;
    }

    @Override
    public boolean isPassivationCapable()
    {
        if (isPassivationCapable != null)
        {
            return isPassivationCapable;
        }
        if(Serializable.class.isAssignableFrom(getReturnType()))
        {
            isPassivationCapable = Boolean.TRUE;
            return true;
        }
        isPassivationCapable = Boolean.FALSE;
        return false;
    }

    /** cache previously calculated result */
    private Boolean isPassivationCapable;

    /**
     * Get web bean type of the bean.
     * 
     * @return web beans type
     */
    @Override
    public WebBeansType getWebBeansType()
    {
        return webBeansType;
    }

    /**
     * Gets type of the producer method/field or the bean class if it's not a producer.
     * This basically determines the class which will get created.
     * 
     * @return type of the producer method
     */
    @Override
    public Class<T> getReturnType()
    {
        return returnType;
    }
    
    /**
     * {@inheritDoc}
     */    
    @Override
    public Set<InjectionPoint> getInjectionPoints()
    {
        return getProducer().getInjectionPoints();
    }
    
    /**
     * {@inheritDoc}
     */    
    @Override
    public void setSpecializedBean(boolean specialized)
    {
        specializedBean = specialized;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }
    
    /**
     * {@inheritDoc}
     */    
    @Override
    public boolean isSpecializedBean()
    {
        return specializedBean;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
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
        String simpleName = getReturnType().getSimpleName();
        builder.append(simpleName);
        builder.append(", WebBeansType:").append(getWebBeansType()).append(", Name:").append(getName());
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
                ParameterizedType parameterizedType = (ParameterizedType) clazz;
                Class<?> rawType = (Class<?>) parameterizedType.getRawType();
                builder.append(rawType.getName());
                builder.append("<");
                Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
                if (actualTypeArguments.length > 0)
                {
                    for (Type actualType : actualTypeArguments)
                    {
                        if (Class.class.isInstance(actualType))
                        {
                            builder.append(Class.class.cast(actualType).getName().replace("java.lang.", ""));
                        }
                        else
                        {
                            builder.append(actualType);
                        }
                        builder.append(",");
                    }
                }
                builder.delete(builder.length() - 1, builder.length());
                builder.append(">");

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

        addToStringInfo(builder);
        
        return builder.toString();
    }

    /**
     * This method can be used to add additional info about the Bean
     * @param builder
     */
    protected void addToStringInfo(StringBuilder builder)
    {
        // by default we do nothing
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

    @Override
    public boolean isDependent()
    {
        return getScope().equals(Dependent.class);
    }

    @Override
    public boolean equals(Object o)  // symmetry for serializable beans otherwise Map are broken, hashcode if fine
    {
        return o == this || (o instanceof SerializableBean && ((SerializableBean) o).getBean().equals(this));
    }

    @Override
    public int hashCode() // defined for checkstyle
    {
        return super.hashCode();
    }

}
