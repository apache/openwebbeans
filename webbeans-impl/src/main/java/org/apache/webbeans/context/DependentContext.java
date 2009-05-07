/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.apache.webbeans.context;

import javax.context.Contextual;
import javax.context.CreationalContext;
import javax.context.Dependent;

import org.apache.webbeans.context.type.ContextTypes;

/**
 * Defines the component {@link Dependent} context.
 * <p>
 * Each web beans component has a dependent context, that saves its depedent
 * objects. Dependent context is destroyed at the end of the component
 * destruction or its dependent objects are destroyed by the container at any
 * time that the dependent object is no longer alive.
 * </p>
 * 
 */
public class DependentContext extends AbstractContext
{
    public DependentContext()
    {
        super(ContextTypes.DEPENDENT);

    }
    
    

    @Override
    protected <T> T getInstance(Contextual<T> component,CreationalContext<T> creationalContext)
    {
        T object = null;
        
        if(creationalContext == null)
        {
            return null;
        }
        else
        {
            object = component.create(creationalContext);
        }
        

        return object;
    }


    @Override
    public void setComponentInstanceMap()
    {

    }



    /* (non-Javadoc)
     * @see org.apache.webbeans.context.AbstractContext#get(javax.context.Contextual)
     */
    @Override
    public <T> T get(Contextual<T> component)
    {
        return null;
    }

}