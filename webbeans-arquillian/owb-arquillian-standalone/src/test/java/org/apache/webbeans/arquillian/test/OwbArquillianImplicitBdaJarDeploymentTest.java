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
package org.apache.webbeans.arquillian.test;

import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.inject.Inject;

import org.apache.webbeans.arquillian.test.beans.NonCdiBean;
import org.apache.webbeans.arquillian.test.beans.SampleUser;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * This test verifies the deployment of a JAR packaging with implicit BDAs.
 * This is if NO beans.xml is present!
 */
@RunWith(Arquillian.class)
public class OwbArquillianImplicitBdaJarDeploymentTest
{
    private @Inject SampleUser sampleUser;

    private @Inject BeanManager bm;

    @Deployment
    public static JavaArchive deploy()
    {
        JavaArchive testJar = ShrinkWrap
                .create(JavaArchive.class, "sampleImplicitBdaTest.jar")
                .addClass(OwbArquillianImplicitBdaJarDeploymentTest.class)
                .addPackage(SampleUser.class.getPackage());

        return testJar;
    }

    @Test
    public void testOwbArqContainerStartup()
    {
        Assert.assertNotNull(sampleUser);
        Assert.assertEquals("Hans", sampleUser.getFirstName());

        sampleUser.setFirstName("Karl");
        Assert.assertEquals("Karl", sampleUser.getFirstName());

        // but the NonCdiBean should not be picked up in an implicit BDA
        Assert.assertEquals(0, bm.getBeans(NonCdiBean.class).size());
    }
}
