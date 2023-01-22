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
package org.apache.webbeans.test.portable.injectiontarget;

import java.util.ArrayList;
import java.util.Collection;

import jakarta.enterprise.context.spi.Contextual;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.InjectionTarget;

import org.junit.Assert;

import org.apache.webbeans.test.AbstractUnitTest;
import org.junit.Test;

public class InjectionTargetTest extends AbstractUnitTest
{
    public static class MyContextual<T> implements Contextual<T>
    {

        @Override
        public T create(CreationalContext<T> context)
        {
            return null;
        }

        @Override
        public void destroy(T instance, CreationalContext<T> context)
        {
            
        }
        
    }
    
    @Test
    public void testInjectionTarget()
    {
        Collection<Class<?>> classes = new ArrayList<Class<?>>();
        classes.add(PersonModel.class);
        startContainer(classes);

        final AnnotatedType<PersonModel> annotatedType = getBeanManager().createAnnotatedType(PersonModel.class);
        InjectionTarget<PersonModel> model = getBeanManager().getInjectionTargetFactory(annotatedType).createInjectionTarget(null);
        PersonModel person = model.produce(getBeanManager().createCreationalContext(new InjectionTargetTest.MyContextual<>()));
        Assert.assertNotNull(person);

        // check that createInjectionTarget did not deploy a second observer method
        getBeanManager().getEvent().fire("test");
        Assert.assertEquals(1, PersonModel.getEventCount());
        shutDownContainer();
        
    }

}
