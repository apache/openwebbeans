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
package org.apache.webbeans.arquillian.standalone;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.corespi.security.SimpleSecurityService;
import org.apache.webbeans.spi.ScannerService;
import org.apache.webbeans.spi.SecurityService;
import org.apache.webbeans.spi.SingletonService;

/**
 *
 */
public class OwbArquillianSingletonService implements SingletonService<WebBeansContext>
{

    private WebBeansContext webBeansContext;

    public OwbArquillianSingletonService()
    {
        initOwb();
    }

    public synchronized void initOwb()
    {
        ScannerService dummyScannerService = new OwbArquillianScannerService();

        Map<Class<?>, Object> initialServices = new HashMap<Class<?>, Object>();
        initialServices.put(ScannerService.class,  dummyScannerService);

        //X TODO this is needed because of a dirty hack in the OpenWebBeansConfiguration
        initialServices.put(SecurityService.class, new SimpleSecurityService());

        Properties initialConfig = new Properties();

        webBeansContext = new WebBeansContext(initialServices, initialConfig);
        webBeansContext.getOpenWebBeansConfiguration().parseConfiguration();
    }

    @Override
    public void clear(Object key)
    {
        webBeansContext.clear();
    }

    @Override
    public WebBeansContext get(Object key)
    {
        return webBeansContext;
    }
}
