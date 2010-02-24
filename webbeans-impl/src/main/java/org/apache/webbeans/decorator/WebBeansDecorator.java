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
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;

import javax.decorator.Delegate;
import javax.enterprise.context.spi.Context;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.Decorator;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;

import org.apache.webbeans.component.AbstractOwbBean;
import org.apache.webbeans.component.AbstractInjectionTargetBean;
import org.apache.webbeans.component.ManagedBean;
import org.apache.webbeans.component.WebBeansType;
import org.apache.webbeans.config.OWBLogConst;
import org.apache.webbeans.container.BeanManagerImpl;
import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.exception.WebBeansException;
import org.apache.webbeans.inject.InjectableField;
import org.apache.webbeans.inject.InjectableMethods;
import org.apache.webbeans.logger.WebBeansLogger;
import org.apache.webbeans.proxy.JavassistProxyFactory;
import org.apache.webbeans.util.AnnotationUtil;
import org.apache.webbeans.util.ClassUtil;

/**
 * Defines decorators. It wraps the bean instance related
 * with decorator class. Actually, each decorator is an instance
 * of the {@link ManagedBean}.
 * 
 * @version $Rev$ $Date$
 *
 * @param <T> decorator type info
 */
public class WebBeansDecorator<T> extends AbstractInjectionTargetBean<T> implements Decorator<T>
{
    private static WebBeansLogger logger = WebBeansLogger.getLogger(WebBeansDecorator.class);

    /** Decorator class */
    private Class<?> clazz;

    /** Decorates api types */
    private Set<Type> decoratedTypes = new HashSet<Type>();

    /** Delegate field class type */
    protected Type delegateType;
    
    /** Delegate field bindings */
    protected Set<Annotation> delegateBindings = new HashSet<Annotation>();
    
    protected Field delegateField;

    /** Wrapped bean*/
    private AbstractInjectionTargetBean<T> wrappedBean;
    
    /**
     * Creates a new decorator bean instance with the given wrapped bean.
     * @param delegateComponent delegate bean instance
     */
    public WebBeansDecorator(AbstractInjectionTargetBean<T> wrappedBean)
    {
        super(WebBeansType.DECORATOR,wrappedBean.getReturnType());
        
        this.wrappedBean = wrappedBean;
        this.clazz = wrappedBean.getReturnType();

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
        Set<InjectionPoint> injectionPoints = getInjectionPoints();
        boolean found = false;
        InjectionPoint ipFound = null;
        for(InjectionPoint ip : injectionPoints)
        {
            if(ip.getAnnotated().isAnnotationPresent(Delegate.class))
            {
                if(!found)
                {
                    found = true;
                    ipFound = ip;                    
                }
                else
                {
                    throw new WebBeansConfigurationException("Decorators must have a one @Delegate injection point. " +
                    		"But the decorator bean : " + toString() + " has more than one");
                }
            }            
        }
        
        
        if(ipFound == null)
        {
            throw new WebBeansConfigurationException("Decorators must have a one @Delegate injection point." +
                    "But the decorator bean : " + toString() + " has none");
        }
        
        String message = new String("Error in decorator : "+ toString() + ". The delegate injection point must be an injected field, " +
        		"initializer method parameter or bean constructor method parameter. ");
        
        if(!(ipFound.getMember() instanceof Constructor))
        {
            AnnotatedElement element = (AnnotatedElement)ipFound.getMember();
            if(!element.isAnnotationPresent(Inject.class))
            {
                throw new WebBeansConfigurationException(message);
            }                
        }
        
        initDelegateInternal(ipFound);
        
    }
    
    @Override
    public boolean isPassivationCapable()
    {
        return this.wrappedBean.isPassivationCapable();
    }

    private void initDelegateInternal(InjectionPoint ip)
    {
        this.delegateType = ip.getType();
        this.delegateBindings = ip.getQualifiers();
        
        if(ip.getMember() instanceof Field)
        {
            this.delegateField = (Field)ip.getMember();
        }
        else
        {
            Field[] fields = ClassUtil.getFieldsWithType(returnType, delegateType);
            if(fields.length == 0)
            {
                throw new WebBeansConfigurationException("Delegate injection field is not found for decorator : " + toString());
            }
            
            if(fields.length > 1)
            {
                throw new WebBeansConfigurationException("More than one delegate injection field is found for decorator : " + toString());
            }
            
            this.delegateField = fields[0];
        }
        
        Type fieldType = this.delegateField.getGenericType();

        for (Type decType : this.decoratedTypes)
        {
            if (!(ClassUtil.getClass(decType)).isAssignableFrom(ClassUtil.getClass(fieldType)))
            {
                throw new WebBeansConfigurationException("Decorator : " + toString() + " delegate attribute must implement all of the decorator decorated types.");
            }
            else
            {
                if(ClassUtil.isParametrizedType(decType) && ClassUtil.isParametrizedType(fieldType))
                {                    
                    if(!fieldType.equals(decType))
                    {
                        throw new WebBeansConfigurationException("Decorator : " + toString() + " generic delegate attribute must be same with decorated type : " + decType);
                    }
                }
            }
        }
    }
    
    private boolean bindingMatchesAnnotations(Annotation bindingType, Set<Annotation> annotations)
    {

        for (Annotation annot : annotations)
        {
            if (AnnotationUtil.hasAnnotationMember(bindingType.annotationType(), annot, bindingType))
            {
                return true;
            }
        }

        return false;
    }

    /**
     * Helper method to check if any of a list of Types are assignable to the
     * delegate type.
     * 
     * @param apiTypes Set of apiTypes to check against the delegate type
     * @return true if one of the types is assignable to the delegate type
     */
    private boolean apiTypesMatchDelegateType(Set<Type> apiTypes)
    {
        boolean ok = false;
        for (Type apiType : apiTypes)
        {
            if (ClassUtil.isAssignable(apiType, this.delegateType))
            {
                ok = true;
                break;
            }
        }
        
        if(ok) 
        {
            return true;
        }

        return false;
    }

    public boolean isDecoratorMatch(Set<Type> apiTypes, Set<Annotation> annotations)
    {

        if (!apiTypesMatchDelegateType(apiTypes))
        {
            return false;
        }

        for (Annotation bindingType : delegateBindings)
        {
            if (!bindingMatchesAnnotations(bindingType, annotations))
            {
                return false;
            }
        }

        return true;
    }

    @Override
    public Set<Annotation> getDelegateQualifiers()
    {
        return delegateBindings;
    }

    @Override
    public Type getDelegateType()
    {
        return delegateType;
    }

    public void setDelegate(Object instance, Object delegate)
    {
        if (!delegateField.isAccessible())
        {
            delegateField.setAccessible(true);
        }

        try
        {
            delegateField.set(instance, delegate);

        }
        catch (IllegalArgumentException e)
        {
            logger.error(OWBLogConst.ERROR_0007, new Object[]{instance.getClass().getName()}, e);
            throw new WebBeansException(e);

        }
        catch (IllegalAccessException e)
        {
            logger.error(OWBLogConst.ERROR_0015, new Object[]{delegateField.getName(), instance.getClass().getName()}, e);
        }

    }

    
    @SuppressWarnings("unchecked")    
    protected  T createInstance(CreationalContext<T> creationalContext)
    {
        Context context = BeanManagerImpl.getManager().getContext(getScope());
        Object actualInstance = context.get((Bean<Object>)this.wrappedBean, (CreationalContext<Object>)creationalContext);
        T proxy = (T)JavassistProxyFactory.createDependentScopedBeanProxy(this.wrappedBean, actualInstance, creationalContext);
        
        return proxy;        
    }

    public void setInjections(Object proxy, CreationalContext<?> cretionalContext)
    {
        // Set injected fields
        ManagedBean<T> delegate = (ManagedBean<T>) this.wrappedBean;

        Set<Field> injectedFields = delegate.getInjectedFromSuperFields();
        for (Field injectedField : injectedFields)
        {
            boolean isDecorates = injectedField.isAnnotationPresent(Delegate.class);

            if (!isDecorates)
            {
                injectField(injectedField, proxy, cretionalContext);
            }
        }
        
        Set<Method> injectedMethods = delegate.getInjectedFromSuperMethods();
        for (Method injectedMethod : injectedMethods)
        {
            injectMethod(injectedMethod, proxy, cretionalContext);
        }        

        injectedFields = delegate.getInjectedFields();
        for (Field injectedField : injectedFields)
        {
            boolean isDecorates = injectedField.isAnnotationPresent(Delegate.class);

            if (!isDecorates)
            {
                injectField(injectedField, proxy, cretionalContext);
            }
        }
        
        injectedMethods = delegate.getInjectedMethods();
        for (Method injectedMethod : injectedMethods)
        {
            injectMethod(injectedMethod, proxy, cretionalContext);
        }        
    }
    
    private void injectField(Field field, Object instance, CreationalContext<?> creationalContext)
    {
        InjectableField f = new InjectableField(field, instance, this.wrappedBean, creationalContext);
        f.doInjection();        
    }

    @SuppressWarnings("unchecked")
    private void injectMethod(Method method, Object instance, CreationalContext<?> creationalContext)
    {
        InjectableMethods m = new InjectableMethods(method, instance, this.wrappedBean, creationalContext);
        m.doInjection();        
    }
        
    @Override
    public Set<Annotation> getQualifiers()
    {
        return wrappedBean.getQualifiers();
    }

    @Override
    public String getName()
    {
        return wrappedBean.getName();
    }

    @Override
    public Class<? extends Annotation> getScope()
    {
        return wrappedBean.getScope();
    }

    
    public Set<Type> getTypes()
    {
        return wrappedBean.getTypes();
    }

    @Override
    public boolean isNullable()
    {
        return wrappedBean.isNullable();
    }

    @Override
    public boolean isSerializable()
    {
        return wrappedBean.isSerializable();
    }

    /**
     * @return the delegateComponent
     */
    public AbstractOwbBean<T> getDelegateComponent()
    {
        return wrappedBean;
    }
    
    public Set<InjectionPoint> getInjectionPoints()
    {
        return wrappedBean.getInjectionPoints();
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
        final WebBeansDecorator<?> other = (WebBeansDecorator<?>) obj;
        if (clazz == null)
        {
            if (other.clazz != null)
                return false;
        }
        else if (!clazz.equals(other.clazz))
            return false;
        return true;
    }

    @Override
    public Class<?> getBeanClass()
    {
        return this.wrappedBean.getBeanClass();
    }

	@Override
	public Set<Class<? extends Annotation>> getStereotypes() 
	{
		return this.wrappedBean.getStereotypes();
	}

	@Override
	public Set<Type> getDecoratedTypes() {
		return this.wrappedBean.getTypes();
	}

    @Override
    public boolean isAlternative()
    {
        return this.wrappedBean.isAlternative();
    }
    
    @Override
    public void validatePassivationDependencies()
    {
        this.wrappedBean.validatePassivationDependencies();
    }    
    
}
