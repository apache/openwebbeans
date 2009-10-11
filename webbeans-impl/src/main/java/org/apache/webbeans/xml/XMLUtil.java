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
package org.apache.webbeans.xml;

import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.webbeans.WebBeansConstants;
import org.apache.webbeans.annotation.DefaultLiteral;
import org.apache.webbeans.annotation.WebBeansAnnotation;
import org.apache.webbeans.component.xml.XMLProducerBean;
import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.exception.WebBeansException;
import org.apache.webbeans.exception.definition.NonexistentTypeException;
import org.apache.webbeans.exception.inject.DefinitionException;
import org.apache.webbeans.inject.xml.XMLInjectionPointModel;
import org.apache.webbeans.proxy.JavassistProxyFactory;
import org.apache.webbeans.util.AnnotationUtil;
import org.apache.webbeans.util.Asserts;
import org.apache.webbeans.util.ClassUtil;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.ElementHandler;
import org.dom4j.ElementPath;
import org.dom4j.Namespace;
import org.dom4j.io.SAXReader;

/**
 * Used for getting information contained in the file web-beans.xml.
 * 
 * @author <a href="mailto:gurkanerdogdu@yahoo.com">Gurkan Erdogdu</a>
 * @since 1.0
 */
@SuppressWarnings("unchecked")
public final class XMLUtil
{

    private XMLUtil()
    {
    }

    private static final Logger log = LogManager.getLogger(XMLUtil.class);

    /**
     * Gets new {@link SAXReader} instance.
     * 
     * @return sax reader instance
     */
    public static SAXReader getSaxReader()
    {
        return new SAXReader();
    }

    /**
     * Gets the root element of the parsed document.
     * 
     * @param stream parsed document
     * @return root element of the document
     * @throws WebBeansException if any runtime exception occurs
     */
    public static Element getRootElement(InputStream stream) throws WebBeansException
    {
        try
        {  
            SAXReader saxReader = getSaxReader();
            saxReader.setMergeAdjacentText(true);
            saxReader.setStripWhitespaceText(true);
            saxReader.setErrorHandler(new WebBeansErrorHandler());
            saxReader.setEntityResolver(new WebBeansResolver());
            saxReader.setValidation(false);
            saxReader.setDefaultHandler(new ElementHandler()
            {
                public void onEnd(ElementPath path)
                {

                }

                public void onStart(ElementPath path)
                {
                    Element element = path.getCurrent();
                    if (element.getNamespaceURI() == null || element.getNamespaceURI().equals(""))
                    {
                        throw new WebBeansConfigurationException("All elements in the beans.xml file have to declare name space.");
                    }
                    else
                    {
                        if (element.isRootElement())
                        {
                            WebBeansNameSpaceContainer.getInstance().addNewPackageNameSpace(element.getNamespace().getURI());

                            List allNs = element.declaredNamespaces();
                            Iterator itNs = allNs.iterator();

                            while (itNs.hasNext())
                            {
                                Namespace namespace = (Namespace)itNs.next();
                                WebBeansNameSpaceContainer.getInstance().addNewPackageNameSpace(namespace.getURI());
                            }
                        }
                    }
                }

            });

            Document document = saxReader.read(stream);

            return document.getRootElement();

        }
        catch (DocumentException e)
        {
            log.fatal("Unable to read root element of the given input stream", e);
            throw new WebBeansException("Unable to read root element of the given input stream", e);
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
            SAXReader saxReader = getSaxReader();
            saxReader.setMergeAdjacentText(true);
            saxReader.setStripWhitespaceText(true);
            saxReader.setErrorHandler(new WebBeansErrorHandler());
            saxReader.setEntityResolver(new WebBeansResolver());
            saxReader.setValidation(false);
            Document document = saxReader.read(stream);

            return document.getRootElement();

        }
        catch (DocumentException e)
        {
            log.fatal("Unable to read root element of the given input stream", e);
            throw new WebBeansException("Unable to read root element of the given input stream", e);
        }
    }
    

    public static boolean isElementInNamespace(Element element, String namespace)
    {
        Asserts.assertNotNull(element, "element parameter can not be null");
        Asserts.assertNotNull(namespace, "namespace parameter can not be null");

        Namespace ns = element.getNamespace();
        if (!Namespace.NO_NAMESPACE.equals(ns))
        {
            if (ns.getURI().equals(namespace))
            {
                return true;
            }
        }

        return false;
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
            String txtName = element.getName();

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

        Namespace ns = element.getNamespace();
        if (!Namespace.NO_NAMESPACE.equals(ns))
        {
            return ns.getURI();
        }

        return null;
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

        List<Element> childs = element.elements();

        for (Element child : childs)
        {
            if (!isElementInWebBeansNameSpaceWithName(child, WebBeansConstants.WEB_BEANS_XML_INITIALIZER_ELEMENT) && !isElementInWebBeansNameSpaceWithName(child, WebBeansConstants.WEB_BEANS_XML_DESTRUCTOR_ELEMENT) && !isElementInWebBeansNameSpaceWithName(child, WebBeansConstants.WEB_BEANS_XML_PRODUCES_ELEMENT) && !isElementInWebBeansNameSpaceWithName(child, WebBeansConstants.WEB_BEANS_XML_DISPOSES_ELEMENT) && !isElementInWebBeansNameSpaceWithName(child, WebBeansConstants.WEB_BEANS_XML_OBSERVES_ELEMENT) && !isElementInWebBeansNameSpaceWithName(child, WebBeansConstants.WEB_BEANS_XML_DECORATES_ELEMENT))
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
        Asserts.nullCheckForDomElement(element);

        List<Element> childs = element.elements();

        for (Element child : childs)
        {
            if (isElementInWebBeansNameSpaceWithName(child, WebBeansConstants.WEB_BEANS_XML_INITIALIZER_ELEMENT) || isElementInWebBeansNameSpaceWithName(child, WebBeansConstants.WEB_BEANS_XML_DESTRUCTOR_ELEMENT) || isElementInWebBeansNameSpaceWithName(child, WebBeansConstants.WEB_BEANS_XML_PRODUCES_ELEMENT) || isElementInWebBeansNameSpaceWithName(child, WebBeansConstants.WEB_BEANS_XML_DISPOSES_ELEMENT) || isElementInWebBeansNameSpaceWithName(child, WebBeansConstants.WEB_BEANS_XML_OBSERVES_ELEMENT))
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

        return element.getName();
    }

    public static Class<?> getElementJavaType(Element element)
    {
        String ns = getElementNameSpace(element);
        List<String> packageNames = WebBeansNameSpaceContainer.getInstance().getPackageNameFromNameSpace(ns);
        
        Class<?> clazz = null; 
        Class<?> foundClazz = null;
        if(packageNames != null)
        {
            boolean found = false;

            for(String packageName : packageNames)
            {
                String className = packageName + XMLUtil.getName(element);
                clazz = ClassUtil.getClassFromName(className);
                
                if(clazz != null)
                {
                   if(found)
                   {
                       throw new DefinitionException("Multiple class with name : " + clazz.getName());
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
        
        if(clazz != null)
        {
            return clazz.getName();
        }
        
        return getName(element);
    }

    private static void nullCheckForElement(Element element)
    {
        Asserts.nullCheckForDomElement(element);
    }

    public static boolean hasChildElement(Element parent, String childName)
    {
        Asserts.assertNotNull(parent, "parent parameter can not be null");
        Asserts.assertNotNull(childName, "childName parameter can not be null");

        return parent.element(childName) != null ? true : false;
    }

    /**
     * Return child element within webbeans namespace with given child name.
     * 
     * @param parent parent element
     * @param childName child element name
     * @return if child element exist within webbeans namespace with given child
     *         name
     */
    public static boolean hasChildElementWithWebBeansNameSpace(Element parent, String childName)
    {
        Asserts.assertNotNull(parent, "parent parameter can not be null");
        Asserts.assertNotNull(childName, "childName parameter can not be null");

        Element child = parent.element(childName);
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
     * Creates new xml injection point model.
     * 
     * @param typeElement injection point API type
     * @param errorMessage error message
     * @return new injection point model object
     */
    public static XMLInjectionPointModel getInjectionPointModel(Element typeElement, String errorMessage)
    {
        Asserts.assertNotNull(typeElement, "typeElement parameter can not be null");

        /* Element <Array> */
        if (typeElement.getName().equals(WebBeansConstants.WEB_BEANS_XML_ARRAY_ELEMENT))
        {
            return getArrayInjectionPointModel(typeElement, errorMessage);
        }
        /* Java class or interface */
        else
        {
            return getTypeInjectionPointModel(typeElement, errorMessage);
        }

    }

    /**
     * Injection point with Java type.
     * 
     * @param typeElement injection point API type
     * @param errorMessage error message
     * @return new injection point model
     */
    private static XMLInjectionPointModel getTypeInjectionPointModel(Element typeElement, String errorMessage)
    {
        XMLInjectionPointModel model = null;

        Class<?> clazz = getElementJavaType(typeElement);
        if (clazz == null)
        {
            throw new NonexistentTypeException(errorMessage + "Java type with name : " + getElementJavaClassName(typeElement) + " is not found in the deployment");
        }

        else if (clazz.isAnnotation() || clazz.isArray() || clazz.isEnum())
        {
            throw new WebBeansConfigurationException(errorMessage + "Java type with name : " + getElementJavaClassName(typeElement) + " must be class or interface type");
        }

        else
        {
            TypeVariable[] typeVariables = clazz.getTypeParameters();
            int actualTypeArgument = typeVariables.length;
            List<Element> childElements = typeElement.elements();
            List<Type> typeArguments = new ArrayList<Type>();
            List<Annotation> bindingAnnots = new ArrayList<Annotation>();

            Class<? extends Annotation> definedBindingType = null;
            for (Element childElement : childElements)
            {
                Type actualType = getElementJavaType(childElement);
                if (actualType == null)
                {
                    throw new NonexistentTypeException(errorMessage + "Java type with name : " + getElementJavaClassName(typeElement) + " is not found in the deployment");
                }
                else if (((Class) actualType).isArray() || ((Class) actualType).isEnum())
                {
                    throw new WebBeansConfigurationException(errorMessage + "Java type with name : " + getElementJavaClassName(typeElement) + " must be class or interface type");
                }
                else if (((Class) actualType).isAnnotation())
                {
                    Class<? extends Annotation> annotClazz = (Class<? extends Annotation>) actualType;
                    if (!AnnotationUtil.isQualifierAnnotation(annotClazz))
                    {
                        throw new WebBeansConfigurationException(errorMessage + "Java type with name : " + getElementJavaClassName(typeElement) + " is not a @Qualifier");
                    }

                    if (definedBindingType == null)
                    {
                        definedBindingType = annotClazz;
                    }
                    else
                    {
                        if (definedBindingType.equals(annotClazz))
                        {
                            throw new IllegalArgumentException(errorMessage + "Java type with name : " + getElementJavaClassName(typeElement) + " is duplicated");
                        }
                    }

                    bindingAnnots.add(getXMLDefinedAnnotationMember(childElement, annotClazz, errorMessage));
                }
                else
                {
                    typeArguments.add(actualType);
                }
            }

            if (actualTypeArgument != typeArguments.size())
            {
                throw new WebBeansConfigurationException(errorMessage + "Java type with name : " + getElementJavaClassName(typeElement) + " actual type parameters size are not equals defined in the xml");
            }

            int i = 0;
            for (Type type : typeArguments)
            {
                TypeVariable typeVariable = typeVariables[i];
                Type[] bounds = typeVariable.getBounds();

                Class<?> clazzBound = (Class<?>) bounds[0];

                if (!clazzBound.isAssignableFrom((Class<?>) type))
                {
                    throw new WebBeansConfigurationException(errorMessage + "Java type with name : " + getElementJavaClassName(typeElement) + " actual type parameter bounded exception");
                }

            }

            Type[] typeArray = new Type[typeArguments.size()];
            typeArray = typeArguments.toArray(typeArray);
            model = new XMLInjectionPointModel(clazz, typeArray);

            if (bindingAnnots.isEmpty())
            {
                model.addBindingType(new DefaultLiteral());
            }

            for (Annotation annot : bindingAnnots)
            {
                model.addBindingType(annot);
            }
        }

        return model;
    }

    /**
     * Creates new annotation with configured members values.
     * 
     * @param annotationElement annotation element
     * @param annotClazz annotation class
     * @param errorMessage error message
     * @return new annotation with members configures
     */
    public static Annotation getXMLDefinedAnnotationMember(Element annotationElement, Class<? extends Annotation> annotClazz, String errorMessage)
    {
        String value = annotationElement.getTextTrim();
        List<Attribute> attrs = annotationElement.attributes();
        List<String> attrsNames = new ArrayList<String>();

        for (Attribute attr : attrs)
        {
            attrsNames.add(attr.getName());
        }

        /* Default value check */
        if (value != null && !value.equals(""))
        {
            if (attrsNames.contains("value"))
            {
                throw new WebBeansConfigurationException(errorMessage + "Annotation with type : " + annotClazz.getName() + " can not have both element 'value' attribute and body text");
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
                    annotClazz.getDeclaredMethod("value", new Class[] {});

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
                annotClazz.getDeclaredMethod(attrName, new Class[] {});

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
        Method[] members = annotClazz.getDeclaredMethods();
        for (Method member : members)
        {
            if (member.getDefaultValue() == null && value == null)
            {
                if (!attrsNames.contains(member.getName()))
                {
                    throw new WebBeansConfigurationException(errorMessage + "Annotation with type : " + annotClazz.getName() + " with non-default member method with name : " + member.getName() + " has to defined in the xml element attribute.");
                }
            }
        }

        return createInjectionPointAnnotation(attrs, annotClazz, value, errorMessage);
    }

    /**
     * Creates new annotation with its member values.
     * 
     * @param attrs list of annotation element attributes
     * @param annotClazz annotation class
     * @param errorMessage error message
     * @return new annotation
     */
    private static WebBeansAnnotation createInjectionPointAnnotation(List<Attribute> attrs, Class<? extends Annotation> annotClazz, String valueText, String errorMessage)
    {
        WebBeansAnnotation annotation = JavassistProxyFactory.createNewAnnotationProxy(annotClazz);
        boolean isValueAttrDefined = false;
        for (Attribute attr : attrs)
        {
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
                returnType = annotClazz.getDeclaredMethod(attrName, new Class[] {}).getReturnType();
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
                    throw new WebBeansConfigurationException(errorMessage + "Annotation with type : " + annotClazz.getName() + " with member : " + attrName + " does not have sutiable member return type");
                }

                if (value == null)
                {
                    throw new WebBeansConfigurationException(errorMessage + "Annotation with type : " + annotClazz.getName() + " with member : " + attrName + " value does not defined correctly");
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

    /**
     * Injection point with array type.
     * 
     * @param typeElement array element
     * @param errorMessage error message
     * @return new injection point model
     */
    public static XMLInjectionPointModel getArrayInjectionPointModel(Element typeElement, String errorMessage)
    {
        XMLInjectionPointModel model = null;

        List<Element> childElements = typeElement.elements();
        boolean isElementTypeDefined = false;

        Set<Annotation> anns = new HashSet<Annotation>();
        for (Element childElement : childElements)
        {
            Class<?> clazz = XMLUtil.getElementJavaType(childElement);

            if (clazz == null)
            {
                throw new NonexistentTypeException(errorMessage + "Class with name : " + XMLUtil.getElementJavaClassName(childElement) + " is not found for Array element type");
            }

            if (clazz.isAnnotation())
            {
                anns.add(getXMLDefinedAnnotationMember(childElement, (Class<? extends Annotation>) clazz, errorMessage));
            }
            else if (clazz.isArray() || clazz.isEnum())
            {
                throw new WebBeansConfigurationException(errorMessage + "<Array> element child with Java type : " + getElementJavaClassName(typeElement) + " must be class or interface type");
            }
            else
            {
                if (isElementTypeDefined)
                {
                    throw new WebBeansConfigurationException(errorMessage + "<Array> element can not have more than one child element. It has one child element that declares its type");
                }
                else
                {
                    model = new XMLInjectionPointModel(clazz);
                    isElementTypeDefined = true;
                }
            }
        }

        if (anns.size() == 0)
        {
            model.addBindingType(new DefaultLiteral());
        }

        for (Annotation ann : anns)
        {
            model.addBindingType(ann);
        }

        return model;
    }

    public static <T> void defineXMLProducerApiTypeFromArrayElement(XMLProducerBean<T> component, Element typeElement, String errorMessage)
    {
        List<Element> childElements = typeElement.elements();
        boolean isElementTypeDefined = false;

        Set<Annotation> anns = new HashSet<Annotation>();
        for (Element childElement : childElements)
        {
            Class<?> clazz = XMLUtil.getElementJavaType(childElement);

            if (clazz == null)
            {
                throw new NonexistentTypeException(errorMessage + "Class with name : " + XMLUtil.getElementJavaClassName(childElement) + " is not found for Array element type");
            }

            if (clazz.isAnnotation())
            {
                anns.add(getXMLDefinedAnnotationMember(childElement, (Class<? extends Annotation>) clazz, errorMessage));
            }
            else if (clazz.isArray() || clazz.isEnum())
            {
                throw new WebBeansConfigurationException(errorMessage + "<Array> element child with Java type : " + getElementJavaClassName(typeElement) + " must be class or interface type");
            }
            else
            {
                if (isElementTypeDefined)
                {
                    throw new WebBeansConfigurationException(errorMessage + "<Array> element can not have more than one child element. It has one child element that declares its type");
                }
                else
                {
                    isElementTypeDefined = true;
                    component.addApiType(Array.newInstance(clazz, 0).getClass());
                }
            }
        }

        if (anns.size() == 0)
        {
            component.addQualifier(new DefaultLiteral());
        }

        for (Annotation ann : anns)
        {
            component.addQualifier(ann);
        }

    }

}