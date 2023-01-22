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
package org.apache.webbeans.test.disposes;

import org.junit.Assert;
import org.apache.webbeans.test.AbstractUnitTest;
import org.apache.webbeans.test.disposes.beans.DisposeModel;
import org.apache.webbeans.test.disposes.beans.DisposerMethodBean;
import org.junit.Test;

import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.InjectionPoint;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;

public class DisposerMethodBeanTest extends AbstractUnitTest
{
    @Test
    @SuppressWarnings("unchecked")
    public void testDisposerMethod()
    {
        Collection<String> beanXmls = new ArrayList<String>();
        
        Collection<Class<?>> beanClasses = new ArrayList<Class<?>>();
        beanClasses.add(DisposerMethodBean.class);
        
        startContainer(beanClasses, beanXmls);        
        Bean<DisposeModel> bean = (Bean<DisposeModel>)getBeanManager().getBeans("produce").iterator().next();
         
        CreationalContext<DisposeModel> cc = getBeanManager().createCreationalContext(bean);
        DisposeModel model = (DisposeModel) getBeanManager().getReference(bean, DisposeModel.class, cc);
        bean.destroy(model, cc);
        
        Assert.assertTrue(DisposerMethodBean.OK);

        shutDownContainer();
    }

    @Test
    public void multipleDisposes()
    {
        final Collection<String> beanXmls = new ArrayList<String>();

        final Collection<Class<?>> beanClasses = new ArrayList<Class<?>>();
        beanClasses.add(MultipleDispose.class);

        startContainer(beanClasses, beanXmls); // we had a regression where we were not starting
        shutDownContainer();
    }

    public static class MultipleDispose
    {
        @Produces
        public InputStream is(final InjectionPoint ip)
        {
            return null;
        }

        @Produces
        public URL url(final InjectionPoint ip)
        {
            return null;
        }

        public void dis(final @Disposes InputStream is)
        {
            // no-op
        }

        public void durl(final @Disposes URL url)
        {
            // no-op
        }
    }
}
