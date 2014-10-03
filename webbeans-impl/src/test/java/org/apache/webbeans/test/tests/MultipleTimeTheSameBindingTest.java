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
package org.apache.webbeans.test.tests;

import org.apache.webbeans.annotation.DefaultLiteral;
import org.apache.webbeans.component.BeanAttributesImpl;
import org.apache.webbeans.test.AbstractUnitTest;
import org.apache.webbeans.test.interceptors.extension.BeforeBeanDiscoveryImplTest;
import org.junit.Test;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessBeanAttributes;
import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Inject;
import javax.inject.Qualifier;
import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.HashSet;
import java.util.Set;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.Assert.assertNotNull;

public class MultipleTimeTheSameBindingTest extends AbstractUnitTest
{
    @Inject
    @TheQualifier(1)
    private TheClass theClass1;

    @Inject
    @TheQualifier(2)
    private TheClass theClass2;

    @Test
    public void run()
    {
        addExtension(new TheExtension());
        startContainer(TheClass.class);
        inject(this);
        assertNotNull(theClass1);
        assertNotNull(theClass2);
    }

    public static class TheExtension implements Extension
    {
        void producerTemplates(@Observes final ProcessBeanAttributes<TheClass> pba)
        {
            final Set<Annotation> annotations = new HashSet<Annotation>(pba.getBeanAttributes().getQualifiers());
            annotations.add(new TheQualifierLitereal(1));
            annotations.add(new TheQualifierLitereal(2));

            pba.setBeanAttributes(new BeanAttributesImpl<TheClass>(pba.getBeanAttributes(), false) {
                public Set<Annotation> getQualifiers() {
                    return annotations;
                }
            });
        }
    }

    public static class TheClass
    {
    }

    public static class TheQualifierLitereal extends AnnotationLiteral<TheQualifier> implements TheQualifier
    {
        private final int val;

        public TheQualifierLitereal(final int val)
        {
            this.val = val;
        }

        @Override
        public int value()
        {
            return val;
        }
    }

    @Target({ TYPE, FIELD })
    @Retention(RUNTIME)
    @Documented
    @Qualifier
    public static @interface TheQualifier
    {
        int value();
    }
}
