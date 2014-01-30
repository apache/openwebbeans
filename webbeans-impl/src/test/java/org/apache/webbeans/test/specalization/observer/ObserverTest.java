/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.webbeans.test.specalization.observer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import javax.enterprise.inject.spi.Bean;

import org.apache.webbeans.test.AbstractUnitTest;
import org.junit.Assert;
import org.junit.Test;


public class ObserverTest extends AbstractUnitTest
{
    private static final String PACKAGE_NAME = ObserverTest.class.getPackage().getName();
    
    @Test
    public void testObserverMethodsInParentOfAlternativeAndSpecializedBeans()
    {
        Collection<String> beanXmls = new ArrayList<String>();
        beanXmls.add(getXmlPath(PACKAGE_NAME, "AlternativeSpecializes"));

        Collection<Class<?>> beanClasses = new ArrayList<Class<?>>();
        beanClasses.add(BeanA.class);
        beanClasses.add(BeanC.class);
        startContainer(beanClasses, beanXmls);
        
        Set<Bean<?>> beans = getBeanManager().getBeans(BeanA.class);
        Assert.assertEquals(1, beans.size());
        
        TestEvent testEvent = new TestEvent();
        getBeanManager().fireEvent(testEvent);

        Assert.assertEquals(1, testEvent.getCalledObservers().size());
        Assert.assertTrue(testEvent.getCalledObservers().iterator().next().endsWith(":[specialize]"));

        shutDownContainer();
    }
    
    @Test
    public void testOverrideObserverMethodsInAlternativeAndSpecializedBeans()
    {
        Collection<String> beanXmls = new ArrayList<String>();
        beanXmls.add(getXmlPath(PACKAGE_NAME, "AlternativeSpecializes"));

        Collection<Class<?>> beanClasses = new ArrayList<Class<?>>();
        beanClasses.add(BeanA.class);
        beanClasses.add(BeanD.class);
        startContainer(beanClasses, beanXmls);
        
        Set<Bean<?>> beans = getBeanManager().getBeans(BeanA.class);
        Assert.assertEquals(1, beans.size());
        
        TestEvent testEvent = new TestEvent();
        getBeanManager().fireEvent(testEvent);
        
        Assert.assertEquals(1, testEvent.getCalledObservers().size());
        Assert.assertTrue(testEvent.getCalledObservers().iterator().next().endsWith(":[alternative]:[specialize]"));
        
        shutDownContainer();
    }
    
    @Test
    public void testObserverMethodsInParentOfSpecializedBeans()
    {
        Collection<Class<?>> beanClasses = new ArrayList<Class<?>>();
        beanClasses.add(BeanA.class);
        beanClasses.add(BeanE.class);
        startContainer(beanClasses, null);

        Set<Bean<?>> beans = getBeanManager().getBeans(BeanA.class);
        Assert.assertEquals(1, beans.size());

        TestEvent testEvent = new TestEvent();
        getBeanManager().fireEvent(testEvent);

        Assert.assertEquals(BeanE.class, beans.toArray(new Bean<?>[0])[0].getBeanClass());
        Assert.assertEquals(1, testEvent.getCalledObservers().size());
        Assert.assertTrue(testEvent.getCalledObservers().iterator().next().endsWith(":[specialize]"));

        shutDownContainer();
    }

    @Test
    public void testOverrideObserverMethodsInSpecializedBeans()
    {
        Collection<Class<?>> beanClasses = new ArrayList<Class<?>>();
        beanClasses.add(BeanA.class);
        beanClasses.add(BeanB.class);
        startContainer(beanClasses, null);

        Set<Bean<?>> beans = getBeanManager().getBeans(BeanA.class);
        Assert.assertEquals(1, beans.size());

        TestEvent testEvent = new TestEvent();
        getBeanManager().fireEvent(testEvent);

        Assert.assertEquals(1, testEvent.getCalledObservers().size());
        Assert.assertTrue(testEvent.getCalledObservers().iterator().next().endsWith(":[specialize]"));

        shutDownContainer();
    }
}
