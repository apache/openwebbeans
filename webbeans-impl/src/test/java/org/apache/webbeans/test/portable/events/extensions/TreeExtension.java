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
package org.apache.webbeans.test.portable.events.extensions;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.ProcessAnnotatedType;

import org.apache.webbeans.test.portable.events.beans.Apple;
import org.apache.webbeans.test.portable.events.beans.AppleTree;
import org.apache.webbeans.test.portable.events.beans.Cherry;
import org.apache.webbeans.test.portable.events.beans.CherryTree;
import org.apache.webbeans.test.portable.events.beans.Tree;

public class TreeExtension implements Extension
{
    public static int GENERIC_CALLED = 0;
    public static int TREE_CALLED = 0;
    public static int APPLE_TREE_GENERIC_CALLED = 0;
    public static int CHERRY_TREE_GENERIC_CALLED = 0;
    public static int APPLE_TREE_CALLED = 0;
    public static int CHERRY_TREE_CALLED = 0;
    
    public static void reset()
    {
        GENERIC_CALLED = 0;
        TREE_CALLED = 0;
        APPLE_TREE_GENERIC_CALLED = 0;
        CHERRY_TREE_GENERIC_CALLED = 0;
        APPLE_TREE_CALLED = 0;
        CHERRY_TREE_CALLED = 0;
    }
    
    public void generic(@Observes ProcessAnnotatedType event)
    {
        GENERIC_CALLED++;
    } 
    
    public void tree(@Observes ProcessAnnotatedType<Tree> event)
    {
        TREE_CALLED++;
    }
    
    public void genericAppleTree(@Observes ProcessAnnotatedType<Tree<Apple>> event)
    {
        APPLE_TREE_GENERIC_CALLED++;
    } 

    public void genericCherryTree(@Observes ProcessAnnotatedType<Tree<Cherry>> event)
    {
        CHERRY_TREE_GENERIC_CALLED++;
    }
    
    public void appleTree(@Observes ProcessAnnotatedType<AppleTree> event)
    {
        APPLE_TREE_CALLED++;
    } 

    public void cherryTree(@Observes ProcessAnnotatedType<CherryTree> event)
    {
        CHERRY_TREE_CALLED++;
    } 
}
