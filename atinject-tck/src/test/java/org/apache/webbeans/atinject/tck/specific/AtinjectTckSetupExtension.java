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
package org.apache.webbeans.atinject.tck.specific;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Typed;
import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.enterprise.inject.spi.BeforeBeanDiscovery;
import jakarta.enterprise.inject.spi.Extension;
import org.atinject.tck.auto.DriversSeat;
import org.atinject.tck.auto.accessories.SpareTire;

/**
 * Fixes some setup weirdness which got created after removal of @New
 */
public class AtinjectTckSetupExtension implements Extension
{
    public void initTckBeans(@Observes BeforeBeanDiscovery bbd)
    {
        bbd.addAnnotatedType(DriversSeat.class, "tck")
            .add(Typed.Literal.of(new Class[]{DriversSeat.class}));

        bbd.addAnnotatedType(SpareTire.class, "tck")
            .add(new AnnotationLiteral<TckNew>() {});
    }
}
