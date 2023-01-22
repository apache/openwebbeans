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
package org.apache.webbeans.test.concepts.alternatives.tests;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.AfterTypeDiscovery;
import jakarta.enterprise.inject.spi.Extension;

import org.apache.webbeans.test.AbstractUnitTest;
import org.apache.webbeans.test.concepts.alternatives.alternativebean.CustomAlternativeBean;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author <a href="mailto:struberg@yahoo.de">Mark Struberg</a>
 */
public class CustomAlternativeBeanTest extends AbstractUnitTest
{

    @Test
    public void testNonEnabledAlternativeBean()
    {
        addExtension(new AddCustomAlternativeBeanExtension());
        startContainer(NormalStringProducer.class);

        String val = getInstance(String.class);
        Assert.assertEquals("normal", val);
    }

    @Test
    public void testBeansXmlEnabledAlternativeBean()
    {
        addExtension(new AddCustomAlternativeBeanExtension());
        startContainer("org/apache/webbeans/test/alternatives/customalternatives.xml", NormalStringProducer.class);

        String val = getInstance(String.class);
        Assert.assertEquals("alternative", val);
    }

    @Test
    public void testExtensionEnabledAlternativeBean()
    {
        addExtension(new AddCustomAlternativeBeanExtension());
        addExtension(new EnableCustomAlternativeBeanExtension());
        startContainer(NormalStringProducer.class);

        String val = getInstance(String.class);
        Assert.assertEquals("alternative", val);
    }


    public static class NormalStringProducer
    {
        @Produces
        @Dependent
        public String createNormalString()
        {
            return "normal";
        }
    }


    public static class AddCustomAlternativeBeanExtension implements Extension
    {
        public void addBean(@Observes AfterBeanDiscovery abd)
        {
            abd.addBean(new CustomAlternativeBean());
        }
    }

    public static class EnableCustomAlternativeBeanExtension implements Extension
    {
        public void enableAlternative(@Observes AfterTypeDiscovery atd)
        {
            atd.getAlternatives().add(CustomAlternativeBean.class);
        }
    }
}

