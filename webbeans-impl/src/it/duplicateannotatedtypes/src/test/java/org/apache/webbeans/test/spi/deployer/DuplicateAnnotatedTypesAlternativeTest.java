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
 */
package org.apache.webbeans.test.spi.deployer;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.LinkedHashSet;
import java.util.Set;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Typed;
import jakarta.enterprise.inject.spi.AnnotatedConstructor;
import jakarta.enterprise.inject.spi.AnnotatedField;
import jakarta.enterprise.inject.spi.AnnotatedMethod;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.BeforeBeanDiscovery;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.CDI;

import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.spi.ContainerLifecycle;
import org.junit.Assert;
import org.junit.Test;

public class DuplicateAnnotatedTypesAlternativeTest
{
    public static final int PLAIN_DUPLICATE_COUNT = 256;

    @Test
    public void shouldBootWithXmlAlternativeAndDuplicateAnnotatedTypes()
    {
        ContainerLifecycle containerLifecycle = WebBeansContext.getInstance().getService(ContainerLifecycle.class);
        containerLifecycle.startApplication(null);

        try
        {
            Assert.assertNotNull(CDI.current().select(SelectedType.class).get());
        }
        finally
        {
            containerLifecycle.stopApplication(null);
        }
    }

    public interface SelectedType
    {
        String value();
    }

    // will be made an Alternative via Extension
    @Typed(SelectedType.class)
    public static class XmlConfiguredAlternativeBean implements SelectedType
    {
        @Override
        public String value()
        {
            return "selected";
        }
    }

}
