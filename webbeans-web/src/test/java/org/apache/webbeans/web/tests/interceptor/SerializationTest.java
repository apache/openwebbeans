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
package org.apache.webbeans.web.tests.interceptor;


import org.junit.Assert;
import org.apache.webbeans.test.AbstractUnitTest;
import org.apache.webbeans.web.lifecycle.test.MockServletContext;
import org.apache.webbeans.web.tests.MockServletRequest;
import org.junit.Test;

import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import javax.servlet.ServletRequestEvent;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;


/**
 *  Tests for various serialization issues
 */
public class SerializationTest extends AbstractUnitTest
{
    @Test
    public void testDeserializationOfRequestScopedBean() throws Exception
    {
        final MockServletContext mockServletContext = new MockServletContext();
        final MockServletRequest mockServletRequest = new MockServletRequest();
        final ServletRequestEvent servletRequestEvent = new ServletRequestEvent(mockServletContext, mockServletRequest);

        final Collection<Class<?>> classes = new ArrayList<Class<?>>();
        classes.add(ReqBean.class);
        startContainer(classes);

        getWebBeansContext().getContextsService().startContext(RequestScoped.class, servletRequestEvent);

        final BeanManager bm = getBeanManager();
        final Set<Bean<?>> beans = getBeanManager().getBeans(ReqBean.class);
        final Bean pdbBean = beans.iterator().next();
        final ReqBean instance = ReqBean.class.cast(getBeanManager().getReference(pdbBean, ReqBean.class, bm.createCreationalContext(pdbBean)));
        Assert.assertNotNull(instance);

        final Object deserial = deSerializeObject(serializeObject(instance));
        Assert.assertTrue(ReqBean.class.isInstance(deserial));

        getWebBeansContext().getContextsService().endContext(RequestScoped.class, servletRequestEvent);
    }
    
    private byte[] serializeObject(Object o) throws IOException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(o);
        return baos.toByteArray();
    }
    
    private Object deSerializeObject(byte[] serial) throws IOException, ClassNotFoundException
    {
        ByteArrayInputStream bais = new ByteArrayInputStream(serial);
        ObjectInputStream ois = new ObjectInputStream(bais);
        return ois.readObject();
    }

    @RequestScoped
    public static class ReqBean {
        public String ok() {
            return "ok";
        }
    }
}
