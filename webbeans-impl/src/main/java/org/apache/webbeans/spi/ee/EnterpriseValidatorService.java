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
package org.apache.webbeans.spi.ee;

import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import org.apache.webbeans.spi.ValidatorService;
import org.apache.webbeans.util.JNDIUtil;

public class EnterpriseValidatorService implements ValidatorService
{
    public static final String _VALIDATOR_JNDI_ = "java:/comp/Validator";
    
    public static final String _VALIDATOR_FACTORY_JNDI_ = "java:/comp/ValidatorFactory";

    @Override
    public Validator getDefaultValidator()
    {
        Validator validator = JNDIUtil.lookup(_VALIDATOR_JNDI_, Validator.class);        
        return validator;
    }

    @Override
    public ValidatorFactory getDefaultValidatorFactory()
    {
        ValidatorFactory factory = JNDIUtil.lookup(_VALIDATOR_FACTORY_JNDI_, ValidatorFactory.class);        
        return factory;
    }

}
