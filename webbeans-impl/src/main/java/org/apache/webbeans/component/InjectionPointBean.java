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

import org.apache.webbeans.annotation.CurrentLiteral;
import org.apache.webbeans.annotation.DependentScopeLiteral;
import org.apache.webbeans.annotation.StandardLiteral;


public class InjectionPointBean extends AbstractBean<InjectionPoint>
{
    private InjectionPoint injectionPoint = null;
    
    public InjectionPointBean(InjectionPoint injectionPoint)
    {
        super(WebBeansType.INJECTIONPOINT,InjectionPoint.class);
        this.injectionPoint = injectionPoint;
        
        addQualifier(new CurrentLiteral());
        setImplScopeType(new DependentScopeLiteral());
        setType(new StandardLiteral());
        addApiType(InjectionPoint.class);
        addApiType(Object.class);
    }

    @Override
    protected InjectionPoint createInstance(CreationalContext<InjectionPoint> creationalContext)
    {
        return injectionPoint;
    }

    @Override
    protected void destroyInstance(InjectionPoint instance)
    {
        this.injectionPoint = null;
    }
    
    
}
