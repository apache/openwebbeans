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
package org.apache.webbeans.exception;

import org.apache.webbeans.exception.helper.DescriptiveException;
import org.apache.webbeans.exception.helper.ExceptionMessageBuilder;

public class WebBeansException extends RuntimeException implements DescriptiveException
{

    private static final long serialVersionUID = -3721839220664165557L;
    private Throwable cause;
    private ExceptionMessageBuilder msg = new ExceptionMessageBuilder();

    public WebBeansException(String message)
    {
        super(message);
    }

    public WebBeansException(Throwable e)
    {
        super(e);
        cause = e;
    }

    public WebBeansException(String message, Throwable e)
    {
        super(message, e);
        cause = e;
    }

    @Override
    public Throwable getCause()
    {
        return cause;
    }

    @Override
    public void addInformation(String additionalInformation)
    {
        msg.addInformation(additionalInformation);
    }

    @Override
    public String getMessage()
    {
        return msg.getAdditionalInformation(super.getMessage());
    }

    @Override
    public String getLocalizedMessage()
    {
        return msg.getAdditionalInformation(super.getLocalizedMessage());
    }

}
