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

import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.InjectionTarget;
import jakarta.enterprise.inject.spi.ProcessInjectionTarget;
import jakarta.inject.Inject;

import org.junit.Assert;
import org.apache.webbeans.test.AbstractUnitTest;
import org.junit.Test;

/**
 * Checks that the InjectionTarget in ProcessInjectionTarget
 * is correctly filled.
 */
public class ProcessInjectionTargetTest extends AbstractUnitTest
{
    @Test
    public void testInjectionTargetIsValid() throws Exception {
        InjectionTargetExtension injectionTargetExtension = new InjectionTargetExtension();
        addExtension(injectionTargetExtension);
        startContainer(SomeBean.class, SomeOtherBean.class);

        Assert.assertNotNull(injectionTargetExtension.getInjectionTarget());
        InjectionTarget injectionTarget = injectionTargetExtension.getInjectionTarget();
        Assert.assertNotNull(injectionTarget);
        Assert.assertNotNull(injectionTarget.getInjectionPoints());
        Assert.assertEquals(1, injectionTarget.getInjectionPoints().size());
    }

    public static class InjectionTargetExtension implements Extension
    {
        private InjectionTarget injectionTarget;


        public void observePit(@Observes ProcessInjectionTarget<SomeBean> pit)
        {
            this.injectionTarget = pit.getInjectionTarget();
        }

        public InjectionTarget getInjectionTarget() {
            return injectionTarget;
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
