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

import javax.interceptor.Interceptor;

import org.apache.webbeans.config.OWBLogConst;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.decorator.DecoratorsManager;
import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.inject.AlternativesManager;
import org.apache.webbeans.intercept.InterceptorsManager;
import org.apache.webbeans.logger.WebBeansLogger;
import org.apache.webbeans.spi.ScannerService;
import org.apache.webbeans.util.AnnotationUtil;
import org.apache.webbeans.util.Asserts;
import org.apache.webbeans.util.ClassUtil;
import org.apache.webbeans.util.WebBeansConstants;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

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
     * Gets error message for XML parsing of the current XML file.
     *
     * @return the error messages
     */
    private String createConfigurationFailedMessage()
    {
        return "WebBeans XML configuration defined in " + CURRENT_SCAN_FILE_NAME + " is failed. Reason is : ";
    }

}
