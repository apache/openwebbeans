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
package org.apache.webbeans.xml;

import org.apache.webbeans.spi.BeanArchiveService;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static java.util.Arrays.asList;
import static org.apache.webbeans.spi.BeanArchiveService.BeanDiscoveryMode.ANNOTATED;
import static org.junit.Assert.assertEquals;

public class DefaultBeanArchiveServiceTest
{
    private static final String CONTENT = "" +
            "<interceptors>\n" +
            "   <class>com.acme.First</class>\n" +
            "   <class>com.acme.Second</class>\n" +
            "</interceptors>" +
            "";

    @Test
    public void parseEE8() throws IOException
    {
        assertBeansXml("" +
                "<beans xmlns=\"http://xmlns.jcp.org/xml/ns/javaee\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" \n" +
                "xsi:schemaLocation=\"http://xmlns.jcp.org/xml/ns/javaee http://xmlns.jcp.org/xml/ns/javaee/beans_2_0.xsd\"" +
                " bean-discovery-mode=\"annotated\" version=\"2.0\"> \n" +
                CONTENT +
                "</beans>" +
                "", "2.0");
    }

    @Test
    public void parseEE9() throws IOException
    {
        assertBeansXml("" +
                "<beans xmlns=\"https://jakarta.ee/xml/ns/jakartaee\"\n" +
                "        xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                "        xsi:schemaLocation=\"https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/beans_3_0.xsd\"\n" +
                "        version=\"3.0\" bean-discovery-mode=\"annotated\">\n" +
                CONTENT +
                "</beans>" +
                "", "3.0");
    }

    @Test
    public void parseEE10() throws IOException
    {
        assertBeansXml("" +
                "<beans xmlns=\"https://jakarta.ee/xml/ns/jakartaee\"\n" +
                "       xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                "       xsi:schemaLocation=\"https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/beans_4_0.xsd\"\n" +
                "       version=\"4.0\" bean-discovery-mode=\"annotated\">\n" +
                CONTENT +
                "</beans>" +
                "", "4.0");
    }

    private void assertBeansXml(final String beansXml, final String version) throws IOException
    {
        try (final InputStream stream = new ByteArrayInputStream(beansXml.getBytes(StandardCharsets.UTF_8))) {
            final BeanArchiveService.BeanArchiveInformation info = new DefaultBeanArchiveService().readBeansXml(stream, "mem");
            assertEquals(version, info.getVersion());
            assertEquals(ANNOTATED, info.getBeanDiscoveryMode());
            assertEquals(asList("com.acme.First", "com.acme.Second"), info.getInterceptors());
        }
    }
}
