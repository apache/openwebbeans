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
package org.apache.webbeans.portable;

import java.lang.reflect.Type;

import javax.enterprise.inject.spi.AnnotatedCallable;
import javax.enterprise.inject.spi.AnnotatedParameter;

/**
 * Implementation of {@link AnnotatedParameter} interface.
 * 
 * @version $Rev$ $Date$
 *
 * @param <X> declaring class info
 */
class AnnotatedParameterImpl<X> extends AbstractAnnotated implements AnnotatedParameter<X>
{
    /**Declaring callable*/
    private AnnotatedCallable<X> declaringCallable;
    
    /**Parameter position*/
    private int position;
    
    AnnotatedParameterImpl(Type baseType, AnnotatedCallable<X> declaringCallable, int position)
    {
        super(baseType);
        this.declaringCallable = declaringCallable;
        this.position = position;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AnnotatedCallable<X> getDeclaringCallable()
    {
        return this.declaringCallable;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getPosition()
    {
        return this.position;
    }

    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append("Annotated Parameter");
        builder.append(",");
        builder.append(super.toString()+ ",");
        builder.append("Annotated Callable : [" + this.declaringCallable.toString() + "],");
        builder.append("Position : " + position);
        
        return builder.toString();
    }
}
