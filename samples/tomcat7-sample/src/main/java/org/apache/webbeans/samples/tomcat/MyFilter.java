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
package org.apache.webbeans.samples.tomcat;

import java.io.IOException;
import java.util.Set;
import java.util.logging.Logger;

import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.inject.Inject;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;


public class MyFilter implements Filter
{
    private static final Logger log = Logger.getLogger(MyFilter.class.getName());

    private @Inject BeanManager manager;


    @Override
    public void destroy()
    {

    }

    @Override
    @SuppressWarnings("unchecked")
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException
    {
        Set<Bean<?>> beans = manager.getBeans(CurrentDateProvider.class);
        log.info("Total found beans : " + beans.size());
        Bean<CurrentDateProvider> provider = (Bean<CurrentDateProvider>)beans.iterator().next();
        CurrentDateProvider instance = (CurrentDateProvider) manager.getReference(provider, CurrentDateProvider.class, manager.createCreationalContext(provider));
        
        log.info("Current time is : " + instance.getCurrentDate());
        
        filterChain.doFilter(request, response);
        
    }

    @Override
    public void init(FilterConfig arg0) throws ServletException
    {
        
        
    }

}
