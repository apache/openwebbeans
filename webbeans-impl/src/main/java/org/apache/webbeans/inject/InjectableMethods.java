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
package org.apache.webbeans.inject;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionPoint;

import org.apache.webbeans.annotation.DefaultLiteral;
import org.apache.webbeans.component.AbstractBean;
import org.apache.webbeans.component.ProducerMethodBean;
import org.apache.webbeans.container.BeanManagerImpl;
import org.apache.webbeans.exception.WebBeansException;

@SuppressWarnings("unchecked")
public class InjectableMethods<T> extends AbstractInjectable
{
    /** Injectable method */
    protected Method method;

    /** Component instance that owns the method */
    protected Object instance;
    
    private boolean disposable;
    
    private Object producerMethodInstance = null;

    /**
     * Constructs new instance.
     * 
     * @param m injectable method
     * @param instance component instance
     */
    public InjectableMethods(Method m, Object instance, AbstractBean<?> owner,CreationalContext<?> creationalContext)
    {
        super(owner,creationalContext);
        this.method = m;
        this.instance = instance;
        this.injectionMember = m;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.webbeans.inject.Injectable#doInjection()
     */
    public T doInjection()
    {
        List<InjectionPoint> injectedPoints = getInjectedPoints(this.method);        
        List<Object> list = new ArrayList<Object>();
                
        
        for(int i=0;i<injectedPoints.size();i++)
        {
            for(InjectionPoint point : injectedPoints)
            {
                AnnotatedParameter<?> parameter = (AnnotatedParameter<?>)point.getAnnotated();
                if(parameter.getPosition() == i)
                {
                    boolean injectionPoint = false;
                    if(this.injectionOwnerBean instanceof ProducerMethodBean)
                    {
                        if(parameter.getBaseType().equals(InjectionPoint.class))
                        {
                            BeanManager manager = BeanManagerImpl.getManager();
                            Bean<?> injectionPointBean = manager.getBeans(InjectionPoint.class, new DefaultLiteral()).iterator().next();
                            Object reference = manager.getReference(injectionPointBean, InjectionPoint.class, manager.createCreationalContext(injectionPointBean));
                            
                            list.add(reference);
                            
                            injectionPoint = true;
                        }
   
                    }
                    
                    if(!injectionPoint)
                    {
                        if(isDisposable() && parameter.getAnnotation(Disposes.class) != null)
                        {
                            list.add(this.producerMethodInstance);
                        }
                        else
                        {
                            list.add(inject(point));    
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
                method.setAccessible(true);
            }

            return (T) method.invoke(instance, list.toArray());

        }
        catch (Exception e)
        {
            throw new WebBeansException(e);
        }
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
        this.producerMethodInstance = instance;
    }
}