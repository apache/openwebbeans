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
package org.apache.webbeans.newtests.contexts.session.tests;

import java.util.ArrayList;
import java.util.Collection;

import javax.enterprise.inject.spi.Bean;

import junit.framework.Assert;

import org.apache.webbeans.common.AbstractUnitTest;
import org.apache.webbeans.newtests.contexts.session.common.PersonalDataBean;
import org.junit.Test;

public class SessionContextTest extends AbstractUnitTest
{
    public SessionContextTest()
    {
        
    }
    
    @Test
    public void testPersonalDataBean()
    {
        Collection<Class<?>> classes = new ArrayList<Class<?>>();
        classes.add(PersonalDataBean.class);
        
        startContainer(classes);
        
        Bean<?> bean = getBeanManager().getBeans("org.apache.webbeans.newtests.contexts.session.common.PersonalDataBean").iterator().next();
        Object instance = getBeanManager().getReference(bean, PersonalDataBean.class, getBeanManager().createCreationalContext(bean));
        
        PersonalDataBean dataBean = (PersonalDataBean)instance;
        Assert.assertNotNull(dataBean);
        
        dataBean.business();
        
        Assert.assertTrue(PersonalDataBean.POST_CONSTRUCT);
                
        shutDownContainer();
        
        Assert.assertTrue(PersonalDataBean.PRE_DESTROY);
    }

}
