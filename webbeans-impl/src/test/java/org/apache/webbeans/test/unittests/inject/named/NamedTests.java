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
package org.apache.webbeans.test.unittests.inject.named;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.util.Set;

import jakarta.enterprise.inject.spi.AnnotatedConstructor;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.inject.Named;

import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.test.AbstractUnitTest;
import org.apache.webbeans.portable.AnnotatedElementFactory;
import org.apache.webbeans.test.component.CheckWithCheckPayment;
import org.apache.webbeans.test.component.IPayment;
import org.apache.webbeans.test.component.inject.named.NamedFieldWithNamedValue;
import org.apache.webbeans.test.component.inject.named.NamedFieldWithoutNamedValue;
import org.apache.webbeans.test.component.inject.named.NamedOtherWithNamedValue;
import org.apache.webbeans.test.component.inject.named.NamedOtherWithoutNamedValue;
import org.apache.webbeans.test.component.inject.named.NamedPayment_PaymentProcessor;
import org.apache.webbeans.test.component.inject.named.NamedPayment_Value;
import org.apache.webbeans.util.WebBeansUtil;
import org.junit.Assert;
import org.junit.Test;

public class NamedTests extends AbstractUnitTest
{
    @Test
    public void testFieldWithNamedValue() throws Exception
    {
        startContainer(NamedFieldWithNamedValue.class, CheckWithCheckPayment.class, NamedPayment_Value.class, NamedPayment_PaymentProcessor.class);
        NamedFieldWithNamedValue instance = getInstance(NamedFieldWithNamedValue.class);
        Assert.assertEquals("value-named", instance.getPayment().pay());
    }
    
    @Test
    public void testFieldWithoutNamedValue() throws Exception
    {
        startContainer(NamedFieldWithoutNamedValue.class, CheckWithCheckPayment.class, NamedPayment_Value.class, NamedPayment_PaymentProcessor.class);
        NamedFieldWithoutNamedValue instance = getInstance(NamedFieldWithoutNamedValue.class);
        Assert.assertEquals("paymentProcessor-named", instance.getPayment().pay());
    }

    @Test
    public void testOtherWithNamedValue() throws Exception
    {
        startContainer(NamedOtherWithNamedValue.class, CheckWithCheckPayment.class, NamedPayment_Value.class, NamedPayment_PaymentProcessor.class);
        NamedOtherWithNamedValue instance = getInstance(NamedOtherWithNamedValue.class);
        Assert.assertEquals("value-named", instance.getPayment().pay());
    }
    
    @Test(expected=WebBeansConfigurationException.class)
    public void testOtherWithoutNamedValue() throws Exception
    {
        startContainer(NamedOtherWithoutNamedValue.class);
        Bean<NamedOtherWithoutNamedValue> bean = getBean(NamedOtherWithoutNamedValue.class);
        Constructor<NamedOtherWithoutNamedValue> constructor = NamedOtherWithoutNamedValue.class.getDeclaredConstructor(new Class<?>[]{IPayment.class});

        AnnotatedElementFactory annotatedElementFactory = WebBeansContext.getInstance().getAnnotatedElementFactory();
        AnnotatedType<NamedOtherWithoutNamedValue> annotatedType = annotatedElementFactory.getAnnotatedType(constructor.getDeclaringClass());
        AnnotatedConstructor<NamedOtherWithoutNamedValue> annotatedConstructor = annotatedElementFactory.newAnnotatedConstructor(constructor, annotatedType);
        InjectionPoint point =
            WebBeansContext.getInstance().getInjectionPointFactory().buildInjectionPoints(bean, annotatedConstructor).get(0);
                
        String value = qualifier(point);
        
        Assert.assertEquals("", value);
        
        WebBeansUtil.checkInjectionPointNamedQualifier(point);

    }
    
    
    private String qualifier(InjectionPoint injectionPoint)
    {
        Set<Annotation> qualifierset = injectionPoint.getQualifiers();
        Named namedQualifier = null;
        for(Annotation qualifier : qualifierset)
        {
            if(qualifier.annotationType().equals(Named.class))
            {
                namedQualifier = (Named)qualifier;
                break;
            }
        }
        
        if(namedQualifier != null)
        {
            return namedQualifier.value();
        }
        
        return null;
    } 
}
