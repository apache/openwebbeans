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
package org.apache.webbeans.annotation;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.BeforeDestroyed;
import jakarta.enterprise.context.ConversationScoped;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.context.SessionScoped;
import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Singleton;
import java.lang.annotation.Annotation;

/**
 * Destroyed literal.
 *
 * @since 1.5.0
 */
public class BeforeDestroyedLiteral extends AnnotationLiteral<BeforeDestroyed> implements BeforeDestroyed
{

    private static final long serialVersionUID = 8867272511520063730L;

    public static final BeforeDestroyedLiteral INSTANCE_APPLICATION_SCOPED = new BeforeDestroyedLiteral(ApplicationScoped.class);
    public static final BeforeDestroyedLiteral INSTANCE_SINGLETON_SCOPED = new BeforeDestroyedLiteral(Singleton.class);
    public static final BeforeDestroyedLiteral INSTANCE_SESSION_SCOPED = new BeforeDestroyedLiteral(SessionScoped.class);
    public static final BeforeDestroyedLiteral INSTANCE_CONVERSATION_SCOPED = new BeforeDestroyedLiteral(ConversationScoped.class);
    public static final BeforeDestroyedLiteral INSTANCE_REQUEST_SCOPED = new BeforeDestroyedLiteral(RequestScoped.class);

    private static final String TOSTRING = "@" + BeforeDestroyed.class.getName() + "(";


    private Class<? extends Annotation> value;

    public BeforeDestroyedLiteral(Class<? extends Annotation> value)
    {
        this.value = value;
    }

    @Override
    public Class<? extends Annotation> value()
    {
        return value;
    }

    public void setValue(Class<? extends Annotation> value)
    {
        this.value = value;
    }

    @Override
    public String toString()
    {
        return TOSTRING + value.getName() + ")";
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (! (o instanceof BeforeDestroyed))
        {
            return false;
        }

        BeforeDestroyed that = (BeforeDestroyed) o;

        return this.value != null ? this.value.equals(that.value()) : that.value() == null;
    }

    // just to make checkstyle happy
    @Override
    public int hashCode()
    {
        return super.hashCode();
    }

}
