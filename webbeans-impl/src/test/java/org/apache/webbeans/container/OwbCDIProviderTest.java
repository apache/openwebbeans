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
package org.apache.webbeans.container;

import org.apache.webbeans.test.AbstractUnitTest;
import org.junit.Test;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Qualifier;
import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class OwbCDIProviderTest extends AbstractUnitTest
{
    @Test
    public void run()
    {
        startContainer(OwbCDIProviderTest.class, QualifiedBean.class);
        assertNotNull(CDI.current());
        assertNotNull(CDI.current().getBeanManager());
        assertFalse(CDI.current().isUnsatisfied());
        assertTrue(CDI.current().isAmbiguous());
    }

    @Test
    public void select()
    {
        startContainer(ABean.class);
        final Instance<ABean> select = CDI.current().select(ABean.class);
        assertFalse(select.isAmbiguous());
        assertFalse(select.isUnsatisfied());
        final ABean bean = select.get();
        assertNotNull(bean);
    }

    @Test
    public void selectQualifier()
    {
        startContainer(QualifiedBean.class, Qual.class);
        final Instance<QualifiedBean> select = CDI.current()
                .select(QualifiedBean.class, new Annotation[0])
                .select(new AnnotationLiteral<Qual>()
                {
                });
        assertFalse(select.isAmbiguous());
        assertFalse(select.isUnsatisfied());
        final QualifiedBean bean = select.get();
        assertNotNull(bean);
    }

    @Test(expected = IllegalStateException.class)
    public void noImplicitStart()
    {
        CDI.current().getBeanManager();
    }

    @ApplicationScoped
    public static class ABean
    {
    }

    @Target(TYPE)
    @Retention(RUNTIME)
    @Qualifier
    public @interface Qual
    {
    }

    @Qual
    @ApplicationScoped
    public static class QualifiedBean
    {
    }
}
