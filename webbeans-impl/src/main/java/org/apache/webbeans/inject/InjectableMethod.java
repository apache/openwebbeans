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
package org.apache.webbeans.inject;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.spi.AnnotatedParameter;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.enterprise.inject.spi.Producer;

import org.apache.webbeans.component.ProducerMethodBean;
import org.apache.webbeans.container.InjectionResolver;
import org.apache.webbeans.context.creational.CreationalContextImpl;
import org.apache.webbeans.exception.WebBeansException;
import org.apache.webbeans.proxy.NormalScopeProxyFactory;
import org.apache.webbeans.proxy.OwbNormalScopeProxy;

@SuppressWarnings("unchecked")
public class InjectableMethod<T> extends AbstractInjectable<T>
{
    /** Injectable method */
    protected Method method;

    /** Bean parent instance that owns the method */
    protected Object ownerInstance;
    
    /**If this method is dispose method*/
    private boolean disposable;
    
    /**Used in dispose method, represents produces method parameter instance*/
    private Object producerMethodInstance;
    
    private Map<Bean<?>, Object> dependentParameters = new HashMap<>();

    private Set<InjectionPoint> injectionPoints;

    public InjectableMethod(Method m, Object instance, Producer<T> owner, CreationalContextImpl<T> creationalContext)
    {
        this(m, instance, owner, creationalContext, new HashSet<>(createInjectionPoints(owner, m)));
    }

    /**
     * Constructs new instance.
     * 
     * @param m injectable method
     * @param instance component instance
     */
    public InjectableMethod(Method m, Object instance, Producer<T> owner, CreationalContextImpl<T> creationalContext, Set<InjectionPoint> ips)
    {
        super(owner,creationalContext);
        method = m;
        ownerInstance = instance;
        injectionPoints = ips;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.webbeans.inject.Injectable#doInjection()
     */
    public T doInjection()
    {
        Object owner = ownerInstance;
        if (owner instanceof OwbNormalScopeProxy)
        {
            owner = NormalScopeProxyFactory.unwrapInstance(owner);
        }

        List<Object> list = new ArrayList<>();
                
        
        for(int i=0;i<injectionPoints.size();i++)
        {
            for(InjectionPoint point : injectionPoints)
            {                
                AnnotatedParameter<?> parameter = (AnnotatedParameter<?>)point.getAnnotated();
                if (parameter.getPosition() == i)
                {
                    if (point.isDelegate())
                    {
                        list.add(creationalContext.getDelegate());
                        break;
                    }

                    boolean injectionPoint = false;
                    if(getBean() instanceof ProducerMethodBean)
                    {
                        if(parameter.getBaseType().equals(InjectionPoint.class))
                        {
                            BeanManager manager = getWebBeansContext().getBeanManagerImpl();
                            Object reference = manager.getInjectableReference(point, creationalContext);
                            list.add(reference);
                            injectionPoint = true;
                        }
                    }
                    
                    if(!injectionPoint)
                    {
                        if(isDisposable() && parameter.getAnnotation(Disposes.class) != null)
                        {
                            list.add(producerMethodInstance);
                        }
                        else
                        {
                            Object instance = inject(point);
                            InjectionResolver injectionResolver = getWebBeansContext().getBeanManagerImpl().getInjectionResolver();

                            Bean<?> injectedBean = injectionResolver.getInjectionPointBean(point);
                            if(injectedBean.getScope() == Dependent.class)
                            {
                                dependentParameters.put(injectedBean, instance);
                            }

                            list.add(instance);    
                        }                        
                    }
                                        
                    break;
                }
            }
        }        
        
        try
        {
            if (!method.isAccessible())
            {
                getWebBeansContext().getSecurityService().doPrivilegedSetAccessible(method, true);
            }

            return (T) method.invoke(owner, list.toArray(new Object[list.size()]));

        }
        catch (Exception e)
        {
            throw new WebBeansException(e);
        }
        finally
        {
            transientCreationalContext.release();
        }
    }

    //X TODO is this still needed? If not we can also drop the whole dependentParameters...
    public Map<Bean<?>,Object> getDependentBeanParameters()
    {
        return dependentParameters;
    }

    /**
     * @return the disposable
     */
    private boolean isDisposable()
    {
        return disposable;
    }

    /**
     * @param disposable the disposable to set
     */
    public void setDisposable(boolean disposable)
    {
        this.disposable = disposable;
    }
    
    public void setProducerMethodInstance(Object instance)
    {
        producerMethodInstance = instance;
    }
}
