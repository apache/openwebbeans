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
package org.apache.webbeans.test.decorators.constructor;

import org.junit.Assert;
import org.apache.webbeans.test.AbstractUnitTest;
import org.junit.Test;

import jakarta.annotation.Priority;
import jakarta.decorator.Decorator;
import jakarta.decorator.Delegate;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

public class DecoratorConstructorInjectionTest extends AbstractUnitTest
{
    @Test
    public void run()
    {
        startContainer(Foo.class, Dec.class, FooImpl.class, ABeanJustToEnsureInjectionsWork.class);
        Assert.assertTrue(getInstance(Foo.class).dec());
    }

    public static interface Foo
    {
        boolean dec();
        boolean normal();
    }

    public static class FooImpl implements Foo
    {
        public boolean dec()
        {
            return false;
        }

        @Override
        public boolean normal()
        {
            return true;
        }
    }

    @Decorator
    @Priority(1)
    public static abstract class Dec implements Foo
    {
        private final Foo del;
        private final Integer number;
        private final ABeanJustToEnsureInjectionsWork injection;

        @Inject
        public Dec(@Delegate @Any Foo delegate, ABeanJustToEnsureInjectionsWork injection, Integer integer) throws IllegalArgumentException
        {
            this.del = delegate;
            this.number = integer;
            this.injection = injection;
        }

        @Override
        public boolean dec()
        {
            return del != null && !Foo.class.cast(del).dec() && Foo.class.cast(del).normal()
                    && number == 1 && injection != null;
        }
    }

    public static class ABeanJustToEnsureInjectionsWork
    {
        @Produces
        public Integer aWrapper = 1;
    }
}
