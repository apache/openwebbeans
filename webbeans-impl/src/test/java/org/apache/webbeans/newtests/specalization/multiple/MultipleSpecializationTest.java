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
package org.apache.webbeans.newtests.specalization.multiple;

import java.util.ArrayList;
import java.util.Collection;
import junit.framework.Assert;
import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.exception.inject.DefinitionException;
import org.apache.webbeans.exception.inject.InconsistentSpecializationException;
import org.apache.webbeans.newtests.AbstractUnitTest;
import org.junit.Test;

public class MultipleSpecializationTest extends AbstractUnitTest
{
    /**
     * Tests that multiple specialization must be possible
     */
    @Test
    public void testMultipleSpecialization()
    {
        Collection<Class<?>> beanClasses = new ArrayList<Class<?>>();
        beanClasses.add(BeanA.class);
        beanClasses.add(BeanB.class);
        beanClasses.add(BeanC.class);

        startContainer(beanClasses, null);

        BeanA beanA = getInstance(BeanA.class);
        Assert.assertEquals(BeanC.class, beanA.getBeanClass());

        beanA = getInstance("beanA");
        Assert.assertEquals(BeanC.class, beanA.getBeanClass());

        BeanB beanB = getInstance(BeanB.class);
        Assert.assertEquals(BeanC.class, beanB.getBeanClass());

        shutDownContainer();
    }
    
    /**
     * Tests that a specialization must not have a @Named annotation
     */
    @Test
    public void testFailMultipleSpecializationWithNamed()
    {
        Exception occuredException = null;
        
        try
        {
            Collection<Class<?>> beanClasses = new ArrayList<Class<?>>();
            beanClasses.add(BeanA.class);
            beanClasses.add(BeanB.class);
            beanClasses.add(BeanC.class);
            beanClasses.add(BeanD.class);

            startContainer(beanClasses, null);
        }
        catch (Exception e)
        {
            occuredException = e;
        }

        Assert.assertNotNull(occuredException);
        Assert.assertEquals(WebBeansConfigurationException.class.getName(), occuredException.getClass().getName());
        Assert.assertEquals(DefinitionException.class.getName(), occuredException.getCause().getClass().getName());
        
        shutDownContainer();
    }
}
