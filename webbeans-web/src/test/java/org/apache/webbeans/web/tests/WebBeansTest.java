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
package org.apache.webbeans.web.tests;

import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.corespi.scanner.xbean.CdiArchive;
import org.apache.webbeans.corespi.scanner.xbean.OwbAnnotationFinder;
import org.apache.webbeans.corespi.se.DefaultScannerService;
import org.apache.webbeans.spi.ContainerLifecycle;
import org.apache.webbeans.spi.ContextsService;
import org.apache.webbeans.spi.ScannerService;
import org.apache.webbeans.web.context.WebContextsService;
import org.apache.webbeans.web.lifecycle.test.MockServletContext;
import org.apache.webbeans.xml.DefaultBeanArchiveService;
import org.apache.xbean.finder.archive.ClassesArchive;
import org.junit.Test;

import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.http.HttpServletRequest;
import java.net.URL;
import java.util.HashMap;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class WebBeansTest {
    @Test
    public void implicitBeans()
    {
        final WebBeansContext webBeansContext = new WebBeansContext(new HashMap<Class<?>, Object>(), new Properties()
        {{
            setProperty(ContextsService.class.getName(), WebContextsService.class.getName());
            setProperty(ScannerService.class.getName(), NoScan.class.getName());
        }});

        final ServletContextEvent ctx = new ServletContextEvent(new MockServletContext()
        {
            {
                setAttribute("test", "ok");
            }
        });
        webBeansContext.getService(ContainerLifecycle.class).startApplication(ctx);
        try
        {
            final BeanManager mgr = webBeansContext.getBeanManagerImpl();
            final Bean<?> context = mgr.resolve(mgr.getBeans(ServletContext.class));
            assertEquals("ok", ServletContext.class.cast(mgr.getReference(context, ServletContext.class, null)).getAttribute("test"));
            assertNotNull(context);
            assertNotNull(mgr.resolve(mgr.getBeans(HttpServletRequest.class)));
        }
        finally
        {
            webBeansContext.getService(ContainerLifecycle.class).stopApplication(ctx);
        }
    }

    public static class NoScan extends DefaultScannerService
    {
        @Override
        public void scan()
        {
            archive = new CdiArchive(new DefaultBeanArchiveService(), Thread.currentThread().getContextClassLoader(), new HashMap<String, URL>(), null, null);
            finder = new OwbAnnotationFinder(new ClassesArchive());
        }
    }
}
