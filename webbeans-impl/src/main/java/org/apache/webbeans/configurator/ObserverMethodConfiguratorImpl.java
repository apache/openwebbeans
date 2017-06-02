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

import javax.enterprise.event.Reception;
import javax.enterprise.event.TransactionPhase;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.ObserverMethod;
import javax.enterprise.inject.spi.configurator.ObserverMethodConfigurator;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Set;

public class ObserverMethodConfiguratorImpl implements ObserverMethodConfigurator
{
    @Override
    public ObserverMethodConfigurator read(Method method)
    {
        throw new UnsupportedOperationException("TODO implement CDI 2.0");
    }

    @Override
    public ObserverMethodConfigurator read(AnnotatedMethod method)
    {
        throw new UnsupportedOperationException("TODO implement CDI 2.0");
    }

    @Override
    public ObserverMethodConfigurator read(ObserverMethod method)
    {
        throw new UnsupportedOperationException("TODO implement CDI 2.0");
    }

    @Override
    public ObserverMethodConfigurator beanClass(Class type)
    {
        throw new UnsupportedOperationException("TODO implement CDI 2.0");
    }

    @Override
    public ObserverMethodConfigurator observedType(Type type)
    {
        throw new UnsupportedOperationException("TODO implement CDI 2.0");
    }

    @Override
    public ObserverMethodConfigurator addQualifier(Annotation qualifier)
    {
        throw new UnsupportedOperationException("TODO implement CDI 2.0");
    }

    @Override
    public ObserverMethodConfigurator addQualifiers(Annotation... qualifiers)
    {
        throw new UnsupportedOperationException("TODO implement CDI 2.0");
    }

    @Override
    public ObserverMethodConfigurator addQualifiers(Set qualifiers)
    {
        throw new UnsupportedOperationException("TODO implement CDI 2.0");
    }

    @Override
    public ObserverMethodConfigurator qualifiers(Annotation... qualifiers)
    {
        throw new UnsupportedOperationException("TODO implement CDI 2.0");
    }

    @Override
    public ObserverMethodConfigurator qualifiers(Set qualifiers)
    {
        throw new UnsupportedOperationException("TODO implement CDI 2.0");
    }

    @Override
    public ObserverMethodConfigurator reception(Reception reception)
    {
        throw new UnsupportedOperationException("TODO implement CDI 2.0");
    }

    @Override
    public ObserverMethodConfigurator transactionPhase(TransactionPhase transactionPhase)
    {
        throw new UnsupportedOperationException("TODO implement CDI 2.0");
    }

    @Override
    public ObserverMethodConfigurator priority(int priority)
    {
        throw new UnsupportedOperationException("TODO implement CDI 2.0");
    }

    @Override
    public ObserverMethodConfigurator notifyWith(EventConsumer callback)
    {
        throw new UnsupportedOperationException("TODO implement CDI 2.0");
    }

    @Override
    public ObserverMethodConfigurator async(boolean async)
    {
        throw new UnsupportedOperationException("TODO implement CDI 2.0");
    }
}
