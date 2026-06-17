/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.webbeans.test.component.classscanning;

import java.io.File;
import java.net.URL;

import org.apache.webbeans.test.AbstractUnitTest;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Test scanning of a class which cannot
 */
public class NoClassDefFoundHandlingTest extends AbstractUnitTest
{

    @Test
    public void testNoClassDefFoundHandling()
    {
        deleteClassFile(this.getClass().getPackageName() + ".NcdfClassA");
        startContainer(NcdfClassB.class, AvailableBean.class);

        assertNotNull(getBean(AvailableBean.class));
        assertNull(getBean(NcdfClassB.class));
    }

    private void deleteClassFile(String className)
    {
        String classResourceName = "/" + className.replace('.', '/') + ".class";
        final URL classRessource = this.getClass().getResource(classResourceName);
        if (classRessource != null) {
            new File(classRessource.getFile()).delete();
        }
    }
}
