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
package org.apache.webbeans.context.control;

import org.apache.webbeans.test.AbstractUnitTest;
import org.junit.Test;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.ContextNotActiveException;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.context.control.ActivateRequestContext;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ActivateRequestContextInterceptorBeanTest extends AbstractUnitTest
{
    @Test
    public void autoStartRequestScope()
    {
        startContainer(Service.class);
        final Service service = getInstance(Service.class);
        getWebBeansContext().getContextsService().endContext(RequestScoped.class, null);
        assertFalse(service.isStarted());
        assertTrue(service.isStartedWithInterceptor());
        assertFalse(service.isStarted());
    }

    @ApplicationScoped
    public static class Service
    {
        @Inject
        private BeanManager beanManager;

        @PostConstruct
        private void postConstruct()
        {
            assertFalse(isStarted());
        }

        @ActivateRequestContext
        public boolean isStartedWithInterceptor()
        {
            return isStarted();
        }

        public boolean isStarted()
        {
            try
            {
                return beanManager.getContext(RequestScoped.class).isActive();
            }
            catch (final ContextNotActiveException cnae)
            {
                return false;
            }
        }
    }
}
