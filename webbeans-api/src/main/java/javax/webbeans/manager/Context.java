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
package javax.webbeans.manager;

import java.lang.annotation.Annotation;

/**
 * @author gurkanerdogdu
 * @since 1.0
 */
public interface Context
{
    /**
     * Gets the context scope type.
     * 
     * @return context scope type
     */
    public Class<? extends Annotation> getScopeType();

    /**
     * Gets the given web beans component instance from the context.
     * <p>
     * If the create argument is true and the instance is not contained in the
     * context, new instance is created and returned.
     * </p>
     * 
     * @param <T> generic type
     * @param container web beans container
     * @param component web beans component
     * @param create creat or not flag
     * @return the web beans component instance
     */
    public <T> T get(Bean<T> component, boolean create);

    /**
     * Removes the given web beans component from the context.
     * 
     * @param <T> generic type
     * @param component web beans component
     */
    @Deprecated
    public <T> void remove(Manager container, Bean<T> component);

    /**
     * True if context is active
     * 
     * @return true if context is active
     */
    boolean isActive();
}
