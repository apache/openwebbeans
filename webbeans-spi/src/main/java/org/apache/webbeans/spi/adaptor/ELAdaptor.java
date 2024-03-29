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
package org.apache.webbeans.spi.adaptor;

import jakarta.el.ELResolver;
import jakarta.el.ExpressionFactory;

/**
 * This SPI allows to separate ExpressionLanguage dependencies from OWB core.
 * This is mainly needed to support different EL specification versions.
 * We currently support EL-2.2 in openwebbeans-impl natively.
 * In older OWB versions we also provideed a
 * pluggable implementation for EL-1.0 in our openwebbeans-el10 module.
 */
public interface ELAdaptor
{
    ELResolver getOwbELResolver();

    ExpressionFactory getOwbWrappedExpressionFactory(ExpressionFactory expressionFactory);
}
