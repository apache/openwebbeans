/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.webbeans.component;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.InjectionPoint;

import org.apache.webbeans.annotation.DefaultLiteral;
import org.apache.webbeans.annotation.DependentScopeLiteral;


public class InjectionPointBean extends AbstractOwbBean<InjectionPoint>
{
    public static ThreadLocal<InjectionPoint> local = new ThreadLocal<InjectionPoint>();
    
    public InjectionPointBean()
    {
        super(WebBeansType.INJECTIONPOINT,InjectionPoint.class);
        
        addQualifier(new DefaultLiteral());
        setImplScopeType(new DependentScopeLiteral());
        addApiType(InjectionPoint.class);
        addApiType(Object.class);
    }

    @Override
    protected InjectionPoint createInstance(CreationalContext<InjectionPoint> creationalContext)
    {
        try
        {
            return local.get();
            
        }finally
        {
            local.remove();
        }
    }

    @Override
    protected void destroyInstance(InjectionPoint instance, CreationalContext<InjectionPoint> creationalContext)
    {
        local.remove();
    }
    
    
}
