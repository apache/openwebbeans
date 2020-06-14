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
package org.apache.openwebbeans.junit5.perclass;

import org.apache.openwebbeans.junit5.Cdi;
import org.apache.openwebbeans.junit5.internal.CdiExtension;
import org.apache.webbeans.container.InjectableBeanManager;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestInstances;

import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.Optional.of;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

@TestInstance(PER_CLASS)
class PerMethodTest
{
    private BeanManager bm1;

    @Test
    @Order(1)
    @ExtendWith(CustomExtension.class)
    void cdiRuns()
    {
        bm1 = CDI.current().getBeanManager();
        assertNotNull(bm1);
    }

    @Test
    @Order(2)
    @ExtendWith(CustomExtension.class)
    void cdiReRuns()
    {
        final BeanManager bm2 = CDI.current().getBeanManager();
        assertNotNull(bm2);
        assertNotEquals(bm1, bm2);
        assertNotEquals(getRealBm(bm1), getRealBm(bm2));
    }

    private BeanManager getRealBm(final BeanManager wrapper) {
        try
        {
            final Field bm = InjectableBeanManager.class.getDeclaredField("bm");
            if (!bm.isAccessible())
            {
                bm.setAccessible(true);
            }
            return BeanManager.class.cast(bm.get(wrapper));
        }
        catch (final Exception e)
        {
            return fail(e);
        }
    }

    // enable to encapsulate in a framework/tool
    // here we just virtually set @Cdi on the method and move the class lifecycle to the method
    // note 1: in real, this kind of impl is not "inline" but this tests the use case more than the impl
    // note 2: by itself this use case is not terrible but sometimes requires by another jupiter extension
    public static class CustomExtension extends CdiExtension
    {
        @Override
        public void beforeEach(final ExtensionContext extensionContext)
        {
            super.beforeAll(new ExtensionContext()
            {
                @Override
                public Optional<AnnotatedElement> getElement()
                {
                    return of(new AnnotatedElement()
                    {
                        @Override
                        public <T extends Annotation> T getAnnotation(final Class<T> annotationClass)
                        {
                            return Stream.of(getAnnotations())
                                    .filter(it -> it.annotationType() == annotationClass)
                                    .map(annotationClass::cast)
                                    .findFirst().orElse(null);
                        }

                        @Override
                        public Annotation[] getAnnotations()
                        {
                            return getDeclaredAnnotations();
                        }

                        @Override
                        public Annotation[] getDeclaredAnnotations()
                        {
                            return new Annotation[]
                            {
                                    new Cdi() {
                                        @Override
                                        public Class<? extends Annotation> annotationType()
                                        {
                                            return Cdi.class;
                                        }

                                        @Override
                                        public Class<?>[] classes()
                                        {
                                            return new Class[0];
                                        }

                                        @Override
                                        public Class<?>[] decorators()
                                        {
                                            return new Class[0];
                                        }

                                        @Override
                                        public Class<?>[] interceptors()
                                        {
                                            return new Class[0];
                                        }

                                        @Override
                                        public Class<?>[] alternatives()
                                        {
                                            return new Class[0];
                                        }

                                        @Override
                                        public Class<? extends Annotation>[] alternativeStereotypes()
                                        {
                                            return new Class[0];
                                        }

                                        @Override
                                        public Class<?>[] packages()
                                        {
                                            return new Class[0];
                                        }

                                        @Override
                                        public Class<?>[] recursivePackages()
                                        {
                                            return new Class[0];
                                        }

                                        @Override
                                        public Property[] properties()
                                        {
                                            return new Property[0];
                                        }

                                        @Override
                                        public boolean disableDiscovery()
                                        {
                                            return false;
                                        }

                                        @Override
                                        public Class<? extends OnStart>[] onStarts()
                                        {
                                            return new Class[0];
                                        }

                                        @Override
                                        public boolean reusable()
                                        {
                                            return false;
                                        }
                                    }
                            };
                        }
                    });
                }

                @Override
                public Optional<ExtensionContext> getParent()
                {
                    return extensionContext.getParent();
                }

                @Override
                public ExtensionContext getRoot()
                {
                    return extensionContext.getRoot();
                }

                @Override
                public String getUniqueId()
                {
                    return extensionContext.getUniqueId();
                }

                @Override
                public String getDisplayName()
                {
                    return extensionContext.getDisplayName();
                }

                @Override
                public Set<String> getTags()
                {
                    return extensionContext.getTags();
                }

                @Override
                public Optional<Class<?>> getTestClass()
                {
                    return extensionContext.getTestClass();
                }

                @Override
                public Class<?> getRequiredTestClass()
                {
                    return extensionContext.getRequiredTestClass();
                }

                @Override
                public Optional<TestInstance.Lifecycle> getTestInstanceLifecycle()
                {
                    return extensionContext.getTestInstanceLifecycle();
                }

                @Override
                public Optional<Object> getTestInstance()
                {
                    return extensionContext.getTestInstance();
                }

                @Override
                public Object getRequiredTestInstance()
                {
                    return extensionContext.getRequiredTestInstance();
                }

                @Override
                public Optional<TestInstances> getTestInstances()
                {
                    return extensionContext.getTestInstances();
                }

                @Override
                public TestInstances getRequiredTestInstances()
                {
                    return extensionContext.getRequiredTestInstances();
                }

                @Override
                public Optional<Method> getTestMethod()
                {
                    return extensionContext.getTestMethod();
                }

                @Override
                public Method getRequiredTestMethod()
                {
                    return extensionContext.getRequiredTestMethod();
                }

                @Override
                public Optional<Throwable> getExecutionException()
                {
                    return extensionContext.getExecutionException();
                }

                @Override
                public Optional<String> getConfigurationParameter(final String key)
                {
                    return extensionContext.getConfigurationParameter(key);
                }

                @Override
                public void publishReportEntry(final Map<String, String> map)
                {
                    extensionContext.publishReportEntry(map);
                }

                @Override
                public void publishReportEntry(final String key, final String value)
                {
                    extensionContext.publishReportEntry(key, value);
                }

                @Override
                public void publishReportEntry(final String value)
                {
                    extensionContext.publishReportEntry(value);
                }

                @Override
                public Store getStore(final Namespace namespace)
                {
                    return extensionContext.getStore(namespace);
                }
            });
            super.beforeEach(extensionContext);
        }

        @Override
        public void afterEach(final ExtensionContext extensionContext)
        {
            super.afterEach(extensionContext);
            super.afterAll(extensionContext);
        }
    }
}
