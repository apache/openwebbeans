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
package org.apache.openwebbeans.junit5;

import org.junit.jupiter.api.Test;

import java.io.Closeable;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Cdi(disableDiscovery = true, onStarts = CdiWithOnStartTest.MyOnStart.class)
class CdiWithOnStartTest {
    @Test
    void run() {
        assertTrue(MyOnStart.started.get());
    }

    public static class MyOnStart implements Cdi.OnStart {
        private static final AtomicBoolean started = new AtomicBoolean();

        @Override
        public Closeable get() {
            assertFalse(started.get());
            started.set(true);
            return () -> started.set(false);
        }
    }
}
