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

/**
 * This interface defines an exception which can later
 * get catched and an additional description can be added.
 *
 * This is useful if you e.g. like to add additional information
 * on an outer level of processing. That way we do not need to
 * prepare description information upfront. This saves memory and CPU cycles.
 */
public interface DescriptiveException
{
    /**
     * Add some additional information to the message.
     * @param additionalInformation
     */
    void addInformation(String additionalInformation);
}
