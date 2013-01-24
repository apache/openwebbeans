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
package org.apache.webbeans.component.creation;

import static org.apache.webbeans.util.InjectionExceptionUtil.throwUnproxyableResolutionException;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.AnnotatedMember;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.Bean;

import org.apache.webbeans.component.BeanAttributesImpl;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.exception.helper.ViolationMessageBuilder;
import org.apache.webbeans.util.Asserts;
import org.apache.webbeans.util.SecurityUtil;

/**
 * Abstract implementation.
 * 
 * @version $Rev$ $Date$
 *
 * @param <T> bean class info
 */
@SuppressWarnings({"ALL", "JavaDoc"})
public abstract class AbstractBeanBuilder<T, A extends Annotated, B extends Bean<T>>
{
    private A annotated;
    
    private WebBeansContext webBeansContext;

    private BeanAttributesImpl<T> beanAttributes;

    private Set<AnnotatedMember<? super T>> injectionPoints = new HashSet<AnnotatedMember<? super T>>();

    /**
     * Creates a bean instance.
     * 
     * @param annotated
     */
    public AbstractBeanBuilder(WebBeansContext webBeansContext, A annotated, BeanAttributesImpl<T> beanAttributes)
    {
        Asserts.assertNotNull(webBeansContext, "webBeansContext may not be null");
        Asserts.assertNotNull(annotated, "annotated may not be null");
        Asserts.assertNotNull(beanAttributes, "beanAttributes may not be null");
        this.annotated = annotated;
        this.webBeansContext = webBeansContext;
        this.beanAttributes = beanAttributes;
    }

    public BeanAttributesImpl<T> getBeanAttributes()
    {
        return beanAttributes;
    }

    /**
     * {@inheritDoc}
     */
    public void checkCreateConditions()
    {
        //Sub-class can override this
    }

    /**
     * Check if the given annotatedMethod overrides some previously defined AnnotatedMethods
     * from a superclass and remove them if non-private.
     *
     *
     * @param alreadyDefinedMethods the methods already calculated from the superclasses. See
     * {@link org.apache.webbeans.intercept.InterceptorUtil#getReverseClassHierarchy(Class)}
     * @param annotatedMethod the AnnotatedMethod to check for.
     * @return <code>true</code> if a method was overridden and got removed, <code>false</code> otherwise.
     */
    protected boolean removeOverriddenMethod(List<AnnotatedMethod> alreadyDefinedMethods, AnnotatedMethod annotatedMethod)
    {
        String methodName = null;
        Class<?>[] methodParameterTypes = null;

        Iterator<AnnotatedMethod> it = alreadyDefinedMethods.iterator();
        while (it.hasNext())
        {
            AnnotatedMethod alreadyDefined = it.next();

            if (alreadyDefined == annotatedMethod)
            {
                // we don't remove ourself
                continue;
            }

            if (methodName == null)
            {
                methodName = annotatedMethod.getJavaMember().getName();
                methodParameterTypes = annotatedMethod.getJavaMember().getParameterTypes();
            }

            // check method overrides
            if (!Modifier.isPrivate(alreadyDefined.getJavaMember().getModifiers()))
            {
                // we only scan non-private methods, as private methods cannot get overridden.
                if (methodName.equals(alreadyDefined.getJavaMember().getName()) &&
                        methodParameterTypes.length == alreadyDefined.getJavaMember().getParameterTypes().length)
                {
                    boolean overridden = true;
                    // same name and param length so we need to check if all the paramTypes are equal.
                    if (methodParameterTypes.length > 0)
                    {
                        Class<?>[] otherParamTypes = alreadyDefined.getJavaMember().getParameterTypes();

                        for (int i = 0; i < otherParamTypes.length; i++)
                        {
                            if (!otherParamTypes[i].equals(methodParameterTypes[i]))
                            {
                                overridden = false;
                                break;
                            }
                        }
                    }

                    if (overridden)
                    {
                        // then we need to remove this method
                        it.remove();
                        return true;
                    }
                }
            }
        }

        return false;
    }

    protected void addInjectionPoint(AnnotatedMember<? super T> member)
    {
        injectionPoints.add(member);
    }

    /**
     * @return the AnnotatedMember of all found injection points <i>before</i> InjectionPoint will be constructed from it.
     */
    protected Set<AnnotatedMember<? super T>> getInjectionPointsAnnotated()
    {
        return injectionPoints;
    }

    /**
     * Checks the unproxiable condition.
     * @throws org.apache.webbeans.exception.WebBeansConfigurationException if bean is not proxied by the container
     */
    protected void checkUnproxiableApiType()
    {
        //Unproxiable test for NormalScoped beans
        if (webBeansContext.getBeanManagerImpl().isNormalScope(beanAttributes.getScope()))
        {
            ViolationMessageBuilder violationMessage = ViolationMessageBuilder.newViolation();

            Class<?> beanClass = getBeanType();
            
            if(!beanClass.isInterface() && beanClass != Object.class)
            {
                if(beanClass.isPrimitive())
                {
                    violationMessage.addLine("It isn't possible to proxy a primitive type (" + beanClass.getName(), ")");
                }

                if(beanClass.isArray())
                {
                    violationMessage.addLine("It isn't possible to proxy an array type (", beanClass.getName(), ")");
                }

                if(!violationMessage.containsViolation())
                {
                    if (Modifier.isFinal(beanClass.getModifiers()))
                    {
                        violationMessage.addLine(beanClass.getName(), " is a final class! CDI doesn't allow to proxy that.");
                    }

                    Method[] methods = SecurityUtil.doPrivilegedGetDeclaredMethods(beanClass);
                    for (Method m : methods)
                    {
                        int modifiers = m.getModifiers();
                        if (Modifier.isFinal(modifiers) && !Modifier.isPrivate(modifiers) &&
                            !m.isSynthetic() && !m.isBridge())
                        {
                            violationMessage.addLine(beanClass.getName(), " has final method "+ m + " CDI doesn't allow to proxy that.");
                        }
                    }

                    Constructor<?> cons = webBeansContext.getWebBeansUtil().getNoArgConstructor(beanClass);
                    if (cons == null)
                    {
                        violationMessage.addLine(beanClass.getName(), " has no explicit no-arg constructor!",
                                "A public or protected constructor without args is required!");
                    }
                    else if (Modifier.isPrivate(cons.getModifiers()))
                    {
                        violationMessage.addLine(beanClass.getName(), " has a >private< no-arg constructor! CDI doesn't allow to proxy that.");
                    }
                }

                //Throw Exception
                if(violationMessage.containsViolation())
                {
                    throwUnproxyableResolutionException(violationMessage);
                }
            }
        }
    }

    protected abstract B createBean(Class<T> returnType);

    /**
     * {@inheritDoc}
     */
    public B getBean()
    {
        return createBean(getBeanType());
    }

    protected A getAnnotated()
    {
        return annotated;
    }
    
    protected abstract Class<T> getBeanType();
}
