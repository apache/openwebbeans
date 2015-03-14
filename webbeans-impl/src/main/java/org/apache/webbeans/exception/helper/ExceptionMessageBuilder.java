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
package org.apache.webbeans.exception.helper;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


/**
 * Helper for {@link DescriptiveException}.
 */
public class ExceptionMessageBuilder implements Serializable
{
    private static final long serialVersionUID = 4391880458753108617L;

    private List<String> additionalInformations;

    public void addInformation(String additionalInformation)
    {
        if (additionalInformations == null)
        {
            additionalInformations = new ArrayList<String>();
        }
        additionalInformations.add(additionalInformation);
    }

    public String getAdditionalInformation(String msg)
    {
        return msg != null ? msg : "" + getAdditionalInformation();
    }

    private String getAdditionalInformation()
    {
        if (additionalInformations == null)
        {
            return "";
        }

        StringBuilder sb = new StringBuilder("\n");
        for (String additionalInformation : additionalInformations)
        {
            sb.append("info: ").append(additionalInformation).append("\n");
        }
        return sb.toString();
    }

}
