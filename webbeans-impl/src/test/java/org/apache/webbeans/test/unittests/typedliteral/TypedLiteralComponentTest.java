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
package org.apache.webbeans.test.unittests.typedliteral;

import java.lang.annotation.Annotation;
import java.util.List;

import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.enterprise.util.TypeLiteral;

import org.junit.Assert;

import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.test.AbstractUnitTest;
import org.apache.webbeans.test.component.ITypeLiteralComponent;
import org.apache.webbeans.test.component.InjectedTypeLiteralComponent;
import org.apache.webbeans.test.component.TypeLiteralComponent;
import org.junit.Test;

public class TypedLiteralComponentTest extends AbstractUnitTest
{
    @Test
    public void testTypedComponent() throws Throwable
    {
        startContainer(TypeLiteralComponent.class, InjectedTypeLiteralComponent.class);

        TypeLiteralComponent userComponent = getInstance(TypeLiteralComponent.class);
        InjectedTypeLiteralComponent tc = getInstance(InjectedTypeLiteralComponent.class);

        Assert.assertNotNull(tc.getComponent());
        Assert.assertNotNull(userComponent);

        Assert.assertTrue(tc.getComponent() instanceof TypeLiteralComponent);
    }

    @Test
    public void testTypedLiteralComponent() throws Throwable
    {
        startContainer(TypeLiteralComponent.class);

        TypeLiteral<ITypeLiteralComponent<List<String>>> tl = new TypeLiteral<ITypeLiteralComponent<List<String>>>()
        {
        };

        Annotation[] anns = new Annotation[1];
        anns[0] = new AnnotationLiteral<Default>()
        {

        };

        Bean<?> s = WebBeansContext.getInstance().getBeanManagerImpl().getBeans(tl.getType(), anns).iterator().next();
        Assert.assertNotNull(s);
    }

}
