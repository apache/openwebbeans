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
package org.apache.webbeans.test.portable.injectiontarget.supportInjections;

import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.InjectionTarget;

import org.apache.webbeans.test.promethods.beans.PersonProducerBean;
import org.junit.Assert;

import org.apache.webbeans.test.AbstractUnitTest;
import org.junit.Test;

public class SupportInjectionTest extends AbstractUnitTest
{

    @Test
    public void testInjectionTarget()
    {
        startContainer(Chair.class, Table.class, PersonProducerBean.class);

        final AnnotatedType<SupportInjectionBean> annotatedType = getBeanManager().createAnnotatedType(SupportInjectionBean.class);
        InjectionTarget<SupportInjectionBean> model = getBeanManager().getInjectionTargetFactory(annotatedType).createInjectionTarget(null);
        CreationalContext cc = getBeanManager().createCreationalContext(null);
        SupportInjectionBean instance = model.produce(cc);
        
        model.inject(instance, cc);
        
        Assert.assertNotNull(instance.getChair());
        Assert.assertNotNull(instance.getTable());
        Assert.assertNotNull(instance.getPerson());
        
        model.postConstruct(instance);
        Assert.assertTrue(SupportInjectionBean.POST_COSTRUCT);
        
        model.preDestroy(instance);
        Assert.assertTrue(SupportInjectionBean.PRE_DESTROY);
        
        shutDownContainer();
        
    }

}
