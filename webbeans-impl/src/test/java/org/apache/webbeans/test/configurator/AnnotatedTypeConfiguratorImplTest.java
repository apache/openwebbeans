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
package org.apache.webbeans.test.configurator;

import org.apache.webbeans.test.AbstractUnitTest;
import org.junit.Test;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.ProcessBeanAttributes;
import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Qualifier;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Set;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AnnotatedTypeConfiguratorImplTest extends AbstractUnitTest
{

    @Test
    public void testAddAnnotationToClass()
    {

        AnnotatedTypeConfiguratorExtension extension = new AnnotatedTypeConfiguratorExtension(pat -> pat.configureAnnotatedType().add(new TheQualifierLiteral("type")),
                                                                                              pba ->
                                                                                              {
                                                                                                  Set<Annotation> annotations = pba.getAnnotated().getAnnotations();
                                                                                                  assertEquals(1, annotations.size());
                                                                                                  assertEquals(TheQualifier.class, annotations.iterator().next().annotationType());
                                                                                              });
        addExtension(extension);
        startContainer(AnnotatedTypeConfigClass.class);
        shutdown();
    }

    @Test
    public void testAddAnnotationToClass_classAlreadyContainsAnnotations()
    {

        AnnotatedTypeConfiguratorExtension extension = new AnnotatedTypeConfiguratorExtension(pat -> pat.configureAnnotatedType().add(new TheQualifierLiteral("type")),
                                                                                              pba ->
                                                                                              {
                                                                                                  Set<Annotation> annotations = pba.getAnnotated().getAnnotations();
                                                                                                  assertEquals(2, annotations.size());
                                                                                              });
        addExtension(extension);
        startContainer(AnnotatedTypeConfigClassWithAnnotation.class);
        shutdown();
    }


    @Test
    public void testRemoveAnnotation()
    {
        AnnotatedTypeConfiguratorExtension extension = new AnnotatedTypeConfiguratorExtension(
                pat -> pat.configureAnnotatedType().add(new TheQualifierLiteral("one"))
                          .add(new TheQualifierLiteral("two"))
                          .remove(a -> ((TheQualifier) a).value().equals("two")),
                pba ->
                {
                    Set<Annotation> annotations = pba.getAnnotated().getAnnotations();
                    assertEquals(1, annotations.size());
                    Annotation annotation = annotations.iterator().next();
                    assertEquals(TheQualifier.class, annotation.annotationType());
                    assertEquals("one", ((TheQualifier) annotation).value());
                });

        addExtension(extension);
        startContainer(AnnotatedTypeConfigClass.class);
        shutdown();
    }

    @Test
    public void testRemoveAnnotation_classAlreadyContainsAnnotations()
    {
        AnnotatedTypeConfiguratorExtension extension = new AnnotatedTypeConfiguratorExtension(
                pat -> pat.configureAnnotatedType().add(new TheQualifierLiteral("one"))
                          .remove(a -> ((TheQualifier) a).value().equals("one")),
                pba ->
                {
                    Set<Annotation> annotations = pba.getAnnotated().getAnnotations();
                    assertEquals(2, annotations.size());
                });

        addExtension(extension);
        startContainer(AnnotatedTypeConfigClassWithAnnotation.class);
        shutdown();
    }

    @Test
    public void testRemoveAllAnnotations()
    {
        AnnotatedTypeConfiguratorExtension extension = new AnnotatedTypeConfiguratorExtension(
                pat -> pat.configureAnnotatedType().add(new TheQualifierLiteral("one"))
                          .add(new TheQualifierLiteral("two"))
                          .removeAll(),
                pba ->
                {
                    Set<Annotation> annotations = pba.getAnnotated().getAnnotations();
                    assertTrue(annotations.isEmpty());
                });

        addExtension(extension);
        startContainer(AnnotatedTypeConfigClass.class);
        shutdown();
    }

    @Test
    public void testRemoveAllAnnotations_classAlreadyContainsAnnotations()
    {
        AnnotatedTypeConfiguratorExtension extension = new AnnotatedTypeConfiguratorExtension(
                pat -> pat.configureAnnotatedType().removeAll(),
                pba ->
                {
                    Set<Annotation> annotations = pba.getAnnotated().getAnnotations();
                    assertTrue(annotations.isEmpty());
                });

        addExtension(extension);
        startContainer(AnnotatedTypeConfigClass.class);
        shutdown();
    }


    public static class AnnotatedTypeConfiguratorExtension implements Extension
    {

        private final Consumer<ProcessAnnotatedType<AnnotatedTypeConfigClass>> typeConfigurator;
        private final Consumer<ProcessBeanAttributes> beanAttributesConsumer;

        AnnotatedTypeConfiguratorExtension(Consumer<ProcessAnnotatedType<AnnotatedTypeConfigClass>> typeConfigurator,
                                           Consumer<ProcessBeanAttributes> beanAttributesConsumer)
        {
            this.typeConfigurator = typeConfigurator;
            this.beanAttributesConsumer = beanAttributesConsumer;
        }

        public void createAnnotatedType(@Observes ProcessAnnotatedType<AnnotatedTypeConfigClass> pat)
        {
            typeConfigurator.accept(pat);
        }

        public void getCreatedAnnotatedType(@Observes ProcessBeanAttributes<AnnotatedTypeConfigClass> pba)
        {
            beanAttributesConsumer.accept(pba);
        }

    }

    public static class AnnotatedTypeConfigClass
    {
    }

    @TheQualifier(value = "default")
    public static class AnnotatedTypeConfigClassWithAnnotation
    {

    }

    @Qualifier
    @Retention(RetentionPolicy.RUNTIME)
    @Target(value = {ElementType.TYPE, ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER, ElementType.CONSTRUCTOR, ElementType.ANNOTATION_TYPE})
    public @interface TheQualifier
    {
        String value();
    }

    public static class TheQualifierLiteral extends AnnotationLiteral<TheQualifier> implements TheQualifier
    {

        private final String value;

        TheQualifierLiteral(String value)
        {
            this.value = value;
        }

        public String value()
        {
            return value;
        }
    }
}
