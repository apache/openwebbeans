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
package javax.enterprise.inject.spi;

import java.lang.annotation.Annotation;
import java.lang.reflect.Member;
import java.lang.reflect.Type;
import java.util.Set;

/**
 * An InjectionPoint object provides metadata information about an injection point.
 * An instance of InjectionPoint may represent one of the following types:
 * <ul>
 *  <li>an injected field</li>
 *  <li>a parameter of a bean constructor</li>
 *  <li>an initializer method</li>
 *  <li>a producer method</li>
 *  <li>a disposer method</li>
 *  <li>an observer method</li>
 * </ul>
 */
public interface InjectionPoint
{
    public Type getType();

    public Set<Annotation> getBindings();

    public Bean<?> getBean();

    public Member getMember();

    /** @deprecated old signatures have to be dropped */
    public <T extends Annotation> T getAnnotation(Class<T> annotationType);

    /** @deprecated old signatures have to be dropped */
    public Annotation[] getAnnotations();

    /** @deprecated old signatures have to be dropped */
    public boolean isAnnotationPresent(Class<? extends Annotation> annotationType);

}
