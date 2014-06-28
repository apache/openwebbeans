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
package org.apache.webbeans.test.events.injectiontarget;

import junit.framework.Assert;
import org.apache.webbeans.test.AbstractUnitTest;
import org.junit.Test;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.enterprise.inject.spi.ProcessBeanAttributes;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.assertEquals;

public class ProcessBeanAttributesTest extends AbstractUnitTest
{
    @Test
    public void checkTypeIsTakenIntoAccount() throws Exception
    {
        final ProcessBeanAttributesExtension processBeanAttributesExtension = new ProcessBeanAttributesExtension();
        addExtension(processBeanAttributesExtension);
        startContainer(SomeBean.class, SomeOtherBean.class);

        assertEquals(1, processBeanAttributesExtension.someBean.size());
        assertEquals(1, processBeanAttributesExtension.someOtherBean.size());
    }

    public static class ProcessBeanAttributesExtension implements Extension
    {
        private Collection<ProcessBeanAttributes<?>> someBean = new ArrayList<ProcessBeanAttributes<?>>();
        private Collection<ProcessBeanAttributes<?>> someOtherBean = new ArrayList<ProcessBeanAttributes<?>>();


        public void someBean(@Observes ProcessBeanAttributes<SomeBean> pba)
        {
            someBean.add(pba);
        }
        public void otherBean(@Observes ProcessBeanAttributes<SomeOtherBean> pba)
        {
            someOtherBean.add(pba);
        }
    }

    @RequestScoped
    public static class SomeBean
    {
        private @Inject SomeOtherBean someOtherBean;

        public SomeOtherBean getSomeOtherBean()
        {
            return someOtherBean;
        }
    }

    @RequestScoped
    public static class SomeOtherBean
    {
        private int meaningOfLife = 42;

        public int getMeaningOfLife()
        {
            return meaningOfLife;
        }

        public void setMeaningOfLife(int meaningOfLife)
        {
            this.meaningOfLife = meaningOfLife;
        }
    }
}
