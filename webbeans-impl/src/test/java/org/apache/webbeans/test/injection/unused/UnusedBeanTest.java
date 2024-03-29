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
package org.apache.webbeans.test.injection.unused;

import org.apache.webbeans.test.AbstractUnitTest;
import org.junit.Test;
import org.junit.Assert;

import jakarta.enterprise.context.RequestScoped;
import java.util.ArrayList;
import java.util.Collection;

/**
 * <p>This test validates OWBs behaviour with unused beans.</p>
 */
public class UnusedBeanTest extends AbstractUnitTest
{
    @Test
    public void testUnusedBean()
    {
        Collection<String> beanXmls = new ArrayList<String>();
        Collection<Class<?>> beanClasses = new ArrayList<Class<?>>();
        beanClasses.add(UnusedBean.class);
        beanClasses.add(UnusedBeanProducer.class);
        beanClasses.add(UnusedBeanDeclarer.class);

        try
        {
            startContainer(beanClasses, beanXmls);

            UnusedBeanDeclarer unusedBeanDeclarer = getInstance(UnusedBeanDeclarer.class);
            Assert.assertNotNull(unusedBeanDeclarer);

            unusedBeanDeclarer.doSomething();

            // end the RequestScope to trigger disposal
            getWebBeansContext().getContextsService().endContext(RequestScoped.class, null);
            Assert.assertFalse(UnusedBeanProducer.failed);

        }
        finally {
            shutDownContainer();
        }
    }
}
