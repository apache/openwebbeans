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
package org.apache.webbeans.test.unittests.inject.parametrized;

import java.lang.reflect.Field;
import java.lang.reflect.Type;

import org.apache.webbeans.test.injection.generics.zoo.Horse;
import org.apache.webbeans.test.injection.generics.zoo.HorseStable;
import org.apache.webbeans.test.injection.generics.zoo.Pig;
import org.apache.webbeans.test.injection.generics.zoo.PigStable;
import org.apache.webbeans.test.injection.generics.zoo.Stable;
import org.apache.webbeans.test.component.inject.parametrized.Dao;
import org.apache.webbeans.test.component.inject.parametrized.UserDao;
import org.apache.webbeans.test.component.inject.parametrized.WithTypeVariable;
import org.apache.webbeans.util.GenericsUtil;
import org.junit.Assert;
import org.junit.Test;

public class GenericClassTest
{
    @Test
    public void testGenericClasses() throws Exception
    {
        Field t = Dao.class.getField("t");
        Field raw = Dao.class.getField("raw");
        Field check22 = Dao.class.getField("check22");
        Field check22Bound = Dao.class.getField("check22WithBound");
        Field check4 = WithTypeVariable.class.getField("check4");

        Assert.assertFalse(GenericsUtil.satisfiesDependency(false, raw.getGenericType(), t.getGenericType()));
        Assert.assertTrue(GenericsUtil.satisfiesDependency(false, check4.getGenericType(), t.getGenericType()));
        Assert.assertTrue(GenericsUtil.satisfiesDependency(false, check22.getGenericType(), t.getGenericType()));
        Assert.assertTrue(GenericsUtil.satisfiesDependency(false, check22Bound.getGenericType(), t.getGenericType()));
    }
    
    @Test
    public void testGenericClasses2() throws Exception
    {
        Field f1 = UserDao.class.getField("field1");
        Field f2 = UserDao.class.getField("field2");
        Field f3 = UserDao.class.getField("field3");
        Field f4 = UserDao.class.getField("field4");


        Assert.assertTrue(GenericsUtil.satisfiesDependency(false, f3.getGenericType(), f1.getGenericType()));
        Assert.assertTrue(GenericsUtil.satisfiesDependency(false, f4.getGenericType(), f1.getGenericType()));
    }

    @Test
    public void testStable() throws Exception
    {
        Type parameterizedPigStableType = this.getClass().getDeclaredField("parameterizedPigStable").getGenericType();
        Type parameterizedHorseStableType = this.getClass().getDeclaredField("parameterizedHorseStable").getGenericType();
        Assert.assertNotNull(parameterizedPigStableType);
        Assert.assertNotNull(parameterizedHorseStableType);

        Type pigStableType = this.getClass().getDeclaredField("pigStable").getType().getGenericSuperclass();
        Type horseStableType = this.getClass().getDeclaredField("horseStable").getType().getGenericSuperclass();

        Assert.assertTrue(GenericsUtil.satisfiesDependency(false, horseStableType, parameterizedHorseStableType));
        Assert.assertTrue(GenericsUtil.satisfiesDependency(false, parameterizedPigStableType, pigStableType));
    }
    // fields for {@link #testStable}
    private Stable<Horse> parameterizedHorseStable;
    private Stable<Pig> parameterizedPigStable;
    private HorseStable horseStable;
    private PigStable pigStable;


    @Test
    public void testGenericProducerType() throws Exception
    {
        Type parameterizedPigStableType = this.getClass().getDeclaredField("parameterizedPigStable").getGenericType();
        Type parameterizedHorseStableType = this.getClass().getDeclaredField("parameterizedHorseStable").getGenericType();
        Type stableProducerMethodType = this.getClass().getDeclaredMethod("stableProducer").getGenericReturnType();

        Assert.assertTrue(GenericsUtil.satisfiesDependency(false, parameterizedPigStableType, stableProducerMethodType));
        Assert.assertTrue(GenericsUtil.satisfiesDependency(false, parameterizedHorseStableType, stableProducerMethodType));
    }
    // method and field for {@link #testGenericProducerType}
    private <T> Stable<T> stableProducer()
    {
        return null;
    }

}
