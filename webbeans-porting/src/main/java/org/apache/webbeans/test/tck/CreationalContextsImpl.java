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

import jakarta.enterprise.context.spi.Contextual;
import org.jboss.cdi.tck.spi.CreationalContexts;

/**
 * @author <a href="mailto:struberg@apache.org">Mark Struberg</a>
 */
public class CreationalContextsImpl implements CreationalContexts
{

    @Override
    public <T> Inspectable<T> create(Contextual<T> contextual)
    {
        return new InspectableImpl<>(contextual);
    }

    public static class InspectableImpl<T> implements CreationalContexts.Inspectable<T>
    {
        private final Contextual<T> instance;
        private T lastPushedBean;
        private boolean releaseCalled;
        private boolean pushCalled;

        public InspectableImpl(Contextual<T> contextual)
        {
            this.instance = contextual;
        }

        @Override
        public Object getLastBeanPushed()
        {
            return lastPushedBean;
        }

        @Override
        public boolean isPushCalled()
        {
            return pushCalled;
        }

        @Override
        public boolean isReleaseCalled()
        {
            return releaseCalled;
        }

        @Override
        public void push(T incompleteInstance)
        {
            lastPushedBean = incompleteInstance;
            pushCalled = true;
        }

        @Override
        public void release()
        {
            releaseCalled = true;
        }
    }

}
