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

import javax.annotation.Priority;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.OptionalInt;

public class PriorityClasses
{
    /**
     * All the stereotypes which are either configured via XML &lt;class&gt; or
     * have a &#064;Priority annotation.
     * key: the class
     * value: the priority. Alternatives from beans.xml have -1 as they are lowest prio.
     */
    private final List<PriorityClass> raw = new ArrayList<>();
    private List<Class<?>> sorted;

    public OptionalInt getPriority(final Class<?> type)
    {
        return raw.stream().filter(it -> it.getClazz() == type).mapToInt(PriorityClass::getPriority).findFirst();
    }

    /**
     * Used for Classes which are annotated with &#064;Priority
     */
    public void add(Class<?> clazz, Priority priority)
    {
        raw.add(new PriorityClass(clazz, priority.value()));
        sorted = null;
    }

    /**
     * Used for Classes which are added by Beans which implement the
     * {@link javax.enterprise.inject.spi.Prioritized} interface
     */
    public void add(Class<?> clazz, int priority)
    {
        raw.add(new PriorityClass(clazz, priority));
        sorted = null;
    }

    public List<Class<?>> getSorted()
    {
        if (sorted == null)
        {
            Collections.sort(raw);

            sorted = new ArrayList<>(raw.size());

            for (PriorityClass priorityAlternative : raw)
            {
                // add in reverse order
                sorted.add(priorityAlternative.getClazz());
            }
        }

        return sorted;
    }

    public boolean contains(Class<?> beanType)
    {
        return getSorted().contains(beanType);
    }

    public void clear()
    {
        raw.clear();
        sorted = null;
    }
}
