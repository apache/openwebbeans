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
package org.apache.webbeans.ejb.resource;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.text.MessageFormat;

import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.Bean;

import org.apache.webbeans.component.ResourceBean;
import org.apache.webbeans.config.OWBLogConst;
import org.apache.webbeans.corespi.ServiceLoader;
import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.exception.WebBeansException;
import org.apache.webbeans.logger.WebBeansLogger;
import org.apache.webbeans.spi.FailOverService;
import org.apache.webbeans.spi.ResourceInjectionService;
import org.apache.webbeans.spi.api.ResourceReference;
import org.apache.webbeans.util.AnnotationUtil;
import org.apache.webbeans.util.SecurityUtil;

public class OpenEjbResourceInjectionService implements ResourceInjectionService
{
    /**
     * When ResourceProxyHandler deserialized, this will instruct owb to create a new actual instance, if
     * the actual resource is not serializable.
     */
    private static final String DUMMY_STRING = "owb.actual.resource.dummy";

    private static final WebBeansLogger logger = WebBeansLogger.getLogger(OpenEjbResourceInjectionService.class);

    @Override
    public void clear()
    {
        
    }

    @Override
    public <X, T extends Annotation> X getResourceReference(ResourceReference<X, T> resourceReference)
    {
        try
        {
            return ResourceFactory.getInstance().getResourceReference(resourceReference);
            
        }
        catch(Exception e)
        {
            logger.error(OWBLogConst.ERROR_0024, e, resourceReference.getResourceType(), resourceReference.getOwnerClass(), resourceReference.getName());
            throw new WebBeansConfigurationException(MessageFormat.format(logger.getTokenString(OWBLogConst.ERROR_0024), resourceReference.getResourceType(),
                                                                          resourceReference.getOwnerClass(), resourceReference.getName()), e);
        }
    }

    @Override
    public void injectJavaEEResources(Object managedBeanInstance) throws Exception
    {
        Field[] fields = SecurityUtil.doPrivilegedGetDeclaredFields(managedBeanInstance.getClass());
        for(Field field : fields)
        {
            if(!field.isAnnotationPresent(Produces.class))
            {
                if(!Modifier.isStatic(field.getModifiers()))
                {
                    Annotation ann = AnnotationUtil.hasOwbInjectableResource(field.getDeclaredAnnotations());
                    if(ann != null)
                    {
                        @SuppressWarnings("unchecked")
                        ResourceReference<Object, ?> resourceRef = new ResourceReference(field.getDeclaringClass(), field.getName(), field.getType(), ann);
                        boolean acess = field.isAccessible();
                        try
                        {
                            SecurityUtil.doPrivilegedSetAccessible(field, true);
                            field.set(managedBeanInstance, getResourceReference(resourceRef));
                            
                        }
                        catch(Exception e)
                        {
                            logger.error(OWBLogConst.ERROR_0025, e, field);
                            throw new WebBeansException(MessageFormat.format(logger.getTokenString(OWBLogConst.ERROR_0025), field), e);
                            
                        }
                        finally
                        {
                            SecurityUtil.doPrivilegedSetAccessible(field, acess);
                        }                                            
                    }
                }                
            }
        }
    }

    /**
     * delegation of serialization behavior
     */
    public <T> void writeExternal(Bean<T> bean, T actualResource, ObjectOutput out) throws IOException
    {
        // try fail over service to serialize the resource object
        FailOverService failoverService = ServiceLoader.getService(FailOverService.class);
        if (failoverService != null)
        {
            Object ret = failoverService.handleResource(bean, actualResource, null, out);
            if (ret != FailOverService.NOT_HANDLED)
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
     * delegation of serialization behavior
     */
    public <T> T readExternal(Bean<T> bean, ObjectInput in) throws IOException,
            ClassNotFoundException
    {
        T actualResource = null;
        // try fail over service to serialize the resource object
        FailOverService failoverService = ServiceLoader.getService(FailOverService.class);
        if (failoverService != null)
        {
            actualResource = (T) failoverService.handleResource(bean, actualResource, in, null);
            if (actualResource != FailOverService.NOT_HANDLED)
            {
                return actualResource;
            }
        }

        // default behavior
        actualResource = (T) in.readObject();
        if (actualResource instanceof javax.rmi.CORBA.Stub)
        {
            // for remote ejb stub, reconnect after deserialization.
            org.omg.CORBA.ORB orb = org.omg.CORBA.ORB.init(new String[0], null);
            ((javax.rmi.CORBA.Stub)actualResource).connect(orb);
        }
        else if (actualResource.equals(DUMMY_STRING))
        {
            actualResource = (T) ((ResourceBean)bean).getActualInstance();
        }
        return actualResource;
    }
}
