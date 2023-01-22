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
package org.apache.webbeans.test.interceptors.inheritance;

import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Named;

@RequestScoped
@Named
@StereotypeParentNotInherited
public class DeckStereotypedNotInherited implements DeckType
{
    protected ArrayList<String> intercepted_by = new ArrayList<String>();
    private int shuffled = 0, intercepted = 0;

    @Override
    public String getName()
    {
        return this.getClass().getSimpleName();
    }

    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        sb.append(getName() + ":" + shuffled + ":" + intercepted + ":" + intercepted_by.toString());
        return sb.toString();
    }

    /**
     * The method to be intercepted
     */
    @Override
    @BindingMethodInterceptor
    public void shuffle()
    {
        shuffled++;
    }

    @Override
    public void setIntercepted(String name)
    {
        intercepted++;
        intercepted_by.add(name);
    }

    @Override
    public List<String> getInterceptors()
    {
        return intercepted_by;
    }

}
