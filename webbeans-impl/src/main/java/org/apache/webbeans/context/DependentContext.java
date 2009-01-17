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

import javax.webbeans.ContextNotActiveException;
import javax.webbeans.Dependent;
import javax.webbeans.manager.Bean;
import javax.webbeans.manager.Contextual;
import javax.webbeans.manager.CreationalContext;

import org.apache.webbeans.component.AbstractComponent;
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
 * @author <a href="mailto:gurkanerdogdu@yahoo.com">Gurkan Erdogdu</a>
 * @since 1.0
 */
public class DependentContext extends AbstractContext
{
    private AbstractComponent<?> owner;

    public DependentContext()
    {
        super(ContextTypes.DEPENDENT);

    }

    @Override
    protected <T> T getInstance(Contextual<T> component, boolean create,CreationalContext<T> creationalContext)
    {
        if (isActive())
        {
            if (create)
            {
                T object = component.create(creationalContext);

                return object;
            }
        }

        else
        {
            throw new ContextNotActiveException("Dependent  context with WebBeans component class : " + owner.getReturnType() + " is not active");
        }

        return null;
    }

    @Override
    protected <T> void removeInstance(Bean<T> component)
    {
        // no-op
    }

    /*
     * (non-Javadoc)
     * @see org.apache.webbeans.context.AbstractContext#destroy()
     */
    @Override
    public <T> void destroy()
    {

    }

    @Override
    public void setComponentInstanceMap()
    {

    }

}