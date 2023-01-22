/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.webbeans.test.instance;

import org.apache.webbeans.test.AbstractUnitTest;
import org.junit.Before;
import org.junit.Test;

import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Inject;
import jakarta.inject.Qualifier;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Collection;
import java.util.stream.Stream;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class InstanceSelectTest extends AbstractUnitTest {
    @Inject
    @Any
    private Instance<Food> allTypesOfFood;

    @Before
    public void init() {
        startContainer(asList(Cherry.class, Strawberry.class), emptyList(), true);
    }

    @Test
    public void findYummyFruit() {
        final Instance<Food> fruits = allTypesOfFood.select(new LiteralFoodType(FoodType.FRUIT));
        final Instance<Food> jummyFruits = fruits.select(new LiteralTasteType(TasteType.JUMMY));
        assertTrue(jummyFruits.stream().findAny().isPresent());
    }

    @Test
    public void thereIsNoJummyVegetable() {
        final Instance<Food> vegetables = allTypesOfFood.select(new LiteralFoodType(FoodType.VEGETABLE));
        assertTrue(vegetables.isUnsatisfied());

        final Instance<Food> jummyVegetables = vegetables.select(new LiteralTasteType(TasteType.JUMMY));
        final Collection<Food> selected = jummyVegetables.stream().collect(toList());
        assertFalse(selected.toString(), selected.stream().findAny().isPresent());
    }

    public static class LiteralTasteType extends AnnotationLiteral<TasteQualifier> implements TasteQualifier {
        private final TasteType taste;

        public LiteralTasteType(TasteType taste) {
            this.taste = taste;
        }

        @Override
        public TasteType value() {
            return taste;
        }
    }

    public static class LiteralFoodType extends AnnotationLiteral<FoodQualifier> implements FoodQualifier {
        private final FoodType food;

        public LiteralFoodType(FoodType food) {
            this.food = food;
        }

        @Override
        public FoodType value() {
            return food;
        }
    }

    @Qualifier
    @Retention(RUNTIME)
    @Target({FIELD, TYPE, METHOD, CONSTRUCTOR, PARAMETER})
    public @interface FoodQualifier {
        FoodType value();
    }

    @Qualifier
    @Retention(RUNTIME)
    @Target({FIELD, TYPE, METHOD, CONSTRUCTOR, PARAMETER})
    public @interface TasteQualifier {
        TasteType value();
    }

    public enum TasteType {
        JUMMY,
        DISGUSTING
    }

    public enum FoodType {
        VEGETABLE,
        FRUIT
    }

    @FoodQualifier(FoodType.FRUIT)
    @TasteQualifier(TasteType.DISGUSTING)
    public static class Cherry extends Food {
        public Cherry() {
            super("Cherry");
        }
    }

    @FoodQualifier(FoodType.FRUIT)
    @TasteQualifier(TasteType.JUMMY)
    public static class Strawberry extends Food {
        public Strawberry() {
            super("Strawberry");
        }
    }

    public static abstract class Food {
        private final String name;

        protected Food(String name) {
            this.name = name;
        }
    }
}
