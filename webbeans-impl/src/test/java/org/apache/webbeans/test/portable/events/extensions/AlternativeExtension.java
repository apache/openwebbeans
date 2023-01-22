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
package org.apache.webbeans.test.portable.events.extensions;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.ProcessAnnotatedType;
import jakarta.enterprise.util.AnnotationLiteral;

import org.apache.webbeans.test.portable.alternative.HalfEgg;
import org.apache.webbeans.test.portable.alternative.WoodEgg;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Set;


public class AlternativeExtension implements Extension
{
    public void observeProcessAnnotatedTypeHalfEgg(@Observes ProcessAnnotatedType<HalfEgg> pat)
    {
        // this just works with OWB and assumes a mutable annotation Set.
        pat.getAnnotatedType().getAnnotations().add(new AnnotationLiteral<Alternative>() {});
        pat.setAnnotatedType(pat.getAnnotatedType());
    }

    public void observeProcessAnnotatedTypeWoodEgg(@Observes ProcessAnnotatedType<WoodEgg> pat)
    {
        // this just works with OWB and assumes a mutable annotation Set.
        Set<Annotation> newAnnotations = new HashSet<Annotation>();
        for (Annotation ann : pat.getAnnotatedType().getAnnotations())
        {
            if (!Alternative.class.isAssignableFrom(ann.annotationType()))
            {
                 newAnnotations.add(ann);
            }
        }

        pat.getAnnotatedType().getAnnotations().clear();
        pat.getAnnotatedType().getAnnotations().addAll(newAnnotations);

        pat.setAnnotatedType(pat.getAnnotatedType());
    }
}
