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

import java.io.IOException;
import java.io.Serializable;
import java.io.Externalizable;
import java.io.ObjectOutput;
import java.io.ObjectInput;
import java.lang.reflect.Method;

import javassist.util.proxy.MethodHandler;

public class ResourceProxyHandler implements MethodHandler, Serializable, Externalizable
{
    private Object actualResource = null;
    
    //DO NOT REMOVE, used by failover and passivation.
    public ResourceProxyHandler() 
    {
    }

    public ResourceProxyHandler(Object actualResource)
    {
        this.actualResource = actualResource;
    }
    
    @Override
    public Object invoke(Object self, Method actualMethod, Method proceed, Object[] args) throws Throwable
    {
        return actualMethod.invoke(this.actualResource, args);
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException 
    {
            if (actualResource instanceof javax.rmi.CORBA.Stub)
            {
                out.writeObject(actualResource);
            }
            else
            {
                //TODO: for other type of resources, I am planning to add some
                // custom property to control whether owb should (de)serialize
                // the object directly or grab a new instance from resource
                // service (on the other jvm.).
                out.writeObject(null);
            }
    }
    
    @Override
    public void readExternal(ObjectInput in) throws IOException,
            ClassNotFoundException 
    {
        Object obj = in.readObject();
        if (obj != null && obj instanceof javax.rmi.CORBA.Stub)
        {
            actualResource = obj;
            org.omg.CORBA.ORB orb = org.omg.CORBA.ORB.init(new String[0], null);
            ((javax.rmi.CORBA.Stub)actualResource).connect(orb);
        }
    }
    
}
