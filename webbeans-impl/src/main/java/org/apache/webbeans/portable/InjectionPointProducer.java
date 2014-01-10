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

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.InjectionPoint;

import org.apache.webbeans.context.creational.CreationalContextImpl;
import org.apache.webbeans.util.ClassUtil;

public class InjectionPointProducer extends AbstractProducer<InjectionPoint>
{
    
    /**
     * {@inheritDoc}
     */
    @Override
    public InjectionPoint produce(CreationalContext<InjectionPoint> creationalContext)
    {
        if (!(creationalContext instanceof CreationalContextImpl))
        {
            return null;
        }
        // the first injection point on the stack is of type InjectionPoint, so we need the second one
        CreationalContextImpl<InjectionPoint> creationalContextImpl = (CreationalContextImpl<InjectionPoint>)creationalContext;
        InjectionPoint first = creationalContextImpl.removeInjectionPoint();
        if (!InjectionPoint.class.isAssignableFrom(ClassUtil.getClass(first.getType())))
        {
            throw new IllegalStateException("Inconsistent injection point stack");
        }
        try
        {
            return creationalContextImpl.getInjectionPoint();
        }
        finally
        {
            creationalContextImpl.putInjectionPoint(first);
        }
    }

    @Override
    public void dispose(InjectionPoint ip)
    {
        // nothing to do
    }
}
