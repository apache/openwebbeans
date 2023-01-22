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

import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.util.AnnotationLiteral;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.test.AbstractUnitTest;
import org.apache.webbeans.test.concepts.alternatives.common.AlternativeBean;
import org.apache.webbeans.test.concepts.alternatives.common.AlternativeOnClassAndProducerMethodBean;
import org.apache.webbeans.test.concepts.alternatives.common.AlternativeOnClassOnlyBean;
import org.apache.webbeans.test.concepts.alternatives.common.AlternativeWithEvenHigherPriorityBean;
import org.apache.webbeans.test.concepts.alternatives.common.AlternativeWithHighPriorityBean;
import org.apache.webbeans.test.concepts.alternatives.common.AlternativeWithPriorityBean;
import org.apache.webbeans.test.concepts.alternatives.common.DefaultBeanProducerWithoutDisposes;
import org.apache.webbeans.test.concepts.alternatives.common.Pen;
import org.apache.webbeans.test.concepts.alternatives.common.Pencil;
import org.apache.webbeans.test.concepts.alternatives.common.PencilProducerBean;
import org.apache.webbeans.test.concepts.alternatives.common.SimpleBean;
import org.apache.webbeans.test.concepts.alternatives.common.SimpleInterface;
import org.apache.webbeans.test.concepts.alternatives.common.YetAnotherPencil;
import org.junit.Test;
import org.junit.Assert;

/**
 * Test bean resolving if &#064;Alternative annotated beans are involved
 * See OWB-658 for the original problem.
 */
public class AlternativeBeanResolvingTest extends AbstractUnitTest
{
    private static final String PACKAGE_NAME = AlternativeProducerMethodTest.class.getPackage().getName();

    /**
     * see OWB-658
     */
    @Test
    public void testObjectBeanQuery()
    {
        Collection<String> beanXmls = new ArrayList<String>();
        beanXmls.add(getXmlPath(PACKAGE_NAME, "AlternativeOnProducerFieldTest"));

        Collection<Class<?>> beanClasses = new ArrayList<Class<?>>();
        beanClasses.add(Pen.class);
        beanClasses.add(Pencil.class);
        beanClasses.add(PencilProducerBean.class);
        beanClasses.add(DefaultBeanProducerWithoutDisposes.class);
        beanClasses.add(AlternativeOnClassAndProducerMethodBean.class);
        beanClasses.add(YetAnotherPencil.class);

        // available but not enabled in beans.xml
        beanClasses.add(AlternativeOnClassOnlyBean.class);

        startContainer(beanClasses, beanXmls);

        Set<Bean<?>> beans = getBeanManager().getBeans(Object.class, new AnnotationLiteral<Pen>(){});
        Assert.assertNotNull(beans);
        Assert.assertEquals(2, beans.size());
    }


    /**
     * Having the same alternative enabled multiple times in the same beans.xml
     * This must abort with a deployment error.
     */
    @Test(expected = WebBeansConfigurationException.class)
    public void testMultipleAlternativeInSameFile()
    {
        Collection<String> beanXmls = new ArrayList<String>();
        beanXmls.add(getXmlPath(PACKAGE_NAME, "MultipleAlternativesInSameFile_broken"));

        Collection<Class<?>> beanClasses = new ArrayList<Class<?>>();
        beanClasses.add(Pen.class);
        beanClasses.add(Pencil.class);
        beanClasses.add(PencilProducerBean.class);
        beanClasses.add(DefaultBeanProducerWithoutDisposes.class);
        beanClasses.add(AlternativeOnClassAndProducerMethodBean.class);
        beanClasses.add(YetAnotherPencil.class);

        startContainer(beanClasses, beanXmls, true);
    }


    @Test
    public void testMultipleAlternativeInDifferentFiles()
    {
        Collection<String> beanXmls = new ArrayList<String>();
        beanXmls.add(getXmlPath(PACKAGE_NAME, "simpleAlternative"));
        beanXmls.add(getXmlPath(PACKAGE_NAME, "simpleAlternative_2"));

        Collection<Class<?>> beanClasses = new ArrayList<Class<?>>();
        beanClasses.add(AlternativeBean.class);

        startContainer(beanClasses, beanXmls, true);

        AlternativeBean alternativeBean = getInstance(AlternativeBean.class);
        Assert.assertNotNull(alternativeBean);
        Assert.assertEquals(AlternativeBean.class, alternativeBean.getImplementationType());

    }

    /**
     * Test alternatives which are NOT in any beans.xml but activated
     * by having a Priority annotation.
     */
    @Test
    public void testPriorityAlternative() throws Exception
    {
        startContainer(AlternativeWithPriorityBean.class, SimpleBean.class);

        SimpleInterface simpleInterface = getInstance(SimpleInterface.class);
        Assert.assertNotNull(simpleInterface);
        Assert.assertEquals(AlternativeWithPriorityBean.class, simpleInterface.getImplementationType());
    }

    /**
     * Test alternatives which are NOT in any beans.xml but activated
     * by having a Priority annotation.
     */
    @Test
    public void testMultiplePriorityAlternative() throws Exception
    {
        startContainer(AlternativeWithPriorityBean.class, AlternativeWithEvenHigherPriorityBean.class, AlternativeWithHighPriorityBean.class, SimpleBean.class);

        SimpleInterface simpleInterface = getInstance(SimpleInterface.class);
        Assert.assertNotNull(simpleInterface);
        Assert.assertEquals(AlternativeWithEvenHigherPriorityBean.class, simpleInterface.getImplementationType());
    }



}
