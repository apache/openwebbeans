package org.apache.webbeans.web.tests;

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

import static java.util.Arrays.asList;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.ConversationScoped;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.servlet.WebBeansConfigurationListener;
import org.apache.webbeans.spi.ContainerLifecycle;
import org.apache.webbeans.spi.ContextsService;
import org.apache.webbeans.spi.ConversationService;
import org.apache.webbeans.spi.ScannerService;
import org.apache.webbeans.web.context.WebContextsService;
import org.apache.webbeans.web.context.WebConversationService;
import org.apache.webbeans.web.lifecycle.WebContainerLifecycle;
import org.apache.webbeans.web.lifecycle.test.MockServletContext;
import org.apache.webbeans.web.scanner.WebScannerService;
import org.apache.webbeans.web.tests.scanner.EmptyScanner;
import org.junit.Test;

public class ConversationLoggingTest
{
    @Test
    public void dontLogIfThereIsNoConversation()
    {
        doTest(EmptyScanner.class, l -> {});
    }

    @Test
    public void ensureUnsupportedScopeIsLogged()
    {
        doTest(EmptyScanner.class, l -> {
                    try {
                        final Field wbc = l.getClass().getDeclaredField("webBeansContext");
                        wbc.setAccessible(true);
                        final ContextsService service = WebBeansContext.class.cast(wbc.get(l)).getContextsService();
                        service.startContext(ConversationScoped.class, null);
                        service.endContext(ConversationScoped.class, null);
                    } catch (final Exception e) {
                        throw new IllegalStateException(e);
                    }
                },
                "CDI-OpenWebBeans container does not support context scope ConversationScoped. " +
                "Scopes @Dependent, @RequestScoped, @ApplicationScoped and @Singleton are supported scope types");
    }

    private void doTest(final Class<? extends WebScannerService> scannerServiceClass,
                        final Consumer<WebBeansConfigurationListener> listener,
                        final String... messages) {
        final Logger logger = Logger.getLogger(WebContextsService.class.getName());
        final Collection<LogRecord> records = new ArrayList<>();
        final Handler spy = new Handler()
        {
            @Override
            public void publish(final LogRecord record)
            {
                records.add(record);
            }

            @Override
            public void flush()
            {
                // no-op
            }

            @Override
            public void close() throws SecurityException
            {
                // no-op
            }
        };
        logger.addHandler(spy);
        final MockServletContext context = new MockServletContext();
        final ServletContextEvent event = new ServletContextEvent(context);
        System.setProperty(ScannerService.class.getName(), scannerServiceClass.getName());
        System.setProperty(ContainerLifecycle.class.getName(), WebContainerLifecycle.class.getName());
        System.setProperty(ConversationService.class.getName(), WebConversationService.class.getName());
        System.setProperty("org.apache.webbeans.application.supportsConversation", "false");
        try
        {
            final WebBeansConfigurationListener wbcl = new WebBeansConfigurationListener();
            wbcl.contextInitialized(event);
            listener.accept(wbcl);
            wbcl.contextDestroyed(event);
        }
        finally
        {
            logger.removeHandler(spy);
            System.clearProperty(ScannerService.class.getName());
            System.clearProperty(ContainerLifecycle.class.getName());
        }

        final String[] values = records.stream().map(LogRecord::getMessage).distinct().toArray(String[]::new);
        assertEquals(asList(values).toString(), messages.length, values.length);
        assertArrayEquals(messages, values);
    }
}
