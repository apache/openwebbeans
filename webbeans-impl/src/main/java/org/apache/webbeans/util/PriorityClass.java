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
package org.apache.webbeans.util;

public class PriorityClass implements Comparable<PriorityClass>
{
    private final int priority;
    private final Class<?> clazz;

    public PriorityClass(Class<?> clazz, int priority)
    {
        this.clazz = clazz;
        this.priority = priority;
    }

    public Class<?> getClazz()
    {
        return clazz;
    }

    public int getPriority()
    {
        return priority;
    }

    @Override
    public int compareTo(PriorityClass o)
    {
        if (priority != o.priority)
        {
            // sort descending
            return Integer.compare(o.priority, priority);
        }

        // we additionally sort according to the class name to at least
        // prevent randomness if 2 classes have the same ordinal.
        // see CDI-437 for more info about why it's broken in CDI-1.1.
        return clazz.getName().compareTo(o.clazz.getName());
    }
}
