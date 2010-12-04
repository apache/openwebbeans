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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.enterprise.inject.spi.AnnotatedConstructor;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;

import org.apache.webbeans.config.OWBLogConst;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.logger.WebBeansLogger;
import org.apache.webbeans.util.Asserts;
import org.apache.webbeans.util.SecurityUtil;

/**
 * Factory for {@link javax.enterprise.inject.spi.Annotated} elements.
 * 
 * @version $Rev$ $Date$
 */
public final class AnnotatedElementFactory
{

    // Logger instance
    private final WebBeansLogger logger = WebBeansLogger.getLogger(AnnotatedElementFactory.class);

    @Deprecated
    public static AnnotatedElementFactory getInstance()
    {
        return WebBeansContext.getInstance().getAnnotatedElementFactory();
    }

    //Cache of the AnnotatedType
    private ConcurrentMap<Class<?>, AnnotatedType<?>> annotatedTypeCache =
        new ConcurrentHashMap<Class<?>, AnnotatedType<?>>();
    
    //Cache of AnnotatedConstructor
    private ConcurrentMap<Constructor<?>, AnnotatedConstructor<?>> annotatedConstructorCache =
        new ConcurrentHashMap<Constructor<?>, AnnotatedConstructor<?>>();
    
    //Cache of AnnotatedMethod
    private ConcurrentMap<Method, AnnotatedMethod<?>> annotatedMethodCache =
        new ConcurrentHashMap<Method, AnnotatedMethod<?>>();
    
    //Cache of AnnotatedField
    private ConcurrentMap<Field, AnnotatedField<?>> annotatedFieldCache =
        new ConcurrentHashMap<Field, AnnotatedField<?>>();
    
    /**
     * No instantiate.
     */
    public AnnotatedElementFactory()
    {
    }

    /**
     * Creates and configures new annotated type.
     * 
     * @param <X> class info
     * @param annotatedClass annotated class
     * @return new annotated type
     */
    @SuppressWarnings("unchecked")
    public <X> AnnotatedType<X> newAnnotatedType(Class<X> annotatedClass)
    {
        Asserts.assertNotNull(annotatedClass, "annotatedClass is null");
        AnnotatedTypeImpl<X> annotatedType = null;
        if(annotatedTypeCache.containsKey(annotatedClass))
        {
            annotatedType = (AnnotatedTypeImpl<X>)annotatedTypeCache.get(annotatedClass);
        }
        else
        {
            try
            {
                annotatedType = new AnnotatedTypeImpl<X>(annotatedClass);

                Field[] fields = SecurityUtil.doPrivilegedGetDeclaredFields(annotatedClass);
                Method[] methods = SecurityUtil.doPrivilegedGetDeclaredMethods(annotatedClass);
                Constructor<X>[] ctxs = (Constructor<X>[])SecurityUtil.doPrivilegedGetDeclaredConstructors(annotatedClass);
                for(Field f : fields)
                {
                    AnnotatedField<X> af = new AnnotatedFieldImpl<X>(f, annotatedType);
                    annotatedType.addAnnotatedField(af);
                }

                for(Method m : methods)
                {
                    AnnotatedMethod<X> am = new AnnotatedMethodImpl<X>(m,annotatedType);
                    annotatedType.addAnnotatedMethod(am);
                }

                for(Constructor<X> ct : ctxs)
                {
                    AnnotatedConstructor<X> ac = new AnnotatedConstructorImpl<X>(ct,annotatedType);
                    annotatedType.addAnnotatedConstructor(ac);
                }
                
                AnnotatedTypeImpl<X> oldType = (AnnotatedTypeImpl<X>)annotatedTypeCache.putIfAbsent(annotatedClass, annotatedType);
                if(oldType != null)
                {
                    annotatedType = oldType;
                }

            } 
            catch (Exception e)
            {
                if (e instanceof ClassNotFoundException || e instanceof ArrayStoreException)
                {
                    if (logger.wblWillLogError())
                    {
                        logger.error(OWBLogConst.ERROR_0027, e, annotatedClass.getName(), e.getCause());
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
                if (logger.wblWillLogError())
                {
                    logger.error(OWBLogConst.ERROR_0027, ncdfe, annotatedClass.getName(), ncdfe.getCause());
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
        Asserts.assertNotNull(constructor, "constructor is null");
        Asserts.assertNotNull(declaringClass, "declaringClass is null");
        
        AnnotatedConstructorImpl<X> annConstructor = null;
        if(annotatedConstructorCache.containsKey(constructor))
        {
            annConstructor = (AnnotatedConstructorImpl<X>)annotatedConstructorCache.get(constructor);
        }
        else
        {
            annConstructor = new AnnotatedConstructorImpl<X>(constructor, declaringClass);
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
        Asserts.assertNotNull(field, "field is null");
        Asserts.assertNotNull(declaringClass, "declaringClass is null");
        
        AnnotatedFieldImpl<X> annotField = null;
        if(annotatedFieldCache.containsKey(field))
        {
            annotField = (AnnotatedFieldImpl<X>)annotatedFieldCache.get(field);
        }
        else
        {
            annotField = new AnnotatedFieldImpl<X>(field, declaringClass);
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
        Asserts.assertNotNull(method, "method is null");
        Asserts.assertNotNull(declaringType, "declaringType is null");
        
        AnnotatedMethodImpl<X> annotMethod = null;
        if(annotatedMethodCache.containsKey(method))
        {
            annotMethod = (AnnotatedMethodImpl<X>)annotatedMethodCache.get(method);
        }
        else
        {
            annotMethod = new AnnotatedMethodImpl<X>(method, declaringType);
            AnnotatedMethodImpl<X> old = (AnnotatedMethodImpl<X>) annotatedMethodCache.putIfAbsent(method, annotMethod);
            if(old != null)
            {
                annotMethod = old;
            }
        }
        
        return annotMethod;          
    }
    
    /**
     * Clear caches.
     */
    public void clear()
    {
        annotatedTypeCache.clear();
        annotatedConstructorCache.clear();
        annotatedFieldCache.clear();
        annotatedMethodCache.clear();
    }
}
