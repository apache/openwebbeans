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
package org.apache.webbeans.test.injection.generics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.apache.webbeans.config.OwbParametrizedTypeImpl;
import org.apache.webbeans.test.AbstractUnitTest;
import org.junit.Test;

public class FastMatchingGenericsTest extends AbstractUnitTest
{
    @Test
    public void concreteGenericsAreNotAmbiguous()
    {
        startContainer(StringRepo.class, IntRepo.class);
        final Object stringRepo = getInstance(new OwbParametrizedTypeImpl(null, Repo.class, String.class));
        assertNotNull(stringRepo);
        assertEquals("string", Repo.class.cast(stringRepo).get());
        validateIntRepoIsOk();
    }

    @Test
    public void genericGenericsAreNotAmbiguous()
    {
        startContainer(IntRepo.class, StringRepo.class, ConcreteRepo.class);
        final ConcreteRepo stringRepo = getInstance(ConcreteRepo.class);
        assertNotNull(stringRepo);
        assertEquals("string", stringRepo.get());
        validateIntRepoIsOk();
    }

    // just to ensure String is not a particular case or there is a iterator().next() breaking other cases
    private void validateIntRepoIsOk()
    {
        final Object intRepo = getInstance(new OwbParametrizedTypeImpl(null, Repo.class, Integer.class));
        assertNotNull(intRepo);
        assertEquals(1, Repo.class.cast(intRepo).get());
    }

    public interface Repo<A> {
        A get();
    }

    public static abstract class BaseRepoAware<A>
    {
        @Inject
        protected Repo<A> repo;
    }

    @ApplicationScoped
    public static class ConcreteRepo extends BaseRepoAware<String>
    {
        public String get()
        {
            return repo.get();
        }
    }

    @ApplicationScoped
    public static class StringRepo implements Repo<String>
    {
        @Override
        public String get()
        {
            return "string";
        }
    }

    @ApplicationScoped
    public static class IntRepo implements Repo<Integer>
    {
        @Override
        public Integer get()
        {
            return 1;
        }
    }
}
