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

import javax.enterprise.inject.spi.configurator.InjectionPointConfigurator;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Set;

public class InjectionPointConfiguratorImpl implements InjectionPointConfigurator
{
    @Override
    public InjectionPointConfigurator type(Type requiredType)
    {
        throw new UnsupportedOperationException("TODO implement CDI 2.0");
    }

    @Override
    public InjectionPointConfigurator addQualifier(Annotation qualifier)
    {
        throw new UnsupportedOperationException("TODO implement CDI 2.0");
    }

    @Override
    public InjectionPointConfigurator addQualifiers(Annotation... qualifiers)
    {
        throw new UnsupportedOperationException("TODO implement CDI 2.0");
    }

    @Override
    public InjectionPointConfigurator addQualifiers(Set<Annotation> qualifiers)
    {
        throw new UnsupportedOperationException("TODO implement CDI 2.0");
    }

    @Override
    public InjectionPointConfigurator qualifiers(Annotation... qualifiers)
    {
        throw new UnsupportedOperationException("TODO implement CDI 2.0");
    }

    @Override
    public InjectionPointConfigurator qualifiers(Set<Annotation> qualifiers)
    {
        throw new UnsupportedOperationException("TODO implement CDI 2.0");
    }

    @Override
    public InjectionPointConfigurator delegate(boolean delegate)
    {
        throw new UnsupportedOperationException("TODO implement CDI 2.0");
    }

    @Override
    public InjectionPointConfigurator transientField(boolean trans)
    {
        throw new UnsupportedOperationException("TODO implement CDI 2.0");
    }
}
