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

import javax.enterprise.inject.spi.configurator.BeanAttributesConfigurator;
import javax.enterprise.util.TypeLiteral;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Set;

public class BeanAttributesConfiguratorImpl implements BeanAttributesConfigurator
{
    @Override
    public BeanAttributesConfigurator addType(Type type)
    {
        throw new UnsupportedOperationException("TODO implement CDI 2.0");
    }

    @Override
    public BeanAttributesConfigurator addType(TypeLiteral typeLiteral)
    {
        throw new UnsupportedOperationException("TODO implement CDI 2.0");
    }

    @Override
    public BeanAttributesConfigurator addTypes(Type... types)
    {
        throw new UnsupportedOperationException("TODO implement CDI 2.0");
    }

    @Override
    public BeanAttributesConfigurator addTypes(Set set)
    {
        throw new UnsupportedOperationException("TODO implement CDI 2.0");
    }

    @Override
    public BeanAttributesConfigurator addTransitiveTypeClosure(Type type)
    {
        throw new UnsupportedOperationException("TODO implement CDI 2.0");
    }

    @Override
    public BeanAttributesConfigurator types(Type... types)
    {
        throw new UnsupportedOperationException("TODO implement CDI 2.0");
    }

    @Override
    public BeanAttributesConfigurator types(Set set)
    {
        throw new UnsupportedOperationException("TODO implement CDI 2.0");
    }

    @Override
    public BeanAttributesConfigurator scope(Class scope)
    {
        throw new UnsupportedOperationException("TODO implement CDI 2.0");
    }

    @Override
    public BeanAttributesConfigurator addQualifier(Annotation qualifier)
    {
        throw new UnsupportedOperationException("TODO implement CDI 2.0");
    }

    @Override
    public BeanAttributesConfigurator addQualifiers(Annotation... qualifiers)
    {
        throw new UnsupportedOperationException("TODO implement CDI 2.0");
    }

    @Override
    public BeanAttributesConfigurator addQualifiers(Set qualifiers)
    {
        throw new UnsupportedOperationException("TODO implement CDI 2.0");
    }

    @Override
    public BeanAttributesConfigurator qualifiers(Annotation... qualifiers)
    {
        throw new UnsupportedOperationException("TODO implement CDI 2.0");
    }

    @Override
    public BeanAttributesConfigurator qualifiers(Set qualifiers)
    {
        throw new UnsupportedOperationException("TODO implement CDI 2.0");
    }

    @Override
    public BeanAttributesConfigurator addStereotype(Class stereotype)
    {
        throw new UnsupportedOperationException("TODO implement CDI 2.0");
    }

    @Override
    public BeanAttributesConfigurator addStereotypes(Set stereotypes)
    {
        throw new UnsupportedOperationException("TODO implement CDI 2.0");
    }

    @Override
    public BeanAttributesConfigurator stereotypes(Set stereotypes)
    {
        throw new UnsupportedOperationException("TODO implement CDI 2.0");
    }

    @Override
    public BeanAttributesConfigurator name(String name)
    {
        throw new UnsupportedOperationException("TODO implement CDI 2.0");
    }

    @Override
    public BeanAttributesConfigurator alternative(boolean value)
    {
        throw new UnsupportedOperationException("TODO implement CDI 2.0");
    }
}
