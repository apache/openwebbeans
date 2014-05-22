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
package org.apache.webbeans.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;

import org.junit.Test;

public class GenericsUtilTest {

    @Test
    public void resolveType() throws NoSuchFieldException {
        Field field = AbstractObject.class.getDeclaredField("field");
        assertEquals(Object.class, GenericsUtil.resolveType(SimpleObject.class, field));
        assertEquals(String.class, GenericsUtil.resolveType(StringObject.class, field));

        Type t = GenericsUtil.resolveType(GenericObject.class, field);
        assertTrue(t instanceof TypeVariable);
        assertEquals("T", ((TypeVariable) t).getName());

        //X TODO assertEquals(Number.class, GenericsUtil.resolveType(GenericNumberObject.class, field));

        assertEquals(Integer.class, GenericsUtil.resolveType(IntegerObject.class, field));
        assertEquals(Object[].class, GenericsUtil.resolveType(GenericArrayObject.class, field));
        assertEquals(Long[].class, GenericsUtil.resolveType(LongArrayObject.class, field));
        //X TODO assertEquals(SubInterface.class, GenericsUtil.resolveType(InterfaceObject.class, field));
    }

    public static abstract class AbstractObject<V> {
    
        private V field;
    }

    private static class SimpleObject extends AbstractObject {

    }

    private static class StringObject extends AbstractObject<String> {

    }

    private static class GenericObject<T, V> extends AbstractObject<T> {

    }

    private static class GenericNumberObject<X, Y extends Number & Comparable<Integer>> extends AbstractObject<Y> {

    }

    private static class IntegerObject extends GenericNumberObject<String, Integer> {

    }

    private static class GenericArrayObject<V> extends GenericObject<V[], V> {

    }

    private static class LongArrayObject extends GenericArrayObject<Long> {

    }

    private static class InterfaceObject<V extends SubInterface & SuperInterface> extends AbstractObject<V> {

    }

    private interface SuperInterface {

    }

    private interface SubInterface extends SuperInterface {

    }
}
