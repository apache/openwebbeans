/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.apache.webbeans.event;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.event.Observer;

import org.apache.webbeans.util.Asserts;

class TransactionalNotifier
{
    private Object event;

    private Set<Observer<Object>> afterCompletion = new HashSet<Observer<Object>>();

    private Set<Observer<Object>> afterCompletionSuccess = new HashSet<Observer<Object>>();

    private Set<Observer<Object>> afterCompletionFailure = new HashSet<Observer<Object>>();

    private Set<Observer<Object>> beforeCompletion = new HashSet<Observer<Object>>();

    public TransactionalNotifier(Object event)
    {
        this.event = event;
    }

    public void addAfterCompletionObserver(Observer<Object> observer)
    {
        checkNull(observer);

        this.afterCompletion.add(observer);

    }

    public void addAfterCompletionSuccessObserver(Observer<Object> observer)
    {
        checkNull(observer);

        this.afterCompletionSuccess.add(observer);
    }

    public void addAfterCompletionFailureObserver(Observer<Object> observer)
    {
        checkNull(observer);

        this.afterCompletionFailure.add(observer);
    }

    public void addBeforeCompletionObserver(Observer<Object> observer)
    {
        checkNull(observer);

        this.beforeCompletion.add(observer);
    }

    public void notifyAfterCompletion() throws Throwable
    {
        Iterator<Observer<Object>> it = this.afterCompletion.iterator();
        handleEvent(it);
    }

    public void notifyAfterCompletionSuccess() throws Throwable
    {
        Iterator<Observer<Object>> it = this.afterCompletionSuccess.iterator();
        handleEvent(it);

    }

    public void notifyAfterCompletionFailure() throws Throwable
    {
        Iterator<Observer<Object>> it = this.afterCompletionFailure.iterator();
        handleEvent(it);

    }

    public void notifyBeforeCompletion() throws Throwable
    {
        Iterator<Observer<Object>> it = this.beforeCompletion.iterator();
        handleEvent(it);

    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((event == null) ? 0 : event.getClass().hashCode());
        return result;
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final TransactionalNotifier other = (TransactionalNotifier) obj;
        if (event == null)
        {
            if (other.event != null)
                return false;
        }
        else if (!event.getClass().equals(other.event.getClass()))
            return false;
        return true;
    }

    private void handleEvent(Iterator<Observer<Object>> it)
    {
        while (it.hasNext())
        {
            it.next().notify(this.event);
        }

    }

    private void checkNull(Observer<Object> observer)
    {
        Asserts.assertNotNull(observer, "observer parameter can not be null");
    }

}
