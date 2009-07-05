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

import javax.enterprise.inject.spi.AnnotatedMember;
import javax.enterprise.inject.spi.ProcessProducer;
import javax.enterprise.inject.spi.Producer;

/**
 * Implementation of {@link ProcessProducer}.
 * 
 * @version $Rev$ $Date$
 *
 * @param <X> bean class
 * @param <T> producer return type class
 */
public class ProcessProducerImpl<X,T> implements ProcessProducer<X, T>
{
    /**Annotated method or annotated field according to producer method or field*/
    private AnnotatedMember<X> annotateMember = null;
    
    /**Used by container to produce instance for producer method or field*/
    private Producer<T> producer = null;
    
    /**Set or not*/
    private boolean set;

    /**
     * {@inheritDoc}
     */
    @Override
    public void addDefinitionError(Throwable t)
    {
        // TODO Definition Error
        
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AnnotatedMember<X> getAnnotatedMember()
    {
        return this.annotateMember;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Producer<T> getProducer()
    {
        return this.producer;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setProducer(Producer<T> producer)
    {
        this.producer = producer;
        this.set = true;
    }
    
    /**
     * Returns set or not.
     * 
     * @return set or not
     */
    public boolean isSet()
    {
        return this.set;
    }

}