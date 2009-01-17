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
package org.apache.webbeans.test.mock;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import javax.persistence.Query;

public class MockEntityManager implements EntityManager
{

    public void clear()
    {
        // TODO Auto-generated method stub

    }

    public void close()
    {
        // TODO Auto-generated method stub

    }

    public boolean contains(Object arg0)
    {
        // TODO Auto-generated method stub
        return false;
    }

    public Query createNamedQuery(String arg0)
    {
        // TODO Auto-generated method stub
        return null;
    }

    public Query createNativeQuery(String arg0)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @SuppressWarnings("unchecked")
    public Query createNativeQuery(String arg0, Class arg1)
    {
        // TODO Auto-generated method stub
        return null;
    }

    public Query createNativeQuery(String arg0, String arg1)
    {
        // TODO Auto-generated method stub
        return null;
    }

    public Query createQuery(String arg0)
    {
        // TODO Auto-generated method stub
        return null;
    }

    public <T> T find(Class<T> arg0, Object arg1)
    {
        // TODO Auto-generated method stub
        return null;
    }

    public void flush()
    {
        // TODO Auto-generated method stub

    }

    public Object getDelegate()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public FlushModeType getFlushMode()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public <T> T getReference(Class<T> arg0, Object arg1)
    {
        // TODO Auto-generated method stub
        return null;
    }

    public EntityTransaction getTransaction()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public boolean isOpen()
    {
        // TODO Auto-generated method stub
        return false;
    }

    public void joinTransaction()
    {
        // TODO Auto-generated method stub

    }

    public void lock(Object arg0, LockModeType arg1)
    {
        // TODO Auto-generated method stub

    }

    public <T> T merge(T arg0)
    {
        // TODO Auto-generated method stub
        return null;
    }

    public void persist(Object arg0)
    {
        // TODO Auto-generated method stub

    }

    public void refresh(Object arg0)
    {
        // TODO Auto-generated method stub

    }

    public void remove(Object arg0)
    {
        // TODO Auto-generated method stub

    }

    public void setFlushMode(FlushModeType arg0)
    {
        // TODO Auto-generated method stub

    }

}
