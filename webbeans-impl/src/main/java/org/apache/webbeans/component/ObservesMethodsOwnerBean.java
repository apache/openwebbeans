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

import java.lang.reflect.Method;
import java.util.Set;

/**
 * Defines contract for beans that coud have observable
 * method.
 * 
 * @version $Rev$ $Date$
 *
 * @param <T> bean type
 */
public interface ObservesMethodsOwnerBean<T>
{
    /**
     * Returns set of observable methods.
     * 
     * @return set of observable methods
     */
    public Set<Method> getObservableMethods();

    /**
     * Adds new observer method.
     * 
     * @param observerMethod observer method
     */
    public void addObservableMethod(Method observerMethod);
    
    /**
     * Returns true if coming from <pre>@Realization</pre>.
     * 
     * @return true if coming from <pre>@Realization</pre>
     */
    @Deprecated //Removed from specification
    public boolean isFromRealizes();
    
    /**
     * Set its realized.
     * 
     * @param realized is realized
     */
    @Deprecated //Removed from specification
    public void setFromRealizes(boolean realized);
}