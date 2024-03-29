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
package org.apache.webbeans.ejb.plugin;

import java.lang.annotation.Annotation;

import jakarta.ejb.PrePassivate;
import jakarta.ejb.PostActivate;
import jakarta.interceptor.AroundTimeout;

import org.apache.webbeans.plugins.OpenWebBeansEjbLCAPlugin;
import org.apache.webbeans.spi.plugins.AbstractOwbPlugin;

/**
 * EJB Plugin for EJB related components.
 */
public class OpenWebBeansEjbLCAPluginImpl extends AbstractOwbPlugin implements OpenWebBeansEjbLCAPlugin  
{
    public OpenWebBeansEjbLCAPluginImpl()
    {
        super();
    }

    @Override
    public Class<? extends Annotation> getPrePassivateClass()
    {
        return PrePassivate.class;
    }

    @Override
    public Class<? extends Annotation> getPostActivateClass()
    {
        return PostActivate.class;
    }

    @Override
    public Class<? extends Annotation> getAroundTimeoutClass()
    {
        return AroundTimeout.class;
    }
        
}

