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
package javax.annotation;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.webbeans.Model;

/**
 * Steretypes are used for inheriting the meta annotations
 * that are defined on the stereotyped annotation from another webbeans
 * component.
 * 
 * <p>
 * It defines two member variables, namely
 * <ul>
 * <li>supportedScopes for restricting the webbeans scope</li>
 * <li>requiredTypes for restricting the webbeans API type</li>
 * </ul>
 * </p>
 * 
 * <p>
 * If a bean annotated with multiple stereotypes, it obeys the all of the
 * stereotypes restrictions.
 * </p>
 * 
 * @see Model
 */
@Retention(RUNTIME)
@Target(ANNOTATION_TYPE)
@Documented
public @interface Stereotype
{
    /**Supported scopes of the stereotype*/
    public Class<? extends Annotation>[] supportedScopes() default {};

    /**Required API type for the webbeans*/
    public Class<?>[] requiredTypes() default {};

}
