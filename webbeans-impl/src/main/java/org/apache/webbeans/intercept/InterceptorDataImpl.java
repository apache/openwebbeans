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
package org.apache.webbeans.intercept;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Interceptor;
import javax.interceptor.AroundInvoke;

import org.apache.webbeans.container.BeanManagerImpl;
import org.apache.webbeans.context.creational.CreationalContextImpl;
import org.apache.webbeans.context.creational.EjbInterceptorContext;
import org.apache.webbeans.decorator.WebBeansDecoratorInterceptor;
import org.apache.webbeans.inject.OWBInjector;
import org.apache.webbeans.intercept.webbeans.WebBeansInterceptor;
import org.apache.webbeans.logger.WebBeansLogger;
import org.apache.webbeans.util.WebBeansUtil;

/**
 * Abstract implementation of the {@link InterceptorData} api contract.
 * @version $Rev$ $Date$ 
 */
public class InterceptorDataImpl implements InterceptorData
{
    //Logger instance
    private static final WebBeansLogger logger = WebBeansLogger.getLogger(InterceptorDataImpl.class);
    
    /** Around invokes method */
    private Method aroundInvoke = null;

    /** Post construct methods */
    private Method postConstruct = null;

    /** Predestroy Method */
    private Method preDestroy = null;

    private Interceptor<?> webBeansInterceptor;

    /** Defined in the interceptor or bean */
    private boolean definedInInterceptorClass;

    /** Whether the interceptor class is defined in the method */
    private boolean definedInMethod;

    /**
     * If defined in method true, then this method holds
     * interceptor binding annotated method
     */
    private Method annotatedMethod;

    /** Defined with webbeans specific interceptor */
    private boolean isDefinedWithWebBeansInterceptor;
    
    private Class<?> interceptorClass = null;
    
    private WebBeansDecoratorInterceptor decoratorInterceptor = null;

    public InterceptorDataImpl(boolean isDefinedWithWebBeansInterceptor)
    {
        this(isDefinedWithWebBeansInterceptor,null);
    }

    public InterceptorDataImpl(boolean isDefinedWithWebBeansInterceptor, WebBeansDecoratorInterceptor decoratorInterceptor)
    {
        this.isDefinedWithWebBeansInterceptor = isDefinedWithWebBeansInterceptor;
        this.decoratorInterceptor = decoratorInterceptor;
    }
    
    public Class<?> getInterceptorClass()
    {
        return this.interceptorClass;
    }
    
    public void setInterceptorClass(Class<?> clazz)
    {
        this.interceptorClass = clazz;
    }


    /*
     * (non-Javadoc)
     * @see
     * org.apache.webbeans.intercept.InterceptorData#setInterceptor(java.lang
     * .reflect.Method, java.lang.Class)
     */
    public void setInterceptorMethod(Method m, Class<? extends Annotation> annotation)
    {
        if (annotation.equals(AroundInvoke.class))
        {
            setAroundInvoke(m);
        }
        else if (annotation.equals(PostConstruct.class))
        {
            setPostConstruct(m);
        }
        else if (annotation.equals(PreDestroy.class))
        {
            setPreDestroy(m);
        }
    }

    /*
     * (non-Javadoc)
     * @see
     * org.apache.webbeans.intercept.InterceptorData#addAroundInvoke(java.lang
     * .reflect.Method)
     */
    public void setAroundInvoke(Method m)
    {
        this.aroundInvoke = m;
    }

    /*
     * (non-Javadoc)
     * @see
     * org.apache.webbeans.intercept.InterceptorData#addPostConstruct(java.lang
     * .reflect.Method)
     */
    protected void setPostConstruct(Method m)
    {
        this.postConstruct = m;
    }

    /*
     * (non-Javadoc)
     * @see
     * org.apache.webbeans.intercept.InterceptorData#addPreDestroy(java.lang
     * .reflect.Method)
     */
    protected void setPreDestroy(Method m)
    {
        this.preDestroy = m;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.webbeans.intercept.InterceptorData#getPostConstruct()
     */
    public Method getPostConstruct()
    {
        return this.postConstruct;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.webbeans.intercept.InterceptorData#getPreDestroy()
     */
    public Method getPreDestroy()
    {
        return this.preDestroy;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.webbeans.intercept.InterceptorData#getAroundInvoke()
     */
    public Method getAroundInvoke()
    {
        return this.aroundInvoke;
    }

    /*
     * (non-Javadoc)
     * @see
     * org.apache.webbeans.intercept.InterceptorData#isDefinedInInterceptorClass
     * ()
     */
    public boolean isDefinedInInterceptorClass()
    {
        return definedInInterceptorClass;
    }

    /*
     * (non-Javadoc)
     * @see
     * org.apache.webbeans.intercept.InterceptorData#setDefinedInInterceptorClass
     * (boolean)
     */
    public void setDefinedInInterceptorClass(boolean definedInInterceptorClass)
    {
        this.definedInInterceptorClass = definedInInterceptorClass;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.webbeans.intercept.InterceptorData#isDefinedInMethod()
     */
    public boolean isDefinedInMethod()
    {
        return definedInMethod;
    }

    /*
     * (non-Javadoc)
     * @see
     * org.apache.webbeans.intercept.InterceptorData#setDefinedInMethod(boolean)
     */
    public void setDefinedInMethod(boolean definedInMethod)
    {
        this.definedInMethod = definedInMethod;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.webbeans.intercept.InterceptorData#getAnnotatedMethod()
     */
    public Method getInterceptorBindingMethod()
    {
        return annotatedMethod;
    }

    /*
     * (non-Javadoc)
     * @see
     * org.apache.webbeans.intercept.InterceptorData#setAnnotatedMethod(java
     * .lang.reflect.Method)
     */
    public void setInterceptorBindingMethod(Method annotatedMethod)
    {
        this.annotatedMethod = annotatedMethod;
    }

    /*
     * (non-Javadoc)
     * @seeorg.apache.webbeans.intercept.InterceptorData#
     * isDefinedWithWebBeansInterceptor()
     */
    public boolean isDefinedWithWebBeansInterceptor()
    {
        return isDefinedWithWebBeansInterceptor;
    }

    /**
     * @return the webBeansInterceptor
     */
    public Interceptor<?> getWebBeansInterceptor()
    {
        return webBeansInterceptor;
    }

    /**
     * @param webBeansInterceptor the webBeansInterceptor to set
     */
    public void setWebBeansInterceptor(Interceptor<?> webBeansInterceptor)
    {
        this.webBeansInterceptor = webBeansInterceptor;
    }
    
    public Method getInterceptorMethod()
    {
        if(aroundInvoke != null)
        {
            return aroundInvoke;
        }
        else if(postConstruct != null)
        {
            return postConstruct;
        }
        else if(preDestroy != null)
        {
            return preDestroy;
        }
        
        else return null;
    }

    @Override
    public boolean isLifecycleInterceptor()
    {
        if(this.preDestroy != null || this.postConstruct != null)
        {
            return true;
        }
        
        return false;
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public Object createNewInstance(Object ownerInstance,CreationalContextImpl<?> ownerCreationalContext)
    {
        //check for this InterceptorData is defined by interceptor class
        if(this.isDefinedWithWebBeansInterceptor && this.definedInInterceptorClass)
        {
            Object interceptor = null;
            
            //Means that it is the last interceptor added by InterceptorHandler
            if(this.webBeansInterceptor == null)
            {
                return this.decoratorInterceptor; 
            }
            
            if(ownerCreationalContext == null)
            {
                System.out.println("Null");
            }
            interceptor = ownerCreationalContext.getDependentInterceptor(ownerInstance,this.webBeansInterceptor);
            //There is no define interceptor, define and add it into dependent
            if(interceptor == null)
            {
                BeanManagerImpl manager = BeanManagerImpl.getManager();
                
                WebBeansInterceptor<Object> actualInterceptor = (WebBeansInterceptor<Object>)this.webBeansInterceptor;
                CreationalContext<Object> creationalContext = manager.createCreationalContext(actualInterceptor);
                interceptor = manager.getReference(actualInterceptor,actualInterceptor.getBeanClass(), creationalContext);
                
                actualInterceptor.setInjections(interceptor, creationalContext);

                ownerCreationalContext.addDependent(ownerInstance, (WebBeansInterceptor<Object>)this.webBeansInterceptor, interceptor);
            }
            
            return interceptor;
        }

        EjbInterceptorContext ctx = null;
        Object interceptor = null;       
        //control for this InterceptorData is defined by interceptor class
        if(this.definedInInterceptorClass)
        {
            ctx = ownerCreationalContext.getEjbInterceptor(ownerInstance, this.interceptorClass);                
            if(ctx == null)
            {                    
                interceptor = WebBeansUtil.newInstanceForced(this.interceptorClass);
                try
                {
                    OWBInjector injector = new OWBInjector();
                    injector.inject(interceptor);
                    
                    ctx = new EjbInterceptorContext();
                    ctx.setInjectorInstance(injector);
                    ctx.setInterceptorInstance(interceptor);
                }
                catch (Exception e)
                {
                    logger.error("Unable to inject dependencies of EJB interceptor instance with class : " + interceptorClass,e);
                }          
                
                ownerCreationalContext.addEjbInterceptor(interceptorClass, ctx);
            }
            else
            {
                interceptor = ctx.getInterceptorInstance();
            }
        }

        return interceptor; 
    }

}