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
package org.apache.openwebbeans.junit5.parameter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.inject.Qualifier;
import org.apache.openwebbeans.junit5.Cdi;
import org.apache.openwebbeans.junit5.bean.MyService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

@Cdi(classes = MyService.class)
class ParameterResolutionTest
{
    @Test
    void testThatParameterGetsInjected(MyService service)
    {
        assertEquals("ok", service.ok());
    }

    static class AnotherParameterResolver implements ParameterResolver
    {
        @Override
        public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
                throws ParameterResolutionException {
            return true;
        }

        @Override
        public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
                throws ParameterResolutionException {
            return null;
        }
    }

    @Qualifier
    @Target({ ElementType.TYPE, ElementType.METHOD, ElementType.PARAMETER, ElementType.FIELD })
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @interface NotResolvedAsCdiBeanIntoJunitParameter {
    }

    @Test
    @ExtendWith(AnotherParameterResolver.class)
    void testThatParameterDoesNotGetInjectedDueToQualifier(@NotResolvedAsCdiBeanIntoJunitParameter MyService service)
    {
        assertNull(service);
    }

    @Test
    @ExtendWith(AnotherParameterResolver.class)
    void testThatParameterDoesNotGetInjectedDueToDontInject(@Cdi.DontInject MyService service)
    {
        assertNull(service);
    }
}
