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
package org.apache.webbeans.test.unittests.specializes.logger;


import junit.framework.Assert;
import org.apache.webbeans.test.AbstractUnitTest;
import org.apache.webbeans.test.component.specializes.logger.ISomeLogger;
import org.apache.webbeans.test.component.specializes.logger.MockNotSpecializedLogger;
import org.apache.webbeans.test.component.specializes.logger.MockSpecializedLogger;
import org.apache.webbeans.test.component.specializes.logger.SpecializedInjector;
import org.apache.webbeans.test.component.specializes.logger.SystemLogger;
import org.junit.Test;

public class LoggerSpecializationTest extends AbstractUnitTest
{
    @Test
    public void testNotSpecializedVersion()
    {
        startContainer("org/apache/webbeans/test/xml/specializes/alternatives.xml",
                       SystemLogger.class, MockNotSpecializedLogger.class, SpecializedInjector.class);
        
        SpecializedInjector injector = getInstance(SpecializedInjector.class);
        ISomeLogger logger = injector.logger();
        Assert.assertTrue(logger instanceof SystemLogger);
        logger.printError("Hello World");
        SystemLogger sysLogger = (SystemLogger)logger;
        Assert.assertEquals("Hello World", sysLogger.getMessage());
    }
    
    @Test
    public void testSpecializedVersion()
    {
        startContainer("org/apache/webbeans/test/xml/specializes/alternatives.xml",
                SystemLogger.class, MockSpecializedLogger.class, SpecializedInjector.class);
        
        SpecializedInjector injector = getInstance(SpecializedInjector.class);
        
        ISomeLogger logger = injector.logger();
        Assert.assertTrue(logger instanceof MockSpecializedLogger);
        logger.printError("Hello World");
        MockSpecializedLogger sysLogger = (MockSpecializedLogger)logger;
        Assert.assertEquals("Hello World", sysLogger.getMessage());
    }
}
