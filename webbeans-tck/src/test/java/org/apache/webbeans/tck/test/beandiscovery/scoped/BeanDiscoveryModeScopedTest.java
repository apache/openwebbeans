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
package org.apache.webbeans.tck.test.beandiscovery.scoped;

import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.inject.Inject;

import java.util.Set;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * New CDI-2.0 feature delivered with owb already.
 */

public class BeanDiscoveryModeScopedTest extends Arquillian
{
    private @Inject BeanManager beanManager;

    @Deployment
    public static WebArchive createTestArchive()
    {
        JavaArchive scopedModeJar = ShrinkWrap.create(JavaArchive.class, "scopedmode.jar")
            .addClasses(ModeScopedModule.ExcplicitlyScopedBeanA.class, ModeScopedModule.NonScopedClassB.class)
            .addAsManifestResource(new StringAsset("<beans bean-discovery-mode=\"scoped\"></beans>"), "beans.xml");

        JavaArchive defaultModeCdiJar = ShrinkWrap.create(JavaArchive.class, "defaultmode.jar")
            .addClasses(DefaultDiscoveryModeModule.ExcplicitlyScopedBeanC.class, DefaultDiscoveryModeModule.AutoDependentClassD.class)
            .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");

        return ShrinkWrap.create(WebArchive.class, BeanDiscoveryModeScopedTest.class.getName() + ".war")
            .addAsLibraries(scopedModeJar, defaultModeCdiJar)
            .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
            .addClass(ScopedBeanConsumer.class);
    }

    @Test
    public void testInjection()
    {
        Set<Bean<?>> beans = beanManager.getBeans(ModeScopedModule.NonScopedClassB.class);
        Assert.assertEquals(beans.size(), 0);
    }


}
