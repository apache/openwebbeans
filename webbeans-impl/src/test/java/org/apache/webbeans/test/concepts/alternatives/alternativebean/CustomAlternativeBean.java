/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.webbeans.test.concepts.alternatives.alternativebean;

import javax.enterprise.context.Dependent;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.webbeans.annotation.DefaultLiteral;

@Alternative
public class CustomAlternativeBean implements Bean<String>
{

    @Override
    public Set<InjectionPoint> getInjectionPoints()
    {
        return Collections.emptySet();
    }

    @Override
    public Class<?> getBeanClass()
    {
        return CustomAlternativeBean.class;
    }

    @Override
    public boolean isNullable()
    {
        return true;
    }

    @Override
    public String create(CreationalContext<String> context)
    {
        return "alternative";
    }

    @Override
    public void destroy(String instance, CreationalContext<String> context)
    {

    }

    @Override
    public Set<Type> getTypes()
    {
        return new HashSet(Arrays.asList(String.class, Object.class));
    }

    @Override
    public Set<Annotation> getQualifiers()
    {
        return DefaultLiteral.SET;
    }

    @Override
    public Class<? extends Annotation> getScope()
    {
        return Dependent.class;
    }

    @Override
    public String getName()
    {
        return null;
    }

    @Override
    public Set<Class<? extends Annotation>> getStereotypes()
    {
        return null;
    }

    @Override
    public boolean isAlternative()
    {
        return true;
    }
}
