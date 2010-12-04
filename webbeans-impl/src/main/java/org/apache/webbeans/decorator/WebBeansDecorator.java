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
package org.apache.webbeans.decorator;

import org.apache.webbeans.component.AbstractInjectionTargetBean;
import org.apache.webbeans.component.AbstractOwbBean;
import org.apache.webbeans.component.ManagedBean;
import org.apache.webbeans.component.WebBeansType;
import org.apache.webbeans.config.OWBLogConst;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.exception.WebBeansException;
import org.apache.webbeans.inject.InjectableField;
import org.apache.webbeans.inject.InjectableMethods;
import org.apache.webbeans.util.AnnotationUtil;
import org.apache.webbeans.util.ClassUtil;
import org.apache.webbeans.util.SecurityUtil;

import javax.decorator.Delegate;
import javax.enterprise.context.spi.Context;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.Decorator;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;

/**
 * Defines decorators. It wraps the bean instance related
 * with decorator class. Actually, each decorator is an instance
 * of the {@link ManagedBean}.
 * 
 * @version $Rev$ $Date$
 *
 * @param <T> decorator type info
 */
public class WebBeansDecorator<T> extends AbstractInjectionTargetBean<T> implements OwbDecorator<T>
{
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
    
    /**Custom Decorator*/
    private Decorator<T> customDecorator = null;
    
    /**
     * Creates a new decorator bean instance with the given wrapped bean.
     * @param delegateComponent delegate bean instance
     */
    public WebBeansDecorator(AbstractInjectionTargetBean<T> wrappedBean, Decorator<T> customDecorator)
    {
        super(WebBeansType.DECORATOR,wrappedBean.getReturnType());
        this.wrappedBean = wrappedBean;
        this.customDecorator = customDecorator;
        initDelegate();
    }

    
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
        if(this.customDecorator != null)
        {
            this.delegateType = this.customDecorator.getDelegateType();
            this.delegateBindings = this.customDecorator.getDelegateQualifiers();
        }
        else
        {
            this.delegateType = ip.getType();
            this.delegateBindings = ip.getQualifiers();    
        }
                
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

        for (Type decType : getDecoratedTypes())
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
            if (AnnotationUtil.isQualifierEqual(annot, bindingType))
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
            if (DecoratorResolverRules.compareType(getDelegateType(), apiType))
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

        for (Annotation bindingType : getDelegateQualifiers())
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
        if(this.customDecorator != null)
        {
            return this.customDecorator.getDelegateQualifiers();
        }
        
        return delegateBindings;
    }

    @Override
    public Type getDelegateType()
    {
        if(this.customDecorator != null)
        {
            return this.customDecorator.getDelegateType();
        }        
        
        return delegateType;
    }

    public void setDelegate(Object instance, Object delegate)
    {
        if (!delegateField.isAccessible())
        {
            SecurityUtil.doPrivilegedSetAccessible(delegateField, true);
        }

        try
        {
            delegateField.set(instance, delegate);

        }
        catch (IllegalArgumentException e)
        {
            logger.error(OWBLogConst.ERROR_0007, e, instance.getClass().getName());
            throw new WebBeansException(e);

        }
        catch (IllegalAccessException e)
        {
            logger.error(OWBLogConst.ERROR_0015, e, delegateField.getName(), instance.getClass().getName());
        }

    }

    
    @SuppressWarnings("unchecked")    
    protected  T createInstance(CreationalContext<T> creationalContext)
    {
        if(this.customDecorator != null)
        {
            return this.customDecorator.create(creationalContext);
        }

        Context context = WebBeansContext.getInstance().getBeanManagerImpl().getContext(getScope());
        Object actualInstance = context.get((Bean<Object>)this.wrappedBean, (CreationalContext<Object>)creationalContext);
        T proxy = (T) WebBeansContext.getInstance().getJavassistProxyFactory().createDependentScopedBeanProxy(this.wrappedBean, actualInstance, creationalContext);
        
        return proxy;        
    }

    public void setInjections(Object proxy, CreationalContext<?> cretionalContext)
    {
        if(this.customDecorator != null)
        {
            Set<InjectionPoint> injections = this.customDecorator.getInjectionPoints();
            if(injections != null)
            {
                for(InjectionPoint ip : injections)
                {
                    if(!ip.isDelegate())
                    {
                        Member member = ip.getMember();
                        if(member instanceof Field)
                        {
                            injectField((Field)member  , proxy, cretionalContext);
                        }
                        if(member instanceof Method)
                        {
                            injectMethod((Method)member  , proxy, cretionalContext);
                        }                        
                    }
                }
            }
        }
        else
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
        if(this.customDecorator != null)
        {
            return this.customDecorator.getQualifiers();
        }
        
        return wrappedBean.getQualifiers();
    }

    @Override
    public String getName()
    {
        if(this.customDecorator != null)
        {
            return this.customDecorator.getName();
        }
        
        return wrappedBean.getName();
    }

    @Override
    public Class<? extends Annotation> getScope()
    {
        if(this.customDecorator != null)
        {
            return this.customDecorator.getScope();
        }
        
        return wrappedBean.getScope();
    }

    
    public Set<Type> getTypes()
    {
        if(this.customDecorator != null)
        {
            return this.customDecorator.getTypes();
        }
        
        return wrappedBean.getTypes();
    }

    @Override
    public boolean isNullable()
    {
        if(this.customDecorator != null)
        {
            return this.customDecorator.isNullable();
        }
        
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
        if(this.customDecorator != null)
        {
            return this.customDecorator.getInjectionPoints();
        }
        
        return wrappedBean.getInjectionPoints();
    }

    /**
     * @return the clazz
     */
    public Class<?> getClazz()
    {
        if(this.customDecorator != null)
        {
            return this.customDecorator.getBeanClass();
        }
        
        return clazz;
    }

    @Override
    public Class<?> getBeanClass()
    {
        if(this.customDecorator != null)
        {
            return this.customDecorator.getBeanClass();
        }
        
        return this.wrappedBean.getBeanClass();
    }

    @Override
    public Set<Class<? extends Annotation>> getStereotypes()
    {
        if(this.customDecorator != null)
        {
            return this.customDecorator.getStereotypes();
        }

        return this.wrappedBean.getStereotypes();
    }

    @Override
    public Set<Type> getDecoratedTypes()
    {
        if(this.customDecorator != null)
        {
            return this.customDecorator.getDecoratedTypes();
        }

        return this.decoratedTypes;
    }

    @Override
    public boolean isAlternative()
    {
        if(this.customDecorator != null)
        {
            return this.customDecorator.isAlternative();
        }

        return this.wrappedBean.isAlternative();
    }

    @Override
    public void validatePassivationDependencies()
    {
        this.wrappedBean.validatePassivationDependencies();
    }

}
