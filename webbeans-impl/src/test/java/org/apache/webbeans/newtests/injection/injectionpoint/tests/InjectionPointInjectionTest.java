/*
 * Copyright 2012 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.webbeans.newtests.injection.injectionpoint.tests;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.Collection;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;

import org.apache.webbeans.annotation.DefaultLiteral;
import org.apache.webbeans.newtests.AbstractUnitTest;
import org.apache.webbeans.newtests.injection.injectionpoint.beans.AbstractInjectionPointOwner;
import org.apache.webbeans.newtests.injection.injectionpoint.beans.ConstructorInjectionPointOwner;
import org.apache.webbeans.newtests.injection.injectionpoint.beans.FieldInjectionPointOwner;
import org.apache.webbeans.newtests.injection.injectionpoint.beans.InjectionPointBeansOwner;
import org.apache.webbeans.newtests.injection.injectionpoint.beans.InjectionPointObserver;
import org.apache.webbeans.newtests.injection.injectionpoint.beans.InjectionPointOwnerInstance;
import org.apache.webbeans.newtests.injection.injectionpoint.beans.InjectionPointOwnerProducer;
import org.apache.webbeans.newtests.injection.injectionpoint.beans.MethodInjectionPointOwner;
import org.apache.webbeans.newtests.injection.injectionpoint.beans.ProducerMethodInjectionPointOwner;
import org.junit.Test;

public class InjectionPointInjectionTest extends AbstractUnitTest {

    @Test
    public void testInjectionPointInjection()
    {
        Collection<Class<?>> beanClasses = new ArrayList<Class<?>>();
        beanClasses.add(ConstructorInjectionPointOwner.class);
        beanClasses.add(FieldInjectionPointOwner.class);
        beanClasses.add(MethodInjectionPointOwner.class);
        beanClasses.add(InjectionPointObserver.class);
        beanClasses.add(InjectionPointBeansOwner.class);
        beanClasses.add(InjectionPointOwnerProducer.class);
        beanClasses.add(ProducerMethodInjectionPointOwner.class);
        beanClasses.add(ConstructorInjectionPointOwner.SomeInnerClassWithInstructorInjectionPoint.class);
        startContainer(beanClasses, null);

        Bean<InjectionPointBeansOwner> bean = (Bean<InjectionPointBeansOwner>) getBeanManager().getBeans(InjectionPointBeansOwner.class).iterator().next();
        CreationalContext<InjectionPointBeansOwner> cc = getBeanManager().createCreationalContext(bean);
        InjectionPointBeansOwner owner = (InjectionPointBeansOwner) getBeanManager().getReference(bean, InjectionPointBeansOwner.class, cc);

        ConstructorInjectionPointOwner.SomeInnerClassWithInstructorInjectionPoint innerClass
                = getInstance(ConstructorInjectionPointOwner.SomeInnerClassWithInstructorInjectionPoint.class);
        assertThat(innerClass, is(notNullValue()));

        assertThat(owner.getConstructorInjectionName(), is("constructorInjection"));
        assertThat(owner.getFieldInjectionName(), is("fieldInjection"));
        assertThat(owner.getMethodInjectionName(), is("methodInjection"));
        assertThat(owner.getProducerMethodInjectionName(), is("producerMethodInjection"));
        assertThat(owner.getConstructorInjectionInstanceName(), is("constructorInjectionInstance"));
        assertThat(owner.getFieldInjectionInstanceName(), is("fieldInjectionInstance"));
        assertThat(owner.getMethodInjectionInstanceName(), is("methodInjectionInstance"));
        assertThat(owner.getProducerMethodInjectionInstanceName(), is("producerMethodInjectionInstance"));
        assertThat(owner.getObserverInjectionName(), is("observerInjection"));
        assertThat(owner.getParameterizedObserverInjectionName(), is("observeParameterizedInjectionPoint"));

        shutDownContainer();
    }

    @Test
    public void testPackagePrivateInjectionPoint()
    {
        startContainer(PackageMethodInjectionPointOwner.class, PackageInjectionPointOwner.class);

        PackageInjectionPointOwner pipo = getInstance(PackageInjectionPointOwner.class);
        assertThat(pipo, is(notNullValue()));
        assertThat(pipo.getName(), is("pimp"));
    }

    @Test
    public void testDynamicResolvingInjectionPoint()
    {
        startContainer(InjectionPointOwnerProducer.class, ProducerMethodInjectionPointOwner.class, AbstractInjectionPointOwner.class);

        ProducerMethodInjectionPointOwner producedInstance = getInstance(ProducerMethodInjectionPointOwner.class);
        assertThat(producedInstance, notNullValue());
        InjectionPoint ip = producedInstance.getInjectionPoint();
        assertThat(ip, notNullValue());
        assertThat(ip.getAnnotated(), nullValue());
    }


    @Test
    public void testDynamicResolvingInstanceInjectionPoint()
    {
        startContainer(InjectionPointOwnerInstance.class, InjectionPointOwnerProducer.class,
                       ProducerMethodInjectionPointOwner.class, AbstractInjectionPointOwner.class,
                       MethodInjectionPointOwner.class);

        InjectionPointOwnerInstance producedInstanceOwner = getInstance(InjectionPointOwnerInstance.class);
        assertThat(producedInstanceOwner, notNullValue());

        MethodInjectionPointOwner ipOwner = producedInstanceOwner.getIpOwnerInstance().select(new DefaultLiteral()).get();
        assertThat(ipOwner, notNullValue());

        InjectionPoint ip = ipOwner.getInjectionPoint();
        assertNotNull(ip);
        assertNotNull(ip.getAnnotated());
        assertThat(ip.getAnnotated(), instanceOf(AnnotatedField.class));
    }


}
