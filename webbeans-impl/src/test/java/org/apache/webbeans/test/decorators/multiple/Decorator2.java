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
package org.apache.webbeans.test.decorators.multiple;

import jakarta.decorator.Decorator;
import jakarta.decorator.Delegate;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Default;
import jakarta.inject.Inject;
import jakarta.inject.Named;

@Decorator
public class Decorator2 implements IOutputProvider
{
    @Inject @Delegate @Default @Any @Named IOutputProvider op;
    @Inject @Default RequestStringBuilder rsb;

    @Override
    public String getOutput()
    {
        rsb.addOutput("Decorator2\n");
        return op.getOutput();
    }
   
    // change biz method 
    @Override
    public String trace()
    {
        return "Decorator2/trace," + op.otherMethod();
    }

    @Override
    public String otherMethod()
    {
        return "Decorator2/otherMethod," + op.otherMethod();
    }

    @Override
    public String getDelayedOutput() throws InterruptedException
    {
        return op.getDelayedOutput();
    }
}
