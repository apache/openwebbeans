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

import org.apache.webbeans.test.AbstractUnitTest;
import org.junit.Assert;
import org.junit.Test;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

/**
 * A few simple generics handling tests.
 */
public class GenericsInTheZooTest extends AbstractUnitTest
{

    @Test
    public void testSpecificAnimalStable() throws Exception
    {
        // create the stables via explicit subclasses;
        startContainer(Animal.class, Stable.class, Horse.class, Pig.class, HorseStable.class, PigStable.class,
                       MySpecificAnimalStables.class);

        verifyAnimalStables();
    }

    @Dependent
    public static class MySpecificAnimalStables
    {
        @Inject
        private Stable<Horse> horseStable;

        @Inject
        private Stable<Pig> pigStable;


        public Stable<Horse> getHorseStable()
        {
            return horseStable;
        }

        public Stable<Pig> getPigStable()
        {
            return pigStable;
        }
    }

    @Test
    public void testGenericProducer() throws Exception
    {
        // create the stables via a single producer method
        startContainer(Stable.class, StableProducer.class, MySpecificAnimalStables.class);

        verifyAnimalStables();
    }


    private void verifyAnimalStables()
    {
        MySpecificAnimalStables stables = getInstance(MySpecificAnimalStables.class);
        Assert.assertNotNull(stables);
        Assert.assertNotNull(stables.getHorseStable());
        Assert.assertNotNull(stables.getHorseStable());
        Assert.assertEquals("horse", stables.getHorseStable().getPet().getName());

        Assert.assertNotNull(stables.getPigStable());
        Assert.assertNotNull(stables.getPigStable());
        Assert.assertEquals("pig", stables.getPigStable().getPet().getName());
    }

}
