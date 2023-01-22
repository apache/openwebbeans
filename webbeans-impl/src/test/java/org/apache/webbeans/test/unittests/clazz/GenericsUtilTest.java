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
package org.apache.webbeans.test.unittests.clazz;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.enterprise.util.TypeLiteral;

import static org.junit.Assert.*;

import org.apache.webbeans.util.GenericsUtil;
import org.junit.Test;

public class GenericsUtilTest
{
    @Test
    public void testStudent()
    {
        Set<Type> set = GenericsUtil.getTypeClosure(Student.class);

        assertEquals(6, set.size());
        assertTrue(set.contains(Student.class));
        assertTrue(set.contains(new TypeLiteral<AbstractSchool<Integer>>() {}.getType()));
        assertTrue(set.contains(new TypeLiteral<BaseSchool<String, Integer>>() {}.getType()));
        assertTrue(set.contains(new TypeLiteral<IBook<List<Integer>>>() {}.getType()));
        assertTrue(set.contains(new TypeLiteral<IBook2<Integer, String>>() {}.getType()));
        assertTrue(set.contains(Object.class));
    }
    
    @Test
    public void testAttribute() throws NoSuchFieldException, SecurityException
    {
    	Type type = GenericsUtil.resolveType(Student.class, AbstractSchool.class.getDeclaredField("attribute"));
    	assertEquals(Integer.class, type);
    }

    @Test
    public void testAbstractSchool()
    {
        Set<Type> set = GenericsUtil.getTypeClosure(new TypeLiteral<IBook2<Integer, String>>() {}.getType(), AbstractSchool.class);

        assertEquals(4, set.size());
        assertTrue(set.contains(new TypeLiteral<AbstractSchool<Integer>>() {}.getType()));
        assertTrue(set.contains(new TypeLiteral<BaseSchool<String, Integer>>() {}.getType()));
        assertTrue(set.contains(new TypeLiteral<IBook2<Integer, String>>() {}.getType()));
        assertTrue(set.contains(Object.class));
    }

    @Test
    public void testStudent2()
    {
        Set<Type> set = GenericsUtil.getTypeClosure(Student2.class);

        assertEquals(6, set.size());
        assertTrue(set.contains(Student2.class));
        assertTrue(set.contains(new TypeLiteral<IBook2<Long, String>>() {}.getType()));
        assertTrue(set.contains(new TypeLiteral<AbstractSchool2<List<String>, Map<String, String>>>() {}.getType()));
        assertTrue(set.contains(new TypeLiteral<AbstractSchool<Long>>() {}.getType()));
        assertTrue(set.contains(new TypeLiteral<BaseSchool<String, Long>>() {}.getType()));
        assertTrue(set.contains(Object.class));
    }

    @Test
    public void testStudent3()
    {
        Set<Type> set = GenericsUtil.getTypeClosure(new TypeLiteral<AbstractSchool<Integer>>() {}.getType(), Student3.class);

        assertEquals(5, set.size());
        assertTrue(set.contains(new TypeLiteral<Student3<Integer>>() {}.getType()));
        assertTrue(set.contains(new TypeLiteral<AbstractSchool<Integer>>() {}.getType()));
        assertTrue(set.contains(new TypeLiteral<BaseSchool<String, Integer>>() {}.getType()));
        assertTrue(set.contains(new TypeLiteral<IBook2<Integer, String>>() {}.getType()));
        assertTrue(set.contains(Object.class));
    }
}
