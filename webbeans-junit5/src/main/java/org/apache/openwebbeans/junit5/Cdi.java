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
package org.apache.openwebbeans.junit5;

import org.apache.openwebbeans.junit5.internal.CdiExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.function.Supplier;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Activates CDI based on SE API for the decorated test.
 *
 * IMPORTANT: this is not thread safe - yet - so ensure to use a single fork in surefire/gradle/....
 */
@Target(TYPE)
@Retention(RUNTIME)
@ExtendWith(CdiExtension.class)
public @interface Cdi
{
    /**
     * @return classes to deploy.
     */
    Class<?>[] classes() default {};

    /**
     * @return decorators to activate.
     */
    Class<?>[] decorators() default {};

    /**
     * @return interceptors to activate.
     */
    Class<?>[] interceptors() default {};

    /**
     * @return alternatives to activate.
     */
    Class<?>[] alternatives() default {};

    /**
     * @return stereotypes to activate.
     */
    Class<? extends Annotation>[] alternativeStereotypes() default {};

    /**
     * @return packages to deploy.
     */
    Class<?>[] packages() default {};

    /**
     * @return packages to deploy recursively.
     */
    Class<?>[] recursivePackages() default {};

    /**
     * @return SeContainer properties.
     */
    Property[] properties() default {};

    /**
     * @return if the automatic scanning must be disabled.
     */
    boolean disableDiscovery() default false;

    /**
     * @return an array of callback to call before the container starts.
     */
    Class<? extends OnStart>[] onStarts() default {};

    /**
     * TIP: it is recommended to alias the configuration when this is true to avoid to have a not unified configuration.
     * IMPORTANT: this is not thread safe so ensure to use a single fork when using it.
     *
     * @return true if the underlying container must stay up until the end of the tests.
     */
    boolean reusable() default false;

    /**
     * Will be execute before the container starts and can return a closeable called after the container stops.
     */
    interface OnStart extends Supplier<AutoCloseable>
    {
    }

    /**
     * A property set in SeContainer (as String).
     */
    @interface Property
    {
        String name();
        String value();
    }
}
