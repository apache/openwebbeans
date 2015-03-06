/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.webbeans.test.portable.events.extensions;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.Extension;

import junit.framework.Assert;

/**
 * Test that an Extension can send  any message to another Extension.
 *
 * @see org.apache.webbeans.test.portable.events.extensions.MessageReceiverExtension
 */
public class MessageSenderExtension implements Extension
{
    public static class ExtensionPayload
    {
        // no content needed
    }

    public void bbd(@Observes BeforeBeanDiscovery beforeBeanDiscovery, BeanManager beanManager)
    {
        Assert.assertNotNull(beforeBeanDiscovery);
        Assert.assertNotNull(beanManager);

        beanManager.fireEvent(new ExtensionPayload());
    }
}
