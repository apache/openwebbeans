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

import java.util.HashSet;
import java.util.Set;

import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.InjectionPoint;

import org.apache.webbeans.component.OwbBean;
import org.apache.webbeans.component.ProducerMethodBean;
import org.apache.webbeans.portable.ProducerMethodProducer;
import org.apache.webbeans.util.Asserts;

public class ProducerMethodProducerBuilder<T, P> {

    private ProducerMethodBean<T> bean;
    private AnnotatedMethod<P> producerMethod;
    private AnnotatedMethod<P> disposalMethod;
    private Set<InjectionPoint> injectionPoints;

    public ProducerMethodProducerBuilder(ProducerMethodBean<T> producerMethodBean) {
        Asserts.assertNotNull(producerMethodBean);
        this.bean = producerMethodBean;
    }

    public ProducerMethodProducer<T, P> build(AnnotatedMethod<P> method) {
        producerMethod = method;
        defineDisposalMethod();
        defineInjectionPoints();
        return new ProducerMethodProducer<T, P>((OwbBean<P>) bean.getParent(), producerMethod, disposalMethod, injectionPoints);
    }

    private void defineDisposalMethod() {
        Set<AnnotatedMethod<? super P>> annotatedMethods = producerMethod.getDeclaringType().getMethods();
        for (AnnotatedMethod<? super P> annotatedMethod : annotatedMethods)
        {            
            if (annotatedMethod.getDeclaringType().equals(producerMethod.getDeclaringType()))
            {
                for (AnnotatedParameter<? super P> annotatedParameter : annotatedMethod.getParameters())
                {
                    if (annotatedParameter.isAnnotationPresent(Disposes.class))
                    {
                        disposalMethod = (AnnotatedMethod<P>)annotatedMethod;
                        break;
                    }
                }
            }
        }
    }    

    private void defineInjectionPoints() {
        injectionPoints = new HashSet<InjectionPoint>(bean.getWebBeansContext().getInjectionPointFactory().getMethodInjectionPointData(bean, producerMethod));
        if (disposalMethod != null)
        {
            injectionPoints.addAll(bean.getWebBeansContext().getInjectionPointFactory().getMethodInjectionPointData(bean, disposalMethod));
        }
    }
}
