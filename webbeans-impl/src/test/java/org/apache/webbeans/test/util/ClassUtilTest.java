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

import org.junit.Assert;

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
    public void testGetNonPrivateMethods_includeJvmBridgeMethods()
    {
        List<Method> withoutBridges = ClassUtil.getNonPrivateMethods(SpecificClass.class, false);
        withoutBridges.removeAll(Arrays.asList(Object.class.getDeclaredMethods()));
        Assert.assertEquals(1, withoutBridges.size());
        Assert.assertFalse(withoutBridges.stream().anyMatch(Method::isBridge));

        List<Method> withBridges = ClassUtil.getNonPrivateMethods(SpecificClass.class, false, true);
        withBridges.removeAll(Arrays.asList(Object.class.getDeclaredMethods()));
        Assert.assertEquals(2, withBridges.size());
        Assert.assertEquals(1, withBridges.stream().filter(Method::isBridge).count());
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

    @Test
    public void testGetAbstractMethods() throws Exception
    {
        List<Method> methods = ClassUtil.getAbstractMethods(AbstractClassWithAbstractMethod.class);
        Assert.assertNotNull(methods);
        Assert.assertEquals(2, methods.size());

        for (Method method : methods)
        {
            String name = method.getName();
            if (!name.equals("getPublicString") && !name.equals("getProtectedInt"))
            {
                Assert.fail("unexpected Method " + name);
            }
        }

        Assert.assertTrue("returned methods must be empty", ClassUtil.getAbstractMethods(MySubClass.class).isEmpty());
    }

    @Test
    public void testIsMethodDeclared() throws Exception
    {
        Class<AbstractClassWithAbstractMethod> clazz = AbstractClassWithAbstractMethod.class;

        Assert.assertTrue(ClassUtil.isMethodDeclared(clazz, "doSomething"));
        Assert.assertFalse(ClassUtil.isMethodDeclared(clazz, "notExistingMethod"));
    }

    /** Covariant return yields a bridge ()Ljava/lang/Object; — same JVM descriptor as Object.clone(). */
    public interface CovariantIface
    {
        Object getV();
    }

    public abstract static class CovariantBase
    {
        public String getV()
        {
            return "x";
        }
    }

    public static final class CovariantImpl extends CovariantBase implements CovariantIface
    {
    }

    @Test
    public void testGetNonPrivateMethods_jvmBridgeDescriptorDistinctFromClone()
    {
        List<Method> methods = ClassUtil.getNonPrivateMethods(CovariantImpl.class, true, true);
        long bridgeGetV = methods.stream().filter(m -> "getV".equals(m.getName()) && m.isBridge()).count();
        Assert.assertEquals(
                "covariant bridge shares ()Ljava/lang/Object; with clone — must still be listed (OWB-923)",
                1,
                bridgeGetV);
    }

    private boolean isOverridden(Class subClass, String methodName) throws Exception
    {
        Method superClassMethod = MySuperClass.class.getDeclaredMethod(methodName);
        Method subClassMethod   = subClass.getDeclaredMethod(methodName);

        return ClassUtil.isOverridden(subClassMethod, superClassMethod);
    }

}

