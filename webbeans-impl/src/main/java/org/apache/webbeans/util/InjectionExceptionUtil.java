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
package org.apache.webbeans.util;

import org.apache.webbeans.exception.helper.ViolationMessageBuilder;
import static org.apache.webbeans.exception.helper.ViolationMessageBuilder.newViolation;

import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.enterprise.inject.AmbiguousResolutionException;
import jakarta.enterprise.inject.UnsatisfiedResolutionException;
import jakarta.enterprise.inject.UnproxyableResolutionException;
import java.util.Set;
import java.lang.annotation.Annotation;

public class InjectionExceptionUtil
{
    private InjectionExceptionUtil()
    {
        // utility class ct
    }

    public static UnproxyableResolutionException createUnproxyableResolutionException(ViolationMessageBuilder violationMessage)
    {
        return new UnproxyableResolutionException(
                newViolation("WebBeans with api type with normal scope must be proxyable.")
                        .addLine(violationMessage.toString())
                        .toString());
    }

    public static void throwUnsatisfiedResolutionException(
            Class type, InjectionPoint injectionPoint, Annotation... qualifiers)
    {
        ViolationMessageBuilder violationMessage =
                newViolation("Api type [", type.getName(), "] is not found with the qualifiers ");

        violationMessage.addLine(createQualifierMessage(injectionPoint, qualifiers));

        if (injectionPoint != null)
        {
            violationMessage.addLine("for injection into ", injectionPoint.toString());
        }

        throw new UnsatisfiedResolutionException(violationMessage.toString());
    }

    public static void throwAmbiguousResolutionExceptionForBeanName(Set<Bean<?>> beans, String beanName)
    {
        throwAmbiguousResolutionExceptionForBeans(beans,
                newViolation("There are more than one WebBeans with name : ", beanName));
    }

    public static void throwAmbiguousResolutionException(Set<Bean<?>> beans)
    {
        throwAmbiguousResolutionException(beans, null, null);
    }

    public static void throwAmbiguousResolutionException(Set<Bean<?>> beans, Class type, InjectionPoint injectionPoint, Annotation... qualifiers)
    {
        String qualifierMessage = createQualifierMessage(injectionPoint, qualifiers);

        String classString = type != null ? ClassUtil.getClass(type).getName() : null;
        if (classString == null && injectionPoint != null)
        {
            classString = ClassUtil.getClass(injectionPoint.getType()).getName();
        }

        ViolationMessageBuilder violationMessage = newViolation("There is more than one Bean ",
                classString != null ? "with type " + classString + " " : ""
                , qualifierMessage);

        if (injectionPoint != null)
        {
            violationMessage.addLine("for injection into ", injectionPoint.toString());
        }

        throwAmbiguousResolutionExceptionForBeans(beans, violationMessage);
    }

    private static void throwAmbiguousResolutionExceptionForBeans(
            Set<Bean<?>> beans, ViolationMessageBuilder violationMessage)
    {
        violationMessage.addLine("found beans: ");

        addBeanInfo(beans, violationMessage);

        throw new AmbiguousResolutionException(violationMessage.toString());
    }

    private static void addBeanInfo(Set<Bean<?>> beans, ViolationMessageBuilder violationMessage)
    {
        String sourcePath;
        for(Bean<?> currentBean : beans)
        {
            try
            {
                Class beanClass = currentBean.getBeanClass();
                sourcePath = beanClass.getResource(beanClass.getSimpleName() + ".class").toExternalForm();
            }
            catch (RuntimeException e)
            {
                sourcePath = "unknown path";
            }

            violationMessage.addLine(currentBean.toString() + " from " + sourcePath);
        }
    }

    private static String createQualifierMessage(InjectionPoint injectionPoint, Annotation... qualifiers)
    {
        if(qualifiers == null || qualifiers.length == 0)
        {
            if (injectionPoint != null)
            {
                qualifiers = injectionPoint.getQualifiers().toArray(new Annotation[injectionPoint.getQualifiers().size()]);
            }
            else
            {
                return "@Default";
            }
        }

        //reused source-code
        StringBuilder qualifierMessage = new StringBuilder("Qualifiers: [");

        int i = 0;
        for(Annotation annot : qualifiers)
        {
            i++;
            qualifierMessage.append(annot);

            if(i != qualifiers.length)
            {
                qualifierMessage.append(",");
            }
        }

        qualifierMessage.append("]");

        return qualifierMessage.toString();
    }
}
