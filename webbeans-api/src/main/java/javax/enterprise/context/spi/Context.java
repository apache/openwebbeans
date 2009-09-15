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
package javax.enterprise.context.spi;

import java.lang.annotation.Annotation;

import javax.enterprise.context.ContextNotActiveException;
import javax.enterprise.context.NormalScope;

/**
 * Every webbeans component has an associated context that are
 * defined by the {@link NormalScope} annotation. Webbeans components
 * that are contained in the context are managed by the webbeans container.
 * 
 * <p>
 * Every context has a well-defined lifecycle. It means that
 * in some time, context is active and some other time context may
 * be passive. Moreover, each context is created and destroyed by the container
 * according to the timing requirements. For example, request context is started by every 
 * http request and destroyed at the end of the http response. According to the current thread,
 * active context is called an thread current context.
 * </p>
 * 
 * <p>
 * Context is responsible for creating and destroying the {@link Contextual} instances of the
 * webbeans components.
 * </p>
 * 
 * @version $Rev$ $Date$
 */
public interface Context
{   
    /**
     * Returns the scope type of the context.
     * 
     * @return the scope type of the context
     */
    public Class<? extends Annotation> getScope();

    /**
     * If the context is not active, throws {@link ContextNotActiveException}.
     * Otherwise, it looks for the given component instance in the context map. If
     * this context contains the given webbeans component instance, it returns the component.
     * If the component is not found in this context map, it looks for the <code>creationalContext</code>
     * argument. If it is null, it returns null, otherwise new webbeans instance is created
     * and puts into the context and returns it.
     * 
     * @param <T> type of the webbeans component
     * @param component webbeans component
     * @param creationalContext {@link CreationalContext} instance
     * @return the contextual instance or null
     */
    public <T> T get(Contextual<T> component, CreationalContext<T> creationalContext);

    /**
     * Returns the instance of the webbeans in this context if exist otherwise return null.
     * 
     * @param <T> type of the webbeans component
     * @param component webbeans component
     * @return the instance of the webbeans in this context if exist otherwise return null
     */
    public <T> T get(Contextual<T> component);

    /**
     * Returns true if context is active according to the current thread,
     * returns false otherwise. 
     * 
     * @return true if context is active according to the current thread, 
     * return false otherwise
     */
    boolean isActive();
}