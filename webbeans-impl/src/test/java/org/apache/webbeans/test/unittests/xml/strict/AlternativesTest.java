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
package org.apache.webbeans.test.unittests.xml.strict;


import jakarta.enterprise.inject.spi.Bean;

import org.apache.webbeans.test.xml.strict.Alternative3;
import org.apache.webbeans.test.xml.strict.AlternativeStereotype;
import org.apache.webbeans.test.xml.strict.OriginalBean;
import org.apache.webbeans.test.xml.strict.SomeInterface;
import org.junit.Assert;

import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.inject.AlternativesManager;
import org.apache.webbeans.test.AbstractUnitTest;
import org.apache.webbeans.test.xml.strict.Alternative1;
import org.apache.webbeans.test.xml.strict.Alternative2;
import org.junit.Test;

public class AlternativesTest extends AbstractUnitTest
{

    @Test
    public void testAlternativeCorrect()
    {
        startContainer("org/apache/webbeans/test/xml/strict/alternatives_correct.xml", Alternative1.class, Alternative2.class);

        Bean<Alternative1> alternative1 = getBean(Alternative1.class);
        Bean<Alternative2> alternative2 = getBean(Alternative2.class);

        AlternativesManager manager = WebBeansContext.getInstance().getAlternativesManager();
        
        Assert.assertTrue(manager.isAlternative(alternative1));
        Assert.assertTrue(manager.isAlternative(alternative2));
        
        manager.clear();
        
    }

    @Test
    public void testPriorityEnabledStereotypeAlternative()
    {
        startContainer(Alternative3.class, AlternativeStereotype.class, OriginalBean.class);

        SomeInterface instance = getInstance(SomeInterface.class);
        Assert.assertNotNull(instance);
        Assert.assertTrue(instance instanceof Alternative3);
    }
    
    @Test(expected=WebBeansConfigurationException.class)
    public void testDoubleAlternativeClass()
    {        
        startContainer("org/apache/webbeans/test/xml/strict/alternatives_failed.xml");
    }
    
    @Test(expected=WebBeansConfigurationException.class)
    public void testDoubleAlternativeStereotype()
    {        
        startContainer("org/apache/webbeans/test/xml/strict/alternatives_failed2.xml");
    }
    
    @Test(expected=WebBeansConfigurationException.class)
    public void testNoClass()
    {        
        startContainer("org/apache/webbeans/test/xml/strict/alternatives_failed3.xml");
    }
    
    @Test(expected=WebBeansConfigurationException.class)
    public void testNoStereotype()
    {        
        startContainer("org/apache/webbeans/test/xml/strict/alternatives_failed4.xml");
    }

    @Test(expected=WebBeansConfigurationException.class)
    public void testNotAnnotationClass()
    {        
        startContainer("org/apache/webbeans/test/xml/strict/alternatives_failed5.xml");
    }

    @Test(expected=WebBeansConfigurationException.class)
    public void testNotStereotype()
    {        
        startContainer("org/apache/webbeans/test/xml/strict/alternatives_failed6.xml");
    }

}
