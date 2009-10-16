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

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Set;

import javax.enterprise.event.Reception;
import javax.enterprise.event.TransactionPhase;

/**
 * <p>ObserverMethod is the SPI to handle an observer method, which is 
 * an event consumer for an event of the given type T. An instance of
 * ObserverMethod exists for every observer method of every enabled bean.</p> 
 * 
 * <p>A class may have n observer methods.</p>
 * <p>Each observer method must have a void return value and exactly one parameter
 * which defines which event it {@code javax.enterprise.event.Observes}.
 * The observed event may be further specified by using an optional Qualifier.</p> 
 * 
 * Sample:
 * <pre>
 * public class UserHandler 
 * {
 *   public void afterUserLogin(@Observes UserLoginEvent userHasLoggedIn) 
 *   {
 *     // prepare some data for the user, ...
 *     int userId = userHadLoggedIn.getUserId();
 *     ...
 *   }
 *   
 *   public void afterAdminLogin(@Observes @Admin UserLoginEvent userHasLoggedIn) 
 *   {
 *     // prepare stuff for the admin user
 *     ...
 *   }
 *   
 *   public void afterUserLogout(@Observes UserLogoutEvent userHasLoggedOut) 
 *   {
 *     // cleanup afterwards 
 *     ...
 *   }
 * }
 * </pre>
 *  
 * @param <T> the event which should be observed
 * @see javax.enterprise.event.Observes
 */
public interface ObserverMethod<T>
{
    public Class<?> getBeanClass();
    
    /**
     * @return the type of the observed event
     */
    public Type getObservedType();
    
    /**
     * @return the defined Qualifiers plus {@code javax.enterprise.inject.Any}
     */
    public Set<Annotation> getObservedQualifiers();
    
    /**
     * @return either {@code Reception#IF_EXISTS} if the observed method must only be called if an instance
     * of the bean which defines the observer method aready exists in the specified context or {@code Reception#ALWAYS}. 
     */
    public Reception getReception();
    
    /**
     * @return the appropriate {@code TransactionPhase} for a transactional observer method or
     * {@code TransactionPhase#IN_PROGRESS} otherwise. 
     */
    public TransactionPhase getTransactionPhase();

    /**
     * will actually cann the underlying observer method
     * @param event
     */
    public void notify(T event);    

}
