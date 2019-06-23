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
package org.apache.webbeans.test.util;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.apache.webbeans.config.OwbParametrizedTypeImpl;
import org.apache.webbeans.util.GenericsUtil;
import org.junit.Assert;
import org.junit.Test;

public class GenericsUtilTest {
    @Test
    public void stackOverFlowProtection() throws Exception
    {
        final Type type = new OwbParametrizedTypeImpl(null, ByDefaultIllDoAStackOverFlowIngenericUtils.class, ByDefaultIllDoAStackOverFlowIngenericUtils.class.getTypeParameters());
        final Collection<Method> methods = new ArrayList<Method>(asList(ByDefaultIllDoAStackOverFlowIngenericUtils.class.getMethods()));
        methods.removeAll(asList(Object.class.getMethods()));
        final Type actualType = methods.iterator().next().getTypeParameters()[0];
        GenericsUtil.resolveTypes(new Type[] { actualType }, type);
    }

    @Test
    public void resolveType() throws NoSuchFieldException {
        Field field = AbstractObject.class.getDeclaredField("field");
        Assert.assertEquals(Object.class, GenericsUtil.resolveType(SimpleObject.class, field));
        assertEquals(String.class, GenericsUtil.resolveType(StringObject.class, field));

        Type t = GenericsUtil.resolveType(GenericObject.class, field);
        assertTrue(t instanceof TypeVariable);
        assertEquals("T", ((TypeVariable<?>)t).getName());

        Type number = GenericsUtil.resolveType(GenericNumberObject.class, field);
        assertTrue(number instanceof TypeVariable);
        assertEquals(Number.class, GenericsUtil.getRawType(number));

        assertEquals(Integer.class, GenericsUtil.resolveType(IntegerObject.class, field));
        GenericArrayType genericArrayType = (GenericArrayType)GenericsUtil.resolveType(GenericArrayObject.class, field);
        assertTrue(genericArrayType.getGenericComponentType() instanceof TypeVariable);
        assertEquals("V", ((TypeVariable)genericArrayType.getGenericComponentType()).getName());
        assertEquals(Object[].class, GenericsUtil.getRawType(genericArrayType));
        assertEquals(Long[].class, GenericsUtil.resolveType(LongArrayObject.class, field));
        
        Type subInterfaceType = GenericsUtil.resolveType(InterfaceObject.class, field);
        assertTrue(subInterfaceType instanceof TypeVariable);
        assertEquals(SubInterface.class, GenericsUtil.getRawType(subInterfaceType));

        assertEquals(JustAPlainClass.class, GenericsUtil.getRawType(JustAPlainClass.class));

        Set<Type> typeClosure = GenericsUtil.getTypeClosure(JustAPlainClass.class);
        assertEquals(2, typeClosure.size());
    }

    @Test
    public void testContainsWildcardTypes() throws Exception
    {
        Assert.assertFalse(GenericsUtil.containsWildcardType(StringObject.class));
        Assert.assertTrue(GenericsUtil.containsWildcardType(GenericNumberObject.class.getMethod("getObject").getGenericReturnType()));
        Assert.assertFalse(GenericsUtil.containsWildcardType(GenericObject.class.getMethod("getObject").getGenericReturnType()));
    }

    @Test
    public void genericsLoop()
    {
        final ParameterizedType injectionPointType = new OwbParametrizedTypeImpl(null, GenericFoo.class, Long.class);
        final TypeVariable<Class<?>> t = new TypeVariable<Class<?>>() {
            @Override
            public Type[] getBounds() {
                final TypeVariable<?> ref = this;
                return new Type[]{
                    new OwbParametrizedTypeImpl(null, Comparable.class, new TypeVariable<Class<?>>() {
                        @Override
                        public <T extends Annotation> T getAnnotation(Class<T> annotationClass)
                        {
                            return null;
                        }

                        @Override
                        public Annotation[] getAnnotations()
                        {
                            return new Annotation[0];
                        }

                        @Override
                        public Annotation[] getDeclaredAnnotations()
                        {
                            return new Annotation[0];
                        }

                        @Override
                        public Type[] getBounds()
                        {
                            return new Type[] { new OwbParametrizedTypeImpl(null, Comparable.class, ref) };
                        }

                        @Override
                        public Class<?> getGenericDeclaration() {
                            return GenericFoo.class;
                        }

                        @Override
                        public String getName()
                        {
                            return "T";
                        }

                        @Override
                        public AnnotatedType[] getAnnotatedBounds()
                        {
                            return new AnnotatedType[0];
                        }
                    })
                };
            }

            @Override
            public Class<?> getGenericDeclaration()
            {
                return GenericFoo.class;
            }

            @Override
            public String getName()
            {
                return "T";
            }

            @Override
            public AnnotatedType[] getAnnotatedBounds()
            {
                return new AnnotatedType[0];
            }

            @Override
            public <T extends Annotation> T getAnnotation(Class<T> annotationClass)
            {
                return null;
            }

            @Override
            public Annotation[] getAnnotations()
            {
                return new Annotation[0];
            }

            @Override
            public Annotation[] getDeclaredAnnotations()
            {
                return new Annotation[0];
            }
        };
        final ParameterizedType beanType = new OwbParametrizedTypeImpl(null, GenericFoo.class, t);
        assertFalse(GenericsUtil.satisfiesDependency(false, false, injectionPointType, beanType, new HashMap<>()));
    }

    public static abstract class AbstractObject<V>
    {
    
        private V field;
    }

    private static class SimpleObject extends AbstractObject
    {

    }

    private static class StringObject extends AbstractObject<String>
    {

    }

    private static class GenericObject<T, V> extends AbstractObject<T>
    {

        public GenericObject<T, V> getObject()
        {
            return new GenericObject<T, V>();
        }

    }

    private static class GenericNumberObject<X, Y extends Number & Comparable<Integer>> extends AbstractObject<Y>
    {

        public GenericNumberObject<?, ? extends Comparable<Integer>> getObject()
        {
            return new GenericNumberObject<X, Integer>();
        }
    }

    private static class IntegerObject extends GenericNumberObject<String, Integer>
    {

    }

    private static class GenericArrayObject<V> extends GenericObject<V[], V>
    {

    }

    private static class LongArrayObject extends GenericArrayObject<Long>
    {

    }

    private static class InterfaceObject<V extends SubInterface & SuperInterface> extends AbstractObject<V>
    {

    }

    private interface SuperInterface
    {

    }

    private interface SubInterface extends SuperInterface
    {

    }

    private static class JustAPlainClass
    {

    }

    public static class Methods
    {
        public AbstractObject raw()
        {
            return new AbstractObject() {
            };
        }

        public <T> AbstractObject<T> generic()
        {
            return new AbstractObject<T>() {
            };
        }
    }

    public static class ByDefaultIllDoAStackOverFlowIngenericUtils<C, R>
    {
        public <P extends Comparable<? super P>> ByDefaultIllDoAStackOverFlowIngenericUtils<C, R> foo(
                final ByDefaultIllDoAStackOverFlowIngenericUtils<? super C, P> att, P value)
        {
            return this;
        }
    }

    public interface GenericFoo<T extends Comparable<T>>
    {
        T someMethod();
    }

    public static class FooImpl<T extends Comparable<T>> implements GenericFoo<T>
    {
        @Override
        public T someMethod()
        {
            return null;
        }
    }

    public static class Bar
    {
        GenericFoo<Long> foo;
    }
}
