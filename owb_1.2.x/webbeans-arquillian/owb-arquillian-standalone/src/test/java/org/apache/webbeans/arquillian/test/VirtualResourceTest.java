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
package org.apache.webbeans.arquillian.test;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(Arquillian.class)
public class VirtualResourceTest
{
    @Deployment
    public static Archive<?> jar()
    {
        return ShrinkWrap.create(JavaArchive.class).addAsResource(new StringAsset("virtual"), "resource.txt");
    }

    @Test
    public void checkResourceIsAvailable() throws IOException
    {
        final InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream("resource.txt");
        assertNotNull(is);

        try
        {
            assertEquals("virtual", new BufferedReader(new InputStreamReader(is)).readLine());
        }
        finally
        {
            try
            {
                is.close();
            }
            catch (final IOException e)
            {
                // no-op
            }
        }
    }
}
