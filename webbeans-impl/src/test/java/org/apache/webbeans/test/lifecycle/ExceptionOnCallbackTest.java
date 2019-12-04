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
package org.apache.webbeans.test.lifecycle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.webbeans.test.AbstractUnitTest;
import org.junit.Test;

public class ExceptionOnCallbackTest extends AbstractUnitTest {
    @Test
    public void exceptionOnPostConstruct() {
        startContainer(MainRunnable.class, ClassUsingThrowingRepository.class, ThrowingRepository.class);
        final MainRunnable instance = getInstance(MainRunnable.class);
        instance.run();
        assertEquals("Wrapping cause.", instance.getError().getMessage());
    }

    @ApplicationScoped
    public static class ClassUsingThrowingRepository {
        @Inject
        ThrowingRepository repo;

        public Object get() {
            return repo.get();
        }
    }

    @ApplicationScoped
    public static class ThrowingRepository {
        private Object fancyObjectWhichNeverSeesTheLightOfDay;

        @PostConstruct
        void init() {
            fancyObjectWhichNeverSeesTheLightOfDay = getFancyObject();
        }

        private Object getFancyObject() {
            throw new RuntimeException(
                    "Wrapping cause.",
                    new NullPointerException("Null pointer exception in initilization of throwing repo."));
        }

        public Object get() {
            return fancyObjectWhichNeverSeesTheLightOfDay;
        }
    }

    @ApplicationScoped
    public static class MainRunnable implements Runnable {
        @Inject
        ClassUsingThrowingRepository repoUsingClass;

        private RuntimeException error;

        @Override
        public void run() {
            try {
                repoUsingClass.get();
            } catch (final RuntimeException exc) {
                error = exc;
            }
        }

        public RuntimeException getError() {
            return error;
        }
    }
}
