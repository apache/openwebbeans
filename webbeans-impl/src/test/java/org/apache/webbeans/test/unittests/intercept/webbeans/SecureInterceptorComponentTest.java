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
package org.apache.webbeans.test.unittests.intercept.webbeans;

import java.util.ArrayList;
import java.util.Collection;

import org.junit.Assert;

import org.apache.webbeans.test.AbstractUnitTest;
import org.apache.webbeans.test.component.intercept.webbeans.SecureComponent;
import org.apache.webbeans.test.component.intercept.webbeans.SecureInterceptor;
import org.junit.Test;

public class SecureInterceptorComponentTest extends AbstractUnitTest
{

    @Test
    public void testSecureInterceptor()
    {
        Collection<String> beanXmls = new ArrayList<String>();
        beanXmls.add(getXmlPath(this.getClass().getPackage().getName(), this.getClass().getSimpleName()));

        Collection<Class<?>> beanClasses = new ArrayList<Class<?>>();
        beanClasses.add(SecureComponent.class);
        beanClasses.add(SecureInterceptor.class);
        startContainer(beanClasses, beanXmls);


        SecureComponent secureComponent = getInstance(SecureComponent.class);

        Assert.assertNotNull(secureComponent);

        boolean value = secureComponent.checkout();

        Assert.assertTrue(SecureInterceptor.CALL);
        Assert.assertTrue(value);

        shutDownContainer();
    }

}
