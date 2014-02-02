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
package org.apache.webbeans.test.xml;


import javax.enterprise.inject.spi.DeploymentException;
import java.net.URL;

import org.apache.webbeans.spi.BeanArchiveService;
import org.apache.webbeans.spi.BeanArchiveService.BeanArchiveInformation;
import org.apache.webbeans.spi.BeanArchiveService.BeanDiscoveryMode;
import org.apache.webbeans.xml.DefaultBeanArchiveService;
import org.junit.Test;
import org.junit.Assert;


public class BeanArchiveServiceTest
{
    @Test
    public void testNotExistingBeansXml() throws Exception
    {
        BeanArchiveInformation bai = scanBeansXml("");
        Assert.assertEquals(BeanDiscoveryMode.ANNOTATED, bai.getBeanDiscoveryMode());
        Assert.assertTrue(bai.getAlternativeClasses().isEmpty());
        Assert.assertTrue(bai.getAlternativeStereotypes().isEmpty());
        Assert.assertTrue(bai.getDecorators().isEmpty());
        Assert.assertTrue(bai.getInterceptors().isEmpty());
    }

    @Test
    public void testEmptyBeansXml() throws Exception
    {
        BeanArchiveInformation bai = scanBeansXml("empty.xml");
        Assert.assertEquals(BeanDiscoveryMode.ALL, bai.getBeanDiscoveryMode());
        Assert.assertTrue(bai.getAlternativeClasses().isEmpty());
        Assert.assertTrue(bai.getAlternativeStereotypes().isEmpty());
        Assert.assertTrue(bai.getDecorators().isEmpty());
        Assert.assertTrue(bai.getInterceptors().isEmpty());
    }

    @Test
    public void testAlternativesBeansXml() throws Exception
    {
        BeanArchiveInformation bai = scanBeansXml("alternatives_correct.xml");
        Assert.assertEquals(BeanDiscoveryMode.ALL, bai.getBeanDiscoveryMode());
        Assert.assertTrue(bai.getDecorators().isEmpty());
        Assert.assertTrue(bai.getInterceptors().isEmpty());

        Assert.assertEquals(1, bai.getAlternativeClasses().size());
        Assert.assertEquals("org.apache.webbeans.test.xml.strict.Alternative1", bai.getAlternativeClasses().get(0));

        Assert.assertEquals(1, bai.getAlternativeStereotypes().size());
        Assert.assertEquals("org.apache.webbeans.test.xml.strict.AlternativeStereotype", bai.getAlternativeStereotypes().get(0));
    }

    @Test
    public void testDecoratorsBeansXml() throws Exception
    {
        BeanArchiveInformation bai = scanBeansXml("decorators.xml");
        Assert.assertEquals(BeanDiscoveryMode.ALL, bai.getBeanDiscoveryMode());
        Assert.assertTrue(bai.getAlternativeClasses().isEmpty());
        Assert.assertTrue(bai.getAlternativeStereotypes().isEmpty());
        Assert.assertTrue(bai.getInterceptors().isEmpty());

        Assert.assertEquals(1, bai.getDecorators().size());
        Assert.assertEquals("org.apache.webbeans.test.xml.strict.DummyDecorator", bai.getDecorators().get(0));
    }

    @Test
    public void testInterceptorsBeansXml() throws Exception
    {
        BeanArchiveInformation bai = scanBeansXml("interceptors.xml");
        Assert.assertEquals(BeanDiscoveryMode.ALL, bai.getBeanDiscoveryMode());
        Assert.assertTrue(bai.getAlternativeClasses().isEmpty());
        Assert.assertTrue(bai.getAlternativeStereotypes().isEmpty());
        Assert.assertTrue(bai.getDecorators().isEmpty());

        Assert.assertEquals(1, bai.getInterceptors().size());
        Assert.assertEquals("org.apache.webbeans.test.xml.strict.DummyInterceptor", bai.getInterceptors().get(0));
    }


    @Test(expected = DeploymentException.class)
    public void testCdi11_Fail_without_discovery_mode() throws Exception
    {
        scanBeansXml("cdi11_failed.xml");
    }

    @Test
    public void testCdi11_discovery_none() throws Exception
    {
        BeanArchiveInformation bai = scanBeansXml("cdi11_discovery_none.xml");
        Assert.assertEquals(BeanDiscoveryMode.NONE, bai.getBeanDiscoveryMode());
        Assert.assertTrue(bai.getAlternativeClasses().isEmpty());
        Assert.assertTrue(bai.getAlternativeStereotypes().isEmpty());
        Assert.assertTrue(bai.getDecorators().isEmpty());
        Assert.assertTrue(bai.getInterceptors().isEmpty());
    }




    private BeanArchiveInformation scanBeansXml(String name)
    {
        URL url = getClass().getClassLoader().getResource("org/apache/webbeans/test/xml/strict/" + name);
        BeanArchiveService bas = new DefaultBeanArchiveService();
        BeanArchiveInformation beanArchiveInformation = bas.getBeanArchiveInformation(url);
        Assert.assertNotNull(beanArchiveInformation);

        return beanArchiveInformation;
    }
}
