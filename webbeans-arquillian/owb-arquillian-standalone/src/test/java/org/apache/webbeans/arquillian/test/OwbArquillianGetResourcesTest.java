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

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import org.apache.webbeans.arquillian.standalone.OwbSWClassLoader;
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
public class OwbArquillianGetResourcesTest
{

    public static final String INTESTCLASSPATH_PROPERTIES = "intestclasspath.properties";
    public static final String INBOTHARCHIVEANDCLASSPATH_PROPERTIES = "inbotharchiveandclasspath.properties";
    public static final String INJAR_PROPERTIES = "injar.properties";
    public static final String INWAR_PROPERTIES = "inwar.properties";

    @Deployment
    public static Archive deploy()
    {
        JavaArchive testJar1 = ShrinkWrap
                .create(JavaArchive.class, "sampleUserTest1.jar")
                .addClass(OwbArquillianGetResourcesTest.class)
                .addPackage(SampleUser.class.getPackage())
                .addAsResource(new StringAsset("injar=true"), INJAR_PROPERTIES)
                .addAsResource(new StringAsset("fromjar=true"), INBOTHARCHIVEANDCLASSPATH_PROPERTIES)
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");


        WebArchive testWar = ShrinkWrap
                .create(WebArchive.class, "sampleUserTest.war")
                .addAsResource(new StringAsset("inwar=true"), INWAR_PROPERTIES)
                .addAsLibrary(testJar1);

        return testWar;
    }

    @Test
    public void testOwbArqGetResources() throws IOException {
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        Assert.assertNotNull(tccl);
        Assert.assertTrue(tccl instanceof OwbSWClassLoader);

        checkExpectedResources(INJAR_PROPERTIES, 1,
            "injar", "true");

        checkExpectedResources(INWAR_PROPERTIES, 1,
            "inwar", "true");

        checkExpectedResources(INTESTCLASSPATH_PROPERTIES, 1,
            "intestclasspath", "1");

        checkExpectedResources(INBOTHARCHIVEANDCLASSPATH_PROPERTIES, 2,
            "fromtestclasspath", "true",
            "fromjar", "true");
    }

    private void checkExpectedResources(String resourceName, int expectedUrls, String... expectedProperties) throws IOException
    {
        List<URL> urls = getResources(resourceName);
        Assert.assertEquals(expectedUrls, urls.size());

        Properties props = new Properties();
        for (URL url : urls)
        {
            props.load(url.openStream());
        }

        Assert.assertEquals(expectedProperties.length / 2, props.size());
        for (int i = 0; i < expectedProperties.length; i+=2)
        {
            Assert.assertEquals(expectedProperties[1], props.getProperty(expectedProperties[0]));
        }
    }

    private List<URL> getResources(String name) throws IOException
    {
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        Enumeration<URL> resources = tccl.getResources(name);
        List<URL> urls = new ArrayList<>();
        while (resources.hasMoreElements())
        {
            urls.add(resources.nextElement());
        }
        return urls;
    }
}
