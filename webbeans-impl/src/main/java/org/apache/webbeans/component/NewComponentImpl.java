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
package org.apache.webbeans.component;

import javax.context.CreationalContext;
import javax.inject.New;

/**
 * Component definition with {@link New} binding annotation.
 * <p>
 * It is defined as concrete java class component.
 * </p>
 * 
 * @author <a href="mailto:gurkanerdogdu@yahoo.com">Gurkan Erdogdu</a>
 * @since 1.0
 */
public class NewComponentImpl<T> extends ComponentImpl<T>
{
    private WebBeansType definedType;

    public NewComponentImpl(Class<T> returnType, WebBeansType definedType)
    {
        super(returnType);
        this.definedType = definedType;        
    }

    @Override
    protected T createInstance(CreationalContext<T> creationalContext)
    {
        return super.createInstance(creationalContext);
    }

    @Override
    protected void destroyInstance(T instance)
    {
        super.destroyInstance(instance);
    }

    /**
     * @return the definedType
     */
    public WebBeansType getDefinedType()
    {
        return definedType;
    }

}
