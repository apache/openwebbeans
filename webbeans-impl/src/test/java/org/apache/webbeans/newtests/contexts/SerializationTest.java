package org.apache.webbeans.newtests.contexts;
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

import org.apache.webbeans.container.SerializableBean;
import org.apache.webbeans.newtests.AbstractUnitTest;
import org.apache.webbeans.newtests.contexts.session.common.PersonalDataBean;
import org.apache.webbeans.newtests.decorators.multiple.Decorator1;
import org.apache.webbeans.newtests.decorators.multiple.OutputProvider;
import org.apache.webbeans.newtests.decorators.multiple.RequestStringBuilder;
import org.apache.webbeans.newtests.injection.circular.beans.CircularApplicationScopedBean;
import org.apache.webbeans.newtests.injection.circular.beans.CircularConstructorOrProducerMethodParameterBean;
import org.apache.webbeans.newtests.injection.circular.beans.CircularDependenScopeBean;
import org.apache.webbeans.newtests.injection.circular.beans.CircularNormalInConstructor;
import junit.framework.Assert;
import org.junit.Test;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;


/**
 *  Tests for various serialization issues
 */
public class SerializationTest extends AbstractUnitTest
{

    @Test
    public void testCreationalContextSerialization() throws Exception
    {
        Collection<Class<?>> classes = new ArrayList<Class<?>>();

        // add a few random classes
        classes.add(PersonalDataBean.class);
        startContainer(classes);

        BeanManager bm = getLifecycle().getBeanManager();
        Set<Bean<?>> beans = getLifecycle().getBeanManager().getBeans(PersonalDataBean.class);
        Assert.assertNotNull(beans);
        Assert.assertTrue(beans.size() == 1);
        Bean pdbBean = beans.iterator().next();
        CreationalContext<PersonalDataBean> pdbCreational = bm.createCreationalContext(pdbBean);
        Assert.assertNotNull(pdbCreational);

        // oki, now let's serializeBean the CreationalContext
        byte[] serial = serializeCreationalContext(pdbCreational);
        CreationalContext<?> cc2 = deSerializeCreationalContext(serial);
        Assert.assertNotNull(cc2);
    }

    @Test
    public void testPersonalDataBean() throws ClassNotFoundException, IOException
    {
        Collection<Class<?>> classes = new ArrayList<Class<?>>();

        // add a few random classes
        classes.add(PersonalDataBean.class);
        classes.add(OutputProvider.class);
        classes.add(Decorator1.class);
        classes.add(CircularApplicationScopedBean.class);
        classes.add(RequestStringBuilder.class);
        classes.add(CircularConstructorOrProducerMethodParameterBean.class);
        classes.add(CircularDependenScopeBean.class);
        classes.add(CircularNormalInConstructor.class);

        startContainer(classes);

        Set<Bean<?>> beans = getLifecycle().getBeanManager().getBeans(Object.class);
        Assert.assertNotNull(beans);
        Assert.assertTrue(beans.size() > 7);

        for (Bean<?> bean : beans)
        {
            if (! (bean instanceof SerializableBean))
            {
                bean = new SerializableBean(bean);
            }
            
            byte[] serial = serializeBean(bean);
            Bean b2 = deSerializeBean(serial);

            Assert.assertEquals(((SerializableBean)bean).getBean(), ((SerializableBean)b2).getBean());
        }

    }

    private byte[] serializeBean(Bean<?> bean) throws IOException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(bean);
        return baos.toByteArray();
    }

    private Bean<?> deSerializeBean(byte[] serial) throws IOException, ClassNotFoundException {
        ByteArrayInputStream baos = new ByteArrayInputStream(serial);
        ObjectInputStream ois = new ObjectInputStream(baos);
        return (Bean<?>) ois.readObject();
    }

    private byte[] serializeCreationalContext(CreationalContext<?> cc) throws IOException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(cc);
        return baos.toByteArray();
    }

    private CreationalContext<?> deSerializeCreationalContext(byte[] serial) throws IOException, ClassNotFoundException {
        ByteArrayInputStream baos = new ByteArrayInputStream(serial);
        ObjectInputStream ois = new ObjectInputStream(baos);
        return (CreationalContext<?>) ois.readObject();
    }


}
