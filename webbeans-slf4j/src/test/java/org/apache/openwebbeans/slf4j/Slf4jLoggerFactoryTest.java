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
package org.apache.openwebbeans.slf4j;

import org.apache.webbeans.logger.WebBeansLoggerFacade;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import static java.lang.String.format;

public class Slf4jLoggerFactoryTest {
    @Test
    public void ensureLogGoesOnSlf4j() {
        final Logger logger = WebBeansLoggerFacade.getLogger(Slf4jLoggerFactoryTest.class);
        assertTrue(logger.getClass().getName(), Slf4jLogger.class.isInstance(logger));

        final PrintStream original = System.err;
        final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        System.setErr(new PrintStream(new OutputStream() {

            @Override
            public void write(final int b) {
                buffer.write(b);
                original.write(b);
            }

            @Override
            public void write(final byte[] b) throws IOException {
                buffer.write(b);
                original.write(b);
            }

            @Override
            public void write(final byte[] b, final int off, final int len) {
                buffer.write(b, off, len);
                original.write(b, off, len);
            }
        }));
        try {
            logger.info("test log");
        } finally {
            System.setErr(original);
        }
        assertEquals(
                format("[main] INFO %s - test log%s", getClass().getName(), System.lineSeparator()),
                new String(buffer.toByteArray(), StandardCharsets.UTF_8));
    }
}
