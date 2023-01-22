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
package org.apache.openwebbeans.se;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;

import org.junit.Test;

public class CDILauncherTest
{
    @Test
    public void namedRunnable()
    {
        assertFalse(MyRunnable.ran);
        CDILauncher.main(new String[]{
                "--openwebbeans.main", "main",
                "--openwebbeans.disableDiscovery", "true",
                "--openwebbeans.classes", MyRunnable.class.getName()
        });
        assertTrue(MyRunnable.ran);
    }

    @Test
    public void typedRunnable()
    {
        assertFalse(MyRunnable2.ran);
        CDILauncher.main(new String[]{
                "--openwebbeans.main", MyRunnable2.class.getName(),
                "--openwebbeans.disableDiscovery", "true",
                "--openwebbeans.classes", MyRunnable2.class.getName()
        });
        assertTrue(MyRunnable2.ran);
    }

    @Test
    public void mainArgs()
    {
        assertNull(MyMain.args);
        CDILauncher.main(new String[]{
                "--openwebbeans.main", MyMain.class.getName(),
                "--openwebbeans.disableDiscovery", "true",
                "--openwebbeans.classes", MyMain.class.getName(),
                "--other", "yes",
                "and", "args"
        });
        assertArrayEquals(new String[]{
                "--other", "yes",
                "and", "args"
        }, MyMain.args);
    }

    @Named("main")
    @ApplicationScoped
    public static class MyRunnable implements Runnable
    {
        static boolean ran = false;

        @Override
        public void run()
        {
            ran = true;
        }
    }

    @ApplicationScoped
    public static class MyRunnable2 implements Runnable
    {
        static boolean ran = false;

        @Override
        public void run()
        {
            ran = true;
        }
    }

    @ApplicationScoped
    public static class MyMain
    {
        static String[] args;

        public void main(final String... args)
        {
            MyMain.args = args;
        }
    }
}
