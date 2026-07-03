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
package org.apache.webbeans.test.performance;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.apache.webbeans.component.ConversationBean;
import org.apache.webbeans.component.ProducerMethodBean;
import org.apache.webbeans.test.AbstractUnitTest;
import org.apache.webbeans.test.component.binding.AnyBindingComponent;
import org.apache.webbeans.test.component.binding.DefaultAnyBinding;
import org.apache.webbeans.test.component.binding.NonAnyBindingComponent;
import org.apache.webbeans.test.component.decorator.clean.AccountComponent;
import org.apache.webbeans.test.component.decorator.clean.LargeTransactionDecorator;
import org.apache.webbeans.test.component.decorator.clean.ServiceDecorator;
import org.apache.webbeans.test.component.definition.BeanTypesDefinedBean;
import org.apache.webbeans.test.component.dependent.DependentComponent;
import org.apache.webbeans.test.component.dependent.DependentOwnerComponent;
import org.apache.webbeans.test.component.dependent.MultipleDependentComponent;
import org.apache.webbeans.test.component.dependent.circular.DependentA;
import org.apache.webbeans.test.component.dependent.circular.DependentB;
import org.apache.webbeans.test.component.event.normal.ComponentWithObservable1;
import org.apache.webbeans.test.component.event.normal.ComponentWithObserves1;
import org.apache.webbeans.test.component.event.normal.ComponentWithObserves2;
import org.apache.webbeans.test.component.event.normal.ComponentWithObserves3;
import org.apache.webbeans.test.component.event.normal.ComponentWithObserves4;
import org.apache.webbeans.test.component.event.normal.ComponentWithObserves5;
import org.apache.webbeans.test.component.event.normal.ComponentWithObserves6;
import org.apache.webbeans.test.component.event.normal.ComponentWithObserves7;
import org.apache.webbeans.test.component.event.normal.Transactional;
import org.apache.webbeans.test.component.event.normal.TransactionalInterceptor;
import org.apache.webbeans.test.component.inheritance.InheritFromMultipleParentComponent;
import org.apache.webbeans.test.component.inheritance.InheritFromParentComponent;
import org.apache.webbeans.test.component.inheritance.ParentComponent;
import org.apache.webbeans.test.component.inheritance.ParentComponentSubClass;
import org.apache.webbeans.test.concepts.alternatives.common.AlternativeBean;
import org.apache.webbeans.test.concepts.alternatives.common.Pencil;
import org.apache.webbeans.test.concepts.alternatives.common.PencilProducerBean;
import org.apache.webbeans.test.concepts.alternatives.common.SimpleBean;
import org.apache.webbeans.test.concepts.alternatives.common.SimpleInjectionTarget;
import org.apache.webbeans.test.contexts.session.common.PersonalDataBean;
import org.apache.webbeans.test.injection.circular.beans.CircularApplicationScopedBean;
import org.apache.webbeans.test.injection.circular.beans.CircularDependentScopedBean;
import org.apache.webbeans.test.managed.instance.beans.DependentBean;
import org.apache.webbeans.test.managed.instance.beans.DependentBeanProducer;
import org.apache.webbeans.test.managed.instance.beans.InstanceForDependentBean;
import org.apache.webbeans.test.managed.instance.beans.InstanceInjectedComponent;
import org.apache.webbeans.test.producer.beans.SomeUserBean;
import org.apache.webbeans.test.profields.beans.classproducer.MyProductBean;
import org.apache.webbeans.test.profields.beans.classproducer.MyProductProducer;
import org.apache.webbeans.test.profields.beans.classproducer.ProductInjectedBean;
import org.apache.webbeans.test.profields.beans.stringproducer.GetterStringFieldInjector;
import org.apache.webbeans.test.profields.beans.stringproducer.GetterStringProducerBean;
import org.apache.webbeans.test.profields.beans.stringproducer.InformationConsumerBean;
import org.apache.webbeans.test.profields.beans.stringproducer.MultipleListProducer;
import org.apache.webbeans.test.profields.beans.stringproducer.StringProducerBean;
import org.apache.webbeans.test.promethods.beans.PersonProducerBean;
import org.apache.webbeans.test.promethods.common.Person;
import org.apache.webbeans.test.specalization.AdvancedPenProducer;
import org.apache.webbeans.test.specalization.DefaultPenProducer;
import org.apache.webbeans.test.specalization.Pen;
import org.apache.webbeans.test.component.CheckWithCheckPayment;
import org.apache.webbeans.test.component.CheckWithMoneyPayment;
import org.apache.webbeans.test.component.IPayment;
import org.apache.webbeans.test.component.PaymentProcessorComponent;
import org.apache.webbeans.test.component.decorator.clean.ServiceDecoratorWithCtInjectionPoint;
import org.apache.webbeans.test.contexts.conversation.ConversationScopedInitBean;
import org.apache.webbeans.test.injection.circular.beans.CircularConstructorOrProducerMethodParameterBean;
import org.apache.webbeans.test.injection.circular.beans.CircularNormalInConstructor;
import org.apache.webbeans.test.interceptors.beans.ApplicationScopedBean;
import org.apache.webbeans.test.interceptors.beans.DependentScopedBean;
import org.apache.webbeans.test.interceptors.beans.RequestScopedBean;
import org.apache.webbeans.test.interceptors.business.common.RuntimeExceptionBindingTypeBean;
import org.apache.webbeans.test.interceptors.business.common.TransactionalBaseBean;
import org.apache.webbeans.test.interceptors.business.common.TransactionalChildBean;
import org.apache.webbeans.test.interceptors.business.common.WithInheritedBindingTypeBean;
import org.apache.webbeans.test.interceptors.business.common.WithInheritedStereoTypeInterceptorBean;
import org.apache.webbeans.test.interceptors.business.common.WithStereoTypeInterceptorBean;
import org.apache.webbeans.test.interceptors.common.DependentInterceptor;
import org.apache.webbeans.test.interceptors.dependent.DependentLifecycleBean;
import org.apache.webbeans.test.interceptors.ejb.ManagedBeanWithEjbInterceptor;
import org.apache.webbeans.test.interceptors.ejb.ManagedBeanWithMethodEjbInterceptor;
import org.apache.webbeans.test.interceptors.ejb.ManagedBeanWithoutInterceptor;
import org.apache.webbeans.test.interceptors.factory.ProtectedUsageBean;
import org.apache.webbeans.test.interceptors.factory.SubPackageInterceptedClass;
import org.apache.webbeans.test.interceptors.factory.beans.ClassInterceptedClass;
import org.apache.webbeans.test.interceptors.factory.beans.ClassMultiInterceptedClass;
import org.apache.webbeans.test.interceptors.factory.beans.DecoratedClass;
import org.apache.webbeans.test.interceptors.factory.beans.InterceptionFactoryBeansProducer;
import org.apache.webbeans.test.interceptors.factory.beans.PartialBeanClass;
import org.apache.webbeans.test.interceptors.factory.beans.SomeBaseClass;
import org.apache.webbeans.test.interceptors.factory.beans.TonsOfMethodsInterceptedClass;
import org.apache.webbeans.test.interceptors.inheritance.Deck;
import org.apache.webbeans.test.interceptors.inheritance.DeckChild;
import org.apache.webbeans.test.interceptors.inheritance.DeckStereotyped;
import org.apache.webbeans.test.interceptors.inheritance.DeckStereotypedChild;
import org.apache.webbeans.test.interceptors.inheritance.DeckStereotypedGrandchild;
import org.apache.webbeans.test.interceptors.inheritance.DeckStereotypedNotInherited;
import org.apache.webbeans.test.interceptors.inheritance.DeckStereotypedNotInheritedChild;
import org.apache.webbeans.test.interceptors.inheritance.InterceptorInherited;
import org.apache.webbeans.test.interceptors.lifecycle.NotAnnotatedBean;
import org.apache.webbeans.test.interceptors.lifecycle.LifecycleBean;
import org.apache.webbeans.test.interceptors.lifecycle.inheritance.SubClassBean;
import org.apache.webbeans.test.interceptors.lifecycle.inheritance.SubClassInheritedBean;
import org.apache.webbeans.test.interceptors.priority.InterceptedBean;
import org.apache.webbeans.test.interceptors.resolution.beans.UtilitySampleBean;
import org.apache.webbeans.test.interceptors.resolution.interceptors.SelfInterceptedClass;
import org.apache.webbeans.test.interceptors.resolution.interceptors.SelfInterceptionSubclass;
import org.apache.webbeans.test.component.literals.InstanceTypeLiteralBean;
import org.apache.webbeans.test.promethods.beans.RequestScopedNullPersonProducerBean;
import org.junit.Test;

/**
 * Small unit test to help testing the startup performance.
 * We just stuff lots of classes into it and check how it performs.
 */
public class StartupPerformanceTest extends AbstractUnitTest
{
    private static final int NUMBER_ITERATIONS = 2;

    private static final Logger log = Logger.getLogger(StartupPerformanceTest.class.getName());


    @Test
    public void testPerformance()
    {
        Collection<Class<?>> beanClasses = new ArrayList<>();
        beanClasses.add(PaymentProcessorComponent.class);
        beanClasses.add(InstanceInjectedComponent.class);
        beanClasses.add(CheckWithCheckPayment.class);
        beanClasses.add(CheckWithMoneyPayment.class);
        beanClasses.add(IPayment.class);
        beanClasses.add(ProductInjectedBean.class);
        beanClasses.add(MyProductProducer.class);
        beanClasses.add(MyProductBean.class);
        beanClasses.add(Person.class);
        beanClasses.add(PersonProducerBean.class);
        beanClasses.add(Pen.class);
        beanClasses.add(DefaultPenProducer.class);
        beanClasses.add(AdvancedPenProducer.class);
        beanClasses.add(PersonalDataBean.class);
        beanClasses.add(CircularDependentScopedBean.class);
        beanClasses.add(CircularApplicationScopedBean.class);
        beanClasses.add(AlternativeBean.class);
        beanClasses.add(Pencil.class);
        beanClasses.add(PencilProducerBean.class);
        beanClasses.add(SimpleBean.class);
        beanClasses.add(SimpleInjectionTarget.class);
        beanClasses.add(DependentBean.class);
        beanClasses.add(DependentBeanProducer.class);
        beanClasses.add(InstanceForDependentBean.class);
        beanClasses.add(MyProductBean.class);
        beanClasses.add(MyProductProducer.class);
        beanClasses.add(ProductInjectedBean.class);
        beanClasses.add(StringProducerBean.class);
        beanClasses.add(GetterStringFieldInjector.class);
        beanClasses.add(GetterStringProducerBean.class);
        beanClasses.add(InformationConsumerBean.class);
        beanClasses.add(MultipleListProducer.class);
        beanClasses.add(GetterStringProducerBean.class);
        beanClasses.add(AnyBindingComponent.class);
        beanClasses.add(DefaultAnyBinding.class);
        beanClasses.add(NonAnyBindingComponent.class);
        beanClasses.add(AccountComponent.class);
        beanClasses.add(BeanTypesDefinedBean.class);
        beanClasses.add(DependentA.class);
        beanClasses.add(DependentB.class);
        beanClasses.add(DependentComponent.class);
        beanClasses.add(DependentOwnerComponent.class);
        beanClasses.add(MultipleDependentComponent.class);
        beanClasses.add(ComponentWithObservable1.class);
        beanClasses.add(ComponentWithObserves1.class);
        beanClasses.add(ComponentWithObserves2.class);
        beanClasses.add(ComponentWithObserves3.class);
        beanClasses.add(ComponentWithObserves4.class);
        beanClasses.add(ComponentWithObserves5.class);
        beanClasses.add(ComponentWithObserves6.class);
        beanClasses.add(ComponentWithObserves7.class);
        beanClasses.add(InheritFromMultipleParentComponent.class);
        beanClasses.add(InheritFromParentComponent.class);
        beanClasses.add(ParentComponent.class);
        beanClasses.add(ParentComponentSubClass.class);

        beanClasses.add(Transactional.class);
        addInterceptor(TransactionalInterceptor.class);

        addDecorator(LargeTransactionDecorator.class);
        addDecorator(ServiceDecorator.class);

        // Interceptors - business
        beanClasses.add(WithInheritedStereoTypeInterceptorBean.class);
        beanClasses.add(WithStereoTypeInterceptorBean.class);
        beanClasses.add(TransactionalBaseBean.class);
        beanClasses.add(TransactionalChildBean.class);
        beanClasses.add(RuntimeExceptionBindingTypeBean.class);
        beanClasses.add(WithInheritedBindingTypeBean.class);

        // Interceptors - lifecycle
        beanClasses.add(NotAnnotatedBean.class);
        beanClasses.add(LifecycleBean.class);
        beanClasses.add(LifecycleBean.FooProducer.class);
        beanClasses.add(SubClassBean.class);
        beanClasses.add(SubClassInheritedBean.class);

        // Interceptors - inheritance
        beanClasses.add(Deck.class);
        beanClasses.add(DeckChild.class);
        beanClasses.add(DeckStereotyped.class);
        beanClasses.add(DeckStereotypedChild.class);
        beanClasses.add(DeckStereotypedGrandchild.class);
        beanClasses.add(DeckStereotypedNotInherited.class);
        beanClasses.add(DeckStereotypedNotInheritedChild.class);
        beanClasses.add(InterceptorInherited.class);

        // Interceptors - factory
        beanClasses.add(ClassInterceptedClass.class);
        beanClasses.add(ClassMultiInterceptedClass.class);
        beanClasses.add(TonsOfMethodsInterceptedClass.class);
        beanClasses.add(PartialBeanClass.class);
        beanClasses.add(SomeBaseClass.class);
        beanClasses.add(DecoratedClass.class);
        beanClasses.add(InterceptionFactoryBeansProducer.class);
        beanClasses.add(SubPackageInterceptedClass.class);
        beanClasses.add(ProtectedUsageBean.class);
        // Interceptors - resolution
        beanClasses.add(SelfInterceptedClass.class);
        beanClasses.add(SelfInterceptionSubclass.class);
        beanClasses.add(UtilitySampleBean.class);
        // Interceptors - ejb
        beanClasses.add(ManagedBeanWithEjbInterceptor.class);
        beanClasses.add(ManagedBeanWithMethodEjbInterceptor.class);
        beanClasses.add(ManagedBeanWithoutInterceptor.class);
        // Interceptors - dependent
        beanClasses.add(DependentInterceptor.class);
        beanClasses.add(DependentLifecycleBean.class);
        // Interceptors - beans
        beanClasses.add(ApplicationScopedBean.class);
        beanClasses.add(RequestScopedBean.class);
        beanClasses.add(DependentScopedBean.class);
        // Interceptors - priority
        beanClasses.add(InterceptedBean.class);
        // Contexts - singleton
        beanClasses.add(SomeUserBean.class);
        // Contexts - request
        beanClasses.add(RequestScopedBean.class);
        beanClasses.add(RequestScopedNullPersonProducerBean.class);
        // Contexts - conversation
        beanClasses.add(ConversationBean.class);
        beanClasses.add(ConversationScopedInitBean.class);
        // Decorators
        beanClasses.add(ServiceDecoratorWithCtInjectionPoint.class);
        // Managed instance
        beanClasses.add(InstanceTypeLiteralBean.class);
        // Circular injection
        beanClasses.add(CircularConstructorOrProducerMethodParameterBean.class);
        beanClasses.add(CircularNormalInConstructor.class);
        // Producers
        beanClasses.add(ProducerMethodBean.class);

        long start = System.nanoTime();
        for (int i=0; i < NUMBER_ITERATIONS; i++)
        {
            startupWithClasses(beanClasses);
        }
        long stop = System.nanoTime();
        log.info("Starting up " + beanClasses.size() + " classes " + NUMBER_ITERATIONS + " times took " + TimeUnit.NANOSECONDS.toMillis(stop - start) + " ms");
    }

    private void startupWithClasses(Collection<Class<?>> beanClasses)
    {
        Collection<String> beanXmls = new ArrayList<>();


        startContainer(beanClasses, beanXmls);

        // do nothing ...

        shutDownContainer();
    }
}
