/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.webbeans.container;

import java.util.Set;

import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.enterprise.inject.spi.InjectionTarget;
import org.apache.webbeans.config.WebBeansContext;

/**
 * InjectionTargetFactory which validates the craeted InjectionTarget.
 * This is only required if the InjectionTarget gets created manually
 * via the BeanManager.
 */
public class ValidatingInjectionTargetFactory<T> extends InjectionTargetFactoryImpl<T>
{
    public ValidatingInjectionTargetFactory(AnnotatedType<T> annotatedType, WebBeansContext webBeansContext)
    {
        super(annotatedType, webBeansContext);
    }

    @Override
    public InjectionTarget<T> createInjectionTarget(Bean<T> bean)
    {
        final InjectionTarget<T> injectionTarget = super.createInjectionTarget(bean);
        final Set<InjectionPoint> injectionPoints = injectionTarget.getInjectionPoints();
        try
        {
            getWebBeansContext().getWebBeansUtil().validate(injectionPoints, null);
        }
        catch (Exception e)
        {
            // This is really a bit weird.
            // The spec does mandate an IAE instead of a CDI DefinitionException for whatever reason.
            throw new IllegalArgumentException("Problem while creating InjectionTarget", e);
        }

        return injectionTarget;
    }
}
