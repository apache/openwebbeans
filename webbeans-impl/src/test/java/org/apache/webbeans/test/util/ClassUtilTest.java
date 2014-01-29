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

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import org.apache.webbeans.util.ClassUtil;
import org.apache.webbeans.test.util.differentPackage.MyOtherPackageSubClass;
import org.junit.Test;

import junit.framework.Assert;

public class ClassUtilTest {

    @Test
    public void testGetAllNonPrivateMethods()
    {
        List<Method> nonPrivateMethods = ClassUtil.getNonPrivateMethods(SpecificClass.class, false);
        nonPrivateMethods.removeAll(Arrays.asList(Object.class.getDeclaredMethods()));

        // getDeclaredMethods also contains the bridge method, so it's really only 1
        Assert.assertEquals(1, nonPrivateMethods.size());
    }

    @Test
    public void testGetAllNonPrivateMethods_packagePrivate()
    {
        List<Method> nonPrivateMethods = ClassUtil.getNonPrivateMethods(MyOtherPackageSubClass.class, false);

        for (Method m : nonPrivateMethods)
        {
            if (MySuperClass.class.equals(m.getDeclaringClass()) &&
                "packageMethod".equals(m.getName()))
            {
                Assert.fail("invisible package private method must not get listed");
            }
        }
    }

    @Test
    public void testIsOverridden() throws Exception
    {
        Assert.assertTrue(isOverridden(MySubClass.class, "publicMethod"));
        Assert.assertTrue(isOverridden(MySubClass.class, "protectedMethod"));

        Assert.assertTrue(isOverridden(MySubClass.class, "packageMethod"));
        Assert.assertFalse(isOverridden(MyOtherPackageSubClass.class, "packageMethod"));

        Assert.assertFalse(isOverridden(MySubClass.class, "privateMethod"));
        Assert.assertFalse(isOverridden(MyOtherPackageSubClass.class, "privateMethod"));
    }

    private boolean isOverridden(Class subClass, String methodName) throws Exception
    {
        Method superClassMethod = MySuperClass.class.getDeclaredMethod(methodName);
        Method subClassMethod   = subClass.getDeclaredMethod(methodName);

        return ClassUtil.isOverridden(subClassMethod, superClassMethod);
    }

}

