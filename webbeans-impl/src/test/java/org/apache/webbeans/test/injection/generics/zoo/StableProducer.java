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
package org.apache.webbeans.test.injection.generics.zoo;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.InjectionPoint;
import java.lang.reflect.ParameterizedType;

/**
 * A stable for horses, pigs, etc
 */
public class StableProducer
{
    @Produces
    @Dependent
    public <PET> Stable<PET> createStable(InjectionPoint injectionPoint)
    {
        Class petClass = (Class) ((ParameterizedType) injectionPoint.getType()).getActualTypeArguments()[0];

        Stable stable = null;
        if (Horse.class.equals(petClass))
        {
            stable = (Stable<PET>) new HorseStable();
            stable.setPet(new Horse());
        }
        if (Pig.class.equals(petClass))
        {
            stable = new PigStable();
            stable.setPet(new Pig());
        }

        return stable;
    }

    @Produces
    @Dependent
    public Stable createStableWithoutGenerics(InjectionPoint injectionPoint)
    {
        throw new UnsupportedOperationException("shouldn't be called");
    }
}
