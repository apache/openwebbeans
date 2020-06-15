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
package org.apache.openwebbeans.junit5;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.se.SeContainer;
import javax.enterprise.inject.se.SeContainerInitializer;
import javax.enterprise.inject.spi.CDI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CdiParameterResolversTest
{
    private static SeContainer container;

    @BeforeAll
    static void start() {
        // simulate another way than @Cdi to bootstrap the container,
        // can be another server (meecrowave, tomee, playx, ...) or just a custom preconfigured setup
        container = SeContainerInitializer.newInstance()
                .disableDiscovery()
                .addBeanClasses(CdiParameterResolversTest.SomeBean.class)
                .initialize();
    }

    @AfterAll
    static void stop() {
        container.close();
    }

    @Test
    void noParam()
    {
        assertNotNull(CDI.current().getBeanManager());
    }

    @Test
    @CdiMethodParameters
    void cdiParam(final SomeBean someBean)
    {
        assertNotNull(someBean);
        assertEquals("yes", someBean.ok());
        assertTrue(someBean.getClass().getName().contains("$$Owb")); // it is cdi proxy
    }

    @Test
    @CdiMethodParameters
    @ExtendWith(CustomParamResolver.class)
    void mixedParams(final SomeBean cdi, @SkipInject final SomeBean notCdi)
    {
        assertNotNull(cdi);
        assertEquals("yes", cdi.ok());
        assertEquals("custom", notCdi.ok());
    }

    @ApplicationScoped
    public static class SomeBean
    {
        public String ok()
        {
            return "yes";
        }
    }

    public static class CustomParamResolver implements ParameterResolver
    {

        @Override
        public boolean supportsParameter(final ParameterContext parameterContext,
                                         final ExtensionContext extensionContext) throws ParameterResolutionException
        {
            return parameterContext.getIndex() == 1;
        }

        @Override
        public Object resolveParameter(final ParameterContext parameterContext, final ExtensionContext extensionContext)
                throws ParameterResolutionException
        {
            return new SomeBean()
            {
                @Override
                public String ok()
                {
                    return "custom";
                }
            };
        }
    }
}
