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

/**
 * Abstract class for producer components.
 * 
 * @version $Rev$ $Date$
 *
 * @param <T> bean type info
 */
public abstract class AbstractProducerBean<T> extends AbstractBean<T> implements IBeanHasParent<T>
{
    /**Owner of the producer field component*/
    protected AbstractBean<?> ownerComponent;

    /**
     * Create a new instance.
     * 
     * @param type webbeans typr
     * @param returnType bean type info
     * @param ownerComponent owner bean
     */
    protected AbstractProducerBean(WebBeansType type, Class<T> returnType, AbstractBean<?> ownerComponent)
    {
        super(type,returnType);
        this.ownerComponent = ownerComponent;
    }
    
    /**
     * {@inheritDoc}
     */
    public AbstractBean<?> getParent()
    {
        return this.ownerComponent;
    }
    
    public void dispose(T instance)
    {
        //Do nothing
    }
    
}