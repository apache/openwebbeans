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
package org.apache.webbeans.test.portable.events;

import java.util.ArrayList;
import java.util.Collection;

import org.junit.Assert;

import org.apache.webbeans.test.AbstractUnitTest;
import org.apache.webbeans.test.portable.events.beans.Apple;
import org.apache.webbeans.test.portable.events.beans.AppleTree;
import org.apache.webbeans.test.portable.events.beans.Cherry;
import org.apache.webbeans.test.portable.events.beans.CherryTree;
import org.apache.webbeans.test.portable.events.beans.Tree;
import org.apache.webbeans.test.portable.events.extensions.AppleExtension;
import org.apache.webbeans.test.portable.events.extensions.AppleExtension1;
import org.apache.webbeans.test.portable.events.extensions.MessageReceiverExtension;
import org.apache.webbeans.test.portable.events.extensions.MessageSenderExtension;
import org.apache.webbeans.test.portable.events.extensions.NotAppleExtnsion;
import org.apache.webbeans.test.portable.events.extensions.RawTypeExtension;
import org.apache.webbeans.test.portable.events.extensions.TreeExtension;
import org.apache.webbeans.test.portable.events.extensions.TypeVariableExtension;
import org.apache.webbeans.test.portable.events.extensions.WildcardExtension;
import org.apache.webbeans.test.portable.events.extensions.WrongTypeVariableExtension;
import org.apache.webbeans.test.portable.events.extensions.WrongWildcardExtension;
import org.junit.Test;

public class PortableEventTest extends AbstractUnitTest
{

    @Test
    public void testAppleExtension()
    {
        Collection<String> beanXmls = new ArrayList<String>();
        
        Collection<Class<?>> beanClasses = new ArrayList<Class<?>>();
        beanClasses.add(Apple.class);
        
        addExtension(new AppleExtension());        
        startContainer(beanClasses, beanXmls);
        
        Assert.assertEquals("apple", AppleExtension.NAME);
        
        shutDownContainer();
    }
    
    @Test

    public void testNotAppleExtension()
    {
        Collection<String> beanXmls = new ArrayList<String>();
        
        Collection<Class<?>> beanClasses = new ArrayList<Class<?>>();
        beanClasses.add(Apple.class);
        
        addExtension(new NotAppleExtnsion());        
        startContainer(beanClasses, beanXmls);
        
        Assert.assertFalse(NotAppleExtnsion.CALLED);
        
        shutDownContainer();
    }
    
    @Test
    public void testRawTypeExtension()
    {
        Collection<String> beanXmls = new ArrayList<String>();
        
        Collection<Class<?>> beanClasses = new ArrayList<Class<?>>();
        beanClasses.add(Apple.class);
        
        addExtension(new RawTypeExtension());        
        startContainer(beanClasses, beanXmls);
        
        Assert.assertTrue(RawTypeExtension.CALLED);
        
        shutDownContainer();
    }
    
    @Test

    public void testTypeVariableExtension()
    {
        Collection<String> beanXmls = new ArrayList<String>();
        
        Collection<Class<?>> beanClasses = new ArrayList<Class<?>>();
        beanClasses.add(Apple.class);
                
        addExtension(new TypeVariableExtension());
        startContainer(beanClasses, beanXmls);
        
        Assert.assertTrue(TypeVariableExtension.CALLED);
        
        shutDownContainer();
    }
    
    @Test

    public void testwildcardExtension()
    {
        Collection<String> beanXmls = new ArrayList<String>();
        
        Collection<Class<?>> beanClasses = new ArrayList<Class<?>>();
        beanClasses.add(Apple.class);
        
        addExtension(new WildcardExtension());       
        startContainer(beanClasses, beanXmls);
        
        Assert.assertTrue(WildcardExtension.CALLED);
        
        shutDownContainer();
    }
    
    @Test

    public void testWrongTypeVariableExtension()
    {
        Collection<String> beanXmls = new ArrayList<String>();
        
        Collection<Class<?>> beanClasses = new ArrayList<Class<?>>();
        beanClasses.add(Apple.class);
        
        addExtension(new WrongTypeVariableExtension());  
        startContainer(beanClasses, beanXmls);
        
        Assert.assertFalse(WrongTypeVariableExtension.CALLED);
        
        shutDownContainer();
    }
    
    @Test

    public void testWrongWildcardTypeExtension()
    {
        Collection<String> beanXmls = new ArrayList<String>();
        
        Collection<Class<?>> beanClasses = new ArrayList<Class<?>>();
        beanClasses.add(Apple.class);
        
        addExtension(new WrongWildcardExtension());        
        startContainer(beanClasses, beanXmls);
        
        Assert.assertFalse(WrongWildcardExtension.CALLED);
        
        shutDownContainer();
    }

    @Test
    public void testCustomMessagesInExtensions()
    {
        addExtension(new MessageSenderExtension());
        addExtension(new MessageReceiverExtension());

        startContainer();
    }

    
    @Test
    public void testNumberCallsNegative()
    {
        AppleExtension1.reset();
        
        Collection<String> beanXmls = new ArrayList<String>();

        Collection<Class<?>> beanClasses = new ArrayList<Class<?>>();
        
        addExtension(new AppleExtension1());        
        startContainer(beanClasses, beanXmls);
        
        Assert.assertSame(AppleExtension1.CALLED, 0);
        Assert.assertSame(AppleExtension1.TYPED_CALLED, 0);
        Assert.assertSame(AppleExtension1.MANAGED_CALLED, 0);
        Assert.assertSame(AppleExtension1.MANAGED_TYPED_CALLED, 0);
        
        shutDownContainer();
    }
    
    @Test
    public void testNumberCallsPositive()
    {
        AppleExtension1.reset();
        
        Collection<String> beanXmls = new ArrayList<String>();

        Collection<Class<?>> beanClasses = new ArrayList<Class<?>>();
        beanClasses.add(Apple.class);
        
        addExtension(new AppleExtension1());        
        startContainer(beanClasses, beanXmls);
        
        Assert.assertSame(AppleExtension1.CALLED, 1);
        Assert.assertSame(AppleExtension1.TYPED_CALLED, 1);
        Assert.assertSame(AppleExtension1.MANAGED_CALLED, 1);
        Assert.assertSame(AppleExtension1.MANAGED_TYPED_CALLED, 1);
        
        shutDownContainer();
    }
    
    @Test
    public void testNumberCallsPositiveMultipleTypes()
    {
        AppleExtension1.reset();
        
        Collection<String> beanXmls = new ArrayList<String>();

        Collection<Class<?>> beanClasses = new ArrayList<Class<?>>();
        beanClasses.add(Apple.class);
        beanClasses.add(Cherry.class);
        
        addExtension(new AppleExtension1());        
        startContainer(beanClasses, beanXmls);
        
        Assert.assertSame(AppleExtension1.CALLED, 2);
        Assert.assertSame(AppleExtension1.TYPED_CALLED, 1);
        Assert.assertSame(AppleExtension1.MANAGED_CALLED, 2);
        Assert.assertSame(AppleExtension1.MANAGED_TYPED_CALLED, 1);
        
        shutDownContainer();
    }
    
    
    @Test
    public void testNumberCallsGenerics()
    {
        TreeExtension.reset();
        
        Collection<String> beanXmls = new ArrayList<String>();

        Collection<Class<?>> beanClasses = new ArrayList<Class<?>>();
        beanClasses.add(Tree.class);
        beanClasses.add(AppleTree.class);
        beanClasses.add(CherryTree.class);
        
        addExtension(new TreeExtension());        
        startContainer(beanClasses, beanXmls);
        
        Assert.assertSame(3, TreeExtension.GENERIC_CALLED);
        Assert.assertSame(3, TreeExtension.TREE_CALLED);
        Assert.assertSame(1, TreeExtension.APPLE_TREE_CALLED);
        Assert.assertSame(1, TreeExtension.CHERRY_TREE_CALLED);
        Assert.assertSame(1, TreeExtension.APPLE_TREE_GENERIC_CALLED);
        Assert.assertSame(1, TreeExtension.CHERRY_TREE_GENERIC_CALLED);
        
        shutDownContainer();
    }
}
