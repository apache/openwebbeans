/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.webbeans.context.creational;

import java.io.IOException;
import java.io.InvalidClassException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;

import org.apache.webbeans.container.BeanManagerImpl;
import org.apache.webbeans.util.WebBeansUtil;

class DependentCreationalContext<S> implements Serializable
{
    private static final long serialVersionUID = 7107949019995422165L;

    private CreationalContext<S> creationalContext;
    
    private Contextual<S> contextual;
    
    /**
     * @return the creationalContext
     */
    public CreationalContext<S> getCreationalContext()
    {
        return creationalContext;
    }

    /**
     * @param creationalContext the creationalContext to set
     */
    public void setCreationalContext(CreationalContext<S> creationalContext)
    {
        this.creationalContext = creationalContext;
    }

    /**
     * @return the contextual
     */
    public Contextual<S> getContextual()
    {
        return contextual;
    }

    /**
     * @param contextual the contextual to set
     */
    public void setContextual(Contextual<S> contextual)
    {
        this.contextual = contextual;
    }

    public DependentCreationalContext(CreationalContext<S> cc, Contextual<S> contextual)
    {
        this.contextual = contextual;
        this.creationalContext = cc;
    }
    
    private synchronized void writeObject(ObjectOutputStream s)
    throws IOException
    {
        s.writeLong(serialVersionUID);
        
        s.writeObject(creationalContext);

        String id = null;
        if (contextual != null)
        {
            if ((id = WebBeansUtil.isPassivationCapable(contextual)) != null)
            {
                s.writeObject(id);
            }
            else
            {
                throw new NotSerializableException("cannot serialize " + contextual.toString());
            }
            
        }
        else
        {
            s.writeObject(null);
        }
    }


    @SuppressWarnings("unchecked")
    private synchronized void readObject(ObjectInputStream s)
    throws IOException, ClassNotFoundException
    {
        long svId = s.readLong();
        if (svId != serialVersionUID)
        {
            throw new InvalidClassException(this.getClass().getSimpleName());
        }
        
        creationalContext = (CreationalContext<S>) s.readObject();

        String id = (String) s.readObject();
        if (id != null)
        {
            contextual = (Contextual<S>) BeanManagerImpl.getManager().getPassivationCapableBean(id);
        }
    }

}