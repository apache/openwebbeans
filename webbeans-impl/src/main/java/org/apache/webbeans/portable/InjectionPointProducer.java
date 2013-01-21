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
package org.apache.webbeans.portable;

import java.util.Stack;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.InjectionPoint;

public class InjectionPointProducer extends AbstractProducer<InjectionPoint>
{

    //X TODO refactor. public static variables are utterly ugly
    private static ThreadLocal<Stack<InjectionPoint>> localThreadlocalStack = new ThreadLocal<Stack<InjectionPoint>>();
    
    /**
     * {@inheritDoc}
     */
    @Override
    public InjectionPoint produce(CreationalContext<InjectionPoint> creationalContext)
    {
        return getStackOfInjectionPoints().peek();
    }

    @Override
    public void dispose(InjectionPoint ip)
    {
        removeThreadLocal();
    }

    private static Stack<InjectionPoint> getStackOfInjectionPoints()
    {
        Stack<InjectionPoint> stackIP = localThreadlocalStack.get();
        if (null == stackIP)
        {
            stackIP = new Stack<InjectionPoint>();
        }
        return stackIP;
    }

    public static boolean setThreadLocal(InjectionPoint ip)
    {
        Stack<InjectionPoint> stackIP = getStackOfInjectionPoints();
        stackIP.push(ip);
        localThreadlocalStack.set(stackIP);
        return true;
    }
    
    public static void unsetThreadLocal()
    {
        Stack<InjectionPoint> stackIP = getStackOfInjectionPoints();
        if (!stackIP.isEmpty())
        {
            stackIP.pop();
        }
    }
    
    /**
     * Removes the ThreadLocal from the ThreadMap to prevent memory leaks.
     */
    public static void removeThreadLocal()
    {
        getStackOfInjectionPoints().clear();
        localThreadlocalStack.remove();
    }
    
    public static boolean isStackEmpty()
    {
        return getStackOfInjectionPoints().isEmpty();
    }
}
