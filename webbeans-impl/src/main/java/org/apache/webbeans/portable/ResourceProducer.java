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
package org.apache.webbeans.portable;

import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.context.creational.CreationalContextImpl;
import org.apache.webbeans.spi.ResourceInjectionService;
import org.apache.webbeans.spi.api.ResourceReference;

import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.Map;
import jakarta.enterprise.inject.spi.AnnotatedField;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.enterprise.inject.spi.Interceptor;

public class ResourceProducer<T, P> extends ProducerFieldProducer<T, P>
{
    private ResourceReference<?, ?> ref;
    private boolean isStatic;

    public ResourceProducer(Bean<P> owner, AnnotatedField<? super P> producerField,
                            WebBeansContext webBeansContext, ResourceReference<?, ?> ref)
    {
        super(owner, producerField, null, Collections.<InjectionPoint>emptySet(), webBeansContext);
        this.isStatic = Modifier.isStatic(producerField.getJavaMember().getModifiers());
        this.ref = ref;
    }

    @Override
    protected T produce(Map<Interceptor<?>, ?> interceptors, CreationalContextImpl<T> creationalContext)
    {
        if (!isStatic)
        {
            return super.produce(interceptors, creationalContext);
        }

        return (T) webBeansContext.getService(ResourceInjectionService.class).getResourceReference(ref);
    }
}
