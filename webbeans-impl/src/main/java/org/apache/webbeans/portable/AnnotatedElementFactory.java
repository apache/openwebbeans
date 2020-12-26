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
package org.apache.webbeans.portable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.inject.spi.AnnotatedConstructor;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;

import org.apache.webbeans.config.OWBLogConst;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.logger.WebBeansLoggerFacade;
import org.apache.webbeans.util.Asserts;
import org.apache.webbeans.util.ClassUtil;

/**
 * Factory for {@link javax.enterprise.inject.spi.Annotated} elements.
 *
 * @version $Rev$ $Date$
 */
public final class AnnotatedElementFactory
{

    public static final String OWB_DEFAULT_KEY = "OWB_DEFAULT_KEY";

    /**
     * Cache of the initial AnnotatedTypes
     */
    private ConcurrentMap<Class<?>, ConcurrentMap<String, AnnotatedType<?>>> annotatedTypeCache =
        new ConcurrentHashMap<>();

    /**
     * Cache of modified AnnotatedTypes.
     */
    private ConcurrentMap<Class<?>, ConcurrentMap<String, AnnotatedType<?>>> modifiedAnnotatedTypeCache =
        new ConcurrentHashMap<>();

    //Cache of AnnotatedConstructor
    private ConcurrentMap<Constructor<?>, AnnotatedConstructor<?>> annotatedConstructorCache =
        new ConcurrentHashMap<>();

    //Cache of AnnotatedMethod
    private ConcurrentMap<Method, AnnotatedMethod<?>> annotatedMethodCache =
        new ConcurrentHashMap<>();

    //Cache of AnnotatedField
    private ConcurrentMap<Field, AnnotatedField<?>> annotatedFieldCache =
        new ConcurrentHashMap<>();

    //Cache of AnnotatedMethod
    private ConcurrentMap<AnnotatedType<?>, Set<AnnotatedMethod<?>>> annotatedMethodsOfTypeCache =
        new ConcurrentHashMap<>();

    private WebBeansContext webBeansContext;

    /**
     * No instantiate.
     */
    public AnnotatedElementFactory(WebBeansContext webBeansContext)
    {
        this.webBeansContext = webBeansContext;
    }

    /**
     * Get an already registered AnnotatedType. This will NOT create a new one!
     * The returned AnnotatedType will reflect all the changes made during the
     * boot process so far.
     * If there was no AnnotatedType created yet for the given Class,
     * <code>null</code> will be returned.
     */
    public <X> AnnotatedType<X> getAnnotatedType(Class<X> annotatedClass)
    {
        ConcurrentMap<String, AnnotatedType<?>> modifiedAnnotatedClasses = modifiedAnnotatedTypeCache.get(annotatedClass);
        if (modifiedAnnotatedClasses != null)
        {
            AnnotatedType<X> annotatedType = (AnnotatedType<X>) modifiedAnnotatedClasses.get(OWB_DEFAULT_KEY);
            if (annotatedType != null)
            {
                return annotatedType;
            }
        }
        return getAnnotatedTypeCache(annotatedClass).get(OWB_DEFAULT_KEY);
    }

    /**
     * Get all already registered AnnotatedTypes of the specified type. This will NOT create a new one!
     * @param annotatedClass
     * @param <X>
     * @return AnnotatedType
     */
    public <X> Iterable<AnnotatedType<X>> getAnnotatedTypes(Class<X> annotatedClass)
    {
        return getAnnotatedTypeCache(annotatedClass).values();
    }
    
    /**
     * This method will get used to manually add AnnoatedTypes to our storage.
     * Those AnnotatedTypes are coming from Extensions and get registered e.g. via
     * {@link javax.enterprise.inject.spi.BeforeBeanDiscovery#addAnnotatedType(AnnotatedType)}
     *
     * Sets the annotatedType and replace the given one.
     * @param annotatedType
     * @param <X>
     * @return the previously registered AnnotatedType or null if not previously defined.
     */
    public <X> AnnotatedType<X> setAnnotatedType(AnnotatedType<X> annotatedType)
    {
        return setAnnotatedType(annotatedType, OWB_DEFAULT_KEY);
    }

    public <X> AnnotatedType<X> setAnnotatedType(AnnotatedType<X> annotatedType, String id)
    {
        Class<X> type = annotatedType.getJavaClass();
        ConcurrentMap<String, AnnotatedType<?>> annotatedTypes = modifiedAnnotatedTypeCache.get(type);
        if (annotatedTypes == null)
        {
            annotatedTypes = new ConcurrentHashMap<>();
        }
        ConcurrentMap<String, AnnotatedType<?>> oldAnnotatedTypes = modifiedAnnotatedTypeCache.putIfAbsent(type, annotatedTypes);
        if (oldAnnotatedTypes != null)
        {
            annotatedTypes = oldAnnotatedTypes;
        }
        return (AnnotatedType<X>) annotatedTypes.put(id, annotatedType);
    }

    /**
     * Creates and configures a new annotated type.
     * This always returns the fresh AnnotatedTypes <b>without</b> any modifications
     * applied by Extensions!.
     *
     * To get any AnnotatedTypes which are modified during the boot process you shall use
     * {@link #getAnnotatedType(Class)}.
     * 
     * @param <X> class info
     * @param annotatedClass annotated class
     * @return new annotated type
     */
    public <X> AnnotatedType<X> newAnnotatedType(Class<X> annotatedClass)
    {
        Asserts.assertNotNull(annotatedClass, "annotatedClass");
        ConcurrentMap<String, AnnotatedType<X>> annotatedTypes = getAnnotatedTypeCache(annotatedClass);
        AnnotatedType<X> annotatedType = annotatedTypes.get(OWB_DEFAULT_KEY);
        if(annotatedType == null)
        {
            try
            {
                AnnotatedType<? super X> supertype = null;
                if (annotatedClass.getSuperclass() != null && !annotatedClass.getSuperclass().equals(Object.class))
                {
                    supertype = newAnnotatedType(annotatedClass.getSuperclass());
                }
                annotatedType = new AnnotatedTypeImpl<>(webBeansContext, annotatedClass, supertype);

                AnnotatedType<X> oldType = annotatedTypes.putIfAbsent(OWB_DEFAULT_KEY, annotatedType);
                if(oldType != null)
                {
                    annotatedType = oldType;
                }
            }
            catch (Exception e)
            {
                if (e instanceof ClassNotFoundException || e instanceof ArrayStoreException)
                {
                    final Logger logger = WebBeansLoggerFacade.getLogger(AnnotatedElementFactory.class);
                    if (logger.isLoggable(Level.SEVERE))
                    {
                        logger.log(Level.SEVERE, WebBeansLoggerFacade.constructMessage(OWBLogConst.ERROR_0027, annotatedClass.getName(), e.getCause()), e);
                    }

                    annotatedType = null;
                } 
                else
                {
                    throw new RuntimeException(e);
                }
            } 
            catch (NoClassDefFoundError ncdfe)
            {
                final Logger logger = WebBeansLoggerFacade.getLogger(AnnotatedElementFactory.class);
                if (logger.isLoggable(Level.SEVERE))
                {
                    logger.log(Level.SEVERE, WebBeansLoggerFacade.constructMessage(OWBLogConst.ERROR_0027, annotatedClass.getName(), ncdfe.getCause()), ncdfe);
                }

                annotatedType = null;
            }
        }
                
        return annotatedType;
    }

    /**
     * Creates and configures new annotated constructor.
     * 
     * @param <X> declaring class
     * @param constructor constructor
     * @return new annotated constructor
     */
    @SuppressWarnings("unchecked")
    public <X> AnnotatedConstructor<X> newAnnotatedConstructor(Constructor<X> constructor, AnnotatedType<X> declaringClass)
    {
        Asserts.assertNotNull(constructor, "constructor");
        Asserts.assertNotNull(declaringClass, "declaringClass");
        
        AnnotatedConstructorImpl<X> annConstructor;
        if(annotatedConstructorCache.containsKey(constructor))
        {
            annConstructor = (AnnotatedConstructorImpl<X>)annotatedConstructorCache.get(constructor);
        }
        else
        {
            annConstructor = new AnnotatedConstructorImpl<>(webBeansContext, constructor, declaringClass);
            AnnotatedConstructorImpl<X> old = (AnnotatedConstructorImpl<X>)annotatedConstructorCache.putIfAbsent(constructor, annConstructor);
            if(old != null)
            {
                annConstructor = old;
            }
        }
        
        return annConstructor;
    }

    /**
     * Creates and configures new annotated field.
     * 
     * @param <X> declaring class
     * @param field field instance
     * @param declaringClass declaring class
     * @return new annotated field
     */
    @SuppressWarnings("unchecked")
    public <X> AnnotatedField<X> newAnnotatedField(Field field, AnnotatedType<X> declaringClass)
    {
        Asserts.assertNotNull(field, "field");
        Asserts.assertNotNull(declaringClass, "declaringClass");
        
        AnnotatedFieldImpl<X> annotField;
        if(annotatedFieldCache.containsKey(field))
        {
            annotField = (AnnotatedFieldImpl<X>)annotatedFieldCache.get(field);
        }
        else
        {
            annotField = new AnnotatedFieldImpl<>(webBeansContext, field, declaringClass);
            AnnotatedFieldImpl<X> old = (AnnotatedFieldImpl<X>) annotatedFieldCache.putIfAbsent(field, annotField);
            if(old != null)
            {
                annotField = old;
            }
        }
        
        return annotField; 
    }

    /**
     * Creates and configures new annotated method.
     * 
     * @param <X> declaring class
     * @param method annotated method
     * @param declaringType declaring class info
     * @return new annotated method
     */
    @SuppressWarnings("unchecked")
    public <X> AnnotatedMethod<X> newAnnotatedMethod(Method method, AnnotatedType<X> declaringType)
    {
        Asserts.assertNotNull(method, "method");
        Asserts.assertNotNull(declaringType, "declaringType");
        
        AnnotatedMethodImpl<X> annotMethod;
        if(annotatedMethodCache.containsKey(method))
        {
            annotMethod = (AnnotatedMethodImpl<X>)annotatedMethodCache.get(method);
        }
        else
        {
            annotMethod = new AnnotatedMethodImpl<>(webBeansContext, method, declaringType);
            AnnotatedMethodImpl<X> old = (AnnotatedMethodImpl<X>) annotatedMethodCache.putIfAbsent(method, annotMethod);
            if(old != null)
            {
                annotMethod = old;
            }
        }
        
        return annotMethod;          
    }
    
    /**
     * Returns the {@link AnnotatedMethod}s of the specified {@link AnnotatedType},
     * filtering out the overridden methods.
     */
    public <T> Set<AnnotatedMethod<? super T>> getFilteredAnnotatedMethods(AnnotatedType<T> annotatedType)
    {
        Asserts.assertNotNull(annotatedType, "annotatedType");

        Set<AnnotatedMethod<?>> methods = annotatedMethodsOfTypeCache.get(annotatedType);
        if (methods != null)
        {
            return cast(methods);
        }
        methods = Collections.unmodifiableSet(getFilteredMethods(annotatedType.getJavaClass(),
                                                                 (Set)annotatedType.getMethods(),
            new HashSet<>()));
        Set<AnnotatedMethod<?>> old = annotatedMethodsOfTypeCache.putIfAbsent(annotatedType, methods);
        if (old != null)
        {
            return cast(old);
        }
        return cast(methods);
    }
    
    private <T> Set<AnnotatedMethod<? super T>> cast(Set<AnnotatedMethod<?>> methods)
    {
        return (Set<AnnotatedMethod<? super T>>)(Set<?>)methods;
    }

    /**
     * Clear caches.
     */
    public void clear()
    {
        modifiedAnnotatedTypeCache.clear();
        annotatedTypeCache.clear();
        annotatedConstructorCache.clear();
        annotatedFieldCache.clear();
        annotatedMethodCache.clear();
        annotatedMethodsOfTypeCache.clear();
    }
    
    private Set<? extends AnnotatedMethod<?>> getFilteredMethods(Class<?> type, Set<AnnotatedMethod<?>> allMethods, Set<AnnotatedMethod<?>> filteredMethods)
    {
        if (type == null)
        {
            return filteredMethods;
        }
        for (AnnotatedMethod<?> annotatedMethod: allMethods)
        {
            if (annotatedMethod.getJavaMember().getDeclaringClass() == type && !isOverridden(annotatedMethod, filteredMethods))
            {
                filteredMethods.add(annotatedMethod);
            }
        }
        return getFilteredMethods(type.getSuperclass(), allMethods, filteredMethods);
    }

    private boolean isOverridden(AnnotatedMethod<?> superclassMethod, Set<AnnotatedMethod<?>> methods)
    {
        for (AnnotatedMethod<?> subclassMethod : methods)
        {
            if (ClassUtil.isOverridden(subclassMethod.getJavaMember(), superclassMethod.getJavaMember()))
            {
                return true;
            }
        }
        return false;
    }

    private <T> ConcurrentMap<String, AnnotatedType<T>> getAnnotatedTypeCache(Class<T> type)
    {
        ConcurrentMap<String, AnnotatedType<?>> annotatedTypes = annotatedTypeCache.get(type);
        if (annotatedTypes == null)
        {
            annotatedTypes = new ConcurrentHashMap<>();
            ConcurrentMap<String, AnnotatedType<?>> oldAnnotatedTypes = annotatedTypeCache.putIfAbsent(type, annotatedTypes);
            if (oldAnnotatedTypes != null)
            {
                annotatedTypes = oldAnnotatedTypes;
            }
        }
        return ConcurrentMap.class.cast(annotatedTypes);
    }
}
