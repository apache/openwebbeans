/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */package org.apache.webbeans.test.producer.broken;

import jakarta.enterprise.inject.spi.DefinitionException;

import org.apache.webbeans.test.AbstractUnitTest;
import org.junit.Test;

/**
 * Test a few situations where producer methods are not allowed
 */
public class BrokenProducesTest extends AbstractUnitTest
{

    @Test(expected = DefinitionException.class)
    public void testProducerMethodInDecorator() throws Exception
    {
        addDecorator(DecoratorWithProducerMethod.class);
        startContainer();
    }

    @Test(expected = DefinitionException.class)
    public void testProducerMethodInInterceptor() throws Exception
    {
        addInterceptor(InterceptorWithProducerMethod.class);
        startContainer();
    }
    @Test(expected = DefinitionException.class)
    public void testProducerFieldInDecorator() throws Exception
    {
        addDecorator(DecoratorWithProducerField.class);
        startContainer();
    }

    @Test(expected = DefinitionException.class)
    public void testProducerFieldInInterceptor() throws Exception
    {
        addInterceptor(InterceptorWithProducerField.class);
        startContainer();
    }
}
