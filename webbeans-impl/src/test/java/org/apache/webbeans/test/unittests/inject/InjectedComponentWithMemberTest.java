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
package org.apache.webbeans.test.unittests.inject;


import org.junit.Assert;

import org.apache.webbeans.test.AbstractUnitTest;
import org.apache.webbeans.test.annotation.binding.AnnotationWithBindingMember;
import org.apache.webbeans.test.annotation.binding.AnnotationWithNonBindingMember;
import org.apache.webbeans.test.component.BindingComponent;
import org.apache.webbeans.test.component.NonBindingComponent;
import org.junit.Test;

import jakarta.enterprise.util.AnnotationLiteral;

public class InjectedComponentWithMemberTest extends AbstractUnitTest
{
    @Test
    public void testTypedComponent() throws Throwable
    {
        startContainer(BindingComponent.class, NonBindingComponent.class);

        Assert.assertNotNull(getBean(BindingComponent.class, new AnnotationWithBindingMemberLiteral("B", 3)));
        Assert.assertNull(getBean(BindingComponent.class, new AnnotationWithBindingMemberLiteral("B", 7)));
        Assert.assertNull(getBean(BindingComponent.class, new AnnotationWithBindingMemberLiteral("X", 3)));
        Assert.assertNull(getBean(BindingComponent.class, new AnnotationWithBindingMemberLiteral("X", 7)));

        NonBindingComponent nbcomp = getInstance(NonBindingComponent.class, new AnnotationWithNonBindingMemberLiteral("B", "x", "y"));
        NonBindingComponent nbcomp2 = getInstance(NonBindingComponent.class, new AnnotationWithNonBindingMemberLiteral("B", "1", "2"));
        Assert.assertEquals(nbcomp.self(), nbcomp2.self());

        BindingComponent bc = nbcomp.getComponent();
        Assert.assertTrue(bc != null);

        BindingComponent bindingComponent = getInstance(BindingComponent.class, new AnnotationWithBindingMemberLiteral("B", 3));
        Assert.assertEquals(bindingComponent.self(), bc.self());
    }

    public class AnnotationWithBindingMemberLiteral extends AnnotationLiteral<AnnotationWithBindingMember> implements AnnotationWithBindingMember
    {
        private final String value;
        private final int number;

        public AnnotationWithBindingMemberLiteral(String value, int number) {
            this.value = value;
            this.number = number;
        }

        @Override
        public String value() {
            return value;
        }

        @Override
        public int number() {
            return number;
        }
    }

    public class AnnotationWithNonBindingMemberLiteral extends AnnotationLiteral<AnnotationWithNonBindingMember> implements AnnotationWithNonBindingMember
    {
        private final String value;
        private final String arg1;
        private final String arg2;

        public AnnotationWithNonBindingMemberLiteral(String value, String arg1, String arg2) {
            this.value = value;
            this.arg1 = arg1;
            this.arg2 = arg2;
        }

        @Override
        public String value() {
            return value;
        }

        @Override
        public String arg1() {
            return arg1;
        }

        @Override
        public String arg2() {
            return arg2;
        }
    }

}
