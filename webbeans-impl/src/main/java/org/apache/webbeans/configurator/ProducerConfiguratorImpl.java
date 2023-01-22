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

import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.enterprise.inject.spi.Producer;
import jakarta.enterprise.inject.spi.configurator.ProducerConfigurator;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

public class ProducerConfiguratorImpl<T> implements ProducerConfigurator<T>
{
    private Function produceWithCallback;
    private Consumer disposeWithCallback;
    private Set<InjectionPoint> injectionPoints = new HashSet<>();

    @Override
    public<U extends T> ProducerConfigurator<T> produceWith(Function<CreationalContext<U>, U> callback)
    {
        this.produceWithCallback = callback;
        return this;
    }

    @Override
    public ProducerConfigurator<T> disposeWith(Consumer<T> callback)
    {
        this.disposeWithCallback = callback;
        return this;
    }

    public ProducerConfigurator<T> addInjectionPoint(InjectionPoint injectionPoint)
    {
        this.injectionPoints.add(injectionPoint);
        return this;
    }

    public <T> Producer<T> getProducer()
    {
        return new ConfiguredProducer();
    }

    public class ConfiguredProducer<T> implements Producer<T>
    {
        @Override
        public T produce(CreationalContext<T> creationalContext)
        {
            return (T) produceWithCallback.apply(creationalContext);
        }

        @Override
        public void dispose(T instance)
        {
            disposeWithCallback.accept(instance);
        }

        @Override
        public Set<InjectionPoint> getInjectionPoints()
        {
            return injectionPoints;
        }
    }
}
