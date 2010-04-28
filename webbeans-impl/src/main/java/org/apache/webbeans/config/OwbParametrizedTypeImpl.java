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
package org.apache.webbeans.config;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class OwbParametrizedTypeImpl implements ParameterizedType
{
    private final Type owner;
    
    private final Type rawType;
    
    private final List<Type> types = new ArrayList<Type>();
    
    public OwbParametrizedTypeImpl(Type owner, Type raw)
    {
        this.owner = owner;
        this.rawType = raw;
    }
    
    @Override
    public Type[] getActualTypeArguments()
    {
        return this.types.toArray(new Type[0]);
    }
    
    public void addTypeArgument(Type type)
    {
        this.types.add(type);
    }

    @Override
    public Type getOwnerType()
    {
        return this.owner;
    }

    @Override
    public Type getRawType()
    {
        return this.rawType;
    }

    
    
    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(getActualTypeArguments());
        result = prime * result + ((owner == null) ? 0 : owner.hashCode());
        result = prime * result + ((rawType == null) ? 0 : rawType.hashCode());
        return result;
    }

    /* (non-Javadoc)
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
        OwbParametrizedTypeImpl other = (OwbParametrizedTypeImpl) obj;
        if (!Arrays.equals(getActualTypeArguments(), other.getActualTypeArguments()))
            return false;
        if (owner == null)
        {
            if (other.owner != null)
                return false;
        }
        else if (!owner.equals(other.owner))
            return false;
        if (rawType == null)
        {
            if (other.rawType != null)
                return false;
        }
        else if (!rawType.equals(other.rawType))
            return false;
        return true;
    }

    public String toString()
    {
        StringBuffer buffer = new StringBuffer();
        buffer.append(((Class<?>)this.rawType).getSimpleName());
        Type[] actualTypes = getActualTypeArguments();
        if(actualTypes.length > 0)
        {
            buffer.append("<");
            int length = actualTypes.length;
            for(int i=0;i<length;i++)
            {
                buffer.append(actualTypes[i].toString());
                if(i != actualTypes.length-1)
                {
                    buffer.append(",");
                }
            }
            
            buffer.append(">");
        }
        
        return buffer.toString();
    }
}
