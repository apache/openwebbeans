/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.webbeans.intercept;

import java.util.List;

import org.apache.webbeans.component.AbstractInjectionTargetBean;
import org.apache.webbeans.component.OwbBean;

/**
 * <p>This class contains a set of static helper functions
 * for creating interceptor subclasses of beans and 
 * evaluating and performing interceptor handling for those
 * contextual instances at runtime</p>
 * 
 * <p>For each {@link OwbBean<T>} which is either decorated with a
 * {@link javax.enterprise.inject.spi.Decorator} or intercepted
 * by a {@link javax.enterprise.inject.spi.Interceptor} we 
 * dynamically create a subclass and use bytecode creation to 
 * override intercepted functions to first delegate to all 
 * registered {@link InterceptorHandler}s.</p>
 */
public class InterceptorRuntimeSupport {

    /**
     * <p>Create a interceptor/decorator subclass for the given bean.</p>
     * 
     * <p>This will first check if we really need to apply subclassing and
     * if not will return <code>null</code> instead. We need subclassing 
     * if the bean contains any {@link InterceptorType.AROUND_INVOKE}
     * or if there are any Decoraors</p>
     * 
     * 
     * @param <T>
     * @param bean
     * @return the interceptor subclass or <code>null</code> if there is no need to.
     */
    public static final <T> Class<? extends T> getInterceptorSubClass(OwbBean<T> bean)
    {
        if (!(bean instanceof AbstractInjectionTargetBean<?>))
        {
            // we can only apply interceptors and decorators to AbstractInjectionTargetBeans
            return null;
        }
        
        AbstractInjectionTargetBean<T> interceptableBean = (AbstractInjectionTargetBean<T>) bean;
        
        List<InterceptorData> interceptorStack =  interceptableBean.getInterceptorStack();
        
        // we only subclass
        List<InterceptorData> aroundInvokes = InterceptorUtil.getInterceptorMethods(interceptorStack, InterceptorType.AROUND_INVOKE);
        
        if (aroundInvokes == null || aroundInvokes.size() == 0)
        {
            return null;
        }
        
        //X TODO continue
        return null;
    }
}
