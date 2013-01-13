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
package org.apache.webbeans.intercept.webbeans;

import org.apache.webbeans.annotation.AnnotationManager;
import org.apache.webbeans.component.AbstractInjectionTargetBean;
import org.apache.webbeans.component.AbstractOwbBean;
import org.apache.webbeans.component.WebBeansType;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.context.creational.CreationalContextImpl;
import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.exception.WebBeansException;
import org.apache.webbeans.inject.InjectableField;
import org.apache.webbeans.inject.InjectableMethod;
import org.apache.webbeans.intercept.OwbInterceptor;
import org.apache.webbeans.portable.InjectionTargetImpl;
import org.apache.webbeans.util.AnnotationUtil;

import javax.enterprise.context.Dependent;
import javax.enterprise.context.spi.Context;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.InterceptionType;
import javax.enterprise.inject.spi.Interceptor;
import javax.enterprise.util.Nonbinding;
import javax.interceptor.AroundInvoke;
import javax.interceptor.AroundTimeout;
import javax.interceptor.InvocationContext;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

/**
 * Defines the webbeans specific interceptors.
 * <p>
 * WebBeans interceptor classes has at least one {@link javax.interceptor.InterceptorBinding}
 * annotation. It can be defined on the class or method level at the component.
 * WebBeans interceptors are called after the EJB related interceptors are
 * called in the chain. Semantics of the interceptors are specified by the EJB
 * specification.
 * </p>
 * 
 * @version $Rev$ $Date$
 * @deprecated this should get replaced via a new version which does <b>not</b> delegate to a ManagedBean!
 */
public class WebBeansInterceptorBeanPleaseRemove<T> extends AbstractOwbBean<T> implements OwbInterceptor<T>
{
    /** InterceptorBindingTypes exist on the interceptor class */
    private Map<Class<? extends Annotation>, Annotation> interceptorBindingSet = new HashMap<Class<? extends Annotation>, Annotation>();

    /** Interceptor class */
    private Class<?> clazz;

    /**
     * The Delegate Bean.
     * OWB currently scans any &#064;Interceptor class as standard bean and then 'wraps'
     * it with this Bean. Imo this is wrong as an InterceptorBean has different rules alltogether.
     */
    private AbstractInjectionTargetBean<T> delegateBean;

    private final WebBeansContext webBeansContext;

    public WebBeansInterceptorBeanPleaseRemove(AbstractInjectionTargetBean<T> delegateBean)
    {
        super(delegateBean.getWebBeansContext(),
              WebBeansType.INTERCEPTOR,
              delegateBean.getTypes(),
              delegateBean.getQualifiers(),
              Dependent.class,
              delegateBean.getBeanClass(),
              delegateBean.getStereotypes());
        this.delegateBean = delegateBean;
        clazz = getDelegate().getReturnType();

        webBeansContext = delegateBean.getWebBeansContext();
    }

    public AbstractOwbBean<T> getDelegate()
    {
        return delegateBean;
    }
    
    public AnnotatedType<T> getAnnotatedType()
    {
        return delegateBean.getAnnotatedType();
    }
    

    /**
     * Add new binding type to the interceptor.
     * 
     * @param binding interceptor binding annotation. class
     * @param annot binding type annotation
     */
    public void addInterceptorBinding(Class<? extends Annotation> binding, Annotation annot)
    {
        Method[] methods = webBeansContext.getSecurityService().doPrivilegedGetDeclaredMethods(binding);

        for (Method method : methods)
        {
            Class<?> clazz = method.getReturnType();
            if (clazz.isArray() || clazz.isAnnotation())
            {
                if (!AnnotationUtil.hasAnnotation(method.getAnnotations(), Nonbinding.class))
                {
                    throw new WebBeansConfigurationException("Interceptor definition class : " + getClazz().getName() + " @InterceptorBinding : "
                                                             + binding.getName()
                                                             + " must have @NonBinding valued members for its array-valued and annotation valued members");
                }
            }
        }

        interceptorBindingSet.put(binding, annot);
    }

    /**
     * Checks whether all of this interceptors binding types are present on the bean, with 
     * {@link Nonbinding} member values.
     * 
     * @param bindingTypes binding types of bean
     * @param annots binding types annots of bean
     * @return true if all binding types of this interceptor exist ow false
     */
    public boolean hasBinding(List<Class<? extends Annotation>> bindingTypes, List<Annotation> annots)
    {
        if (bindingTypes == null || annots == null)
        {
            return false;
        }
        if (bindingTypes.size() != annots.size())
        {
            return false;
        }
        if (bindingTypes.size() == 0)
        {
            return false;
        }

        /* This interceptor is enabled if all of its interceptor bindings are present on the bean */
        for (Annotation ann : getInterceptorBindings())
        {
            Class<? extends Annotation> bindingType = ann.annotationType();
            int index = bindingTypes.indexOf(bindingType);
            if (index < 0)
            {
                return false; /* at least one of this interceptors types is not in the beans bindingTypes */
            }

            if (!AnnotationUtil.isQualifierEqual(ann, annots.get(index)))

            {
                return false;
            }
        }
        
        return true;
    }

    /**
     * Gets the interceptor class.
     * 
     * @return interceptor class
     */
    public Class<?> getClazz()
    {
        return clazz;
    }

    public Set<Interceptor<?>> getMetaInceptors()
    {
        Set<Interceptor<?>> set = new HashSet<Interceptor<?>>();

        Set<Annotation> keys = getInterceptorBindings();

        AnnotationManager annotationManager = webBeansContext.getAnnotationManager();

        for (Annotation key : keys)
        {
            Class<? extends Annotation> clazzAnnot = key.annotationType();
            Set<Annotation> declared = null;
            Annotation[] anns = null;

            if (webBeansContext.getInterceptorsManager().hasInterceptorBindingType(clazzAnnot))
            {
                declared = webBeansContext.getInterceptorsManager().getInterceptorBindingTypeMetaAnnotations(clazzAnnot);
                anns = new Annotation[declared.size()];
                anns = declared.toArray(anns);
            }

            else if (annotationManager.hasInterceptorBindingMetaAnnotation(clazzAnnot.getDeclaredAnnotations()))
            {
                anns = annotationManager.getInterceptorBindingMetaAnnotations(clazzAnnot.getDeclaredAnnotations());
            }

            /*
             * For example: @InterceptorBinding @Transactional @Action
             * public @interface ActionTransactional @ActionTransactional
             * @Production { }
             */

            if (anns != null && anns.length > 0)
            {
                // For example : @Transactional @Action Interceptor
                Set<Interceptor<?>> metas = webBeansContext.getWebBeansInterceptorConfig().findDeployedWebBeansInterceptor(anns);
                set.addAll(metas);

                // For each @Transactional and @Action Interceptor
                for (Annotation ann : anns)
                {
                    Annotation[] simple = new Annotation[1];
                    simple[0] = ann;
                    metas = webBeansContext.getWebBeansInterceptorConfig().findDeployedWebBeansInterceptor(simple);
                    set.addAll(metas);
                }

            }

        }

        return set;
    }

    public Set<Annotation> getInterceptorBindings()
    {
        Set<Annotation> set = new HashSet<Annotation>();
        Set<Class<? extends Annotation>> keySet = interceptorBindingSet.keySet();
        Iterator<Class<? extends Annotation>> itSet = keySet.iterator();

        while (itSet.hasNext())
        {
            set.add(interceptorBindingSet.get(itSet.next()));
        }

        return set;
    }

    /**
     * TODO WTF? these checks must not be done at runtime but boot time!
     */
    private Method getMethod(InterceptionType type)
    {
        Method method = null;
        
        if(type.equals(InterceptionType.AROUND_INVOKE))
        {
            Set<Method> methods = webBeansContext.getWebBeansUtil().checkAroundInvokeAnnotationCriterias(getClazz(), AroundInvoke.class);
            method = methods == null ? null : methods.iterator().next();
        }

        else if(type.equals(InterceptionType.AROUND_TIMEOUT))
        {
            Set<Method> methods = webBeansContext.getWebBeansUtil().checkAroundInvokeAnnotationCriterias(getClazz(), AroundTimeout.class);
            method = methods == null ? null : methods.iterator().next();
        }
        
        else
        {
            Class<? extends Annotation> interceptorTypeAnnotationClazz =
                webBeansContext.getInterceptorUtil().getInterceptorAnnotationClazz(type);
            Set<Method> methods = getWebBeansContext().getWebBeansUtil().checkCommonAnnotationCriterias(getClazz(), interceptorTypeAnnotationClazz, true);
            method = methods == null ? null : methods.iterator().next();
        }
        
        return method;
    }

    
    @SuppressWarnings("unchecked")
    protected T createInstance(CreationalContext<T> creationalContext)
    {
        Context context = webBeansContext.getBeanManagerImpl().getContext(getScope());
        Object actualInstance = context.get((Bean<Object>) delegateBean, (CreationalContext<Object>)creationalContext);
        T proxy = (T) webBeansContext.getProxyFactory().createDependentScopedBeanProxy(delegateBean, actualInstance, creationalContext);
        
        return proxy;
    }
    
    private void injectField(Field field, Object instance, CreationalContext<?> creationalContext)
    {
        InjectionTargetImpl<T> injectionTarget = new InjectionTargetImpl<T>(getAnnotatedType(), getInjectionPoints(), getWebBeansContext());
        InjectableField f = new InjectableField(field, instance, injectionTarget, (CreationalContextImpl) creationalContext);
        f.doInjection();        
    }

    @SuppressWarnings("unchecked")
    private void injectMethod(Method method, Object instance, CreationalContext<?> creationalContext)
    {
        InjectionTargetImpl<T> injectionTarget = new InjectionTargetImpl<T>(getAnnotatedType(), getInjectionPoints(), getWebBeansContext());
        InjectableMethod m = new InjectableMethod(method, instance, injectionTarget, (CreationalContextImpl) creationalContext);
        m.doInjection();        
    }
    
    @Override
    public Set<Annotation> getQualifiers()
    {
        return delegateBean.getQualifiers();
    }

    @Override
    public String getName()
    {
        return delegateBean.getName();
    }

    @Override
    public Class<? extends Annotation> getScope()
    {
        return delegateBean.getScope();
    }

    public Set<Type> getTypes()
    {
        return delegateBean.getTypes();
    }
    
    public Set<InjectionPoint> getInjectionPoints()
    {
        return delegateBean.getInjectionPoints();
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        return "WebBeans Interceptor with class : " + "[" + clazz.getName() + "]";
    }

    @Override
    public boolean isNullable()
    {
        return delegateBean.isNullable();
    }

    @Override
    public Class<?> getBeanClass()
    {
        return delegateBean.getBeanClass();
    }

    @Override
    public Set<Class<? extends Annotation>> getStereotypes()
    {
        return delegateBean.getStereotypes();
    }

    /**
     * This is the main invocation point for an interceptor!
     */
    public Object intercept(InterceptionType type, T instance, InvocationContext ctx)
    {
        Method method = getMethod(type);
        try
        {
            return method.invoke(instance, ctx);
        }
        catch (Exception e)
        {
            getLogger().log(Level.SEVERE, e.getMessage(), e);
            throw new WebBeansException(e);
        }
    }

    public boolean intercepts(InterceptionType type)
    {
        Method method = getMethod(type);

        return method != null ? true : false;
    }

    @Override
    public boolean isAlternative()
    {
        return delegateBean.isAlternative();
    }
    
    @Override
    public boolean isPassivationCapable()
    {
        return delegateBean.isPassivationCapable();
    }

    @Override
    public void validatePassivationDependencies()
    {
        delegateBean.validatePassivationDependencies();
    }    
    
    
}
