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
package org.apache.webbeans.test.concepts.alternatives.tests;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.Bean;

import org.apache.webbeans.test.AbstractUnitTest;
import org.apache.webbeans.test.concepts.alternatives.common.AlternativeBean;
import org.apache.webbeans.test.concepts.alternatives.common.SimpleBean;
import org.apache.webbeans.test.concepts.alternatives.common.SimpleInjectionTarget;
import org.apache.webbeans.test.concepts.alternatives.common.SimpleInterface;
import org.junit.Assert;
import org.junit.Test;

/**
 * see OWB-742
 */
public class AlternativeInstanceTest extends AbstractUnitTest
{
    private static final String PACKAGE_NAME = AlternativeProducerMethodTest.class.getPackage().getName();

    @Test
    public void testAlternativeDisabled()
    {
        startContainer(SimpleBean.class, AlternativeBean.class, SimpleInjectionTarget.class);

        Bean<?> simpleInjectionTargetBean = getBeanManager().getBeans(SimpleInjectionTarget.class).iterator().next();
        CreationalContext<?> context = getBeanManager().createCreationalContext(simpleInjectionTargetBean);
        SimpleInjectionTarget target = (SimpleInjectionTarget) getBeanManager().getReference(simpleInjectionTargetBean, SimpleInjectionTarget.class, context);
        
        Assert.assertFalse(target.isSimpleInterfaceAmbiguous());
        Iterator<SimpleInterface> simpleInterfaceInstances = target.getSimpleInterfaceInstances();
        Assert.assertTrue(simpleInterfaceInstances.hasNext());
        Assert.assertEquals(SimpleBean.class, simpleInterfaceInstances.next().getImplementationType());
        Assert.assertFalse(simpleInterfaceInstances.hasNext());
        Assert.assertEquals(SimpleBean.class, target.getSimpleInterface1().getImplementationType());
        Assert.assertEquals(SimpleBean.class, target.getSimpleInterface2().getImplementationType());
        
        shutDownContainer();
    }

    @Test
    public void testAlternativeEnabled()
    {
        Collection<String> beanXmls = new ArrayList<String>();
        beanXmls.add(getXmlPath(PACKAGE_NAME, "simpleAlternative"));

        Collection<Class<?>> beanClasses = new ArrayList<Class<?>>();
        beanClasses.add(SimpleBean.class);
        beanClasses.add(AlternativeBean.class);
        beanClasses.add(SimpleInjectionTarget.class);

        startContainer(beanClasses, beanXmls);

        Bean<?> simpleInjectionTargetBean = getBeanManager().getBeans(SimpleInjectionTarget.class).iterator().next();
        CreationalContext<?> context = getBeanManager().createCreationalContext(simpleInjectionTargetBean);
        SimpleInjectionTarget target = (SimpleInjectionTarget) getBeanManager().getReference(simpleInjectionTargetBean, SimpleInjectionTarget.class, context);
        
        Assert.assertFalse(target.isSimpleInterfaceAmbiguous());
        Iterator<SimpleInterface> simpleInterfaceInstances = target.getSimpleInterfaceInstances();
        Assert.assertTrue(simpleInterfaceInstances.hasNext());
        Assert.assertEquals(AlternativeBean.class, simpleInterfaceInstances.next().getImplementationType());
        Assert.assertFalse(simpleInterfaceInstances.hasNext());
        Assert.assertEquals(AlternativeBean.class, target.getSimpleInterface1().getImplementationType());
        Assert.assertEquals(AlternativeBean.class, target.getSimpleInterface2().getImplementationType());
        
        shutDownContainer();
    }

}
