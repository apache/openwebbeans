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
package org.apache.webbeans.intercept;

import javax.webbeans.manager.InterceptionType;

import org.apache.webbeans.exception.WebBeansException;

/**
 * Type of the interceptors. Defines in the EJB specification and common
 * annotations.
 * 
 * @author <a href="mailto:gurkanerdogdu@yahoo.com">Gurkan Erdogdu</a>
 * @since 1.0
 */
public enum InterceptorType
{
    AROUND_INVOKE, POST_CONSTRUCT, PRE_DESTROY, PRE_PASSIVATE, POST_ACTIVATE;

    public static InterceptorType getType(InterceptionType type)
    {
        if (type.equals(InterceptionType.AROUND_INVOKE))
        {
            return AROUND_INVOKE;
        }
        else if (type.equals(InterceptionType.POST_CONSTRUCT))
        {
            return POST_CONSTRUCT;
        }
        else if (type.equals(InterceptionType.PRE_DESTROY))
        {
            return PRE_DESTROY;
        }
        else if (type.equals(InterceptionType.PRE_PASSIVATE))
        {
            return PRE_PASSIVATE;
        }
        else if (type.equals(InterceptionType.POST_ACTIVATE))
        {
            return POST_ACTIVATE;
        }
        else
        {
            throw new WebBeansException("Undefined interceptor type!");
        }

    }
}