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

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.decorator.Decorator;
import javax.enterprise.context.NormalScope;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Scope;
import javax.interceptor.Interceptor;

import org.apache.webbeans.annotation.DefaultLiteral;
import org.apache.webbeans.component.AbstractOwbBean;
import org.apache.webbeans.component.xml.XMLManagedBean;
import org.apache.webbeans.component.xml.XMLProducerBean;
import org.apache.webbeans.config.DefinitionUtil;
import org.apache.webbeans.config.ManagedBeanConfigurator;
import org.apache.webbeans.config.OpenWebBeansConfiguration;
import org.apache.webbeans.config.OWBLogConst;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.decorator.DecoratorsManager;
import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.exception.definition.NonexistentConstructorException;
import org.apache.webbeans.exception.definition.NonexistentFieldException;
import org.apache.webbeans.exception.definition.NonexistentMethodException;
import org.apache.webbeans.exception.definition.NonexistentTypeException;
import org.apache.webbeans.exception.inject.DeploymentException;
import org.apache.webbeans.inject.AlternativesManager;
import org.apache.webbeans.inject.impl.InjectionPointFactory;
import org.apache.webbeans.inject.xml.XMLInjectableConstructor;
import org.apache.webbeans.inject.xml.XMLInjectionModelType;
import org.apache.webbeans.inject.xml.XMLInjectionPointModel;
import org.apache.webbeans.intercept.InterceptorsManager;
import org.apache.webbeans.jms.JMSModel;
import org.apache.webbeans.jms.JMSModel.JMSType;
import org.apache.webbeans.logger.WebBeansLogger;
import org.apache.webbeans.plugins.OpenWebBeansJmsPlugin;
import org.apache.webbeans.proxy.JavassistProxyFactory;
import org.apache.webbeans.spi.ScannerService;
import org.apache.webbeans.spi.plugins.OpenWebBeansEjbPlugin;
import org.apache.webbeans.util.AnnotationUtil;
import org.apache.webbeans.util.Asserts;
import org.apache.webbeans.util.ClassUtil;
import org.apache.webbeans.util.WebBeansConstants;
import org.apache.webbeans.util.WebBeansUtil;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

/**
 * Configures the web beans from the xml declerations.
 */
@SuppressWarnings("unchecked")
public final class WebBeansXMLConfigurator
{
    private static final WebBeansLogger logger = WebBeansLogger.getLogger(WebBeansXMLConfigurator.class);

    /**
     * Current configuration file name
     */
    private String CURRENT_SCAN_FILE_NAME = null;

    /**
     * OWB specific or not
     */
    private boolean owbSpecificConfiguration = false;

    /**
     * Annotation type manager that manages the XML defined annotations
     */
    private XMLAnnotationTypeManager xmlAnnotTypeManager = WebBeansContext.getInstance().getxMLAnnotationTypeManager();

    /**
     * Creates a new instance of the <code>WebBeansXMLConfigurator</code>
     */
    public WebBeansXMLConfigurator()
    {
        String usage = WebBeansContext.getInstance().getOpenWebBeansConfiguration().getProperty(OpenWebBeansConfiguration.USE_OWB_SPECIFIC_XML_CONFIGURATION);
        this.owbSpecificConfiguration = Boolean.parseBoolean(usage);
    }

    /**
     * Configures XML configuration file.
     *
     * @param xmlStream xml configuration file
     */
    public void configure(InputStream xmlStream)
    {
        try
        {
            if (xmlStream.available() > 0)
            {
                //Use OWB Specific XML Configuration
                if (this.owbSpecificConfiguration)
                {
                    configureOwbSpecific(xmlStream, "No-name XML Stream",null);
                }
                else
                {
                    configureSpecSpecific(xmlStream, "No-name XML Stream");
                }

            }
        }
        catch (IOException e)
        {
            throw new WebBeansConfigurationException(e);
        }

    }
    
    /**
     * Configures XML configuration file.
     *
     * @param xmlStream xml configuration file
     * @param fileName  file name
     */
    public void configure(InputStream xmlStream, String fileName)
    {
        configure(xmlStream, fileName, null);
    }

    /**
     * Configures XML configuration file.
     *
     * @param xmlStream xml configuration file
     * @param fileName  file name
     * @param scanner null or current ScannerService ref
     */
    public void configure(InputStream xmlStream, String fileName, ScannerService scanner)
    {
        try
        {
            if (xmlStream.available() > 0)
            {
                //Use OWB Specific XML Configuration
                if (this.owbSpecificConfiguration)
                {
                    configureOwbSpecific(xmlStream, fileName,scanner);
                }
                else
                {
                    configureSpecSpecific(xmlStream, fileName,scanner);
                }

            }
        }
        catch (IOException e)
        {
            throw new WebBeansConfigurationException(e);
        }

    }

    /**
     * Configures the web beans from the given input stream.
     *
     * @param xmlStream xml file containing the web beans definitions.
     * @param fileName  name of the configuration file
     */
    public void configureOwbSpecific(InputStream xmlStream, String fileName)
    {
        configureOwbSpecific(xmlStream, fileName, null);
    }

    /**
     * Configures the web beans from the given input stream.
     *
     * @param xmlStream xml file containing the web beans definitions.
     * @param fileName  name of the configuration file
     * @param scanner null or ref to ScannerService
     */
    public void configureOwbSpecific(InputStream xmlStream, String fileName,ScannerService scanner)
    {
        try
        {
            if (xmlStream.available() > 0)
            {
                Asserts.assertNotNull(xmlStream, "xmlStream parameter can not be null!");
                Asserts.assertNotNull(fileName, "fileName parameter can not be null!");

                CURRENT_SCAN_FILE_NAME = fileName;

                //Get root element of the XML document
                Element webBeansRoot = XMLUtil.getRootElement(xmlStream);

                //Start configuration
                configureOwbSpecific(webBeansRoot,fileName,scanner);
            }
        }
        catch (IOException e)
        {
            throw new WebBeansConfigurationException(e);
        }
    }

    /**
     * Configures the web beans from the given input stream.
     *
     * @param xmlStream xml file containing the web beans definitions.
     * @param fileName  name of the configuration file
     */
    public void configureSpecSpecific(InputStream xmlStream, String fileName)
    {
        configureSpecSpecific(xmlStream, fileName, null);
    }
    
    
    /**
     * Configures the web beans from the given input stream.
     *
     * @param xmlStream xml file containing the web beans definitions.
     * @param fileName  name of the configuration file
     * @param scanner null or scanner ref
     */
    public void configureSpecSpecific(InputStream xmlStream, String fileName,ScannerService scanner)
    {
        try
        {
            if (xmlStream.available() > 0)
            {
                Asserts.assertNotNull(xmlStream, "xmlStream parameter can not be null!");
                Asserts.assertNotNull(fileName, "fileName parameter can not be null!");

                CURRENT_SCAN_FILE_NAME = fileName;

                //Get root element of the XML document
                Element webBeansRoot = XMLUtil.getSpecStrictRootElement(xmlStream);

                //Start configuration
                configureSpecSpecific(webBeansRoot,fileName,scanner);
            }
        }
        catch (IOException e)
        {
            throw new WebBeansConfigurationException(e);
        }
    }


    /**
     * Configures the xml file root element.
     *
     * @param webBeansRoot root element of the configuration xml file
     */
    private void configureOwbSpecific(Element webBeansRoot, String fileName,ScannerService scanner)
    {
        List<Element> webBeanDeclerationList = new ArrayList<Element>();

        Node node;
        Element child;
        NodeList ns = webBeansRoot.getChildNodes();
        for (int i = 0; i < ns.getLength(); i++)
        {
            node = ns.item(i);
            if (!(node instanceof Element))
            {
                continue;
            }
            child = (Element) node;

            /* WebBean element decleration */
            if (XMLUtil.isElementWebBeanDeclaration(child))
            {
                webBeanDeclerationList.add(child);

            }
            /* <Interceptors> element decleration */
            else if (XMLUtil.isElementInterceptorsDeclaration(child))
            {
                configureInterceptorsElement(child,fileName,scanner);
            }
            /* <Decorators> element decleration */
            else if (XMLUtil.isElementDecoratosDeclaration(child))
            {
                configureDecoratorsElement(child,fileName,scanner);
            }
            /* <BindingType> annotation element decleration */
            else if (XMLUtil.isElementBindingTypeDecleration(child))
            {
                addNewBindingType(child);

            }

            //X TODO <Resource> annotation element declaration */

            /* <InterceptorBindingType> annotation element decleration */
            else if (XMLUtil.isElementInterceptorBindingTypeDecleration(child))
            {
                addNewInterceptorBindingType(child);

            }

            /* <Stereotype> annotation element decleration */
            else if (XMLUtil.isElementStereoTypeDecleration(child))
            {
                addNewStereoTypeType(child);
            }
            else if (XMLUtil.getName(child).equals(WebBeansConstants.WEB_BEANS_XML_OWB_SPECIFIC_ALTERNATIVES))
            {
                configureAlternativesElement(child,fileName,scanner);
            }

        }

        // Configures the WebBeans components
        configureWebBeansComponents(webBeanDeclerationList);

    }

    /**
     * Configures the xml file root element.
     *
     * @param webBeansRoot root element of the configuration xml file
     */
    private void configureSpecSpecific(Element webBeansRoot, String fileName,ScannerService scanner)
    {
        Node node;
        Element child;
        NodeList ns = webBeansRoot.getChildNodes();
        for (int i = 0; i < ns.getLength(); i++)
        {
            node = ns.item(i);
            if (!(node instanceof Element))
            {
                continue;
            }
            child = (Element) node;

            /* <Interceptors> element decleration */
            if (XMLUtil.getName(child).equals(WebBeansConstants.WEB_BEANS_XML_SPEC_SPECIFIC_INTERCEPTORS_ELEMENT))
            {
                configureInterceptorsElement(child,fileName,scanner);
            }
            /* <Decorators> element decleration */
            else if (XMLUtil.getName(child).equals(WebBeansConstants.WEB_BEANS_XML_SPEC_SPECIFIC_DECORATORS_ELEMENT))
            {
                configureDecoratorsElement(child,fileName,scanner);
            }
            else if (XMLUtil.getName(child).equals(WebBeansConstants.WEB_BEANS_XML_SPEC_SPECIFIC_ALTERNATIVES))
            {
                configureAlternativesElement(child,fileName,scanner);
            }
        }

    }

    /**
     * Configures the webbeans defined in the xml file.
     *
     * @param listOfWebBeanDecleration list of element that specifies new
     *                                 webbean decleration
     */
    private void configureWebBeansComponents(List<Element> listOfWebBeanDecleration)
    {
        if (!listOfWebBeanDecleration.isEmpty())
        {
            Iterator<Element> it = listOfWebBeanDecleration.iterator();
            while (it.hasNext())
            {
                Element child = it.next();
                /* JMS webbeans */
                if (XMLUtil.isElementJMSDeclaration(child))
                {
                    configureJMSEndpointComponent(child);
                }
                /* Simple or Enterprise webbean */
                else
                {
                    configureNewWebBeanComponent(child);
                }
            }
        }
    }

    /**
     * Configure and add new binding type annotation.
     *
     * @param bindingTypeElement new binding type element
     */
    private void addNewBindingType(Element bindingTypeElement)
    {
        Class<?> clazz = XMLUtil.getElementJavaType(bindingTypeElement);
        if (clazz == null)
        {
            throw new NonexistentTypeException(createConfigurationFailedMessage() + "Binding type with given class : " + bindingTypeElement.getLocalName()
                                               + " not found");
        }

        Class<? extends Annotation> clazzAnnot = null;
        if (!clazz.isAnnotation())
        {
            throw new WebBeansConfigurationException(
                    createConfigurationFailedMessage() + "Binding type with given class : " + bindingTypeElement.getLocalName() + " is not an annotation type");
        }
        else
        {
            clazzAnnot = (Class<? extends Annotation>) clazz;
        }

        if (xmlAnnotTypeManager.hasBindingType(clazzAnnot))
        {
            throw new DeploymentException(
                    createConfigurationFailedMessage() + "Binding type with given class : " + bindingTypeElement.getLocalName()
                    + " is already defined in the XML");
        }

        xmlAnnotTypeManager.addBindingType(clazzAnnot);
    }

    /**
     * Configures and adds new interceptor binding type annotation.
     *
     * @param interceptorBindingTypeElement new interceptor binding type element
     */
    private void addNewInterceptorBindingType(Element interceptorBindingTypeElement)
    {
        Class<?> clazz = XMLUtil.getElementJavaType(interceptorBindingTypeElement);
        if (clazz == null)
        {
            throw new NonexistentTypeException(
                    createConfigurationFailedMessage() + "InterceptorBinding type with given class : " + interceptorBindingTypeElement.getLocalName() + " not found");
        }

        Class<? extends Annotation> clazzAnnot = null;
        if (!clazz.isAnnotation())
        {
            throw new WebBeansConfigurationException(
                    createConfigurationFailedMessage() + "InterceptorBinding type with given class : " + interceptorBindingTypeElement.getLocalName() +
                    " is not an annotation type");
        }
        else
        {
            clazzAnnot = (Class<? extends Annotation>) clazz;
        }

        if (xmlAnnotTypeManager.hasInterceptorBindingType(clazzAnnot))
        {
            throw new DeploymentException(createConfigurationFailedMessage() + "InterceptorBinding type with given class : " + interceptorBindingTypeElement.getLocalName() +
                                          " is already defined in the XML");
        }

        Node node;
        Element child;
        NodeList ns = interceptorBindingTypeElement.getChildNodes();
        for (int i = 0; i < ns.getLength(); i++)
        {
            node = ns.item(i);
            if (!(node instanceof Element))
            {
                continue;
            }
            child = (Element) node;
            Class<?> clz = XMLUtil.getElementJavaType(child);
            if (clz == null)
            {
                throw new NonexistentTypeException(
                        createConfigurationFailedMessage() + "InterceptorBinding type with given class : " + XMLUtil.getElementJavaClassName(child) + " not found " +
                        "in namespace : " + XMLUtil.getElementNameSpace(child));
            }

            if (!clz.isAnnotation() || !AnnotationUtil.isInterceptorBindingAnnotation((Class<? extends Annotation>) clz))
            {
                throw new WebBeansConfigurationException(
                        createConfigurationFailedMessage() + "InterceptorBinding type with given class : " + XMLUtil.getElementJavaClassName(child) +
                        " is not interceptor binding annotation type");
            }

            Annotation inherited = XMLUtil.getXMLDefinedAnnotationMember(child, (Class<? extends Annotation>) clz, createConfigurationFailedMessage());
            xmlAnnotTypeManager.addInterceotorBindingTypeInheritAnnotation(clazzAnnot, inherited);
        }

    }

    /**
     * Configures and adds new stereotype annotation.
     *
     * @param stereoTypeElement new stereotype annotation element
     */
    private void addNewStereoTypeType(Element stereoTypeElement)
    {
        Class<?> clazz = XMLUtil.getElementJavaType(stereoTypeElement);
        if (clazz == null)
        {
            throw new NonexistentTypeException(createConfigurationFailedMessage() + "Stereotype with given class : " + stereoTypeElement.getLocalName() + " not found");
        }

        Class<? extends Annotation> clazzAnnot = null;
        if (!clazz.isAnnotation())
        {
            throw new WebBeansConfigurationException(
                    createConfigurationFailedMessage() + "Stereotype with given class : " + stereoTypeElement.getLocalName() + " is not an annotation type");
        }
        else
        {
            clazzAnnot = (Class<? extends Annotation>) clazz;
        }

        if (xmlAnnotTypeManager.hasStereoType(clazzAnnot))
        {
            throw new DeploymentException(
                    createConfigurationFailedMessage() + "Stereotype with given class : " + stereoTypeElement.getLocalName() + " is already defined in the XML");
        }

        xmlAnnotTypeManager.addStereoType(clazzAnnot, stereoTypeElement, clazzAnnot.getName(), createConfigurationFailedMessage());

    }

    /**
     * Configures enablements of the interceptors.
     *
     * @param interceptorsElement interceptors element
     */
    private void configureInterceptorsElement(Element interceptorsElement, String fileName,ScannerService scanner)
    {
        InterceptorsManager manager = WebBeansContext.getInstance().getInterceptorsManager();
        Node node;
        Element child;
        NodeList ns = interceptorsElement.getChildNodes();
        for (int i = 0; i < ns.getLength(); i++)
        {
            node = ns.item(i);
            if (!(node instanceof Element))
            {
                continue;
            }
            child = (Element) node;
            Class<?> clazz = null;

            if (this.owbSpecificConfiguration)
            {
                clazz = XMLUtil.getElementJavaType(child);
            }
            else
            {
                clazz = ClassUtil.getClassFromName(child.getTextContent().trim());
            }

            if (clazz == null)
            {
                throw new WebBeansConfigurationException(createConfigurationFailedMessage() + "Interceptor class : " +
                                                         (this.owbSpecificConfiguration ? XMLUtil.getName(child) : child.getTextContent().trim()) + " not found");
            }
            else
            {
                if (AnnotationUtil.hasAnnotation(clazz.getDeclaredAnnotations(), Interceptor.class) &&
                    !AnnotationUtil.hasInterceptorBindingMetaAnnotation(clazz.getDeclaredAnnotations()))
                {
                    throw new WebBeansConfigurationException(createConfigurationFailedMessage() + "Interceptor class : " +
                                                             (this.owbSpecificConfiguration ? XMLUtil.getName(child) : child.getTextContent().trim()) +
                                                             " must have at least one @InterceptorBindingType");
                }
                boolean isBDAScanningEnabled=(scanner!=null && scanner.isBDABeansXmlScanningEnabled());
                if ((!isBDAScanningEnabled && manager.isInterceptorEnabled(clazz)) ||
                        (isBDAScanningEnabled && !scanner.getBDABeansXmlScanner().addInterceptor(clazz, fileName)))
                {
                    throw new WebBeansConfigurationException(createConfigurationFailedMessage() + "Interceptor class : " +
                                                             (this.owbSpecificConfiguration ? XMLUtil.getName(child) : child.getTextContent().trim()) + " is already defined");
                }

                manager.addNewInterceptor(clazz);
            }

        }

    }

    /**
     * Configures enablements of the decorators.
     *
     * @param decoratorsElement decorators element
     */
    private void configureDecoratorsElement(Element decoratorsElement,String fileName,ScannerService scanner)
    {
        DecoratorsManager manager = WebBeansContext.getInstance().getDecoratorsManager();
        Node node;
        Element child;
        NodeList ns = decoratorsElement.getChildNodes();
        for (int i = 0; i < ns.getLength(); i++)
        {
            node = ns.item(i);
            if (!(node instanceof Element))
            {
                continue;
            }
            child = (Element) node;
            Class<?> clazz = null;

            if (this.owbSpecificConfiguration)
            {
                clazz = XMLUtil.getElementJavaType(child);
            }
            else
            {
                clazz = ClassUtil.getClassFromName(child.getTextContent().trim());
            }

            if (clazz == null)
            {
                throw new WebBeansConfigurationException(createConfigurationFailedMessage() + "Decorator class : " +
                                                         (this.owbSpecificConfiguration ? XMLUtil.getName(child) : child.getTextContent().trim()) + " not found");
            }
            else
            {
                boolean isBDAScanningEnabled=(scanner!=null && scanner.isBDABeansXmlScanningEnabled());
                if ((isBDAScanningEnabled && !scanner.getBDABeansXmlScanner().addDecorator(clazz, fileName))||
                        (!isBDAScanningEnabled && manager.isDecoratorEnabled(clazz)))
                {
                    throw new WebBeansConfigurationException(createConfigurationFailedMessage() + "Decorator class : " +
                                                             (this.owbSpecificConfiguration ? XMLUtil.getName(child) : child.getTextContent().trim()) + " is already defined");
                }

                manager.addNewDecorator(clazz);
            }

        }

    }

    /**
     * Configures enablements of the decorators.
     *
     * @param alternativesElement decorators element
     */
    private void configureAlternativesElement(Element alternativesElement,String fileName,ScannerService scanner)
    {
        Node node;
        Element child;
        NodeList ns = alternativesElement.getChildNodes();
        for (int i = 0; i < ns.getLength(); i++)
        {
            node = ns.item(i);
            if (!(node instanceof Element))
            {
                continue;
            }
            child = (Element) node;

            if (XMLUtil.getName(child).equals(WebBeansConstants.WEB_BEANS_XML_SPEC_SPECIFIC_STEREOTYPE) ||
                XMLUtil.getName(child).equals(WebBeansConstants.WEB_BEANS_XML_OWB_SPECIFIC_STEREOTYPE))
            {
                addAlternative(child, true,fileName,scanner);
            }
            else if (XMLUtil.getName(child).equals(WebBeansConstants.WEB_BEANS_XML_SPEC_SPECIFIC_CLASS)
                     || XMLUtil.getName(child).equals(WebBeansConstants.WEB_BEANS_XML_OWB_SPECIFIC_CLASS))
            {
                addAlternative(child, false,fileName,scanner);
            }
            else
            {
                if (logger.wblWillLogWarn())
                {
                    logger.warn(OWBLogConst.WARN_0002, XMLUtil.getName(child));
                }
            }
        }
    }

    private void addAlternative(Element child, boolean isStereoType,String fileName,ScannerService scanner)
    {
        Class<?> clazz = null;

        if (this.owbSpecificConfiguration)
        {
            clazz = XMLUtil.getElementJavaType(child);
        }
        else
        {
            clazz = ClassUtil.getClassFromName(child.getTextContent().trim());
        }

        if (clazz == null)
        {
            throw new WebBeansConfigurationException(createConfigurationFailedMessage() + "Alternative class : " + XMLUtil.getName(child) + " not found");
        }
        else
        {
            AlternativesManager manager = WebBeansContext.getInstance().getAlternativesManager();
            if (isStereoType)
            {
                manager.addStereoTypeAlternative(clazz,fileName,scanner);
            }
            else
            {
                manager.addClazzAlternative(clazz,fileName,scanner);
            }
        }
    }

    /**
     * Configures new webbeans component from the given webbeans element.
     *
     * @param webBeanElement web beans element
     */
    private void configureNewWebBeanComponent(Element webBeanElement)
    {
        Class<?> clazz = XMLUtil.getElementJavaType(webBeanElement);

        if (clazz == null)
        {
            throw new NonexistentTypeException(createConfigurationFailedMessage() + "Class with name : " + XMLUtil.getName(webBeanElement) + " is not found in namespace " +
                                               XMLUtil.getElementNameSpace(webBeanElement));
        }

        boolean ok = false;

        /* Enterprise WebBean */
        OpenWebBeansEjbPlugin plugin = WebBeansContext.getInstance().getPluginLoader().getEjbPlugin();
        if (plugin != null && plugin.isSessionBean(clazz))
        {
            // Configure for EJB
            configureEJBWebBean(clazz);
            ok = true;
        }
        else
        {
            /* Simple WebBeans */
            if (ManagedBeanConfigurator.isManagedBean(clazz))
            {
                // Configure Simple WebBean
                configureSimpleWebBean(clazz, webBeanElement);
                ok = true;
            }
        }

        /* If not applicable for configuration */
        if (!ok)
        {
            throw new WebBeansConfigurationException(createConfigurationFailedMessage() + "Given class with name : " + clazz.getName() +
                                                     " is not resolved to any WebBeans type in {Simple WebBeans, Enterprise WebBeans, JMS WebBeans}");
        }

    }

    /**
     * Configures the simple webbean from the class.
     *
     * @param simpleClass        concrete java class defined in XML
     * @param webBeanDecleration webbeans decleration root element
     */
    public <T> XMLManagedBean<T> configureSimpleWebBean(Class<T> simpleClass, Element webBeanDecleration)
    {
        /* Checking XML defined simple webbeans condition check. Spec : 3.2.4 */
        XMLDefinitionUtil.checkSimpleWebBeansInXML(simpleClass, webBeanDecleration, createConfigurationFailedMessage());

        /* If interceptor, check this is enabled */
        if (XMLUtil.hasChildElementWithWebBeansNameSpace(webBeanDecleration, WebBeansConstants.WEB_BEANS_XML_INTERCEPTOR_ELEMENT))
        {
            if (!WebBeansContext.getInstance().getInterceptorsManager().isInterceptorEnabled(simpleClass))
            {
                return null;
            }
        }

        /* If decorator, check this is enabled */
        if (XMLUtil.hasChildElementWithWebBeansNameSpace(webBeanDecleration, WebBeansConstants.WEB_BEANS_XML_DECORATOR_ELEMENT))
        {
            if (!WebBeansContext.getInstance().getDecoratorsManager().isDecoratorEnabled(simpleClass))
            {
                return null;
            }
        }

        /* Create new XML component with class name */
        XMLManagedBean<T> component = new XMLManagedBean<T>(simpleClass);

        /* Configures API type of the webbeans component */
        DefinitionUtil.defineApiTypes(component, simpleClass);

        /* Configures child elements of this webbeans decleration element */
        configureWebBeanDeclerationChilds(component, webBeanDecleration);

        WebBeansContext.getInstance().getBeanManagerImpl().addBean(component);

        return component;
    }

    /**
     * Configures the childs element of the given webbeans decleration element.
     *
     * @param component          xml webbeans element
     * @param webBeanDecleration webbeans element
     */
    private <T> void configureWebBeanDeclerationChilds(XMLManagedBean<T> component, Element webBeanDecleration)
    {
        /* Constructor api type list */
        List<Class<?>> constTypeList = new ArrayList<Class<?>>();

        /* Constructor parameters element */
        List<Element> constructorParameterElementList = new ArrayList<Element>();

        /* Annotation set defined for webbeans */
        List<Class<? extends Annotation>> annotationSet = new ArrayList<Class<? extends Annotation>>();

        /* Annotation defined element list */
        List<Element> annotationElementList = new ArrayList<Element>();

        boolean isConstructor = false;
        String fieldOrMethodName = null;

        Node node;
        Element child;
        NodeList ns = webBeanDecleration.getChildNodes();

        for (int i = 0; i < ns.getLength(); i++)
        {
            node = ns.item(i);
            if (!(node instanceof Element))
            {
                continue;
            }
            child = (Element) node;

            Class<?> type = XMLUtil.getElementJavaType(child);

            boolean isElementApplicable = false;

            // Java type then 2 possible outcome, Annotation type meta-data or
            // constructor
            if (type != null)
            {
                if (type.isAnnotation())
                {
                    // Annotation types defined on the webbeans
                    Class<? extends Annotation> annot = (Class<Annotation>) type;
                    annotationSet.add(annot);
                    annotationElementList.add(child);

                    isElementApplicable = true;

                }
                else
                {
                    if (!isConstructor)
                    {
                        isConstructor = true;
                    }

                    /* Constructor parameter Element */
                    constructorParameterElementList.add(child);

                    // Adding constructor parameter class types
                    constTypeList.add(type);

                    isElementApplicable = true;

                }
            }
            // Field or method decleration
            else
            {
                // Namespace check
                if (XMLUtil.getElementNameSpace(child).equals(XMLUtil.getElementNameSpace(webBeanDecleration)))
                {
                    String name = XMLUtil.getName(child);

                    // Duplicate definition for field/method
                    if (fieldOrMethodName != null)
                    {
                        if (fieldOrMethodName.equals(name))
                        {
                            throw new WebBeansConfigurationException(
                                    createConfigurationFailedMessage() + "This field/method with name : " + fieldOrMethodName + " is already defined.");
                        }

                        else
                        {
                            configureFieldOrMethodMetaData(component, child);
                            fieldOrMethodName = name;

                            isElementApplicable = true;
                        }
                    }
                    else
                    {
                        configureFieldOrMethodMetaData(component, child);
                        fieldOrMethodName = name;

                        isElementApplicable = true;
                    }
                }
                else
                {
                    throw new WebBeansConfigurationException(
                            createConfigurationFailedMessage() + "Parent and child namespace has to be the same for field/method element decleration");
                }
            }

            /* This element netiher constructor, annotation , field or method */
            if (!isElementApplicable)
            {
                throw new WebBeansConfigurationException(
                        createConfigurationFailedMessage() + "Element with name : " + XMLUtil.getName(child) + " is not applicable for child of the simple webbeans class :  " +
                        component.getReturnType().getName());
            }

        }// end of while

        // Check Annotation Types
        XMLDefinitionUtil.checkTypeMetaDataClasses(annotationSet, createConfigurationFailedMessage());

        // Configure type-level metadata
        configureTypeLevelMetaData(component, annotationSet, annotationElementList, webBeanDecleration);

        // Constructor decleration
        if (isConstructor)
        {
            // Configure constructor parameters
            configureConstructorMetaData(component, constTypeList, constructorParameterElementList);
        }
        else
        {
            // Default constructor
            component.setConstructor(WebBeansUtil.defineConstructor(component.getReturnType()));
        }

    }

    /**
     * Configures the type level meta data of the webbeans component.
     *
     * @param component          xml webbeans component
     * @param annotationSet      type level annotation set
     * @param webBeanDecleration webbeans decleration element
     */
    public <T> void configureTypeLevelMetaData(XMLManagedBean<T> component, List<Class<? extends Annotation>> annotationSet,
                                               List<Element> annotationElementList, Element webBeanDecleration)
    {
        boolean isInterceptor = false;
        boolean isDecorator = false;

        /* Check this is an Interceptor */
        if (annotationSet.contains(Interceptor.class))
        {
            isInterceptor = true;
        }

        /* Check this is a Decorator */
        if (annotationSet.contains(Decorator.class))
        {
            isDecorator = true;
        }

        // StereoType
        configureStereoType(component, annotationSet, annotationElementList);

        // Scope Type
        configureScopeType(component, annotationSet, annotationElementList);

        // Binding Type
        configureBindingType(component, annotationSet, annotationElementList);

        if (!isInterceptor && !isDecorator)
        {
            // InterceptorBinding Type
            configureInterceptorBindingType(component, annotationSet, annotationElementList);
        }

        // Name configuration
        configureNamed(component, annotationSet, webBeanDecleration);

        // Specializations
        configureSpecializes(component, annotationSet);

        /* Interceptor Definition */
        if (isInterceptor)
        {
            configureInterceptor(component, annotationSet, annotationElementList);
        }

        /* Decorator Definition */
        if (isDecorator)
        {
            configureDecorator(component, annotationSet, annotationElementList, webBeanDecleration);
        }
    }

    /**
     * Configures the type level meta data of the webbeans component.
     *
     * @param component          xml webbeans component
     * @param annotationSet      type level annotation set
     * @param webBeanDecleration webbeans decleration element
     */
    public <T> void configureProducerTypeLevelMetaData(AbstractOwbBean<T> component, List<Class<? extends Annotation>> annotationSet,
                                                       List<Element> annotationElementList, Element webBeanDecleration)
    {
        configureBindingType(component, annotationSet, annotationElementList);

        // StereoType
        configureStereoType(component, annotationSet, annotationElementList);

        // Scope Type
        configureScopeType(component, annotationSet, annotationElementList);

        // Name configuration
        configureNamed(component, annotationSet, webBeanDecleration);
    }

    /**
     * Configures the component constructor. When resolution dependency of the
     * constructor injection points, constructor parameter type defined in the
     * xml is used.
     *
     * @param component xml webbeans component
     * @param typeList  list of the constructor parameter types
     * @param constructorParameterListElement
     *                  parameter list element
     * @throws org.apache.webbeans.exception.inject.DefinitionException if more than one constructor exists
     * @throws NonexistentConstructorException
     *                             if no constructor exists
     */
    private <T> void configureConstructorMetaData(XMLManagedBean<T> component, List<Class<?>> typeList, List<Element> constructorParameterListElement)
    {
        Class<T> implClazz = component.getReturnType();
        Constructor<T> cons[] = ClassUtil.getConstructors(implClazz);

        boolean found = false;
        Constructor<T> componentConstructor = null;
        for (Constructor<T> constructor : cons)
        {
            Class<?>[] pt = constructor.getParameterTypes();

            // same parameter size
            if (pt.length == typeList.size())
            {
                int j = 0;
                boolean ok = false;

                for (Class<?> parameterType : pt)
                {
                    Class<?> xmlType = typeList.get(j);

                    if (ClassUtil.isClassAssignable(parameterType, xmlType))
                    {
                        if (!ok)
                        {
                            ok = true;
                        }
                    }
                    else
                    {
                        ok = false;
                    }

                    j++;
                }

                if (ok)
                {
                    if (found)
                    {
                        throw new WebBeansConfigurationException(createConfigurationFailedMessage() + "More than one constructor decleration exist.");
                    }
                    else
                    {
                        found = true;
                        componentConstructor = constructor;
                    }
                }

            }

        }

        if (!found)
        {
            throw new NonexistentConstructorException(createConfigurationFailedMessage() + "Constructor decleration not found in the class.");
        }

        XMLInjectableConstructor<T> injectableConstructor = new XMLInjectableConstructor<T>(componentConstructor, component, null);
        int i = 0;
        Constructor<?> constructor = injectableConstructor.getConstructor();
        for (Element element : constructorParameterListElement)
        {
            XMLInjectionPointModel model = XMLUtil.getInjectionPointModel(element, createConfigurationFailedMessage());
            injectableConstructor.addInjectionPointModel(model);

            Annotation[] paramAnnos = constructor.getParameterAnnotations()[i++];

            for (Annotation paramAnno : paramAnnos)
            {
                model.addAnnotation(paramAnno);
            }

            model.setInjectionMember(constructor);
            model.setType(XMLInjectionModelType.CONSTRUCTOR);

            component.addInjectionPoint(InjectionPointFactory.getXMLInjectionPointData(component, model));
        }

        component.setInjectableConstructor(injectableConstructor);
    }

    /**
     * Configures the field or method of the xml webbeans component. Checks for
     * the field or method definition and call corresponding method.
     *
     * @param component xml webbeans component
     * @param child     field or method child element
     */
    private <T> void configureFieldOrMethodMetaData(XMLManagedBean<T> component, Element child)
    {
        if (XMLUtil.isElementField(child))
        {
            configureField(component, child);

        }
        else if (XMLUtil.isElementMethod(child))
        {
            configureMethod(component, child);
        }
    }

    private boolean hasTextOnlyChild(Element element)
    {
        NodeList ns = element.getChildNodes();
        for (int i = 0; i < ns.getLength(); i++)
        {
            if (ns.item(i) instanceof Text)
            {
                continue;
            }
            return false;
        }
        return true;
    }

    /**
     * Configures the given child element as field of the webbeans component.
     *
     * @param component xml webbeans component
     * @param child     field element
     * @throws NonexistentFieldException if field not exist
     * @throws org.apache.webbeans.exception.inject.DefinitionException
     *         if field type declared in the xml is not assignable to the type declared in class
     * @throws org.apache.webbeans.exception.inject.DefinitionException
     *         if contains more than one &lt;valuegt; element childs
     */
    private <T> void configureField(XMLManagedBean<T> component, Element child)
    {
        Class<?> clazz = component.getReturnType();

        String fieldName = XMLUtil.getName(child);
        Field field = ClassUtil.getFieldWithName(clazz, fieldName);

        if (field == null)
        {
            throw new NonexistentFieldException(createConfigurationFailedMessage() + "Field name : " + fieldName + " decleration not found in the class : " + clazz.getName());
        }

        boolean isValueElement = false;

        boolean isApplicable = false;
        if (hasTextOnlyChild(child))
        {
            if (!isValueElement)
            {
                isValueElement = true;
                isApplicable = true;
            }
        }

        boolean isTypeElement = false;

        // it has some other elements
        int childElementNumber = 0;
        Node node;
        Element directChild = null;
        NodeList directChilds = child.getChildNodes();
        for (int i = 0; i < directChilds.getLength(); i++)
        {
            node = directChilds.item(i);
            if (!(node instanceof Element))
            {
                continue;
            }
            directChild = (Element) node;
            childElementNumber++;
            if (XMLUtil.isElementInWebBeansNameSpaceWithName(directChild, WebBeansConstants.WEB_BEANS_XML_VALUE_ELEMENT))
            {
                if (!isValueElement)
                {
                    isValueElement = true;
                    isApplicable = true;
                }

            }
            else
            {
                Class<?> directChildType = XMLUtil.getElementJavaType(directChild);
                if (!ClassUtil.isClassAssignable(field.getType(), directChildType))
                {
                    throw new WebBeansConfigurationException(createConfigurationFailedMessage() + "Declared field type is not assignable to class field type");
                }
                else
                {
                    XMLInjectionPointModel injectionPointModel = XMLUtil.getInjectionPointModel(directChild, createConfigurationFailedMessage());
                    component.addFieldInjectionPoint(field, injectionPointModel);

                    Annotation[] annots = field.getAnnotations();
                    for (Annotation annotation : annots)
                    {
                        injectionPointModel.addAnnotation(annotation);
                    }

                    injectionPointModel.setInjectionMember(field);
                    injectionPointModel.setType(XMLInjectionModelType.FIELD);
                    component.addInjectionPoint(InjectionPointFactory.getXMLInjectionPointData(component, injectionPointModel));

                    isTypeElement = true;
                    isApplicable = true;
                }
            }
        }

        if (!isApplicable)
        {
            throw new WebBeansConfigurationException(createConfigurationFailedMessage() + "Field with name : " + fieldName + " element is not correctly defined");
        }

        if (childElementNumber > 1)
        {
            if (!isValueElement)
            {
                throw new WebBeansConfigurationException(createConfigurationFailedMessage() + "One of the direct childs of the field must be element <value>");
            }
            else
            {
                if (isValueElement && isTypeElement)
                {
                    throw new WebBeansConfigurationException(createConfigurationFailedMessage() + "Direct child of the field can not contains both value element and type element");
                }
            }
        }

        // configure field values if available.
        if (directChild != null)
        {
            configureFieldValues(component, field, directChild, isValueElement);
        }
        else
        {
            configureFieldValues(component, field, child, isValueElement);
        }
    }

    /**
     * Configures the method of the webbeans component.
     *
     * @param component xml webbeans component
     * @param child     method element
     * @throws NonexistentMethodException if method with name does not exist
     * @throws NonexistentTypeException   if method parameter types can not found
     * @throws org.apache.webbeans.exception.inject.DefinitionException if any other configuration related exception occurs.
     */
    private <T> void configureMethod(XMLManagedBean<T> component, Element child)
    {
        if (!ClassUtil.hasMethodWithName(component.getReturnType(), XMLUtil.getName(child)))
        {
            throw new NonexistentMethodException(createConfigurationFailedMessage() + "Method declaration with name " + XMLUtil.getName(child) + " is not found in the class : " +
                                                 component.getReturnType().getName());
        }

        boolean isDefineType = false;

        /*
         * <Initializes> = 0; <Produces> = 1; <Disposes> = 2; <Observes> = 3;
         * <Destructor> = 4; InterceptorBindingType annotation on method = 5;
         */
        //X TODO replace this with an enum or constants for better readability!
        int type = 0;

        /* Method parameters classes */
        List<Class<?>> methodParameters = new ArrayList<Class<?>>();

        /* Method parameter xml elements */
        List<Element> methodParameterElements = new ArrayList<Element>();

        Node node;
        Element methodChild = null;
        NodeList mns = child.getChildNodes();
        for (int j = 0; j < mns.getLength(); j++)
        {
            node = mns.item(j);
            if (!(node instanceof Element))
            {
                continue;
            }
            methodChild = (Element) node;
            Class<?> childClazz = XMLUtil.getElementJavaType(methodChild);

            final String moreThanOneChildTypeErrorMesg = createConfigurationFailedMessage() + XMLUtil.getName(methodChild) +
                                                      "method declaration can not contain more than one <Initializer>, <Destructor>, <Produces>, <Disposes> or <Observes> element";

            if (childClazz == null)
            {
                throw new NonexistentTypeException(
                        createConfigurationFailedMessage() + "Direct child element of method : " + XMLUtil.getName(methodChild) + " does not corresponds to any Java type");
            }
            else
            {
                if (childClazz.isAnnotation())
                {
                    if (childClazz.equals(Disposes.class))
                    {
                        if (isDefineType)
                        {
                            throw new WebBeansConfigurationException(moreThanOneChildTypeErrorMesg);
                        }
                        else
                        {
                            checkConfigureDisposes(component, methodChild);
                            isDefineType = true;
                            type = 2;
                        }

                    }
                    else if (childClazz.equals(Observes.class))
                    {
                        if (isDefineType)
                        {
                            throw new WebBeansConfigurationException(moreThanOneChildTypeErrorMesg);
                        }
                        else
                        {
                            checkConfigureObserves(component, methodChild);
                            isDefineType = true;
                            type = 3;
                        }
                    }
                    else if (childClazz.equals(Inject.class))
                    {
                        if (isDefineType)
                        {
                            throw new WebBeansConfigurationException(moreThanOneChildTypeErrorMesg);
                        }
                        else
                        {
                            isDefineType = true;
                            type = 0;
                        }

                    }
                    else if (childClazz.equals(Produces.class))
                    {
                        if (isDefineType)
                        {
                            throw new WebBeansConfigurationException(moreThanOneChildTypeErrorMesg);
                        }
                        else
                        {
                            isDefineType = true;
                            type = 1;
                        }
                    }
                    else if (AnnotationUtil.isInterceptorBindingAnnotation((Class<? extends Annotation>) childClazz))
                    {
                        // InterceptorBindingType with method
                        type = 5;
                    }
                    else
                    {
                        throw new WebBeansConfigurationException(
                                createConfigurationFailedMessage() + "Direct child element of method : " + XMLUtil.getName(methodChild) + " with Java type : " + childClazz +
                                " is unknown");
                    }
                }
                // Method parameter
                else
                {
                    methodParameters.add(childClazz);
                    methodParameterElements.add(methodChild);
                }
            }
        }

        // Check method conditions with method name and its parameter types.
        Method annotatedMethod = checkConfigureMethodConditions(component, child, methodParameters);

        // Configures method according to the type of the element in 0,1,2,3,4,5
        configureMethodAnnotationType(component, annotatedMethod, child, type, methodParameterElements);

    }

    /**
     * Configures the webbeans component methods that are defines in the xml.
     *
     * @param component       xml webbeans component
     * @param annotatedMethod annotated method to configure
     * @param annotChild      element child
     * @param type            type of the configuration method
     */
    private <T> void configureMethodAnnotationType(XMLManagedBean<T> component, Method annotatedMethod, Element annotChild, int type, List<Element> methodParameterElements)
    {
        switch (type)
        {
            case 0:

                configureMethodInitializeAnnotation(component, annotatedMethod, methodParameterElements);
                break;

            case 1:
                configureMethodProducesAnnotation(component, annotatedMethod, annotChild);
                break;

            case 2:
                configureMethodDisposesAnnotation(component, annotatedMethod, annotChild);
                break;

            case 3:
                configureMethodObservesAnnotation(component, annotatedMethod, annotChild);
                break;

            case 5:
                configureMethodInterceptorBindingTypeAnnotation(component, annotatedMethod, annotChild);
                break;

            default:
                throw new WebBeansConfigurationException("unknown MethodAnnotytionType " + type); 
        }
    }

    /**
     * Configures the initializor method of the webbeans component.
     *
     * @param component        xml webbeans component
     * @param initializeMethod initialize method of the webbeans component
     * @param methodParameterElements element childs
     */
    private <T> void configureMethodInitializeAnnotation(XMLManagedBean<T> component, Method initializeMethod, List<Element> methodParameterElements)
    {
        if (methodParameterElements.isEmpty())
        {
            component.addMethodInjectionPoint(initializeMethod, null);
        }
        else
        {
            for (Element element : methodParameterElements)
            {
                XMLInjectionPointModel model = XMLUtil.getInjectionPointModel(element, createConfigurationFailedMessage());
                component.addMethodInjectionPoint(initializeMethod, model);

                component.addInjectionPoint(XMLDefinitionUtil.getXMLMethodInjectionPoint(component, model, initializeMethod));
            }
        }
    }

    private <T> void configureMethodProducesAnnotation(XMLManagedBean<T> component, Method producesMethod, Element annotChild)
    {
        XMLProducerBean<T> producer = XMLDefinitionUtil.defineXMLProducerMethod(this, component, producesMethod, annotChild, createConfigurationFailedMessage());
        WebBeansContext.getInstance().getBeanManagerImpl().addBean(producer);
    }

    private <T> void configureMethodDisposesAnnotation(XMLManagedBean<T> component, Method disposalMethod, Element annotChild)
    {
        XMLDefinitionUtil.defineXMLDisposalMethod(component, disposalMethod, annotChild, createConfigurationFailedMessage());

    }

    private <T> void configureMethodObservesAnnotation(XMLManagedBean<T> component, Method observesMethod, Element annotChild)
    {
        XMLDefinitionUtil.defineXMLObservesMethod(component, observesMethod, annotChild, createConfigurationFailedMessage());
    }

    private <T> void configureMethodInterceptorBindingTypeAnnotation(XMLManagedBean<T> component, Method interceptorMethod, Element annotChild)
    {
        XMLDefinitionUtil.defineXMLMethodLevelInterceptorType(component, interceptorMethod, annotChild, createConfigurationFailedMessage());
    }

    /**
     * Check method conditions for the webbeans component.
     *
     * @param component        xml webbeans component
     * @param child            method element
     * @param methodParameters method parameter types
     * @throws NonexistentMethodException if no method is exist with given name
     *                                    and method parameter types
     * @throws org.apache.webbeans.exception.inject.DefinitionException if more than one method satisfies the conditions
     */
    private <T> Method checkConfigureMethodConditions(XMLManagedBean<T> component, Element child, List<Class<?>> methodParameters)
    {
        // Check with name and also parameter types
        List<Method> definedMethods = ClassUtil.getClassMethodsWithTypes(component.getReturnType(), XMLUtil.getName(child), methodParameters);

        if (definedMethods.size() == 0)
        {
            throw new NonexistentMethodException(createConfigurationFailedMessage() + "Method declaration with name " + XMLUtil.getName(child) + " is not found in the class : " +
                                                 component.getReturnType().getName());

        }
        else if (definedMethods.size() > 1)
        {
            throw new WebBeansConfigurationException(
                    createConfigurationFailedMessage() + "More than one method : " + XMLUtil.getName(child) + " is found in the class : " + component.getReturnType().getName());
        }

        return definedMethods.get(0);
    }

    /**
     * Configures the disposal method of the webbeans component.
     *
     * @param component xml webbeans component
     * @param disposes  disposes element
     * @throws org.apache.webbeans.exception.inject.DefinitionException if disposes element can not contain exactly one child element
     */
    private <T> void checkConfigureDisposes(AbstractOwbBean<T> component, Element disposes)
    {
        NodeList disposesChilds = disposes.getChildNodes();

        if (disposesChilds.getLength() != 1)
        {
            throw new WebBeansConfigurationException(
                    createConfigurationFailedMessage() + "Disposes element : " + XMLUtil.getName(disposes) + " can not contain more than one direct child elements");
        }

    }


    /**
     * Configures the observer method of the webbeans component
     *
     * @param component xml webbeans component
     * @param observes  observes element
     * @throws org.apache.webbeans.exception.inject.DefinitionException if disposes element can not contain exactly
     *                             one child element and it is not one of the {IfExists,
     *                             AfterTransactionCompletion, AfterTransactionSuccess,
     *                             AfterTransactionFailure, BeforeTransactionCompletion}
     *                             element.
     */
    private <T> void checkConfigureObserves(AbstractOwbBean<T> component, Element observes)
    {
        int childElementCount = 0;
        NodeList observesChilds = observes.getChildNodes();
        for (int i = 0; i < observesChilds.getLength(); i++)
        {
            if (observesChilds.item(i) instanceof Element)
            {
                childElementCount++;
            }
        }
        if (childElementCount != 1)
        {
            throw new WebBeansConfigurationException(
                    createConfigurationFailedMessage() + "Observes element : " + XMLUtil.getName(observes) + " can not contain more than one direct child elements");
        }

    }

    /**
     * Configures the webbeans scope type.
     *
     * @param component     xml defined web beans component
     * @param annotationSet all annotation defined in XML
     */
    private <T> void configureScopeType(AbstractOwbBean<T> component, List<Class<? extends Annotation>> annotationSet, List<Element> annotationElementList)
    {
        Class<? extends Annotation> scopeType = XMLDefinitionUtil.defineXMLTypeMetaData(component, annotationSet, NormalScope.class,
                                                                                        createConfigurationFailedMessage()
                                                                                        + "@Scope/@NormalScope annotation is not configured correctly");

        if (scopeType == null)
        {
            scopeType = XMLDefinitionUtil.defineXMLTypeMetaData(component, annotationSet, Scope.class,
                                                                createConfigurationFailedMessage() + "@Scope/@NormalScope annotation is not configured correctly");
        }

        if (scopeType == null)
        {
            // From stereotype
            DefinitionUtil.defineDefaultScopeType(component, createConfigurationFailedMessage() + "@Scope annotation is not configured correctly");
        }
        else
        {
            component.setImplScopeType(JavassistProxyFactory.createNewAnnotationProxy(scopeType));
        }

    }

    /**
     * Configures the binding types of the web beans component.
     *
     * @param component web beans xml component
     * @param annotationSet annotations defined in the xml documents
     * @param annotationElementList child elements
     */
    private <T> void configureBindingType(AbstractOwbBean<T> component, List<Class<? extends Annotation>> annotationSet, List<Element> annotationElementList)
    {
        boolean isDefined = XMLDefinitionUtil.defineXMLBindingType(component, annotationSet, annotationElementList, createConfigurationFailedMessage());

        if (!isDefined)
        {
            component.addQualifier(new DefaultLiteral());
        }

    }

    /**
     * Configures the class level interceptor binding types.
     *
     * @param component web beans xml component
     * @param annotationSet annotations defined in the xml documents
     * @param annotationElementList child elements
     */
    private <T> void configureInterceptorBindingType(XMLManagedBean<T> component, List<Class<? extends Annotation>> annotationSet, List<Element> annotationElementList)
    {
        XMLDefinitionUtil.defineXMLClassLevelInterceptorType(component, annotationSet, annotationElementList, createConfigurationFailedMessage());
    }

    /**
     * Defines the component stereotypes.
     *
     * @param component     webbeans component
     * @param annotationSet type-level metadata annotation set
     */
    private <T> void configureStereoType(AbstractOwbBean<T> component, List<Class<? extends Annotation>> annotationSet, List<Element> annotationElementList)
    {
        XMLDefinitionUtil.defineXMLStereoType(component, annotationSet);
    }

    /**
     * Configures the webbeans name.
     *
     * @param component          webbeans component
     * @param annotationSet      type-level metadata annotation set
     * @param webBeanDecleration webbeans decleration element
     */
    private <T> void configureNamed(AbstractOwbBean<T> component, List<Class<? extends Annotation>> annotationSet, Element webBeanDecleration)
    {
        boolean isDefined = XMLDefinitionUtil.defineXMLName(component, annotationSet);
        if (isDefined)
        {
            Element element = (Element) webBeanDecleration.getElementsByTagName(WebBeansConstants.WEB_BEANS_XML_NAMED_ELEMENT).item(0);
            String name = element.getTextContent().trim();

            if (name != null && !name.equals(""))
            {
                component.setName(name);
            }
            else
            {
                component.setName(WebBeansUtil.getManagedBeanDefaultName(component.getReturnType().getName()));
            }
        }
        else
        {
            DefinitionUtil
                    .defineName(component, component.getReturnType().getDeclaredAnnotations(), WebBeansUtil.getManagedBeanDefaultName(component.getReturnType().getSimpleName()));
        }
    }

    /**
     * Configures the webbeans specializations.
     *
     * @param component     webbeans component
     * @param annotationSet type-level metadata annotation set
     */
    private <T> void configureSpecializes(XMLManagedBean<T> component, List<Class<? extends Annotation>> annotationSet)
    {
        XMLDefinitionUtil.defineXMLSpecializes(component, annotationSet);
    }

    /**
     * Configures the interceptors simple webbeans.
     *
     * @param component     webbeans component
     * @param annotationSet type-level metadata annotation set
     */
    private <T> void configureInterceptor(XMLManagedBean<T> component, List<Class<? extends Annotation>> annotationSet, List<Element> annotationElementList)
    {
        XMLDefinitionUtil.defineXMLInterceptors(component, annotationSet, annotationElementList, createConfigurationFailedMessage());

    }

    /**
     * Configures the decorators simple webbeans.
     *
     * @param component     webbeans component
     * @param annotationSet type-level metadata annotation set
     */
    private <T> void configureDecorator(XMLManagedBean<T> component, List<Class<? extends Annotation>> annotationSet,
                                        List<Element> annotationElementList, Element webBeanDecleration)
    {
        XMLDefinitionUtil.defineXMLDecorators(component, annotationSet, webBeanDecleration, createConfigurationFailedMessage());
    }

    /**
     * Configures the enterprise web bean from ejb class.
     *
     * @param ejbClass ejb class
     */
    private void configureEJBWebBean(Class<?> ejbClass)
    {
        // TODO EJB Decleration
    }

    /**
     * Configures JMS endpoint.
     *
     * @param webBeanElement element
     */
    private void configureJMSEndpointComponent(Element webBeanElement)
    {
        Element resource = (Element) webBeanElement.getElementsByTagName(WebBeansConstants.WEB_BEANS_XML_JMS_RESOURCE).item(0);

        if (resource == null)
        {
            throw new WebBeansConfigurationException("Topic or Queue resource mut be defined in the XML");
        }

        Element name = (Element) resource.getElementsByTagName(WebBeansConstants.WEB_BEANS_XML_JMS_RESOURCE_NAME).item(0);
        Element mappedName = (Element) resource.getElementsByTagName(WebBeansConstants.WEB_BEANS_XML_JMS_RESOURCE_MAPPED_NAME).item(0);

        if (name == null && mappedName == null)
        {
            throw new WebBeansConfigurationException("Topic or Queue must define name or mapped name for the JNDI");
        }

        List<Annotation> bindingTypes = new ArrayList<Annotation>();
        Node node;
        Element child;
        NodeList ns = webBeanElement.getChildNodes();
        for (int i = 0; i < ns.getLength(); i++)
        {
            node = ns.item(i);
            if (!(node instanceof Element))
            {
                continue;
            }
            child = (Element) node;
            if (child.getLocalName() != WebBeansConstants.WEB_BEANS_XML_JMS_RESOURCE)
            {
                Class<? extends Annotation> binding = (Class<Annotation>) XMLUtil.getElementJavaType(child);

                if (AnnotationUtil.isQualifierAnnotation(binding))
                {
                    bindingTypes.add(JavassistProxyFactory.createNewAnnotationProxy(binding));
                }
            }
        }

        JMSType type = null;

        if (webBeanElement.getLocalName().equals(WebBeansConstants.WEB_BEANS_XML_TOPIC_ELEMENT))
        {
            type = JMSType.TOPIC;
        }
        else
        {
            type = JMSType.QUEUE;
        }


        String jndiName = name == null ? null : name.getTextContent().trim();
        String mapName = mappedName == null ? null : mappedName.getTextContent().trim();


        JMSModel model = new JMSModel(type, jndiName, mapName);
        WebBeansContext.getInstance().getjMSManager().addJmsModel(model);

        for (Annotation ann : bindingTypes)
        {
            model.addBinding(ann);
        }

        //Adding JMS Beans
        OpenWebBeansJmsPlugin plugin = WebBeansContext.getInstance().getPluginLoader().getJmsPlugin();
        WebBeansContext.getInstance().getBeanManagerImpl().addBean(plugin.getJmsBean(model));
    }

    /**
     * Cofigures the initial value of the given field.
     *
     * @param component      webbeans component
     * @param field          field of the webbean
     * @param child          child element that declares field
     * @param isValueElement is it applicable for value setting
     */
    private <T> void configureFieldValues(XMLManagedBean<T> component, Field field, Element child, boolean isValueElement)
    {
        if (isValueElement)
        {
            String errorMessage = createConfigurationFailedMessage() + "Field value of field name : " + field.getName()
                                  + " is not applicable for initial value assignment";

            /* Field type */
            Class<?> fieldType = field.getType();

            String value = child.getTextContent().trim();

            try
            {

                if (!ClassUtil.isInValueTypes(fieldType))
                {
                    throw new WebBeansConfigurationException(
                            createConfigurationFailedMessage() + "Field type with field name : " + field.getName()
                            + " is not compatible for initial value assignment");
                }

                /*Primitive type*/
                if (ClassUtil.isPrimitive(fieldType) || ClassUtil.isPrimitiveWrapper(fieldType))
                {
                    Object objVal = ClassUtil.isValueOkForPrimitiveOrWrapper(fieldType, value);

                    if (objVal != null)
                    {
                        component.addFieldValue(field, objVal);

                    }
                    else
                    {
                        throw new WebBeansConfigurationException(errorMessage);
                    }

                }
                /* Enumeration value */
                else if (ClassUtil.isEnum(fieldType))
                {
                    Enum enumValue = ClassUtil.isValueOkForEnum(fieldType, value);

                    if (enumValue == null)
                    {
                        throw new WebBeansConfigurationException(errorMessage);
                    }

                    component.addFieldValue(field, enumValue);

                }
                /* String value */
                else if (fieldType.equals(String.class))
                {
                    component.addFieldValue(field, value);

                }

                /*
                * Date, Time, Sql Date,
                * Time stamp, Calendar
                * value
                */
                else if (fieldType.equals(Date.class)
                         || fieldType.equals(java.sql.Date.class) || fieldType.equals(Time.class) || fieldType.equals(Timestamp.class))
                {
                    Date date = ClassUtil.isValueOkForDate(value);

                    if (date == null)
                    {
                        throw new WebBeansConfigurationException(errorMessage);
                    }
                    else
                    {
                        component.addFieldValue(field, date);
                    }

                }
                else if (fieldType.equals(Calendar.class))
                {
                    Calendar calendar = ClassUtil.isValueOkForCalendar(value);

                    if (calendar == null)
                    {
                        throw new WebBeansConfigurationException(errorMessage);
                    }
                    else
                    {
                        component.addFieldValue(field, calendar);
                    }

                }
                /*
                 * BigDecimal
                 * or
                 * BigInteger
                 */
                else if (fieldType.equals(BigDecimal.class) || fieldType.equals(BigInteger.class))
                {
                    Object bigValue = ClassUtil.isValueOkForBigDecimalOrInteger(fieldType, value);

                    if (bigValue == null)
                    {
                        throw new WebBeansConfigurationException(errorMessage);
                    }
                    else
                    {
                        component.addFieldValue(field, bigValue);
                    }

                }
                /* Class value */
                else if (fieldType.equals(Class.class))
                {
                    Class<?> clazz = ClassUtil.getClassFromName(value);

                    if (clazz == null)
                    {
                        throw new WebBeansConfigurationException(errorMessage);
                    }
                    else
                    {
                        component.addFieldValue(field, clazz);
                    }
                }

                /*
                * List value
                */
                else if (List.class.isAssignableFrom(fieldType))
                {
                    configureFieldListValue(component, field, child, errorMessage);
                }

                /* Set value */
                else if (Set.class.isAssignableFrom(fieldType))
                {
                    configureFieldSetValue(component, field, child, errorMessage);
                }

            }
            catch (ParseException e)
            {
                throw new WebBeansConfigurationException(errorMessage, e);
            }
        }
    }

    /**
     * Configures the xml field with {@link List} type.
     *
     * @param component    xml component
     * @param field        list field
     * @param child        list field xml element
     * @param errorMessage error message
     */
    private void configureFieldListValue(XMLManagedBean<?> component, Field field, Element child, String errorMessage)
    {
        boolean isString = false;
        boolean isEnum = false;
        boolean isClazz = false;
        Type type = field.getGenericType();
        Class<?> argClazz = null;
        List list = null;

        /*
         * Type must be parametrized type
         * to mark type
         */
        if (type instanceof ParameterizedType)
        {
            ParameterizedType pt = (ParameterizedType) type;
            Type arg = pt.getActualTypeArguments()[0];

            if (ClassUtil.isFirstParametricTypeArgGeneric(pt))
            {
                throw new WebBeansConfigurationException(
                        createConfigurationFailedMessage() + "List field type with field name : " + field.getName() + " must be declared as ParametrizedType " +
                        "but parametric type can not be TypeVariable or Wildcard Type");
            }

            else
            {
                argClazz = (Class<?>) arg;

                if (argClazz.equals(String.class))
                {
                    isString = true;
                    list = new ArrayList<String>();
                }
                else if (Enum.class.isAssignableFrom(argClazz))
                {
                    isEnum = true;
                    list = new ArrayList<Enum>();
                }
                else if (argClazz.equals(Class.class))
                {
                    isClazz = true;
                    list = new ArrayList<Class>();
                }
            }
        }
        else
        {
            throw new WebBeansConfigurationException(
                    createConfigurationFailedMessage() + "List field type with field name : " + field.getName() + " must be declared as ParametrizedType");
        }

        Node valueNode;
        Text valueElement;
        NodeList valueElements = child.getChildNodes();
        for (int i = 0; i < valueElements.getLength(); i++)
        {
            valueNode = valueElements.item(i);
            if (!(valueNode instanceof Text))
            {
                continue;
            }
            valueElement = (Text) valueNode;
            String value = valueElement.getTextContent().trim();
            if (isString)
            {
                list.add(value);
            }
            else if (isEnum)
            {
                Enum en = ClassUtil.isValueOkForEnum(argClazz, value);
                if (en == null)
                {
                    throw new WebBeansConfigurationException(errorMessage);
                }
                else
                {
                    list.add(en);
                }
            }
            else if (isClazz)
            {
                Class<?> clazz = ClassUtil.getClassFromName(value);

                if (clazz == null)
                {
                    throw new WebBeansConfigurationException(errorMessage);
                }
                else
                {
                    list.add(clazz);
                }
            }
        }

        component.addFieldValue(field, list);
    }

    /**
     * Configures the xml field with {@link Set} type.
     *
     * @param component    xml component
     * @param field        list field
     * @param child        list field xml element
     * @param errorMessage error message
     */
    private void configureFieldSetValue(XMLManagedBean<?> component, Field field, Element child, String errorMessage)
    {
        boolean isString = false;
        boolean isEnum = false;
        boolean isClazz = false;
        Type type = field.getGenericType();
        Class<?> argClazz = null;
        Set set = null;

        /*
         * Type must be parametrized type
         * to mark type
         */
        if (type instanceof ParameterizedType)
        {
            ParameterizedType pt = (ParameterizedType) type;
            Type arg = pt.getActualTypeArguments()[0];

            if (ClassUtil.isFirstParametricTypeArgGeneric(pt))
            {
                throw new WebBeansConfigurationException(
                        createConfigurationFailedMessage() + "Set field type with field name : " + field.getName() + " must be declared as ParametrizedType " +
                        "but parametric type can not be TypeVariable or Wildcard Type");
            }

            else
            {
                argClazz = (Class<?>) arg;

                if (argClazz.equals(String.class))
                {
                    isString = true;
                    set = new HashSet<String>();
                }
                else if (Enum.class.isAssignableFrom(argClazz))
                {
                    isEnum = true;
                    set = new HashSet<Enum>();
                }
                else if (argClazz.equals(Class.class))
                {
                    isClazz = true;
                    set = new HashSet<Class>();
                }
            }
        }
        else
        {
            throw new WebBeansConfigurationException(
                    createConfigurationFailedMessage() + "Set field type with field name : " + field.getName() + " must be declared as ParametrizedType");
        }

        Node valueNode;
        Element valueElement;
        NodeList valueElements = child.getChildNodes();
        for (int i = 0; i < valueElements.getLength(); i++)
        {
            valueNode = valueElements.item(i);
            if (!(valueNode instanceof Element))
            {
                continue;
            }
            valueElement = (Element) valueNode;
            String value = valueElement.getTextContent().trim();
            if (isString)
            {
                set.add(value);
            }
            else if (isEnum)
            {
                Enum en = ClassUtil.isValueOkForEnum(argClazz, value);
                if (en == null)
                {
                    throw new WebBeansConfigurationException(errorMessage);
                }
                else
                {
                    set.add(en);
                }
            }
            else if (isClazz)
            {
                Class<?> clazz = ClassUtil.getClassFromName(value);

                if (clazz == null)
                {
                    throw new WebBeansConfigurationException(errorMessage);
                }
                else
                {
                    set.add(clazz);
                }
            }
        }

        component.addFieldValue(field, set);
    }

    /**
     * Gets error message for XML parsing of the current XML file.
     *
     * @return the error messages
     */
    private String createConfigurationFailedMessage()
    {
        return "WebBeans XML configuration defined in " + CURRENT_SCAN_FILE_NAME + " is failed. Reason is : ";
    }

}
