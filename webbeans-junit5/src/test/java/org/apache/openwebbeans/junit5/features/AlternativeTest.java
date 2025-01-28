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
package org.apache.openwebbeans.junit5.features;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Default;
import javax.inject.Inject;
import org.apache.openwebbeans.junit5.Cdi;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Cdi(disableDiscovery = true, classes = {
        AlternativeTest.Service.class, AlternativeTest.Provider.class, AlternativeTest.DefaultProvider.class, AlternativeTest.AlternativeProvider.class
}, alternatives = AlternativeTest.AlternativeProvider.class)
public class AlternativeTest
{
    @Inject
    private Service service;

    @Test
    void test1()
    {
        assertEquals("alternative", service.run());
    }

    public interface Provider
    {
        String provide();
    }

    @Alternative
    public static class AlternativeProvider implements Provider
    {
        @Override
        public String provide()
        {
            return "alternative";
        }
    }

    @Default
    public static class DefaultProvider implements Provider
    {
        @Override
        public String provide()
        {
            return "default";
        }
    }

    @ApplicationScoped
    public static class Service
    {

        @Inject
        private Provider provider;

        public String run()
        {
            return provider.provide();
        }

    }
}
