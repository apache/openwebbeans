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

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.Set;

import jakarta.enterprise.inject.Default;

/**
 * {@link Default} literal annotation.
 * 
 * @since 1.0
 */
public class DefaultLiteral extends EmptyAnnotationLiteral<Default> implements Default
{
    public static final DefaultLiteral INSTANCE = new DefaultLiteral();
    public static final Annotation[] ARRAY = {INSTANCE};
    public static final Set<Annotation> SET = Collections.singleton(INSTANCE);

    private static final long serialVersionUID = 6788272256977634238L;
}
