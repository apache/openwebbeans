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

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.decorator.Delegate;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.Decorator;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.InjectionTarget;

import org.apache.webbeans.annotation.DefaultLiteral;
import org.apache.webbeans.config.OWBLogConst;
import org.apache.webbeans.config.inheritance.BeanInheritedMetaData;
import org.apache.webbeans.config.inheritance.IBeanInheritedMetaData;
import org.apache.webbeans.context.creational.CreationalContextImpl;
import org.apache.webbeans.decorator.WebBeansDecorator;
import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.exception.WebBeansException;
import org.apache.webbeans.inject.InjectableField;
import org.apache.webbeans.inject.InjectableMethods;
import org.apache.webbeans.intercept.InterceptorData;
import org.apache.webbeans.intercept.InterceptorHandler;
import org.apache.webbeans.intercept.InterceptorType;
import org.apache.webbeans.intercept.InvocationContextImpl;
import org.apache.webbeans.intercept.webbeans.WebBeansInterceptor;
import org.apache.webbeans.logger.WebBeansLogger;
import org.apache.webbeans.spi.ResourceInjectionService;
import org.apache.webbeans.spi.ServiceLoader;
import org.apache.webbeans.util.ClassUtil;
import org.apache.webbeans.util.WebBeansUtil;

/**
 * Abstract class for injection target beans.
 * 
 * @version $Rev$ $Date$
 * @param <T> bean class
 */
public abstract class AbstractInjectionTargetBean<T> extends AbstractOwbBean<T> implements InjectionTargetBean<T>
{
    /** Logger instance */
    private final WebBeansLogger logger = WebBeansLogger.getLogger(getClass());

    /** Bean observable method */
    private Set<Method> observableMethods = new HashSet<Method>();

    /** Injected fields of the bean */
    private Set<Field> injectedFields = new HashSet<Field>();

    /** Injected methods of the bean */
    private Set<Method> injectedMethods = new HashSet<Method>();
    
    /** Injected fields of the bean */
    private Set<Field> injectedFromSuperFields = new HashSet<Field>();

    /** Injected methods of the bean */
    private Set<Method> injectedFromSuperMethods = new HashSet<Method>();
    
    /**Annotated type for bean*/
    private AnnotatedType<T> annotatedType;
    
    /**Fully initialize, true as default*/
    private boolean fullyInitialize = true;
    
    /**
     * Holds the all of the interceptor related data, contains around-invoke,
     * post-construct and pre-destroy
     */
    protected List<InterceptorData> interceptorStack = new ArrayList<InterceptorData>();

    /**Decorators*/
    protected List<Decorator<?>> decorators = new ArrayList<Decorator<?>>();
    
    /**Bean inherited meta data*/
    protected IBeanInheritedMetaData inheritedMetaData;    
    
    
    /**
     * InjectionTargt instance. If this is not null, it is used for creating
     * instance.
     * 
     * @see InjectionTarget
     */
    protected InjectionTarget<T> injectionTarget;
    
    /**
     * Creates a new observer owner component.
     * 
     * @param webBeansType webbean type
     * @param returnType bean class type
     */
    protected AbstractInjectionTargetBean(WebBeansType webBeansType, Class<T> returnType)
    {
        super(webBeansType, returnType);
    }

    /**
     * {@inheritDoc}
     */
    protected T createInstance(CreationalContext<T> creationalContext)
    {
        T instance = null;

        //If injection target is set by Exntesion Observer, use it
        if (isInjectionTargetSet())
        {
            //Create instance
            instance = getInjectionTarget().produce(creationalContext);
            
            //Injection Operation
            getInjectionTarget().inject(instance, creationalContext);
            
            //Call @PostConstrcut
            postConstruct(instance, creationalContext);
        }
        //Default operations
        else
        {
            //Default creation phases
            instance = createDefaultInstance(creationalContext);
        }

        return instance;
    }

    /**
     * Returns bean instance.
     * 
     * @param creationalContext creational context
     * @return bean instance
     */
    protected T createDefaultInstance(CreationalContext<T> creationalContext)
    {
        beforeConstructor();

        T instance = createComponentInstance(creationalContext);
        
        if(!isFullyInitialize())
        {
            return instance;
        }
                
        //Push instance into creational context, this is necessary because
        //Context objects look for instance in the interceptors. If we do not
        //push instance into cretional context, circular exception occurs.
        //Context instance first look into creational context object whether
        //Or not it exist.
        if(creationalContext instanceof CreationalContextImpl)
        {
            CreationalContextImpl<T> cc = (CreationalContextImpl<T>)creationalContext;
            cc.push(instance);
        }
        
        afterConstructor(instance, creationalContext);
        
        //Clear instance from creational context
        if(creationalContext instanceof CreationalContextImpl)
        {
            CreationalContextImpl<?> cc = (CreationalContextImpl<?>)creationalContext;
            cc.remove();
            
            cc.setProxyInstance(null);
        }
        
        return instance;
    }

    /**
     * {@inheritDoc}
     */
    protected void destroyInstance(T instance, CreationalContext<T> creationalContext)
    {
        destroyComponentInstance(instance,creationalContext);
    }

    /**
     * Sub-classes must override this method to create bean instance.
     * 
     * @param creationalContext creational context
     * @return bean instance
     */
    protected T createComponentInstance(CreationalContext<T> creationalContext)
    {
        return null;
    }

    /**
     * Sub-classes must override this method to destroy bean instance.
     * 
     * @param instance object instance.
     */
    protected void destroyComponentInstance(T instance, CreationalContext<T> creationalContext)
    {
        preDestroy(instance, creationalContext);
    }

    /**
     * Called before constructor.
     */
    protected void beforeConstructor()
    {

    }

    /**
     * Called after bean instance is created.
     * 
     * @param instance bean instance
     * @param creationalContext cretional context object
     */
    protected void afterConstructor(T instance, CreationalContext<T> creationalContext)
    {
        //Inject resources
        injectResources(instance, creationalContext);
        
        injectSuperFields(instance, creationalContext);
        injectSuperMethods(instance, creationalContext);
        
        // Inject fields
        injectFields(instance, creationalContext);

        // Inject methods
        injectMethods(instance, creationalContext);
        
        //Post construct
        postConstruct(instance, creationalContext);
    }

    /**
     * {@inheritDoc}
     */
    public void postConstruct(T instance, CreationalContext<T> cretionalContext)
    {
        if (isInjectionTargetSet())
        {
            getInjectionTarget().postConstruct(instance);
        }
        else
        {
            postConstructDefault(instance, cretionalContext);
        }
    }

    /**
     * Default post construct.
     * 
     * @param instance bean instance
     */
    protected void postConstructDefault(T instance, CreationalContext<T> ownerCreationalContext)
    {
        if(getWebBeansType().equals(WebBeansType.MANAGED))
        {
            // Call Post Construct
            if (WebBeansUtil.isContainsInterceptorMethod(getInterceptorStack(), InterceptorType.POST_CONSTRUCT))
            {
                InterceptorHandler.injectInterceptorFields(getInterceptorStack(), (CreationalContextImpl<T>)ownerCreationalContext);
                
                InvocationContextImpl impl = new InvocationContextImpl(null, instance, null, null, WebBeansUtil.getInterceptorMethods(getInterceptorStack(), InterceptorType.POST_CONSTRUCT), InterceptorType.POST_CONSTRUCT);
                try
                {
                    impl.proceed();
                }

                catch (Exception e)
                {
                    logger.error(OWBLogConst.ERROR_0008, new Object[]{"@PostConstruct."}, e);
                    throw new WebBeansException(e);
                }
            }            
        }        
    }

    /**
     * {@inheritDoc}
     */
    public void preDestroy(T instance, CreationalContext<T> creationalContext)
    {
        if (isInjectionTargetSet())
        {
            getInjectionTarget().preDestroy(instance);
        }
        else
        {
            preDestroyDefault(instance, creationalContext);
        }
    }

    /**
     * Default predestroy.
     * 
     * @param instance bean instance
     */
    protected void preDestroyDefault(T instance, CreationalContext<T> creationalContext)
    {
        if(getWebBeansType().equals(WebBeansType.MANAGED) ||
                getWebBeansType().equals(WebBeansType.DECORATOR))                
        {
            if (WebBeansUtil.isContainsInterceptorMethod(getInterceptorStack(), InterceptorType.PRE_DESTROY))
            {
                InterceptorHandler.injectInterceptorFields(getInterceptorStack(),(CreationalContextImpl<T>) creationalContext);
                
                InvocationContextImpl impl = new InvocationContextImpl(null, instance, null, null, WebBeansUtil.getInterceptorMethods(getInterceptorStack(), InterceptorType.PRE_DESTROY), InterceptorType.PRE_DESTROY);
                try
                {
                    impl.proceed();
                }
                catch (Exception e)
                {
                    getLogger().error(OWBLogConst.ERROR_0008, new Object[]{"@PreDestroy."}, e);
                    throw new WebBeansException(e);
                }
            }            
        }
    }

    /**
     * Injects fields of the bean after constructing.
     * 
     * @param instance bean instance
     * @param creationalContext creational context
     */
    public void injectFields(T instance, CreationalContext<T> creationalContext)
    {
        Set<Field> fields = getInjectedFields();
        for (Field field : fields)
        {
            if (field.getAnnotation(Delegate.class) == null)
            {
                if(!field.getType().equals(InjectionPoint.class))
                {
                    injectField(field, instance, creationalContext);   
                }
                //InjectionPoint.
                else
                {
                    Bean<?> injectionPointBean = getManager().getBeans(InjectionPoint.class, new DefaultLiteral()).iterator().next();
                    Object reference = getManager().getReference(injectionPointBean, InjectionPoint.class, getManager().createCreationalContext(injectionPointBean));
                    
                    ClassUtil.setField(instance, field, reference);
                }
            }
        }                
    }
    
    public void injectSuperFields(T instance, CreationalContext<T> creationalContext)
    {
        Set<Field> fields = getInjectedFromSuperFields();
        for (Field field : fields)
        {
            if (field.getAnnotation(Delegate.class) == null)
            {
                injectField(field, instance, creationalContext);
            }
        }                        
    }
    
    public void injectSuperMethods(T instance, CreationalContext<T> creationalContext)
    {
        Set<Method> methods = getInjectedFromSuperMethods();

        for (Method method : methods)
        {
            injectMethod(method, instance, creationalContext);
        }        
    }
    
    
    private void injectField(Field field, Object instance, CreationalContext<?> creationalContext)
    {
        InjectableField f = new InjectableField(field, instance, this, creationalContext);
        f.doInjection();        
    }

    /**
     * Injects all {@link Initializer} methods of the bean instance.
     * 
     * @param instance bean instance
     * @param creationalContext creational context instance
     */
    public void injectMethods(T instance, CreationalContext<T> creationalContext)
    {
        Set<Method> methods = getInjectedMethods();

        for (Method method : methods)
        {
            injectMethod(method, instance, creationalContext);
        }
    }
    
    @SuppressWarnings("unchecked")
    private void injectMethod(Method method, Object instance, CreationalContext<?> creationalContext)
    {
        InjectableMethods m = new InjectableMethods(method, instance, this, creationalContext);
        m.doInjection();        
    }

    /**
     * {@inheritDoc}
     */
    public void injectResources(T instance, CreationalContext<T> creationalContext)
    {
        if(getWebBeansType().equals(WebBeansType.MANAGED))
        {
            try
            {
                ResourceInjectionService service = null;
                try
                {
                    service = ServiceLoader.getService(ResourceInjectionService.class);
                    
                }catch(Exception e)
                {
                    // When running in tests
                }
                
                if(service != null)
                {
                    service.injectJavaEEResources(instance);   
                }
            }
            catch (Exception e)
            {
                logger.error("Error is occured while injection Java EE Resources for the bean instance : " + instance);
                throw new WebBeansException("Error is occured while injection Java EE Resources for the bean instance : " + instance,e);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void addObservableMethod(Method observerMethod)
    {
        this.observableMethods.add(observerMethod);
    }

    /**
     * {@inheritDoc}
     */
    public Set<Method> getObservableMethods()
    {
        return this.observableMethods;
    }

    /**
     * Gets injected fields.
     * 
     * @return injected fields
     */
    public Set<Field> getInjectedFields()
    {
        return this.injectedFields;
    }

    /**
     * Add new injected field.
     * 
     * @param field new injected field
     */
    public void addInjectedField(Field field)
    {
        this.injectedFields.add(field);
    }
    
    /**
     * Gets injected from super fields.
     * 
     * @return injected fields
     */
    public Set<Field> getInjectedFromSuperFields()
    {
        return this.injectedFromSuperFields;
    }

    /**
     * Add new injected field.
     * 
     * @param field new injected field
     */
    public void addInjectedFieldToSuper(Field field)
    {
        this.injectedFromSuperFields.add(field);
    }
    

    /**
     * Gets injected methods.
     * 
     * @return injected methods
     */
    public Set<Method> getInjectedMethods()
    {
        return this.injectedMethods;
    }

    /**
     * Add new injected method.
     * 
     * @param field new injected method
     */
    public void addInjectedMethod(Method method)
    {
        this.injectedMethods.add(method);
    }

    /**
     * Gets injected from super methods.
     * 
     * @return injected methods
     */
    public Set<Method> getInjectedFromSuperMethods()
    {
        return this.injectedFromSuperMethods;
    }

    /**
     * Add new injected method.
     * 
     * @param field new injected method
     */
    public void addInjectedMethodToSuper(Method method)
    {
        this.injectedFromSuperMethods.add(method);
    }
    
    /**
     * Sets injection target instance.
     * 
     * @param injectionTarget injection target instance
     */
    public void setInjectionTarget(InjectionTarget<T> injectionTarget)
    {
        this.injectionTarget = injectionTarget;
    }
        
    /**
     * {@inheritDoc}
     */
    public List<InterceptorData> getInterceptorStack()
    {
        return this.interceptorStack;
    }
    
    public List<Decorator<?>> getDecoratorStack()
    {
        return this.decorators;
    }

    /**
     * {@inheritDoc}
     */
    public IBeanInheritedMetaData getInheritedMetaData()
    {
        return this.inheritedMetaData;
    }
    
    /**
     * Sets inherited meta data.
     */
    protected void setInheritedMetaData()
    {
        this.inheritedMetaData = new BeanInheritedMetaData<T>(this);
    }
    
    
    /**
     * Returns injection target.
     * 
     * @return injection target
     */
    public InjectionTarget<T> getInjectionTarget()
    {
        return this.injectionTarget;
    }

    /**
     * Returns true if injection target instance set, false otherwise.
     * 
     * @return true if injection target instance set, false otherwise
     */
    protected boolean isInjectionTargetSet()
    {
        return this.injectionTarget != null ? true : false;
    }

    /**
     * Returns bean logger instance.
     * 
     * @return logger
     */
    protected WebBeansLogger getLogger()
    {
        return this.logger;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<InjectionPoint> getInjectionPoints()
    {
        if (isInjectionTargetSet())
        {
            return getInjectionTarget().getInjectionPoints();
        }

        return super.getInjectionPoints();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public AnnotatedType<T> getAnnotatedType()
    {
        return this.annotatedType;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void setAnnotatedType(AnnotatedType<T> annotatedType)
    {
        this.annotatedType = annotatedType;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void setFullyInitialize(boolean initialize)
    {
        this.fullyInitialize = initialize;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isFullyInitialize()
    {
        return this.fullyInitialize;
    }

    /* (non-Javadoc)
     * @see org.apache.webbeans.component.AbstractOwbBean#validatePassivationDependencies()
     */
    @Override
    public void validatePassivationDependencies()
    {        
        super.validatePassivationDependencies();
        
        //Check for interceptors and decorators
        for(Decorator<?> dec : this.decorators)
        {
            WebBeansDecorator<?> decorator = (WebBeansDecorator<?>)dec;
            if(!decorator.isPassivationCapable())
            {
                throw new WebBeansConfigurationException("Passivation bean : " + toString() + " decorators must be passivating capable");
            }
            else
            {
                decorator.validatePassivationDependencies();
            }
        }
        
        for(InterceptorData interceptorData : this.interceptorStack)
        {
            if(interceptorData.isDefinedWithWebBeansInterceptor())
            {
                WebBeansInterceptor<?> interceptor = (WebBeansInterceptor<?>)interceptorData.getWebBeansInterceptor();
                if(!interceptor.isPassivationCapable())
                {
                    throw new WebBeansConfigurationException("Passivation bean : " + toString() + " interceptors must be passivating capable");
                }
                else
                {
                    interceptor.validatePassivationDependencies();
                }
            }
            else
            {
                Object interceptorInstance = interceptorData.getInterceptorInstance();
                if(interceptorInstance != null)
                {
                    Class<?> interceptorClass = interceptorInstance.getClass();
                    if(!Serializable.class.isAssignableFrom(interceptorClass))
                    {
                        throw new WebBeansConfigurationException("Passivation bean : " + toString() + " interceptors must be passivating capable");
                    }                    
                }
            }
        }
    }    
    
    
    
}