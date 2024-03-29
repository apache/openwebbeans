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

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

@ApplicationScoped
@Named("op")
public class OutputProvider implements IOutputProvider
{

    @Inject
    RequestStringBuilder rsb = null;

    @Override
    @MyIntercept
    public String getOutput()
    {
        rsb.addOutput("OutputProvider\n");
        return rsb.toString();
    }

    @Override
    public String trace() {
        return "delegate/trace";
    }

    @Override
    public String otherMethod() {
        return "delegate/otherMethod";
    }


    @Override
    @MyIntercept
    public String getDelayedOutput() throws InterruptedException
    {
        Thread.sleep(5L);
        rsb.addOutput("OutputProvider\n");
        return rsb.toString();
    }

}
