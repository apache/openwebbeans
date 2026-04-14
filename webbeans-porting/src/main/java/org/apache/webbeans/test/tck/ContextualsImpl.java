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
package org.apache.webbeans.test.tck;

import jakarta.enterprise.context.spi.Context;
import jakarta.enterprise.context.spi.CreationalContext;
import org.jboss.cdi.tck.spi.Contextuals;

/**
 * @author <a href="mailto:struberg@apache.org">Mark Struberg</a>
 */
public class ContextualsImpl implements Contextuals
{

    @Override
    public <T> Inspectable<T> create(T instance, Context context)
    {
        return new InspectableImpl<>(instance, context);
    }

    public static class InspectableImpl<T> implements Inspectable<T>
    {
        private final T instance;
        private final Context context;

        private CreationalContext<T> ccCreate;
        private CreationalContext<T> ccDestroy;
        private T destroyedInstance;

        InspectableImpl(T instance, Context context)
        {
            this.instance = instance;
            this.context = context;
        }

        @Override
        public CreationalContext<T> getCreationalContextPassedToCreate()
        {
            return ccCreate;
        }

        @Override
        public T getInstancePassedToDestroy()
        {
            return destroyedInstance;
        }

        @Override
        public CreationalContext<T> getCreationalContextPassedToDestroy()
        {
            return ccDestroy;
        }

        @Override
        public T create(CreationalContext<T> creationalContext)
        {
            ccCreate = creationalContext;
            return instance;
        }

        @Override
        public void destroy(T instance, CreationalContext<T> creationalContext)
        {
            destroyedInstance = instance;
            ccDestroy = creationalContext;
        }
    }
}
