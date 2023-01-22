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
package org.apache.webbeans.test.interceptors.ejb;

import org.apache.webbeans.test.interceptors.lifecycle.LifecycleBinding;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.BeforeBeanDiscovery;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.ProcessAnnotatedType;
import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.interceptor.Interceptors;


public class EjbInterceptorExtension implements Extension
{
    /**
     * we add the InterceptorBinding via Extension to test OWB-593
     * @param event
     */
    public void registerInterceptorBinding(@Observes BeforeBeanDiscovery event)
    {
        event.addInterceptorBinding(LifecycleBinding.class);
    }

    public static class InterceptorsLit extends  AnnotationLiteral<Interceptors> implements Interceptors
    {
        @Override
        public Class[] value()
        {
            return new Class[]{EjbInterceptor.class};
        }
    }

    public void observeNotInterceptedBean(@Observes ProcessAnnotatedType<ManagedBeanWithoutInterceptor> process)
    {
        AnnotationLiteral<Interceptors> intAnnot = new InterceptorsLit();

        process.getAnnotatedType().getAnnotations().add(intAnnot);
        process.setAnnotatedType(process.getAnnotatedType());
    }

}
