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
package org.apache.webbeans.test.containertests;

import java.util.Set;

import jakarta.enterprise.inject.spi.Bean;

import org.junit.Assert;

import org.apache.webbeans.test.AbstractUnitTest;
import org.apache.webbeans.test.annotation.binding.AnnotationWithBindingMember;
import org.apache.webbeans.test.annotation.binding.AnnotationWithNonBindingMember;
import org.apache.webbeans.test.component.BindingComponent;
import org.apache.webbeans.test.component.NonBindingComponent;
import org.junit.Test;

public class ComponentResolutionByTypeTest extends AbstractUnitTest
{
    public @AnnotationWithBindingMember(value = "B", number = 3)
    BindingComponent s1 = null;
    public @AnnotationWithBindingMember(value = "B")
    BindingComponent s2 = null;

    public @AnnotationWithNonBindingMember(value = "B", arg1 = "arg1", arg2 = "arg2")
    NonBindingComponent s3 = null;
    public @AnnotationWithNonBindingMember(value = "B", arg1 = "arg11", arg2 = "arg21")
    NonBindingComponent s4 = null;
    public @AnnotationWithNonBindingMember(value = "C", arg1 = "arg11", arg2 = "arg21")
    NonBindingComponent s5 = null;

    private static final String CLAZZ_NAME = ComponentResolutionByTypeTest.class.getName();

    

    @Test
    public void testBindingTypeOk() throws Exception
    {
        startContainer(this.getClass());
        getBeanManager().getBeans(BindingComponent.class, ComponentResolutionByTypeTest.class.getDeclaredField("s1").getAnnotations());
    }

    @Test
    public void testBindingTypeNonOk() throws Exception
    {
        startContainer(this.getClass());
        getBeanManager().getBeans(BindingComponent.class, ComponentResolutionByTypeTest.class.getDeclaredField("s2").getAnnotations());
    }

    @Test
    public void testNonBindingTypeOk1() throws Exception
    {
        startContainer(this.getClass());
        getBeanManager().getBeans(NonBindingComponent.class, ComponentResolutionByTypeTest.class.getDeclaredField("s3").getAnnotations());
    }

    @Test
    public void testNonBindingTypeOk2() throws Exception
    {
        startContainer(this.getClass());
        Set<Bean<?>> beans = getBeanManager().getBeans(NonBindingComponent.class, ComponentResolutionByTypeTest.class.getDeclaredField("s4").getAnnotations());
        Assert.assertNotNull(beans);
        Assert.assertTrue(beans.isEmpty());
    }

    @Test
    public void testNonBindingTypeNonOk() throws Exception
    {
        startContainer(this.getClass());
        getBeanManager().getBeans(NonBindingComponent.class, ComponentResolutionByTypeTest.class.getDeclaredField("s5").getAnnotations());
    }

}
