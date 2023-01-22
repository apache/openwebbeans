/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.webbeans.test.specalization.multiple;

import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.Specializes;
import jakarta.enterprise.inject.spi.DeploymentException;

import org.junit.Assert;
import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.test.AbstractUnitTest;
import org.junit.Test;


/**
 * Test that a DeploymentException happens if there are multiple
 * specializations for the same producer method.
 */
public class MultipleSpecializedProducerMethodsTest extends AbstractUnitTest
{

    @Test
    public void testFailMultipleSpecializedProducerMethods()
    {
        try
        {
            startContainer(BaseProducerMethod.class, SpecializedProducer1.class, SpecializedProducer2.class);
            Assert.fail("OWB doesn't properly detect conflicting producer method specialization");
        }
        catch (Exception e)
        {
            Assert.assertTrue(e instanceof WebBeansConfigurationException);
            Assert.assertTrue(e.getCause() instanceof DeploymentException);
            Assert.assertTrue(e.getCause().getMessage().contains("Multiple specializations"));
        }
    }

    public static class BaseProducerMethod
    {
        @Produces
        public String producerMethod()
        {
            return "BASE";
        }
    }

    public static class SpecializedProducer1 extends BaseProducerMethod
    {
        @Produces
        @Specializes
        public String producerMethod()
        {
            return "SPEZ1";
        }
    }

    public static class SpecializedProducer2 extends BaseProducerMethod
    {
        @Produces
        @Specializes
        public String producerMethod()
        {
            return "SPEZ2";
        }
    }
}
