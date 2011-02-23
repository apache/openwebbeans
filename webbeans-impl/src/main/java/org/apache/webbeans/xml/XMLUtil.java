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
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.exception.WebBeansException;
import org.apache.webbeans.exception.inject.DefinitionException;
import org.apache.webbeans.logger.WebBeansLogger;
import org.apache.webbeans.proxy.JavassistProxyFactory;
import org.apache.webbeans.util.AnnotationUtil;
import org.apache.webbeans.util.Asserts;
import org.apache.webbeans.util.ClassUtil;
import org.apache.webbeans.util.SecurityUtil;
import org.apache.webbeans.util.WebBeansConstants;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Attr; 
import org.w3c.dom.NodeList;


@SuppressWarnings("unchecked")
public class XMLUtil
{

    private XMLUtil()
    {
    }

    private static WebBeansLogger log = WebBeansLogger.getLogger(XMLUtil.class);

    protected static boolean verifyNameSpace(Element element)
    {
        boolean ret;
        if (element.getNamespaceURI() == null)
        {
            return false;
        }
        Node node;
        NodeList ns = element.getChildNodes();
        for (int i = 0; i < ns.getLength(); i++)
        {
            node = ns.item(i);
            if (!(node instanceof Element))
            {
                continue;
            }
            ret = verifyNameSpace((Element) node);
            if (!ret)
            {
                return false;
            }
        }
        return true;
    }

    protected static void updateNameSpacePackageMapping(Element root)
    {
        if (!verifyNameSpace(root))
        {
            throw new WebBeansConfigurationException(log.getTokenString(OWBLogConst.EXCEPT_0012));
        }
        WebBeansContext webBeansContext = WebBeansContext.getInstance();
        if (root.getNamespaceURI() != null)
        {
            webBeansContext.getWebBeansNameSpaceContainer().
                    addNewPackageNameSpace(root.getNamespaceURI());
        }
        else
        {
            String attr = root.getAttribute("xmlns");
            if (attr != null)
            {
                webBeansContext.getWebBeansNameSpaceContainer().
                        addNewPackageNameSpace(root.getNamespaceURI());
            }
        }
        NamedNodeMap attrs = root.getAttributes();
        for (int i = 0; i < attrs.getLength(); i++)
        {
            // hack the code here, since I did'nt find NameSpace support
            // in Java DOM.
            Attr attr = (Attr) attrs.item(i);
            if (attr.getName().toLowerCase().startsWith("xmlns"))
            {
                webBeansContext.getWebBeansNameSpaceContainer().addNewPackageNameSpace(attr.getValue());
            }
        }
    }

    public static Element getRootElement(InputStream stream) throws WebBeansException
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

            Document doc = documentBuilder.parse(stream);
            Element root = doc.getDocumentElement();
            updateNameSpacePackageMapping(root);
            return root;
        }
        catch (Exception e)
        {
            log.fatal(e, OWBLogConst.FATAL_0002);
            throw new WebBeansException(log.getTokenString(OWBLogConst.EXCEPT_0013), e);
        }
    }

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

    public static boolean isElementInNamespace(Element element, String namespace)
    {
        Asserts.assertNotNull(element, "element parameter can not be null");
        Asserts.assertNotNull(namespace, "namespace parameter can not be null");
        String nsURI = element.getNamespaceURI();
        return nsURI.equals(namespace);
    }

    public static boolean isElementInWebBeansNameSpace(Element element)
    {
        nullCheckForElement(element);
        String ns = getElementNameSpace(element);

        if (ns != null && ns.equals(WebBeansConstants.WEB_BEANS_NAMESPACE))
        {
            return true;
        }

        return false;
    }

    public static boolean isElementInWebBeansNameSpaceWithName(Element element, String name)
    {
        nullCheckForElement(element);

        if (isElementInWebBeansNameSpace(element))
        {
            String txtName = element.getLocalName();
            if (name.equals(txtName))
            {
                return true;
            }
        }

        return false;
    }

    public static String getElementNameSpace(Element element)
    {
        nullCheckForElement(element);

        return element.getNamespaceURI();
    }

    public static boolean isElementWebBeanDeclaration(Element element)
    {
        nullCheckForElement(element);

        if (!isElementInWebBeansNameSpaceWithName(element, WebBeansConstants.WEB_BEANS_XML_DEPLOY_ELEMENT) &&
            !isElementInWebBeansNameSpaceWithName(element, WebBeansConstants.WEB_BEANS_XML_INTERCEPTORS_ELEMENT) &&
            !isElementInWebBeansNameSpaceWithName(element, WebBeansConstants.WEB_BEANS_XML_DECORATORS_ELEMENT) &&
            !hasChildElement(element, WebBeansConstants.WEB_BEANS_XML_BINDING_TYPE) &&
            !hasChildElement(element, WebBeansConstants.WEB_BEANS_XML_INTERCEPTOR_BINDING_TYPE) &&
            !hasChildElement(element, WebBeansConstants.WEB_BEANS_XML_STEREOTYPE))
        {
            return true;
        }

        return false;

    }

    /**
     * Returns true if element has a bindingtype child element in webbeans
     * namespace false otherwise.
     *
     * @param element parent element
     * @return true if element has a bindingtype child element in webbeans
     *         namespace
     */
    public static boolean isElementBindingTypeDecleration(Element element)
    {
        nullCheckForElement(element);

        if (hasChildElementWithWebBeansNameSpace(element, WebBeansConstants.WEB_BEANS_XML_BINDING_TYPE))
        {
            return true;
        }

        return false;
    }

    /**
     * Returns true if element has a interceptor bindingtype child element in
     * webbeans namespace false otherwise.
     *
     * @param element parent element
     * @return true if element has a interceptor bindingtype child element in
     *         webbeans namespace
     */
    public static boolean isElementInterceptorBindingTypeDecleration(Element element)
    {
        nullCheckForElement(element);

        if (hasChildElementWithWebBeansNameSpace(element, WebBeansConstants.WEB_BEANS_XML_INTERCEPTOR_BINDING_TYPE))
        {
            return true;
        }

        return false;
    }

    /**
     * Returns true if element has a stereotype child element in webbeans
     * namespace false otherwise.
     *
     * @param element parent element
     * @return true if element has a stereotype child element in webbeans
     *         namespace
     */
    public static boolean isElementStereoTypeDecleration(Element element)
    {
        nullCheckForElement(element);

        if (hasChildElementWithWebBeansNameSpace(element, WebBeansConstants.WEB_BEANS_XML_STEREOTYPE))
        {
            return true;
        }

        return false;
    }

    public static boolean isElementDeployDeclaration(Element element)
    {
        nullCheckForElement(element);

        if (isElementInWebBeansNameSpaceWithName(element, WebBeansConstants.WEB_BEANS_XML_DEPLOY_ELEMENT))
        {
            return true;
        }

        return false;

    }

    public static boolean isElementInterceptorsDeclaration(Element element)
    {
        nullCheckForElement(element);

        if (isElementInWebBeansNameSpaceWithName(element, WebBeansConstants.WEB_BEANS_XML_INTERCEPTORS_ELEMENT))
        {
            return true;
        }

        return false;

    }

    public static boolean isElementDecoratosDeclaration(Element element)
    {
        nullCheckForElement(element);

        if (isElementInWebBeansNameSpaceWithName(element, WebBeansConstants.WEB_BEANS_XML_DECORATORS_ELEMENT))
        {
            return true;
        }

        return false;

    }

    /**
     * Returns true if this element defines JMS webbeans, false otherwise.
     *
     * @param element webbeans element decleration
     * @return true if this element defines JMS webbeans, false otherwise
     */
    public static boolean isElementJMSDeclaration(Element element)
    {
        nullCheckForElement(element);

        if (isElementWebBeanDeclaration(element))
        {
            if (isElementInWebBeansNameSpaceWithName(element, WebBeansConstants.WEB_BEANS_XML_QUEUE_ELEMENT)
                || isElementInWebBeansNameSpaceWithName(element, WebBeansConstants.WEB_BEANS_XML_TOPIC_ELEMENT))
            {
                return true;
            }
        }

        return false;
    }

    public static boolean isElementHasDecoratesChild(Element element)
    {
        nullCheckForElement(element);
        if (hasChildElementWithWebBeansNameSpace(element, WebBeansConstants.WEB_BEANS_XML_DECORATES_ELEMENT))
        {
            return true;
        }

        return false;
    }

    /**
     * Returns true if this element defines field, false otherwise.
     *
     * @param element webbeans decleration child element
     * @return true if this element defines field, false otherwise
     */
    public static boolean isElementField(Element element)
    {
        nullCheckForElement(element);

        Node node;
        Element child;
        NodeList ns = element.getChildNodes();
        for (int i = 0; i < ns.getLength(); i++)
        {
            node = ns.item(i);
            if (!(node instanceof Element))
            {
                continue;
            }
            child = (Element) node;
            if (!isElementInWebBeansNameSpaceWithName(child, WebBeansConstants.WEB_BEANS_XML_INITIALIZER_ELEMENT) &&
                !isElementInWebBeansNameSpaceWithName(child, WebBeansConstants.WEB_BEANS_XML_DESTRUCTOR_ELEMENT) &&
                !isElementInWebBeansNameSpaceWithName(child, WebBeansConstants.WEB_BEANS_XML_PRODUCES_ELEMENT) &&
                !isElementInWebBeansNameSpaceWithName(child, WebBeansConstants.WEB_BEANS_XML_DISPOSES_ELEMENT) &&
                !isElementInWebBeansNameSpaceWithName(child, WebBeansConstants.WEB_BEANS_XML_OBSERVES_ELEMENT) &&
                !isElementInWebBeansNameSpaceWithName(child, WebBeansConstants.WEB_BEANS_XML_DECORATES_ELEMENT))
            {

                Class<?> clazz = getElementJavaType(child);
                if (clazz != null)
                {
                    if (clazz.isAnnotation())
                    {
                        if (AnnotationUtil.isInterceptorBindingAnnotation((Class<? extends Annotation>) clazz))
                        {
                            return false;
                        }
                    }

                }

            }
            else
            {
                return false;
            }

        }

        return true;

    }

    /**
     * Checks that given element is a webbeans method or not.
     *
     * @param element dom element represents method decleration
     * @return true if the given element is a true element decleration false
     *         otherwise
     */
    public static boolean isElementMethod(Element element)
    {
        nullCheckForElement(element);

        Node node;
        Element child;
        NodeList ns = element.getChildNodes();
        for (int i = 0; i < ns.getLength(); i++)
        {
            node = ns.item(i);
            if (!(node instanceof Element))
            {
                continue;
            }
            child = (Element) node;
            if (isElementInWebBeansNameSpaceWithName(child, WebBeansConstants.WEB_BEANS_XML_INITIALIZER_ELEMENT) ||
                isElementInWebBeansNameSpaceWithName(child, WebBeansConstants.WEB_BEANS_XML_DESTRUCTOR_ELEMENT) ||
                isElementInWebBeansNameSpaceWithName(child, WebBeansConstants.WEB_BEANS_XML_PRODUCES_ELEMENT) ||
                isElementInWebBeansNameSpaceWithName(child, WebBeansConstants.WEB_BEANS_XML_DISPOSES_ELEMENT) ||
                isElementInWebBeansNameSpaceWithName(child, WebBeansConstants.WEB_BEANS_XML_OBSERVES_ELEMENT))
            {
                return true;

            }
            else
            {
                Class<?> clazz = getElementJavaType(child);
                if (clazz != null)
                {
                    if (clazz.isAnnotation())
                    {
                        if (AnnotationUtil.isInterceptorBindingAnnotation((Class<? extends Annotation>) clazz))
                        {
                            return true;
                        }
                    }

                }
            }

        }

        return false;

    }

    public static String getName(Element element)
    {
        nullCheckForElement(element);

        return element.getLocalName();
    }

    public static Class<?> getElementJavaType(Element element)
    {
        String ns = getElementNameSpace(element);
        List<String> packageNames = WebBeansContext.getInstance().getWebBeansNameSpaceContainer().getPackageNameFromNameSpace(ns);

        Class<?> clazz = null;
        Class<?> foundClazz = null;
        if (packageNames != null)
        {
            boolean found = false;

            for (String packageName : packageNames)
            {
                String className = packageName + XMLUtil.getName(element);
                clazz = ClassUtil.getClassFromName(className);

                if (clazz != null)
                {
                    if (found)
                    {
                        throw new DefinitionException(log.getTokenString(OWBLogConst.EXCEPT_0014) + clazz.getName());
                    }
                    else
                    {
                        foundClazz = clazz;
                        found = true;
                    }
                }
            }

        }

        return foundClazz;
    }

    public static String getElementJavaClassName(Element element)
    {
        Class<?> clazz = getElementJavaType(element);

        if (clazz != null)
        {
            return clazz.getName();
        }

        return getName(element);
    }

    private static void nullCheckForElement(Element element)
    {
        Asserts.assertNotNull(element, "element argument can not be null");
    }


    public static boolean hasChildElement(Element parent, String childName)
    {
        Node node;
        Asserts.assertNotNull(parent, "parent parameter can not be null");
        Asserts.assertNotNull(childName, "childName parameter can not be null");
        NodeList nl = parent.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++)
        {
            node = nl.item(i);
            if (node instanceof Element)
            {
                if (node.getNodeName().equals(childName))
                {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Return child element within webbeans namespace with given child name.
     *
     * @param parent    parent element
     * @param childName child element name
     * @return if child element exist within webbeans namespace with given child
     *         name
     */
    public static boolean hasChildElementWithWebBeansNameSpace(Element parent, String childName)
    {
        Asserts.assertNotNull(parent, "parent parameter can not be null");
        Asserts.assertNotNull(childName, "childName parameter can not be null");
        Node node;
        Element child = null;
        NodeList nl = parent.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++)
        {
            node = nl.item(i);
            if (node instanceof Element)
            {
                if (node.getNodeName().equals(childName))
                {
                    child = (Element) node;
                }
            }
        }

        if (child == null)
        {
            return false;
        }
        else
        {
            return isElementInWebBeansNameSpace(child);
        }
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

