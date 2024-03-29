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
package org.apache.webbeans.test.decorators.simple;

import java.io.Serializable;

import jakarta.decorator.Decorator;
import jakarta.decorator.Delegate;
import jakarta.inject.Inject;
import jakarta.inject.Named;

@Named("org.apache.webbeans.test.decorators.simple.LogDecorator")
@Decorator
public class LogDecorator implements ILog, Serializable {

    private static final long serialVersionUID = 1L;
    
    public static String MESSAGE = ""; 
    private @Inject @Delegate ILog ilog;
    
    @Override
    public void log(String logMessage)
    {
        MESSAGE = logMessage;
        ilog.log(logMessage);
    }

}
