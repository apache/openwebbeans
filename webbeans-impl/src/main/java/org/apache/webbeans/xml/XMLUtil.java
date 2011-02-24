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
package org.apache.webbeans.xml;

import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.apache.webbeans.annotation.WebBeansAnnotation;
import org.apache.webbeans.config.OWBLogConst;
import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.exception.WebBeansException;
import org.apache.webbeans.logger.WebBeansLogger;
import org.apache.webbeans.proxy.JavassistProxyFactory;
import org.apache.webbeans.util.Asserts;
import org.apache.webbeans.util.ClassUtil;
import org.apache.webbeans.util.SecurityUtil;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Attr; 


@SuppressWarnings("unchecked")
public class XMLUtil
{

    private XMLUtil()
    {
    }

    private static WebBeansLogger log = WebBeansLogger.getLogger(XMLUtil.class);

    /**
     * Gets the root element of the parsed document.
     *
     * @param stream parsed document
     * @return root element of the document
     * @throws WebBeansException if any runtime exception occurs
     */
    public static Element getSpecStrictRootElement(InputStream stream) throws WebBeansException
    {
        try
        {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setCoalescing(false);
            factory.setExpandEntityReferences(true);
            factory.setIgnoringComments(true);
            factory.setIgnoringElementContentWhitespace(true);
            factory.setNamespaceAware(true);
            factory.setValidating(false);
            DocumentBuilder documentBuilder = factory.newDocumentBuilder();
            documentBuilder.setErrorHandler(new WebBeansErrorHandler());
            documentBuilder.setEntityResolver(new WebBeansResolver());

            Element root = documentBuilder.parse(stream).getDocumentElement();
            return root;
        }
        catch (Exception e)
        {
            log.fatal(e, OWBLogConst.FATAL_0002);
            throw new WebBeansException(log.getTokenString(OWBLogConst.EXCEPT_0013), e);
        }
    }

    public static String getName(Element element)
    {
        nullCheckForElement(element);

        return element.getLocalName();
    }

    private static void nullCheckForElement(Element element)
    {
        Asserts.assertNotNull(element, "element argument can not be null");
    }
    /**
     * Creates new annotation with configured members values.
     *
     * @param annotationElement annotation element
     * @param annotClazz        annotation class
     * @param errorMessage      error message
     * @return new annotation with members configures
     */
    public static Annotation getXMLDefinedAnnotationMember(Element annotationElement, Class<? extends Annotation> annotClazz, String errorMessage)
    {
        String value = annotationElement.getTextContent().trim();
        NamedNodeMap attrs = annotationElement.getAttributes();
        List<String> attrsNames = new ArrayList<String>();

        for (int i = 0; i < attrs.getLength(); i++)
        {
            Attr attr = (Attr) attrs.item(i);
            attrsNames.add(attr.getName());
        }

        /* Default value check */
        if (value != null && !value.equals(""))
        {
            if (attrsNames.contains("value"))
            {
                throw new WebBeansConfigurationException(
                        errorMessage + "Annotation with type : " + annotClazz.getName() + " can not have both element 'value' attribute and body text");
            }
        }
        /* Check for attribute "value" */
        else
        {
            if (attrsNames.contains("value"))
            {
                try
                {
                    /* Contains value member method */
                    SecurityUtil.doPrivilegedGetDeclaredMethod(annotClazz, "value", new Class[]{});

                }
                catch (SecurityException e)
                {
                    throw new WebBeansException(e);

                }
                catch (NoSuchMethodException e)
                {
                    throw new WebBeansConfigurationException(errorMessage + "Annotation with type : " + annotClazz.getName() + " must have 'value' method");
                }
            }
        }

        /* Check annotation members with name attrs */
        for (String attrName : attrsNames)
        {
            try
            {
                SecurityUtil.doPrivilegedGetDeclaredMethod(annotClazz, attrName, new Class[]{});
            }
            catch (SecurityException e)
            {
                throw new WebBeansException(e);

            }
            catch (NoSuchMethodException e)
            {
                throw new WebBeansConfigurationException(errorMessage + "Annotation with type : " + annotClazz.getName() + " does not have member with name : " + attrName);
            }
        }

        /* Non-default members must defined in the xml */
        Method[] members = ClassUtil.getDeclaredMethods(annotClazz);
        for (Method member : members)
        {
            if (member.getDefaultValue() == null && value == null)
            {
                if (!attrsNames.contains(member.getName()))
                {
                    throw new WebBeansConfigurationException(
                            errorMessage + "Annotation with type : " + annotClazz.getName() + " with non-default member method with name : " + member.getName() +
                            " has to defined in the xml element attribute.");
                }
            }
        }

        return createInjectionPointAnnotation(attrs, annotClazz, value, errorMessage);
    }

    /**
     * Creates new annotation with its member values.
     *
     * @param attrs        list of annotation element attributes
     * @param annotClazz   annotation class
     * @param errorMessage error message
     * @return new annotation
     */
    private static WebBeansAnnotation createInjectionPointAnnotation(NamedNodeMap attrs, Class<? extends Annotation> annotClazz, String valueText, String errorMessage)
    {
        WebBeansAnnotation annotation = JavassistProxyFactory.createNewAnnotationProxy(annotClazz);
        boolean isValueAttrDefined = false;
        for (int i = 0; i < attrs.getLength(); i++)
        {
            Attr attr = (Attr) attrs.item(i);
            String attrName = attr.getName();
            String attrValue = attr.getValue();

            if (!isValueAttrDefined)
            {
                if (attrName.equals("value"))
                {
                    isValueAttrDefined = true;
                }
            }

            Class returnType = null;
            try
            {
                returnType = SecurityUtil.doPrivilegedGetDeclaredMethod(annotClazz, attrName, new Class[]{}).getReturnType();
                Object value = null;
                if (returnType.isPrimitive())
                {
                    value = ClassUtil.isValueOkForPrimitiveOrWrapper(returnType, attrValue);
                }
                else if (returnType.equals(String.class))
                {
                    value = attrValue;
                }
                else if (returnType.equals(Class.class))
                {
                    value = ClassUtil.getClassFromName(attrValue);

                }
                else if (returnType.isEnum())
                {
                    value = ClassUtil.isValueOkForEnum(returnType, attrValue);
                }
                else
                {
                    throw new WebBeansConfigurationException(
                            errorMessage + "Annotation with type : " + annotClazz.getName() + " with member : " + attrName + " does not have sutiable member return type");
                }

                if (value == null)
                {
                    throw new WebBeansConfigurationException(
                            errorMessage + "Annotation with type : " + annotClazz.getName() + " with member : " + attrName + " value does not defined correctly");
                }

                annotation.setMemberValue(attrName, value);

            }
            catch (SecurityException e)
            {
                throw new WebBeansException(e);

            }
            catch (NoSuchMethodException e)
            {
                throw new WebBeansConfigurationException(errorMessage + "Annotation with type : " + annotClazz.getName() + " does not have member with name : " + attrName);
            }
        }

        if (!isValueAttrDefined)
        {
            if (valueText != null && !valueText.equals(""))
            {
                annotation.setMemberValue("value", valueText);
            }
        }

        return annotation;
    }


}

