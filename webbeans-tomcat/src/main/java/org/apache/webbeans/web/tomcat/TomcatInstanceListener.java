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
package org.apache.webbeans.web.tomcat;

import java.net.URL;

import org.apache.catalina.InstanceEvent;
import org.apache.catalina.InstanceListener;
import org.apache.catalina.Wrapper;
import org.apache.catalina.core.StandardContext;

public class TomcatInstanceListener implements InstanceListener
{
    public TomcatInstanceListener()
    {
    }

    @Override
    public void instanceEvent(InstanceEvent event)
    {
        String type = event.getType();
        
        if(type.equals(InstanceEvent.BEFORE_INIT_EVENT) ||
                type.equals(InstanceEvent.BEFORE_FILTER_EVENT))
        {
            Wrapper wrapper = event.getWrapper();
            StandardContext context = (StandardContext) wrapper.getParent();

            try
            {
                URL url =  context.getServletContext().getResource("/WEB-INF/beans.xml");
                
                if(url == null)
                {
                    return;
                }
                
                Object object = event.getServlet();
                if(object == null)
                {
                    object = event.getFilter();
                }
                
                if(object != null)
                {
                    ClassLoader loader = wrapper.getLoader().getClassLoader();
                    TomcatUtil.inject(object, loader);
                }
                
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }            
        }
    }

}
