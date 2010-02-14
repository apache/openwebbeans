/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.webbeans.inject.resource;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

public class ResourceModel<T>
{
    private Class<?> owner;
    
    private Class<T> resourceType;
    
    private Annotation[] resourceAnnotations;
    
    private String name;
    
    private Field member;
    
    public ResourceModel()
    {
        
    }
    
    /**
     * @return the member
     */
    public Field getMember()
    {
        return member;
    }

    /**
     * @param member the member to set
     */
    public void setMember(Field member)
    {
        this.member = member;
    }



    /**
     * @return the owner
     */
    public Class<?> getOwner()
    {
        return owner;
    }

    /**
     * @param owner the owner to set
     */
    public void setOwner(Class<?> owner)
    {
        this.owner = owner;
    }

    /**
     * @return the resourceType
     */
    public Class<T> getResourceType()
    {
        return resourceType;
    }

    /**
     * @param resourceType the resourceType to set
     */
    public void setResourceType(Class<T> resourceType)
    {
        this.resourceType = resourceType;
    }

    /**
     * @return the resourceAnnotations
     */
    public Annotation[] getResourceAnnotations()
    {
        return resourceAnnotations;
    }

    /**
     * @param resourceAnnotations the resourceAnnotations to set
     */
    public void setResourceAnnotations(Annotation[] resourceAnnotations)
    {
        this.resourceAnnotations = resourceAnnotations;
    }

    /**
     * @return the name
     */
    public String getName()
    {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name)
    {
        this.name = name;
    }

}
