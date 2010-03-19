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
package org.apache.webbeans.newtests.concepts.alternatives.tests;

import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.util.AnnotationLiteral;

import junit.framework.Assert;

import org.apache.webbeans.newtests.AbstractUnitTest;
import org.apache.webbeans.newtests.concepts.alternatives.common.AlternativeBeanProducer3;
import org.apache.webbeans.newtests.concepts.alternatives.common.AlternativeBeanProducer4;
import org.apache.webbeans.newtests.concepts.alternatives.common.DefaultBeanProducerWithoutDisposes;
import org.apache.webbeans.newtests.concepts.alternatives.common.IProducedBean;
import org.apache.webbeans.newtests.concepts.alternatives.common.QualifierProducerBased;
import org.junit.Test;

public class Alternative4Test  extends AbstractUnitTest {
	
    @Test
    @SuppressWarnings("unchecked")
    public void testDisposerMethodInAlternativeBean()
    {
        Collection<URL> beanXmls = new ArrayList<URL>();
        
        Collection<Class<?>> beanClasses = new ArrayList<Class<?>>();
        beanClasses.add(DefaultBeanProducerWithoutDisposes.class);
        beanClasses.add(AlternativeBeanProducer3.class);
        beanClasses.add(AlternativeBeanProducer4.class);
        
        startContainer(beanClasses, beanXmls);        

        Annotation[] anns = new Annotation[1];
        anns[0] = new AnnotationLiteral<QualifierProducerBased>()
        {
        };

        Set beans = getBeanManager().getBeans(IProducedBean.class, anns);
        System.out.println("Size of the bean set is " + beans.size());
        Bean<IProducedBean> bean = (Bean<IProducedBean>)beans.iterator().next();
        CreationalContext<IProducedBean> cc = getBeanManager().createCreationalContext(bean);
        IProducedBean producedBean = (IProducedBean) getBeanManager().getReference(bean, IProducedBean.class, cc);
        System.out.println(producedBean.getID());
        
        shutDownContainer();
        
        Assert.assertTrue(Boolean.TRUE);

    }

}
