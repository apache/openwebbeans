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
package org.apache.webbeans.portable.events;

import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.ProcessSessionBean;
import javax.enterprise.inject.spi.SessionBeanType;

/**
 * Implementation of {@link ProcessSessionBean}.
 * 
 * @version $Rev$ $Date$
 *
 * @param <X> ejb class info
 */
public class ProcessSessionBeanImpl<X> extends ProcessBeanImpl<Object> implements ProcessSessionBean<X>
{
    /**Session bean annotated type*/
    private AnnotatedType<X> annotatedBeanClass;
    
    /**Ejb name*/
    private String ejbName;
    
    /**Session bean type*/
    private SessionBeanType type;

    /**
     * {@inheritDoc}
     */
    @Override
    public AnnotatedType<X> getAnnotatedBeanClass()
    {
        return this.annotatedBeanClass;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getEjbName()
    {
        return this.ejbName;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public SessionBeanType getSessionBeanType()
    {
        return this.type;
    }
    
}