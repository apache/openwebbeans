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
import org.junit.Assert;
import org.junit.Test;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;
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

import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AnnotatedTypeConfiguratorImplTest extends AbstractUnitTest
{

    @Test
    public void testAddAnnotationToClass()
    {

        checkAnnotatedType(
            pat -> pat.configureAnnotatedType().add(new TheQualifierLiteral("type")),
            pba ->
            {
                Set<Annotation> annotations = pba.getAnnotated().getAnnotations();
                assertEquals(1, annotations.size());
                assertEquals(TheQualifier.class, annotations.iterator().next().annotationType());
                assertTrue(pba.getAnnotated() instanceof AnnotatedType);
                AnnotatedMethod<? super AnnotatedTypeConfigClass> methodWithParameters = ((AnnotatedType<AnnotatedTypeConfigClass>) pba.getAnnotated()).getMethods().stream()
                        .filter(am -> am.getJavaMember().getName().equals("methodWithParameters"))
                        .findFirst().get();
                TheQualifier param1Annotation = methodWithParameters.getParameters().stream()
                        .filter(ap -> ap.getPosition() == 0)
                        .findFirst().get().getAnnotation(TheQualifier.class);
                assertNotNull(param1Annotation);
                assertEquals("blub", param1Annotation.value());
            },
            AnnotatedTypeConfigClass.class);
    }

    @Test
    public void testAddAnnotationToClass_classAlreadyContainsAnnotations()
    {

        checkAnnotatedType(
            pat -> pat.configureAnnotatedType().add(new TheQualifierLiteral("type")),
            pba ->
            {
                Set<Annotation> annotations = pba.getAnnotated().getAnnotations();
                assertEquals(2, annotations.size());
            },
            AnnotatedTypeConfigClassWithAnnotation.class);
    }


    @Test
    public void testRemoveAnnotation()
    {
        checkAnnotatedType(
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
            },
            AnnotatedTypeConfigClass.class);
    }

    @Test
    public void testRemoveAnnotation_classAlreadyContainsAnnotations()
    {
        checkAnnotatedType(
            pat -> pat.configureAnnotatedType().add(new TheQualifierLiteral("one"))
                      .remove(a -> ((TheQualifier) a).value().equals("one")),
            pba ->
            {
                Set<Annotation> annotations = pba.getAnnotated().getAnnotations();
                assertEquals(2, annotations.size());
            },
            AnnotatedTypeConfigClassWithAnnotation.class);
    }

    @Test
    public void testRemoveAllAnnotations()
    {
        checkAnnotatedType(
            pat -> pat.configureAnnotatedType().add(new TheQualifierLiteral("one"))
                      .add(new TheQualifierLiteral("two"))
                      .removeAll(),
            pba ->
            {
                Set<Annotation> annotations = pba.getAnnotated().getAnnotations();
                assertTrue(annotations.isEmpty());
            },
            AnnotatedTypeConfigClass.class);
    }

    @Test
    public void testRemoveAllAnnotations_classAlreadyContainsAnnotations()
    {
        checkAnnotatedType(
            pat -> pat.configureAnnotatedType().removeAll(),
            pba ->
            {
                Set<Annotation> annotations = pba.getAnnotated().getAnnotations();
                assertTrue(annotations.isEmpty());
            },
            AnnotatedTypeConfigClass.class);
    }

    @Test
    public void testAddAnnotationToMethod()
    {
        checkAnnotatedType(
            pat ->
                pat.configureAnnotatedType()
                    .filterMethods(m -> m.getJavaMember().getName().equals("method1"))
                    .findFirst()
                    .get()
                    .add(new TheQualifierLiteral("Method1")),
            pba ->
            {
                Assert.assertTrue(pba.getAnnotated() instanceof AnnotatedType);
                AnnotatedType<?> at = (AnnotatedType<?>) pba.getAnnotated();
                Set<Annotation> annotations = at.getMethods().stream()
                    .filter(m -> m.getJavaMember().getName().equals("method1"))
                    .findFirst()
                    .get().getAnnotations();
                assertEquals(1, annotations.size());
                Annotation ann = annotations.iterator().next();
                assertEquals(TheQualifier.class, ann.annotationType());
                assertEquals("Method1", ((TheQualifier) ann).value());

            }, AnnotatedTypeConfigClass.class);
    }

    @Test
    public void testRemoveAnnotationFromMethod()
    {
        final String methodName = "method2";

        checkAnnotatedType(
                pat -> pat.configureAnnotatedType()
                          .filterMethods(m -> methodName.equals(m.getJavaMember().getName()))
                          .findFirst()
                          .get()
                          .removeAll(),
                pba -> assertTrue(((AnnotatedType<?>) pba.getAnnotated()).getMethods()
                                                                         .stream()
                                                                         .filter(m -> methodName.equals(m.getJavaMember().getName()))
                                                                         .findFirst()
                                                                         .get()
                                                                         .getAnnotations()
                                                                         .isEmpty()),
                AnnotatedTypeConfigClassWithAnnotation.class);
    }

    @Test
    public void testAddAnnotationToField()
    {
        checkAnnotatedType(
            pat ->
                pat.configureAnnotatedType()
                    .filterFields(m -> m.getJavaMember().getName().equals("field1"))
                    .findFirst()
                    .get()
                    .add(new TheQualifierLiteral("Field1")),
            pba ->
            {
                Assert.assertTrue(pba.getAnnotated() instanceof AnnotatedType);
                AnnotatedType<?> at = (AnnotatedType<?>) pba.getAnnotated();
                Set<Annotation> annotations = at.getFields().stream()
                    .filter(m -> m.getJavaMember().getName().equals("field1"))
                    .findFirst()
                    .get().getAnnotations();
                assertEquals(1, annotations.size());
                Annotation ann = annotations.iterator().next();
                assertEquals(TheQualifier.class, ann.annotationType());
                assertEquals("Field1", ((TheQualifier) ann).value());

            }, AnnotatedTypeConfigClass.class);
    }

    @Test
    public void testRemoveAnnotationFromField()
    {
        checkAnnotatedType(pat -> pat.configureAnnotatedType()
                                     .filterFields(af -> "field2".equals(af.getJavaMember().getName()))
                                     .forEach(c -> c.remove(a -> a.annotationType() == TheQualifier.class)),
                           pba ->
                           {
                               Assert.assertTrue(pba.getAnnotated() instanceof AnnotatedType);
                               assertTrue(((AnnotatedType<?>) pba.getAnnotated()).getFields()
                                                                                 .stream()
                                                                                 .filter(m -> m.getJavaMember().getName().equals("field2"))
                                                                                 .findFirst()
                                                                                 .get()
                                                                                 .getAnnotations()
                                                                                 .isEmpty());
                           },
                           AnnotatedTypeConfigClassWithAnnotation.class);
    }


    private void checkAnnotatedType(Consumer<ProcessAnnotatedType<AnnotatedTypeConfigClass>> typeConfigurator,
                                    Consumer<ProcessBeanAttributes> beanAttributesConsumer,
                                    Class<?> classToCheck)
    {
        AnnotatedTypeConfiguratorExtension extension
            = new AnnotatedTypeConfiguratorExtension(typeConfigurator,beanAttributesConsumer);
        addExtension(extension);
        startContainer(classToCheck);
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
        private String field1;
        private Integer field2;

        public void method1()
        {
            // do nothing
        }

        public String method2()
        {
            return "method 2";
        }

        public String methodWithParameters(@TheQualifier("blub") String param1, Integer param2)
        {
            return "parameters: " + param1 + ", " + param2;
        }
    }

    @TheQualifier(value = "default")
    public static class AnnotatedTypeConfigClassWithAnnotation
    {

        @TheQualifier(value = "default_field1")
        private String field1;

        @TheQualifier(value = "default_field2")
        private Integer field2;

        @TheQualifier(value = "default_method1")
        public void method1()
        {
            // do nothing
        }

        @TheQualifier(value = "default_method2")
        public String method2()
        {
            return "method 2";
        }

        @TheQualifier(value = "default_methodWithParameters")
        public String methodWithParameters(String param1, String param2)
        {
            return "parameters: " + param1 + ", " + param2;
        }

        public void methodWithAnnotatedParameters(@TheQualifier(value = "default_param1") String param1,
                                                  @TheQualifier(value = "default_param2") Integer param2)
        {
            // nothing to do here
        }
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
