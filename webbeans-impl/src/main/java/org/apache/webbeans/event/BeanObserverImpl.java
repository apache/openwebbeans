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
package org.apache.webbeans.event;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import javax.context.Context;
import javax.context.Dependent;
import javax.event.Observer;
import javax.event.Observes;
import javax.inject.manager.Manager;

import org.apache.webbeans.annotation.CurrentLiteral;
import org.apache.webbeans.component.AbstractComponent;
import org.apache.webbeans.component.ObservesMethodsOwner;
import org.apache.webbeans.container.InjectionResolver;
import org.apache.webbeans.container.activity.ActivityManager;
import org.apache.webbeans.context.ContextFactory;
import org.apache.webbeans.exception.WebBeansException;
import org.apache.webbeans.logger.WebBeansLogger;
import org.apache.webbeans.util.AnnotationUtil;
import org.apache.webbeans.util.WebBeansUtil;

public class BeanObserverImpl<T> implements Observer<T>
{
    private WebBeansLogger logger = WebBeansLogger.getLogger(BeanObserverImpl.class);

    private ObservesMethodsOwner<?> bean;

    private Method observerMethod;

    private boolean ifExist;

    private TransactionalObserverType type;

    public BeanObserverImpl(ObservesMethodsOwner<?> bean, Method observerMethod, boolean ifExist, TransactionalObserverType type)
    {
        this.bean = bean;
        this.observerMethod = observerMethod;
        this.ifExist = ifExist;
        this.type = type;
    }

    @SuppressWarnings("unchecked")
    public void notify(T event)
    {
        AbstractComponent<Object> baseComponent = (AbstractComponent<Object>) bean;

        AbstractComponent<Object> specializedComponent = null;

        Object object = null;

        boolean dependentContext = false;

        try
        {
            if (!ContextFactory.checkDependentContextActive())
            {
                ContextFactory.activateDependentContext();
                dependentContext = true;
            }

            Manager manager = ActivityManager.getInstance().getCurrentActivity();

            specializedComponent = WebBeansUtil.getMostSpecializedBean(manager, baseComponent);
            
            Context context = manager.getContext(specializedComponent.getScopeType());
            
            if(this.ifExist)
            {
                object = context.get(specializedComponent);
            }
            else
            {
                object = manager.getInstance(specializedComponent);    
            }
            

            if (this.ifExist && object == null)
            {
                return;
            }

            if (object != null)
            {
                Object[] args = null;

                List<Object> argsObjects = getMethodArguments(event);

                args = new Object[argsObjects.size()];

                args = argsObjects.toArray(args);

                if (!this.observerMethod.isAccessible())
                {
                    this.observerMethod.setAccessible(true);
                }

                if (Modifier.isStatic(this.observerMethod.getModifiers()))
                {
                    object = null;
                }

                this.observerMethod.invoke(object, args);
            }

        }
        catch (Exception e)
        {
            if (!getType().equals(TransactionalObserverType.NONE))
            {
                logger.error("Error is occured while notifying observer in class : " + observerMethod.getDeclaringClass().getName() + " in method : " + observerMethod.getName(), e);

            }
            else
            {
                throw new WebBeansException(e.getCause());
            }
        }
        finally
        {
            if (baseComponent.getScopeType().equals(Dependent.class))
            {
                baseComponent.destroy(object);
            }

            if (dependentContext)
            {
                ContextFactory.passivateDependentContext();
            }
        }

    }

    protected List<Object> getMethodArguments(Object event)
    {
        Type[] types = this.observerMethod.getGenericParameterTypes();

        Annotation[][] annots = this.observerMethod.getParameterAnnotations();

        List<Object> list = new ArrayList<Object>();

        Manager manager = ActivityManager.getInstance().getCurrentActivity();

        if (types.length > 0)
        {
            int i = 0;
            for (Type type : types)
            {
                Annotation[] annot = annots[i];

                boolean observesAnnotation = false;

                if (annot.length == 0)
                {
                    annot = new Annotation[1];
                    annot[0] = new CurrentLiteral();
                }
                else
                {
                    for (Annotation observersAnnot : annot)
                    {
                        if (observersAnnot.annotationType().equals(Observes.class))
                        {
                            list.add(event);
                            observesAnnotation = true;
                            break;
                        }
                    }
                }

                if (!observesAnnotation)
                {
                    Type[] args = new Type[0];
                    Class<?> clazz = null;
                    if (type instanceof ParameterizedType)
                    {
                        ParameterizedType pt = (ParameterizedType) type;
                        args = pt.getActualTypeArguments();

                        clazz = (Class<?>) pt.getRawType();
                    }
                    else
                    {
                        clazz = (Class<?>) type;
                    }

                    Annotation[] bindingTypes = AnnotationUtil.getBindingAnnotations(annot);

                    if (bindingTypes.length > 0)
                    {
                        list.add(manager.getInstance(InjectionResolver.getInstance().implResolveByType(clazz, args, bindingTypes).iterator().next()));
                    }
                    else
                    {
                        list.add(null);
                    }

                }

                i++;
            }
        }

        return list;
    }

    /**
     * @return the bean
     */
    public ObservesMethodsOwner<?> getBean()
    {
        return bean;
    }

    /**
     * @return the type
     */
    public TransactionalObserverType getType()
    {
        return type;
    }

}
