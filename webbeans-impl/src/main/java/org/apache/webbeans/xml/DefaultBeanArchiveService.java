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

import javax.enterprise.inject.spi.DefinitionException;
import javax.enterprise.inject.spi.DeploymentException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.webbeans.config.OWBLogConst;
import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.exception.WebBeansException;
import org.apache.webbeans.logger.WebBeansLoggerFacade;
import org.apache.webbeans.spi.BeanArchiveService;
import org.apache.webbeans.util.WebBeansConstants;
import org.w3c.dom.Element;

/**
 * Please note that this implementation is not thread safe.
 */
public class DefaultBeanArchiveService implements BeanArchiveService
{
    private static final Logger logger = WebBeansLoggerFacade.getLogger(BeanArchiveService.class);

    /**
     * Contains a map from the URL externalForm to the stored BeanArchiveInformation
     */
    private Map<String, BeanArchiveInformation> beanArchiveInformations = new HashMap<String, BeanArchiveInformation>();


    @Override
    public BeanArchiveInformation getBeanArchiveInformation(URL beansXmlUrl)
    {
        String beansXmlLocation = beansXmlUrl.toExternalForm();
        BeanArchiveInformation bdaInfo = beanArchiveInformations.get(beansXmlLocation);

        if (bdaInfo == null)
        {
            bdaInfo = readBeansXml(beansXmlUrl, beansXmlLocation);
            beanArchiveInformations.put(beansXmlLocation, bdaInfo);
        }

        return bdaInfo;
    }

    /**
     * This method exists for extensibility reasons.
     */
    protected DefaultBeanArchiveInformation createBeanArchiveInformation()
    {
        return new DefaultBeanArchiveInformation();
    }

    private BeanArchiveInformation readBeansXml(URL beansXmlUrl, String beansXmlLocation)
    {
        if (beansXmlUrl == null)
        {
            throw new DefinitionException("beans.xml URL must be given!");
        }

        if (!beansXmlLocation.endsWith(".xml"))
        {
            // handle jars without beans.xml file
            DefaultBeanArchiveInformation bdaInfo = createBeanArchiveInformation();
            bdaInfo.setBeanDiscoveryMode(BeanDiscoveryMode.ANNOTATED);
            return bdaInfo;
        }


        InputStream xmlStream = null;
        try
        {
            xmlStream = beansXmlUrl.openStream();

            return readBeansXml(xmlStream);

        }
        catch (Exception e)
        {
            throw new DeploymentException("Error while parsing the beans.xml file " + beansXmlLocation, e);
        }
        finally
        {
            try
            {
                xmlStream.close();
            }
            catch (IOException ioe)
            {
                throw new DeploymentException("Error while closing the input stream!", ioe);
            }
        }
    }

    /**
     * Read the information from the given beans.xml and fill it into a
     * BeanArchiveInformation instance.
     */
    protected BeanArchiveInformation readBeansXml(InputStream xmlStream) throws IOException
    {
        DefaultBeanArchiveInformation bdaInfo = createBeanArchiveInformation();


        if (xmlStream.available() > 0)
        {
            //Get root element of the XML document
            Element webBeansRoot = getBeansRootElement(xmlStream);
            if (webBeansRoot != null)
            {
                if (!"beans".equalsIgnoreCase(webBeansRoot.getLocalName()))
                {
                    throw new WebBeansConfigurationException("beans.xml must have a <beans> root element, but has: " + webBeansRoot.getLocalName());
                }

                String version = webBeansRoot.getAttribute("version");
                bdaInfo.setVersion((version != null && version.length() > 0) ? version : null);

                readBeanChildren(bdaInfo, webBeansRoot);
            }


            if (bdaInfo.getVersion() != null && !"1.0".equals(bdaInfo.getVersion()) && bdaInfo.getBeanDiscoveryMode() == null)
            {
                throw new WebBeansConfigurationException("beans.xml with version 1.1 and higher must declare a bean-discovery-mode!");
            }


            if (bdaInfo.getBeanDiscoveryMode() == null)
            {
                // an empty beans.xml file lead to backward compat mode with CDI-1.1.
                bdaInfo.setBeanDiscoveryMode(BeanDiscoveryMode.ALL);
            }
        }

        if (bdaInfo.getBeanDiscoveryMode() == null)
        {
            // no beans.xml file at all will lead to 'implicit bean archive' behaviour.
            bdaInfo.setBeanDiscoveryMode(BeanDiscoveryMode.ANNOTATED);
        }

        return bdaInfo;
    }

    private void readBeanChildren(DefaultBeanArchiveInformation bdaInfo, Element webBeansRoot)
    {
        ElementIterator elit = new ElementIterator(webBeansRoot);
        while (elit.hasNext())
        {
            Element child = elit.next();

            if (WebBeansConstants.WEB_BEANS_XML_SPEC_SPECIFIC_BEAN_DISCOVERY_MODE_ELEMENT.equalsIgnoreCase(child.getLocalName()))
            {
                fillScanMode(bdaInfo, child);
            }
            else if (WebBeansConstants.WEB_BEANS_XML_SPEC_SPECIFIC_DECORATORS_ELEMENT.equalsIgnoreCase(child.getLocalName()))
            {
                fillDecorators(bdaInfo, child);
            }
            else if (WebBeansConstants.WEB_BEANS_XML_SPEC_SPECIFIC_INTERCEPTORS_ELEMENT.equalsIgnoreCase(child.getLocalName()))
            {
                fillInterceptors(bdaInfo, child);
            }
            else if (WebBeansConstants.WEB_BEANS_XML_SPEC_SPECIFIC_ALTERNATIVES_ELEMENT.equalsIgnoreCase(child.getLocalName()))
            {
                fillAlternatives(bdaInfo, child);
            }
        }
    }

    protected void fillScanMode(DefaultBeanArchiveInformation bdaInfo, Element beanDiscoveryModeElement)
    {
        String scanMode = beanDiscoveryModeElement.getTextContent().trim();
        bdaInfo.setBeanDiscoveryMode(BeanDiscoveryMode.valueOf(scanMode.toUpperCase()));
    }

    private void fillDecorators(DefaultBeanArchiveInformation bdaInfo, Element decoratorsElement)
    {
        ElementIterator elit = new ElementIterator(decoratorsElement);
        while (elit.hasNext())
        {
            Element child = elit.next();
            if (WebBeansConstants.WEB_BEANS_XML_SPEC_SPECIFIC_CLASS.equalsIgnoreCase(child.getLocalName()))
            {
                String clazz = child.getTextContent().trim();
                if (clazz.isEmpty())
                {
                    throw new WebBeansConfigurationException("decorators <class> element must not be empty!");
                }
                bdaInfo.getDecorators().add(clazz);
            }
        }
    }

    private void fillInterceptors(DefaultBeanArchiveInformation bdaInfo, Element interceptorsElement)
    {
        ElementIterator elit = new ElementIterator(interceptorsElement);
        while (elit.hasNext())
        {
            Element child = elit.next();
            if (WebBeansConstants.WEB_BEANS_XML_SPEC_SPECIFIC_CLASS.equalsIgnoreCase(child.getLocalName()))
            {
                String clazz = child.getTextContent().trim();
                if (clazz.isEmpty())
                {
                    throw new WebBeansConfigurationException("interceptors <class> element must not be empty!");
                }
                bdaInfo.getInterceptors().add(clazz);
            }
        }
    }

    private void fillAlternatives(DefaultBeanArchiveInformation bdaInfo, Element alternativesElement)
    {
        ElementIterator elit = new ElementIterator(alternativesElement);
        while (elit.hasNext())
        {
            Element child = elit.next();
            if (WebBeansConstants.WEB_BEANS_XML_SPEC_SPECIFIC_CLASS.equalsIgnoreCase(child.getLocalName()))
            {
                String clazz = child.getTextContent().trim();
                if (clazz.isEmpty())
                {
                    throw new WebBeansConfigurationException("alternatives <class> element must not be empty!");
                }
                bdaInfo.getAlternativeClasses().add(clazz);
            }
            if (WebBeansConstants.WEB_BEANS_XML_SPEC_SPECIFIC_STEREOTYPE.equalsIgnoreCase(child.getLocalName()))
            {
                String stereotype = child.getTextContent().trim();
                if (stereotype.isEmpty())
                {
                    throw new WebBeansConfigurationException("alternatives <stereotype> element must not be empty!");
                }
                bdaInfo.getAlternativeStereotypes().add(stereotype);
            }
        }
    }


    @Override
    public void release()
    {
        beanArchiveInformations.clear();
    }


    /**
     * Gets the root element of the parsed document.
     *
     * @param xmlStream parsed document
     * @return root element of the document
     * @throws org.apache.webbeans.exception.WebBeansException if any runtime exception occurs
     */
    private Element getBeansRootElement(InputStream xmlStream) throws WebBeansException
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

            Element root = documentBuilder.parse(xmlStream).getDocumentElement();

            return root;
        }
        catch (Exception e)
        {
            logger.log(Level.SEVERE, OWBLogConst.FATAL_0002, e);
            throw new WebBeansException(WebBeansLoggerFacade.getTokenString(OWBLogConst.EXCEPT_0013), e);
        }
    }

}
