/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.apache.webbeans.event;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import javax.event.AfterTransactionCompletion;
import javax.event.AfterTransactionFailure;
import javax.event.AfterTransactionSuccess;
import javax.event.BeforeTransactionCompletion;
import javax.event.Event;
import javax.event.Observes;
import javax.inject.Disposes;
import javax.inject.Initializer;
import javax.inject.Produces;

import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.util.AnnotationUtil;
import org.apache.webbeans.util.Asserts;
import org.apache.webbeans.util.ClassUtil;

public final class EventUtil
{
    private EventUtil()
    {

    }

    public static void checkEventType(Class<?> eventType)
    {
        Asserts.assertNotNull(eventType, "eventType parameter can not be null");

        if (ClassUtil.isParametrized(eventType))
        {
            throw new IllegalArgumentException("Event type : " + eventType.getName() + " can not be generic");
        }
    }

    public static void checkEventBindings(Annotation... annotations)
    {
        AnnotationUtil.checkBindingTypeConditions(annotations);
    }

    public static TransactionalObserverType getObserverMethodTransactionType(Method observerMethod)
    {
        if (AnnotationUtil.isMethodParameterAnnotationExist(observerMethod, AfterTransactionCompletion.class))
        {
            return TransactionalObserverType.AFTER_TRANSACTION_COMPLETION;
        }
        else if (AnnotationUtil.isMethodParameterAnnotationExist(observerMethod, AfterTransactionSuccess.class))
        {
            return TransactionalObserverType.AFTER_TRANSACTION_SUCCESS;
        }
        else if (AnnotationUtil.isMethodParameterAnnotationExist(observerMethod, AfterTransactionFailure.class))
        {
            return TransactionalObserverType.AFTER_TRANSACTION_FAILURE;
        }
        else if (AnnotationUtil.isMethodParameterAnnotationExist(observerMethod, BeforeTransactionCompletion.class))
        {
            return TransactionalObserverType.BEFORE_TRANSACTION_COMPLETION;
        }
        else
        {
            return TransactionalObserverType.NONE;
        }

    }

    public static void checkObserverMethodConditions(Method candidateObserverMethod, Class<?> clazz)
    {
        Asserts.assertNotNull(candidateObserverMethod, "candidateObserverMethod parameter can not be null");
        Asserts.nullCheckForClass(clazz);

        if (AnnotationUtil.isMethodMultipleParameterAnnotationExist(candidateObserverMethod, Observes.class))
        {
            throw new WebBeansConfigurationException("Observer method : " + candidateObserverMethod.getName() + " in class : " + clazz.getName() + " can not define two parameters with annotated @Observes");
        }

        if (AnnotationUtil.isMethodHasAnnotation(candidateObserverMethod, Produces.class) || AnnotationUtil.isMethodHasAnnotation(candidateObserverMethod, Initializer.class))
        {
            throw new WebBeansConfigurationException("Observer method : " + candidateObserverMethod.getName() + " in class : " + clazz.getName() + " can not annotated with annotation in the list {@Produces, @Initializer, @Destructor}");

        }

        if (AnnotationUtil.isMethodParameterAnnotationExist(candidateObserverMethod, Disposes.class))
        {
            throw new WebBeansConfigurationException("Observer method : " + candidateObserverMethod.getName() + " in class : " + clazz.getName() + " can not annotated with annotation @Disposes");
        }

        Type type = AnnotationUtil.getMethodFirstParameterWithAnnotation(candidateObserverMethod, Observes.class);
        Class<?> eventType = null;
        if (type instanceof ParameterizedType)
        {
            ClassUtil.checkParametrizedType((ParameterizedType) type);
            eventType = (Class<?>) ((ParameterizedType) type).getRawType();
        }
        else
        {
            eventType = (Class<?>) type;
        }

        if (ClassUtil.isParametrized(eventType))
        {
            throw new WebBeansConfigurationException("Observer method : " + candidateObserverMethod.getName() + " in class : " + clazz.getName() + " can not defined as generic");
        }
    }

    public static void checkObservableMethodParameterConditions(Type[] parametersType, String fieldName, String errorMessage)
    {
        Asserts.assertNotNull(parametersType, "parametersType parameter can not be null");

        for (Type observableType : parametersType)
        {
            EventUtil.checkObservableFieldConditions(observableType, fieldName, errorMessage);
        }

    }

    public static void checkObservableFieldConditions(Type fieldType, String fieldName, String errorMessage)
    {
        Asserts.assertNotNull(fieldType, "fieldType parameter can not be null");
        Asserts.assertNotNull(fieldName, "fieldName parameter can not be null");

        if (errorMessage == null)
        {
            errorMessage = "Failed on check @Observable field/method-parameter with name : " + fieldName;
        }

        if (!ClassUtil.isParametrizedType(fieldType))
        {
            throw new WebBeansConfigurationException("@Observable field/method-parameter in " + errorMessage + " with field : " + fieldName + " must be ParametrizedType with actual type argument");
        }
        else
        {
            ParameterizedType pType = (ParameterizedType) fieldType;
            Class<?> clazz = (Class<?>) pType.getRawType();

            if (!clazz.equals(Event.class))
            {
                throw new WebBeansConfigurationException("@Observable field/method-parameter in " + errorMessage + " with field : " + fieldName + " must be ParametrizedType with raw type argument javax.webbeans.Event");
            }

            if (!ClassUtil.checkParametrizedType(pType))
            {
                throw new WebBeansConfigurationException("@Observable field/method-parameter in " + errorMessage + " with field : " + fieldName + " can not be ParametrizedType with type variable or wildcard type arguments");
            }
        }

    }

}
