/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package javax.enterprise.inject.spi;

/**
 * Container fires this event for each
 * producer field/method including resources.
 * 
 * @version $Rev$ $Date$
 *
 * @param <X> bean class info
 * @param <T> producer return type
 */
public interface ProcessProducer<X, T>
{
    /**
     * Returns annotated member.
     * 
     * @return annotated member
     */
    public AnnotatedMember<X> getAnnotatedMember();
    
    /**
     * Returns producer instance.
     * 
     * @return producer instance
     */
    public Producer<T> getProducer();
    
    /**
     * Replaces producer instance.
     * 
     * @param producer new producer
     */
    public void setProducer(Producer<T> producer);

    /**
     * Adding definition error. Container aborts processing.
     * 
     * @param t throwable
     */
    public void addDefinitionError(Throwable t);
    

}