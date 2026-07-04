/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.webbeans.test.spi.deployer;

import java.io.File;
import java.net.URL;
import java.util.Set;

import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;

import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.spi.ContainerLifecycle;
import org.junit.Assert;
import org.junit.Test;

public class NoClassDefFoundBootTest
{
    @Test
    public void shouldIgnoreBeanWithMissingDependencyDuringBoot()
    {
        deleteClassFile("org.apache.webbeans.test.spi.deployer.NcdfMissingDependency");

        ContainerLifecycle containerLifecycle = WebBeansContext.getInstance().getService(ContainerLifecycle.class);
        containerLifecycle.startApplication(null);

        try
        {
            BeanManager beanManager = WebBeansContext.getInstance().getBeanManagerImpl();

            final Set<Bean<?>> beans = beanManager.getBeans(NcdfAvailableBean.class);
            Bean<NcdfAvailableBean> availableBean = (Bean<NcdfAvailableBean>) beanManager.resolve(beans);
            Assert.assertNotNull(availableBean);
            final CreationalContext<NcdfAvailableBean> cc = beanManager.createCreationalContext(availableBean);
            NcdfAvailableBean instance = (NcdfAvailableBean) beanManager.getReference(availableBean, NcdfAvailableBean.class, cc);
            Assert.assertEquals(42, instance.meaningOfLife());

            Assert.assertTrue(beanManager.getBeans(NcdfBrokenBean.class).isEmpty());
        }
        finally
        {
            containerLifecycle.stopApplication(null);
        }
    }

    private void deleteClassFile(String className)
    {
        String classResourceName = "/" + className.replace('.', '/') + ".class";
        URL classResource = getClass().getResource(classResourceName);
        if (classResource != null)
        {
            File classFile = new File(classResource.getFile());
            Assert.assertTrue(classFile.exists());
            Assert.assertTrue(classFile.delete());
        }
    }
}
