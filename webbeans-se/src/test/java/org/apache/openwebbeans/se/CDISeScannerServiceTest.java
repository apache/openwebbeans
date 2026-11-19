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
package org.apache.openwebbeans.se;

import org.junit.Test;

import java.net.URL;
import java.util.Map;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CDISeScannerServiceTest
{
    @Test
    public void beanXmlsIncludeEmbeddedDeployment()
    {
        final CDISeScannerService scanner = new CDISeScannerService();
        scanner.addClassesDeploymentUrl();

        assertEquals(1, scanner.getBeanXmls().size());
        assertTrue(scanner.getBeanXmls().stream()
                .anyMatch(url -> "openwebbeans".equals(url.getProtocol()) && "cdise".equals(url.getPath())));
    }

    @Test
    public void beanXmlsWithoutEmbeddedDeployment()
    {
        assertTrue(new CDISeScannerService().getBeanXmls().isEmpty());
    }

    @Test
    public void beanXmlsUseDeploymentUrlsAccessor() throws Exception
    {
        final URL embedded = new URL("file:/embedded");
        final CDISeScannerService scanner = new CDISeScannerService()
        {
            @Override
            public Map<String, URL> getBeanDeploymentUrls()
            {
                return singletonMap(CDISeBeanArchiveService.EMBEDDED_URL, embedded);
            }
        };

        assertEquals(singleton(embedded), scanner.getBeanXmls());
    }
}
