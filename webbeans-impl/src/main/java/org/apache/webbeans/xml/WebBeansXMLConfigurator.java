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
import java.util.List;

import javax.enterprise.context.NormalScope;
import javax.inject.Scope;
import javax.interceptor.Interceptor;

import org.apache.webbeans.annotation.DefaultLiteral;
import org.apache.webbeans.component.AbstractOwbBean;
import org.apache.webbeans.config.DefinitionUtil;
import org.apache.webbeans.config.OWBLogConst;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.decorator.DecoratorsManager;
import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.exception.definition.NonexistentTypeException;
import org.apache.webbeans.exception.inject.DeploymentException;
import org.apache.webbeans.inject.AlternativesManager;
import org.apache.webbeans.intercept.InterceptorsManager;
import org.apache.webbeans.logger.WebBeansLogger;
import org.apache.webbeans.proxy.JavassistProxyFactory;
import org.apache.webbeans.spi.ScannerService;
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
     * Annotation type manager that manages the XML defined annotations
     */
    private XMLAnnotationTypeManager xmlAnnotTypeManager = WebBeansContext.getInstance().getxMLAnnotationTypeManager();

    /**
     * Creates a new instance of the <code>WebBeansXMLConfigurator</code>
     */
    public WebBeansXMLConfigurator()
    {
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
                configureSpecSpecific(xmlStream, "No-name XML Stream");
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
                configureSpecSpecific(xmlStream, fileName,scanner);
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

            clazz = ClassUtil.getClassFromName(child.getTextContent().trim());

            if (clazz == null)
            {
                throw new WebBeansConfigurationException(createConfigurationFailedMessage() + "Interceptor class : " +
                                                         child.getTextContent().trim() + " not found");
            }
            else
            {
                if (AnnotationUtil.hasAnnotation(clazz.getDeclaredAnnotations(), Interceptor.class) &&
                    !AnnotationUtil.hasInterceptorBindingMetaAnnotation(clazz.getDeclaredAnnotations()))
                {
                    throw new WebBeansConfigurationException(createConfigurationFailedMessage() + "Interceptor class : " +
                                                             child.getTextContent().trim() +
                                                             " must have at least one @InterceptorBindingType");
                }
                boolean isBDAScanningEnabled=(scanner!=null && scanner.isBDABeansXmlScanningEnabled());
                if ((!isBDAScanningEnabled && manager.isInterceptorEnabled(clazz)) ||
                        (isBDAScanningEnabled && !scanner.getBDABeansXmlScanner().addInterceptor(clazz, fileName)))
                {
                    throw new WebBeansConfigurationException(createConfigurationFailedMessage() + "Interceptor class : " +
                                                             child.getTextContent().trim() + " is already defined");
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

            clazz = ClassUtil.getClassFromName(child.getTextContent().trim());

            if (clazz == null)
            {
                throw new WebBeansConfigurationException(createConfigurationFailedMessage() + "Decorator class : " +
                                                         child.getTextContent().trim() + " not found");
            }
            else
            {
                boolean isBDAScanningEnabled=(scanner!=null && scanner.isBDABeansXmlScanningEnabled());
                if ((isBDAScanningEnabled && !scanner.getBDABeansXmlScanner().addDecorator(clazz, fileName))||
                        (!isBDAScanningEnabled && manager.isDecoratorEnabled(clazz)))
                {
                    throw new WebBeansConfigurationException(createConfigurationFailedMessage() + "Decorator class : " +
                                                             child.getTextContent().trim() + " is already defined");
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

        clazz = ClassUtil.getClassFromName(child.getTextContent().trim());

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
     * Gets error message for XML parsing of the current XML file.
     *
     * @return the error messages
     */
    private String createConfigurationFailedMessage()
    {
        return "WebBeans XML configuration defined in " + CURRENT_SCAN_FILE_NAME + " is failed. Reason is : ";
    }

}
