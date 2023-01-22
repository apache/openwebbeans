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
package org.apache.webbeans.test.producer;

import org.apache.webbeans.test.AbstractUnitTest;
import org.junit.Test;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.SessionScoped;
import jakarta.enterprise.inject.Produces;
import java.io.Serializable;

import static org.junit.Assert.assertTrue;

public class SerializableAtRuntimeProducerTest extends AbstractUnitTest
{
    @Test
    public void run()
    {
        startContainer(TheProducer.class);
        getWebBeansContext().getContextsService().startContext(SessionScoped.class, null);
        final NotSerializable bean = getInstance(NotSerializable.class);
        bean.touch();
        assertTrue(Serializable.class.isInstance(bean));
        getWebBeansContext().getContextsService().endContext(SessionScoped.class, null);
    }

    @ApplicationScoped
    public static class TheProducer
    {
        @Produces
        @SessionScoped
        public NotSerializable produce()
        {
            return new NotSerializableAndSerializable()
            {
                @Override
                public void touch()
                {
                    // no-op
                }
            };
        }
    }

    public interface NotSerializable
    {
        void touch();
    }

    public interface NotSerializableAndSerializable extends Serializable, NotSerializable
    {
    }
}
