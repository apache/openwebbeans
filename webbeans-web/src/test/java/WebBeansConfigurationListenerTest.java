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

import static java.util.Collections.emptySet;
import static org.apache.webbeans.util.Asserts.assertNotNull;

import java.util.EventListener;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;

import org.apache.webbeans.servlet.WebBeansConfigurationListener;
import org.apache.webbeans.spi.ContainerLifecycle;
import org.apache.webbeans.spi.ScannerService;
import org.apache.webbeans.web.lifecycle.test.MockServletContext;
import org.apache.webbeans.web.tests.lifeycle.SingleStartContainerLifecycle;
import org.apache.webbeans.web.tests.scanner.EmptyScanner;
import org.junit.Test;

public class WebBeansConfigurationListenerTest
{
    @Test
    public void avoidDoubleInitWithSci()
    {
        final WebBeansConfigurationListener.Auto auto = new WebBeansConfigurationListener.Auto();
        final AtomicReference<ServletContextListener> listener = new AtomicReference<>();
        final MockServletContext context = new MockServletContext()
        {
            @Override
            public <T extends EventListener> void addListener(final T t)
            {
                listener.set(ServletContextListener.class.cast(t));
            }
        };
        context.setInitParameter("openwebbeans.web.sci.active", "true");
        System.setProperty(ScannerService.class.getName(), EmptyScanner.class.getName());
        System.setProperty(ContainerLifecycle.class.getName(), SingleStartContainerLifecycle.class.getName());
        auto.onStartup(emptySet(), context);
        System.clearProperty(ScannerService.class.getName());
        System.clearProperty(ContainerLifecycle.class.getName());

        final ServletContextListener wbcl = listener.get();
        assertNotNull(wbcl);

        final ServletContextEvent event = new ServletContextEvent(context);
        wbcl.contextInitialized(event);
        wbcl.contextDestroyed(event);
    }
}
