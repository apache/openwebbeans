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
package org.apache.webbeans.configurator;

import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.test.AbstractUnitTest;
import org.junit.Test;

import javax.enterprise.inject.spi.AnnotatedConstructor;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.configurator.AnnotatedConstructorConfigurator;
import javax.enterprise.inject.spi.configurator.AnnotatedFieldConfigurator;
import javax.enterprise.inject.spi.configurator.AnnotatedMethodConfigurator;
import javax.enterprise.inject.spi.configurator.AnnotatedParameterConfigurator;
import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertTrue;


public class TrackingAnnotatedTypeConfiguratorImplTest extends AbstractUnitTest {

    @Test
    public void shouldTrackAllConfiguratorActions() {
        AnnotatedTypeConfiguratorImpl<MyBean> base = new FullStubTypeConfigurator<>();

        TrackingAnnotatedTypeConfiguratorImpl<MyBean> tracking = new TrackingAnnotatedTypeConfiguratorImpl<>(base);

        tracking.add(getDummyAnn());
        tracking.remove(a -> a.annotationType().equals(DummyAnn.class));
        tracking.removeAll();

        tracking.fields().forEach(f -> {
            f.add(getDummyAnn());
            f.remove(a -> a.annotationType().equals(DummyAnn.class));
            f.removeAll();
        });

        tracking.methods().forEach(m -> {
            m.add(getDummyAnn());
            m.remove(a -> a.annotationType().equals(DummyAnn.class));
            m.removeAll();

            m.params().forEach(p -> {
                p.add(getDummyAnn());
                p.remove(a -> a.annotationType().equals(DummyAnn.class));
                p.removeAll();
            });
        });

        tracking.constructors().forEach(c -> {
            c.add(getDummyAnn());
            c.remove(a -> a.annotationType().equals(DummyAnn.class));
            c.removeAll();

            c.params().forEach(p -> {
                p.add(getDummyAnn());
                p.remove(a -> a.annotationType().equals(DummyAnn.class));
                p.removeAll();
            });
        });

        tracking.filterFields(f -> true).collect(Collectors.toList());
        tracking.filterMethods(m -> true).collect(Collectors.toList());
        tracking.filterConstructors(c -> true).collect(Collectors.toList());

        final String passivationId = tracking.getPassivationId();

        assertTrue(passivationId.contains("+@interface " + DummyAnn.class.getName()));
        assertTrue(passivationId.contains("--")); // at least one removeAll()
        assertTrue(passivationId.contains("-@")); // from any predicate matched removal
        assertTrue(passivationId.contains("-f@")); // filterFields
        assertTrue(passivationId.contains("-m@")); // filterMethods
        assertTrue(passivationId.contains("-c@")); // filterConstructors
        assertTrue(passivationId.contains("-p@")); // param filter
    }

    public static Annotation getDummyAnn() {
        return WithDummyAnn.class.getAnnotation(DummyAnn.class);
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface DummyAnn {}

    @DummyAnn
    static class WithDummyAnn {}

    public static class MyBean {
        @DummyAnn
        public String field;

        @DummyAnn
        public void method(@DummyAnn String param) {}

        @DummyAnn
        public MyBean() {}
    }

    public static class FullStubTypeConfigurator<T> extends AnnotatedTypeConfiguratorImpl<MyBean> {
        public FullStubTypeConfigurator() {
            super(WebBeansContext.currentInstance(),
                  WebBeansContext.currentInstance()
                                 .getBeanManagerImpl()
                                 .createAnnotatedType(MyBean.class));
        }

        @Override
        public Set<AnnotatedFieldConfigurator<? super MyBean>> fields() {
            Set<AnnotatedFieldConfigurator<? super MyBean>> annotatedFieldConfigurators = new HashSet<>();
            annotatedFieldConfigurators.add(new FullStubFieldConfigurator<>());
            return annotatedFieldConfigurators;
        }

        @Override
        public Set<AnnotatedMethodConfigurator<? super MyBean>> methods() {
            Set<AnnotatedMethodConfigurator<? super MyBean>> annotatedMethodConfigurators = new HashSet<>();
            annotatedMethodConfigurators.add(new FullStubMethodConfigurator<>());
            return annotatedMethodConfigurators;
        }

        @Override
        public Set<AnnotatedConstructorConfigurator<MyBean>> constructors() {
            Set<AnnotatedConstructorConfigurator<MyBean>> annotatedConstructorConfigurators = new HashSet<>();
            annotatedConstructorConfigurators.add(new FullStubConstructorConfigurator<>());
            return annotatedConstructorConfigurators;
        }

        @Override
        public Stream<AnnotatedFieldConfigurator<? super MyBean>> filterFields(Predicate<AnnotatedField<?
            super MyBean>> p) {
            p.test(null);
            return fields().stream();
        }

        @Override
        public Stream<AnnotatedMethodConfigurator<? super MyBean>> filterMethods(Predicate<AnnotatedMethod<?
            super MyBean>> p) {
            p.test(null);
            return methods().stream();
        }

        @Override
        public Stream<AnnotatedConstructorConfigurator<MyBean>> filterConstructors(Predicate<AnnotatedConstructor<MyBean>> p) {
            p.test(null);
            return constructors().stream();
        }

        @Override
        public AnnotatedType<MyBean> getAnnotated() {
            return super.getAnnotated();
        }
    }

    public static class FullStubFieldConfigurator<T> implements AnnotatedFieldConfigurator<MyBean> {
        @Override public AnnotatedField<MyBean> getAnnotated() {return null;}

        @Override public AnnotatedFieldConfigurator<MyBean> removeAll() {return this;}

        @Override public AnnotatedFieldConfigurator<MyBean> remove(Predicate<Annotation> predicate) {
            predicate.test(getDummyAnn()); return this;
        }

        @Override public AnnotatedFieldConfigurator<MyBean> add(Annotation annotation) {return this;}
    }

    public static class FullStubMethodConfigurator<T> implements AnnotatedMethodConfigurator<MyBean> {
        @Override public AnnotatedMethodConfigurator<MyBean> add(Annotation annotation) {return this;}

        @Override
        public Stream<AnnotatedParameterConfigurator<MyBean>> filterParams(Predicate<AnnotatedParameter<MyBean>> p) {
            p.test(null); return params().stream();
        }

        @Override public AnnotatedMethod<MyBean> getAnnotated() {return null;}

        @Override public List<AnnotatedParameterConfigurator<MyBean>> params() {
            List<AnnotatedParameterConfigurator<MyBean>> annotatedParameterConfigurators = new ArrayList<>();
            annotatedParameterConfigurators.add(new FullStubParamConfigurator<>());
            return annotatedParameterConfigurators;
        }

        @Override public AnnotatedMethodConfigurator<MyBean> remove(Predicate<Annotation> predicate) {
            predicate.test(getDummyAnn()); return this;
        }

        @Override public AnnotatedMethodConfigurator<MyBean> removeAll() {return this;}
    }

    public static class FullStubConstructorConfigurator<T> implements AnnotatedConstructorConfigurator<MyBean> {
        @Override public AnnotatedConstructor<MyBean> getAnnotated() {return null;}

        @Override public AnnotatedConstructorConfigurator<MyBean> add(Annotation annotation) {return this;}

        @Override
        public AnnotatedConstructorConfigurator<MyBean> remove(Predicate<Annotation> predicate) {
            predicate.test(getDummyAnn()); return this;
        }

        @Override public AnnotatedConstructorConfigurator<MyBean> removeAll() {return this;}

        @Override public List<AnnotatedParameterConfigurator<MyBean>> params() {
            List<AnnotatedParameterConfigurator<MyBean>> annotatedParameterConfigurators = new ArrayList<>();
            annotatedParameterConfigurators.add(new FullStubParamConfigurator<>());
            return annotatedParameterConfigurators;
        }

        @Override
        public Stream<AnnotatedParameterConfigurator<MyBean>> filterParams(Predicate<AnnotatedParameter<MyBean>> p) {
            p.test(null); return params().stream();
        }
    }

    public static class FullStubParamConfigurator<T> implements AnnotatedParameterConfigurator<MyBean> {
        @Override public AnnotatedParameterConfigurator<MyBean> add(Annotation annotation) {return this;}

        @Override public AnnotatedParameter<MyBean> getAnnotated() {return null;}

        @Override public AnnotatedParameterConfigurator<MyBean> remove(Predicate<Annotation> predicate) {
            predicate.test(getDummyAnn()); return this;
        }

        @Override public AnnotatedParameterConfigurator<MyBean> removeAll() {return this;}
    }
}