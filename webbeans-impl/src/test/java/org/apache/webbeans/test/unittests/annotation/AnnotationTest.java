/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.apache.webbeans.test.unittests.annotation;

import java.lang.annotation.Annotation;

import org.apache.webbeans.annotation.WebBeansAnnotation;
import org.apache.webbeans.proxy.JavassistProxyFactory;
import org.apache.webbeans.test.annotation.binding.AnnotationWithBindingMember;
import org.apache.webbeans.test.servlet.TestContext;
import org.junit.Assert;
import org.junit.Test;

public class AnnotationTest extends TestContext
{
    public AnnotationTest()
    {
        super(AnnotationTest.class.getName());
    }

    @Test
    public void testAnnotationLiteral()
    {
        Annotation annotation = AnnotatedClass.class.getAnnotation(AnnotationWithBindingMember.class);
        WebBeansAnnotation webBeansAnnotation = new WebBeansAnnotation(AnnotationWithBindingMember.class);

        LiteralType al = new LiteralType()
        {

            public int number()
            {
                // TODO Auto-generated method stub
                return 5;
            }

            public String value()
            {
                return "Gurkan";
            }

        };

        webBeansAnnotation.setMemberValue("value", "Gurkan");
        webBeansAnnotation.setMemberValue("number", 5);

        Assert.assertTrue(annotation.equals(al));

    }

    @Test
    public void testAnnotationWebBeans()
    {
        Annotation annotation = AnnotatedClass.class.getAnnotation(AnnotationWithBindingMember.class);
        Annotation defAnnotation = DefaultAnnotatedClass.class.getAnnotation(AnnotationWithBindingMember.class);
        WebBeansAnnotation webBeansAnnotation = JavassistProxyFactory.createNewAnnotationProxy(AnnotationWithBindingMember.class);

        webBeansAnnotation.setMemberValue("value", "Gurkan");
        webBeansAnnotation.setMemberValue("number", 5);

        Assert.assertTrue(annotation.equals(webBeansAnnotation));
        Assert.assertTrue(webBeansAnnotation.equals(annotation));

        WebBeansAnnotation webBeansAnnotation2 = JavassistProxyFactory.createNewAnnotationProxy(AnnotationWithBindingMember.class);

        Assert.assertTrue(webBeansAnnotation2.equals(defAnnotation));
        Assert.assertTrue(defAnnotation.equals(webBeansAnnotation2));
        Assert.assertTrue(!webBeansAnnotation2.equals(annotation));
        Assert.assertTrue(!annotation.equals(webBeansAnnotation2));
    }

}
