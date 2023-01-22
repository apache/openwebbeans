/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.webbeans.test.definition.proxyable;


import org.apache.webbeans.test.AbstractUnitTest;
import org.apache.webbeans.test.definition.proxyable.beans.BaseClassWithPublicFinalMethod;
import org.apache.webbeans.test.definition.proxyable.beans.BeanWithPrivateFinalMethod;
import org.apache.webbeans.test.definition.proxyable.beans.BeanWithPublicFinalMethod;
import org.apache.webbeans.test.definition.proxyable.beans.DependentBeanWithoutDefaultCt;
import org.apache.webbeans.test.definition.proxyable.beans.NonAbstractSubClassBean;
import org.apache.webbeans.test.definition.proxyable.beans.SubClassWithNormalScope;
import org.junit.Assert;
import org.junit.Test;

import jakarta.enterprise.inject.UnproxyableResolutionException;
import java.util.ArrayList;
import java.util.Collection;

/**
 * This test checks for various conditions about NormalScope
 * Bean criteria regarding the ability to proxy those classes.
 * See CDI-spec 5.4.1. This got changed in CDI-1.1 to also allow
 * static and private methods to be final.
 */
public class ProxyableBeanTypeTest extends AbstractUnitTest
{
    @Test
    public void testBeanWithPrivateFinalMethods()
    {
        Collection<Class<?>> beanClasses = new ArrayList<Class<?>>();
        beanClasses.add(BeanWithPrivateFinalMethod.class);

        try
        {
            startContainer(beanClasses, null);

            BeanWithPrivateFinalMethod testInstance = getInstance(BeanWithPrivateFinalMethod.class);
            Assert.assertNotNull(testInstance);

            Assert.assertEquals(42, testInstance.externalMethod());
            Assert.assertEquals(4711, BeanWithPrivateFinalMethod.staticMethod());
        }
        finally
        {
            shutDownContainer();
        }

    }

    @Test(expected = UnproxyableResolutionException.class)
    public void testBeanWithPublicFinalMethods()
    {
        startContainer(BeanWithPublicFinalMethod.class);
        getInstance(BeanWithPublicFinalMethod.class);
    }

    @Test(expected = UnproxyableResolutionException.class)
    public void testSubclassBeanWithPublicFinalMethods()
    {
        startContainer(SubClassWithNormalScope.class, BaseClassWithPublicFinalMethod.class);
        getInstance(SubClassWithNormalScope.class);
    }

    @Test
    public void testNotInjectedBeanWithoutDefaultCt()
    {
        Collection<String> beanXmls = new ArrayList<String>();

        Collection<Class<?>> beanClasses = new ArrayList<Class<?>>();
        beanClasses.add(DependentBeanWithoutDefaultCt.class);
        beanClasses.add(NonAbstractSubClassBean.class);

        startContainer(beanClasses, beanXmls);
    }
}
