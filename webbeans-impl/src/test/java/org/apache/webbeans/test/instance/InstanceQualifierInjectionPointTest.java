/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.webbeans.test.instance;

import org.apache.webbeans.annotation.AnyLiteral;
import org.apache.webbeans.test.AbstractUnitTest;
import org.junit.Test;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Inject;
import javax.inject.Qualifier;
import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

public class InstanceQualifierInjectionPointTest extends AbstractUnitTest
{
    @Inject
    private QualifiersHolder holder;

    @Inject
    @Any
    private Instance<ShardContract> instance1;

    @Inject
    private Instance<ShardContract> instance2;


    @Test
    public void checkQualfiers() {
        startContainer(Arrays.<Class<?>>asList(
            Qualifier1.class,
            QualifiersHolder.class,
            Factory.class), Collections.<String>emptyList(), true);

        assertNotNull(instance1.select(new AnnotationLiteral<Qualifier1>() {}).get());
        assertEquals(1, holder.getQualifiers().size());
        assertEquals(Qualifier1.class, holder.getQualifiers().iterator().next().annotationType());

        assertNotNull(instance2.select(AnyLiteral.INSTANCE).get());
        assertEquals(1, holder.getQualifiers().size());
        assertEquals(Any.class, holder.getQualifiers().iterator().next().annotationType());

        assertNotNull(instance2.get());
        assertEquals(1, holder.getQualifiers().size());
        assertEquals(Default.class, holder.getQualifiers().iterator().next().annotationType());



    }


    public static class Factory
    {
        @Inject
        @Any
        private Instance<ShardContract> instance;

        @Inject
        private QualifiersHolder holder;

        @Produces
        @Qualifier1
        @Default
        public ShardContract produces(final InjectionPoint ip)
        {
            holder.setQualifiers(ip.getQualifiers());
            return new ShardContract()
            {
            };
        }
    }

    @Target(METHOD)
    @Retention(RUNTIME)
    @Qualifier
    public @interface Qualifier1
    {
    }

    public interface ShardContract
    {
    }

    @ApplicationScoped
    public static class QualifiersHolder
    {
        private Collection<Annotation> qualifiers;

        public Collection<Annotation> getQualifiers()
        {
            return qualifiers;
        }

        public void setQualifiers(final Collection<Annotation> qualifiers)
        {
            this.qualifiers = qualifiers;
        }
    }
}
