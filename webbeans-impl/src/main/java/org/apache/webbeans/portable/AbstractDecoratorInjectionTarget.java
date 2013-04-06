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
package org.apache.webbeans.portable;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Set;

import javax.enterprise.inject.spi.AnnotatedConstructor;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.InjectionPoint;

import org.apache.webbeans.config.WebBeansContext;

public class AbstractDecoratorInjectionTarget<T> extends InjectionTargetImpl<T>
{
    private Class<T> proxySubClass = null;

    public AbstractDecoratorInjectionTarget(AnnotatedType<T> annotatedType, Set<InjectionPoint> points, WebBeansContext webBeansContext,
                                             List<AnnotatedMethod<?>> postConstructMethods, List<AnnotatedMethod<?>> preDestroyMethods)
    {
        super(annotatedType, points, webBeansContext, postConstructMethods, preDestroyMethods);
    }

    @Override
    protected AnnotatedConstructor<T> createConstructor()
    {
        // create proxy subclass
        ClassLoader classLoader = this.getClass().getClassLoader();
        Class<T> classToProxy = annotatedType.getJavaClass();

        proxySubClass = webBeansContext.getSubclassProxyFactory().createImplementedSubclass(classLoader, classToProxy);

        //X TODO what about @Inject constructors?
        Constructor<T> ct = webBeansContext.getWebBeansUtil().getNoArgConstructor(proxySubClass);
        return new AnnotatedConstructorImpl<T>(webBeansContext, ct, annotatedType);
    }

}
