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
package org.apache.webbeans.test.decorators.tests;

import org.apache.webbeans.test.AbstractUnitTest;
import org.junit.Test;

import jakarta.decorator.Decorator;
import jakarta.decorator.Delegate;
import jakarta.inject.Inject;

import static org.junit.Assert.assertEquals;

// OWB-861
public class DecoratorInheritanceTest extends AbstractUnitTest
{
    @Test
    public void checkItWorks()
    {
        addDecorator(ServiceDecorator.class);
        startContainer(BaseDecorator.class, BaseModuleDecorator.class, ServiceDecorator.class, TheService.class);

        assertEquals("the decorator", getInstance(TheService.class).name());
        shutDownContainer();
    }

    @Test
    public void checkItWorksWithGenerics()
    {
        addDecorator(GServiceDecorator.class);
        startContainer(GBaseDecorator.class, GBaseModuleDecorator.class, GServiceDecorator.class, TheService.class);

        assertEquals("the decorator 1", getInstance(TheService.class).name());
        shutDownContainer();
    }

    public static abstract class BaseDecorator implements AService
    {

    }

    public static abstract class BaseModuleDecorator extends BaseDecorator
    {

    }

    @Decorator
    public static class ServiceDecorator extends BaseModuleDecorator
    {
        @Delegate
        @Inject
        private AService service;

        @Override
        public String name()
        {
            return service.name() + "decorator";
        }
    }

    public static class TheService implements AService
    {
        @Override
        public String name()
        {
            return "the ";
        }
    }

    public static interface AService
    {
        String name();
    }

    public static abstract class GBaseDecorator<T> implements AService
    {
        abstract T generateT();
    }

    public static abstract class GBaseModuleDecorator<T> extends GBaseDecorator<T>
    {

    }

    @Decorator
    public static class GServiceDecorator extends GBaseModuleDecorator<Integer>
    {
        @Delegate
        @Inject
        private AService service;

        @Override
        public String name()
        {
            return service.name() + "decorator " + generateT();
        }

        @Override
        Integer generateT() {
            return 1;
        }
    }
}
