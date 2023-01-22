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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import jakarta.inject.Inject;

import org.apache.webbeans.arquillian.test.beans.SampleUser;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * This test verifies the deployment of a JAR packaging.
 */
@RunWith(Arquillian.class)
public class OwbArquillianWebJarDeploymentTest
{
    private static final String RESOURCE_NAME = "some/resource.properties";

    @Inject
    private SampleUser sampleUser;


    @Deployment
    public static Archive deploy()
    {
        JavaArchive testJar1 = ShrinkWrap
                .create(JavaArchive.class, "sampleUserTest1.jar")
                .addClass(OwbArquillianWebJarDeploymentTest.class)
                .addPackage(SampleUser.class.getPackage())
                .addAsResource(new StringAsset("hello1"), RESOURCE_NAME)
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");

        JavaArchive testJar2 = ShrinkWrap
                .create(JavaArchive.class, "sampleUserTest2.jar")
                .addAsResource(new StringAsset("hello2"), RESOURCE_NAME);


        WebArchive testWar = ShrinkWrap
                .create(WebArchive.class, "sampleUserTest.war")
                .addAsResource(new StringAsset("hello3"), RESOURCE_NAME)
                .addAsLibrary(testJar1)
                .addAsLibrary(testJar2);

        return testWar;
    }

    @Test
    public void testOwbArqContainerStartup() throws IOException {
        Assert.assertNotNull(sampleUser);
        Assert.assertEquals("Hans", sampleUser.getFirstName());

        sampleUser.setFirstName("Karl");
        Assert.assertEquals("Karl", sampleUser.getFirstName());

        Enumeration<URL> resources = Thread.currentThread().getContextClassLoader().getResources(RESOURCE_NAME);
        Set<String> contents = new HashSet<>();

        while (resources.hasMoreElements())
        {
            URL url = resources.nextElement();
            contents.add(new BufferedReader(new InputStreamReader(url.openStream())).readLine());
        }

        Assert.assertEquals(3, contents.size());
        Assert.assertTrue(contents.contains("hello1"));
        Assert.assertTrue(contents.contains("hello2"));
        Assert.assertTrue(contents.contains("hello3"));
    }
}
