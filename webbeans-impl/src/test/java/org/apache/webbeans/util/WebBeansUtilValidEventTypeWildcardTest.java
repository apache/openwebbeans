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
package org.apache.webbeans.util;

import jakarta.enterprise.context.ApplicationScoped;

import org.apache.webbeans.test.AbstractUnitTest;
import org.junit.Test;

/**
 * {@link WebBeansUtil#validEventType(java.lang.reflect.Type, java.lang.reflect.Type)} must not throw
 * on every {@link org.apache.webbeans.config.OwbWildcardTypeImpl} in resolved args (OWB-1128; CDI TCK
 * {@code ParameterizedEventTest#testWildcardIsResolvable}). {@link Enum} hits {@link GenericsUtil}'s
 * shortcut that adds such a wildcard — the old blanket check failed here.
 *
 * @see <a href="https://issues.apache.org/jira/browse/OWB-1128">OWB-1128</a>
 */
public class WebBeansUtilValidEventTypeWildcardTest extends AbstractUnitTest
{

    @Test
    public void validEventTypeAcceptsOwbWildcardFromEnumResolution()
    {
        startContainer(Sentinel.class);

        final WebBeansUtil util = getWebBeansContext().getWebBeansUtil();

        util.validEventType(Enum.class, Object.class);
    }

    @ApplicationScoped
    public static class Sentinel
    {
    }
}
