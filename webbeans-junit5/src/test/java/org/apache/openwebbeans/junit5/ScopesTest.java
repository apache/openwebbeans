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

import org.apache.openwebbeans.junit5.extension.DummyScoped;
import org.apache.openwebbeans.junit5.extension.MyScope;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.context.AbstractContext;
import org.apache.webbeans.corespi.se.StandaloneContextsService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import javax.enterprise.context.ContextException;
import javax.enterprise.context.ContextNotActiveException;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.context.SessionScoped;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import java.lang.annotation.Annotation;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Cdi(disableDiscovery = true, properties =
    @Cdi.Property(
            name = "org.apache.webbeans.spi.ContextsService",
            value = "org.apache.openwebbeans.junit5.ScopesTest$ContextsService"))
@Scopes(SessionScoped.class)
class ScopesTest
{
    @Inject
    private BeanManager beanManager;
    private static BeanManager beanManagerRef;

    @AfterEach
    void captureBm() {
        beanManagerRef = beanManager;
    }

    @AfterAll
    static void after() {
        assertThrows(ContextNotActiveException.class, () -> beanManagerRef.getContext(DummyScoped.class).isActive());
    }

    @Test
    void classScopeStarted()
    {
        assertThrows(ContextNotActiveException.class, () -> beanManager.getContext(RequestScoped.class).isActive());
        assertThrows(ContextNotActiveException.class, () -> beanManager.getContext(DummyScoped.class).isActive());
        assertTrue(beanManager.getContext(SessionScoped.class).isActive());
    }

    @Test
    @Scopes(DummyScoped.class)
    void methodScopeStarted()
    {
        assertThrows(ContextNotActiveException.class, () -> beanManager.getContext(RequestScoped.class).isActive());
        assertTrue(beanManager.getContext(SessionScoped.class).isActive());
        assertTrue(beanManager.getContext(DummyScoped.class).isActive());
    }

    // not required but enables to control the activation as any built-in scope so good enough for this test
    public static class ContextsService extends StandaloneContextsService
    {
        public ContextsService(final WebBeansContext webBeansContext)
        {
            super(webBeansContext);
        }

        private AbstractContext getDummyScope()
        {
            return webBeansContext.getBeanManagerImpl().getExtension(MyScope.class).getScope();
        }

        @Override
        public void endContext(final Class<? extends Annotation> scopeType, final Object endParameters)
        {
            if (scopeType == DummyScoped.class)
            {
                getDummyScope().setActive(false);
            }
            else
            {
                super.endContext(scopeType, endParameters);
            }
        }

        @Override
        public void startContext(final Class<? extends Annotation> scopeType, final Object startParameter) throws ContextException
        {
            if (scopeType == DummyScoped.class)
            {
                getDummyScope().setActive(true);
            }
            else
            {
                super.startContext(scopeType, startParameter);
            }
        }
    }
}
