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
package org.apache.webbeans.component.creation;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.inject.spi.AnnotatedType;

import org.apache.webbeans.config.OWBLogConst;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.logger.WebBeansLoggerFacade;

public class AnnotatedTypeBeanBuilder<T> extends ManagedBeanBuilder<T>
{
    private final static Logger logger = WebBeansLoggerFacade.getLogger(AnnotatedTypeBeanBuilder.class);

    public AnnotatedTypeBeanBuilder(AnnotatedType<T> annotatedType, WebBeansContext context)
    {
        super(context, annotatedType);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void defineConstructor()
    {
        try
        {
            addConstructorInjectionPointMetaData();
        }
        catch(Exception e)
        {
            // if no constructor could be found, we just leave the empty set.
            logger.log(Level.INFO, OWBLogConst.WARN_0012, getAnnotated().getJavaClass());
        }
    }
}
