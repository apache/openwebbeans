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
package org.apache.webbeans.annotation;

import javax.enterprise.util.AnnotationLiteral;
import java.lang.annotation.Annotation;

/**
 * Base class for AnnotationLiterals which have no members.
 * @param <T>
 */
public abstract class EmptyAnnotationLiteral<T extends Annotation> extends AnnotationLiteral<T>
{
    /**
     * Implemented for performance reasons.
     * This is needed because an Annotation always returns 0 as hashCode
     * if there is no method in it.
     * Contrary to this the generic {@link javax.enterprise.util.AnnotationLiteral#hashCode()}
     * always does search for methods via reflection and only then returns 0.
     * Not very well performing ...
     * @return always 0
     */
    @Override
    public int hashCode()
    {
        return 0;
    }

    /**
     * Just checks whether the 2 classes have the same annotationType.
     * We do not need to dynamically evaluate the member values via reflection
     * as there are no members in this annotation at all.
     */
    @Override
    public boolean equals(final Object other)
    {
        // implemented for performance reasons
        return Annotation.class.isInstance(other) &&
                Annotation.class.cast(other).annotationType().equals(annotationType());
    }
}
