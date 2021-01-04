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
package org.apache.webbeans.test.config;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.logging.Level.FINE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import org.apache.webbeans.config.BeansDeployer;
import org.apache.webbeans.test.AbstractUnitTest;
import org.apache.webbeans.test.component.intercept.webbeans.bindings.Transactional;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

public class BeansDeployerTest extends AbstractUnitTest
{
    @Rule
    public final TestName testName = new TestName();

    @Test
    public void skipValidations()
    {
        addConfiguration("org.apache.webbeans.spi.deployer.skipValidations", "true");
        startContainer(asList(TransactionalInterceptor.class, MyService.class),
                singletonList(Thread.currentThread().getContextClassLoader()
                        .getResource(getClass().getName().replace('.', '/') + "/interceptorLogging/beans.xml")
                        .toExternalForm()));
        assertEquals("tx", getInstance(MyService.class).tx());
    }

    @Test
    public void interceptorLogging()
    {
        final Logger logger = Logger.getLogger(BeansDeployer.class.getName());
        final Level originalLevel = logger.getLevel();
        logger.setLevel(FINE);
        final Collection<LogRecord> records = new ArrayList<>();
        final Handler testHandler = new Handler()
        {
            {
                setLevel(FINE);
            }

            @Override
            public void publish(final LogRecord record)
            {
                if (!record.getMessage().contains("Interceptor"))
                {
                    return;
                }
                synchronized (records)
                {
                    records.add(record);
                }
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
        logger.addHandler(testHandler);
        try
        {
            startContainer(singletonList(TransactionalInterceptor.class),
                    singletonList(Thread.currentThread().getContextClassLoader()
                            .getResource(getClass().getName().replace('.', '/') + '/' + testName.getMethodName() + "/beans.xml")
                            .toExternalForm()));
        }
        finally
        {
            logger.removeHandler(testHandler);
            logger.setLevel(originalLevel);
        }
        assertTrue( records.size() >= 1);
        final LogRecord record = records.iterator().next();
        assertEquals(Level.FINE, record.getLevel());
        assertEquals("Interceptor class : " + TransactionalInterceptor.class.getName() +
                " is already defined with priority 1000", record.getMessage());
    }

    @ApplicationScoped
    public static class MyService
    {
        @Transactional
        public String tx()
        {
            return "service";
        }
    }

    @Interceptor
    @Transactional
    @Priority(Interceptor.Priority.LIBRARY_BEFORE)
    public static class TransactionalInterceptor implements Serializable
    {
        @AroundInvoke
        public Object caller(final InvocationContext context) throws Exception
        {
            return "tx";
        }
    }
}
