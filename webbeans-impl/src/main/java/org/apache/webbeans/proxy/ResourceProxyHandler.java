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
package org.apache.webbeans.proxy;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.lang.reflect.Method;

import org.apache.webbeans.component.ResourceBean;
import org.apache.webbeans.container.BeanManagerImpl;
import org.apache.webbeans.corespi.ServiceLoader;
import org.apache.webbeans.spi.FailOverService;
    
import javassist.util.proxy.MethodHandler;

public class ResourceProxyHandler implements MethodHandler, Serializable, Externalizable
{
    /**
     * 
     */
    private static final long serialVersionUID = 4171212030439910547L;
    
    /**
     * When ResourceProxyHandler deserialized, this will instruct owb to create a new actual instance, if
     * the actual resource is not serializable.
     */
    private static final String DUMMY_STRING = "owb.actual.resource.dummy";
    
    private Object actualResource = null;

    private transient ResourceBean bean = null;

    //DO NOT REMOVE, used by failover and passivation.
    public ResourceProxyHandler()
    {
    }
    
    public ResourceProxyHandler(ResourceBean bean, Object actualResource)
    {
        this.bean = bean;
        this.actualResource = actualResource;
    }
    
    @Override
    public Object invoke(Object self, Method actualMethod, Method proceed, Object[] args) throws Throwable
    {
        return actualMethod.invoke(this.actualResource, args);
    }

    /**
     * When serialized, first try container provided failover service. If the failover service 
     * does not handle the actual instance, the default behavior is:
     * 1. If actual object is serializable, then serialize it directly.
     * 2. If not, serialize the DUMMY_STRING.
     */
    @Override
    public void writeExternal(ObjectOutput out) throws IOException 
    {        
        // write bean id first
        out.writeObject(bean.getId());

        // try fail over service to serialize the resource object
        FailOverService failoverService = (FailOverService) ServiceLoader.getService(FailOverService.class);
        if (failoverService != null)
        {
            Object ret = failoverService.handleResource(bean, actualResource, null, out);
            if (ret != failoverService.NOT_HANDLED)
            {
                return;
            }
        }
        
        // default behavior
        if (actualResource instanceof Serializable)
        {
            // for remote ejb stub and other serializable resources
            out.writeObject(actualResource);
        } 
        else 
        {
            // otherwise, write a dummy string. 
            out.writeObject(DUMMY_STRING);
        }
    }

    /**
     * When deserialized, first try container provided failover service. If the failover service does not 
     * handle the actual instance, the default behavior is:
     * 1. Read the object from the stream,
     * 2. If the object is renote ejb stub, reconnect it.
     * 3. if the object is DUMMY_STRING, invoke ResourceBean.getActualInstance to get a new instance of the resource.
     */
    @Override
    public void readExternal(ObjectInput in) throws IOException,
            ClassNotFoundException 
    {
        String id = (String)in.readObject();
        bean = (ResourceBean)BeanManagerImpl.getManager().getPassivationCapableBean(id);
        
        // try fail over service to serialize the resource object
        FailOverService failoverService = (FailOverService)
        ServiceLoader.getService(FailOverService.class);
        if (failoverService != null) 
        {
            actualResource = failoverService.handleResource(bean, actualResource, in, null);
            if (actualResource != failoverService.NOT_HANDLED)
            {
                return;
            }
        }

        // default behavior
        Object obj = in.readObject();
        if (obj instanceof javax.rmi.CORBA.Stub) 
        {
            // for remote ejb stub, reconnect after deserialization.
            actualResource = obj;
            org.omg.CORBA.ORB orb = org.omg.CORBA.ORB.init(new String[0], null);
            ((javax.rmi.CORBA.Stub)actualResource).connect(orb);
        } 
        else if (obj.equals(DUMMY_STRING))
        {
            actualResource = bean.getActualInstance();
        }
    }    
}
