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
package org.apache.webbeans.newtests.el;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.webbeans.el.WebBeansELResolver;
import org.apache.webbeans.newtests.AbstractUnitTest;
import org.junit.Test;

public class ELPerformanceTest extends AbstractUnitTest
{
    /**
     * Test our bean creation for thread safety.
     */
    @Test
    public void testBeanCreation() throws Exception
    {
        Collection<Class<?>> classes = new ArrayList<Class<?>>();

        classes.add(SampleBean.class);
        startContainer(classes);
        
        List<ParallelBeanStarter> strarters = new ArrayList<ParallelBeanStarter>();
        WebBeansELResolver resolver = new WebBeansELResolver();
        for(int i=0;i<500;i++)
        {
            ParallelBeanStarter starter = new ParallelBeanStarter(resolver);
            strarters.add(starter);
        }
        
        for(ParallelBeanStarter start : strarters)
        {
            start.start();
        }        
        
        System.out.println("Completed");
        
        shutDownContainer();
    }
    
    private static class ParallelBeanStarter extends Thread
    {
        private WebBeansELResolver resolver;
        private static AtomicInteger n = new AtomicInteger(0);
        
        public ParallelBeanStarter(WebBeansELResolver resolver)
        {
            this.resolver = resolver;
        }
        
        @Override
        public void run()
        {
            try
            {
                System.out.println(n.incrementAndGet());
                resolver.getValue(new MockELContext() , null, "sampleBean");
                
            }catch(Exception e)
            {
                e.printStackTrace();
            }
        }
    }
}
