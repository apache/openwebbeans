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

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.webbeans.config.OWBLogConst;
import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.exception.WebBeansDeploymentException;
import org.apache.webbeans.exception.WebBeansException;
import org.apache.webbeans.logger.WebBeansLoggerFacade;
import org.apache.webbeans.spi.BeanArchiveService;
import org.apache.webbeans.util.UrlSet;
import org.apache.webbeans.util.WebBeansConstants;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Please note that this implementation is not thread safe.
 */
public class DefaultBeanArchiveService implements BeanArchiveService
{
    public static final String WEB_INF_BEANS_XML = "WEB-INF/beans.xml";
    public static final String WEB_INF_CLASSES = "WEB-INF/classes";

    private static final Logger logger = WebBeansLoggerFacade.getLogger(BeanArchiveService.class);

    /**
     * Contains a map from the URL externalForm to the stored BeanArchiveInformation
     */
    private Map<String, BeanArchiveInformation> beanArchiveInformations = new HashMap<String, BeanArchiveInformation>();

    private UrlSet registeredBeanArchives = new UrlSet();


    @Override
    public BeanArchiveInformation getBeanArchiveInformation(URL beanArchiveUrl)
    {
        String beanArchiveLocation = beanArchiveUrl.toExternalForm();
        BeanArchiveInformation bdaInfo = beanArchiveInformations.get(beanArchiveLocation);

        if (bdaInfo == null && !beanArchiveLocation.contains(".xml"))
        {
            // probably the beanArchiveUrl is a JAR classpath and not a beans.xml itself
            // in this case we need to look whether we have a corresponding beans.xml already scanned

            String strippedBeanArchiveUrl = stripProtocol(beanArchiveLocation);

            if (strippedBeanArchiveUrl.contains(WEB_INF_BEANS_XML))
            {
                // this is a very special case for beans.xml in a WAR file
                // in this case we are looking for the WEB-INF/classes URL
                strippedBeanArchiveUrl = strippedBeanArchiveUrl.replace(WEB_INF_BEANS_XML, WEB_INF_CLASSES);
            }

            for (Map.Entry<String, BeanArchiveInformation> entry : beanArchiveInformations.entrySet())
            {
                if (stripProtocol(entry.getKey()).startsWith(strippedBeanArchiveUrl))
                {
                    bdaInfo = entry.getValue();
                    break;
                }
            }

        }

        if (bdaInfo == null)
        {
            // if we still did not find anything, then this is a 'new' bean archive
            bdaInfo = readBeansXml(beanArchiveUrl, beanArchiveLocation);
            beanArchiveInformations.put(beanArchiveLocation, bdaInfo);
            registeredBeanArchives.add(beanArchiveUrl);
        }

        return bdaInfo;
    }

    @Override
    public Set<URL> getRegisteredBeanArchives()
    {
        return registeredBeanArchives;
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
            throw new WebBeansConfigurationException("beans.xml URL must be given!");
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
            throw new WebBeansDeploymentException("Error while parsing the beans.xml file " + beansXmlLocation, e);
        }
        finally
        {
            try
            {
                xmlStream.close();
            }
            catch (IOException ioe)
            {
                throw new WebBeansDeploymentException("Error while closing the input stream!", ioe);
            }
        }
    }

    /**
     * Get rid of any protocol header from the url externalForm
     * @param urlPath
     */
    protected String stripProtocol(String urlPath)
    {
        int pos = urlPath.lastIndexOf(":/");
        if (pos > 0)
        {
            return urlPath.substring(pos+1);
        }

        return urlPath;
    }


    /**
     * Read the information from the given beans.xml and fill it into a
     * BeanArchiveInformation instance.
     */
    protected BeanArchiveInformation readBeansXml(InputStream xmlStreamIn) throws IOException
    {
        DefaultBeanArchiveInformation bdaInfo = createBeanArchiveInformation();

        if (xmlStreamIn != null)
        {
            PushbackInputStream xmlStream = new PushbackInputStream(xmlStreamIn);

            // try to read from the stream
            int firstVal = xmlStream.read();
            if (firstVal < 0)
            {
                // this means the stream is empty
                bdaInfo.setBeanDiscoveryMode(BeanDiscoveryMode.ALL);
            }
            else
            {
                // put the first byte back on the stream so we can properly parse the XML.
                xmlStream.unread(firstVal);

                //Get root element of the XML document
                Element webBeansRoot = getBeansRootElement(xmlStream);
                if (webBeansRoot == null)
                {
                    bdaInfo.setBeanDiscoveryMode(BeanDiscoveryMode.ALL);
                }
                else
                {
                    if (!"beans".equalsIgnoreCase(webBeansRoot.getLocalName()))
                    {
                        throw new WebBeansConfigurationException("beans.xml must have a <beans> root element, but has: " + webBeansRoot.getLocalName());
                    }

                    bdaInfo.setVersion(getTrimmedAttribute(webBeansRoot, "version"));

                    String beanDiscoveryMode = getTrimmedAttribute(webBeansRoot, "bean-discovery-mode");
                    bdaInfo.setBeanDiscoveryMode(beanDiscoveryMode != null ? BeanDiscoveryMode.valueOf(beanDiscoveryMode.toUpperCase()) : null);

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

            if (WebBeansConstants.WEB_BEANS_XML_DECORATORS_ELEMENT.equalsIgnoreCase(child.getLocalName()))
            {
                fillDecorators(bdaInfo, child);
            }
            else if (WebBeansConstants.WEB_BEANS_XML_INTERCEPTORS_ELEMENT.equalsIgnoreCase(child.getLocalName()))
            {
                fillInterceptors(bdaInfo, child);
            }
            else if (WebBeansConstants.WEB_BEANS_XML_ALTERNATIVES_ELEMENT.equalsIgnoreCase(child.getLocalName()))
            {
                fillAlternatives(bdaInfo, child);
            }
            else if (WebBeansConstants.WEB_BEANS_XML_SCAN_ELEMENT.equalsIgnoreCase(child.getLocalName()))
            {
                fillExcludes(bdaInfo, child);
            }
        }
    }

    private void fillDecorators(DefaultBeanArchiveInformation bdaInfo, Element decoratorsElement)
    {
        ElementIterator elit = new ElementIterator(decoratorsElement);
        while (elit.hasNext())
        {
            Element child = elit.next();
            if (WebBeansConstants.WEB_BEANS_XML_CLASS.equalsIgnoreCase(child.getLocalName()))
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
            if (WebBeansConstants.WEB_BEANS_XML_CLASS.equalsIgnoreCase(child.getLocalName()))
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
            if (WebBeansConstants.WEB_BEANS_XML_CLASS.equalsIgnoreCase(child.getLocalName()))
            {
                String clazz = child.getTextContent().trim();
                if (clazz.isEmpty())
                {
                    throw new WebBeansConfigurationException("alternatives <class> element must not be empty!");
                }
                bdaInfo.getAlternativeClasses().add(clazz);
            }
            if (WebBeansConstants.WEB_BEANS_XML_STEREOTYPE.equalsIgnoreCase(child.getLocalName()))
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


    private void fillExcludes(DefaultBeanArchiveInformation bdaInfo, Element scanElement)
    {
        final ClassLoader loader = Thread.currentThread().getContextClassLoader();
        final NodeList childNodes = scanElement.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++)
        {
            final Node nd = childNodes.item(i);
            if (!Element.class.isInstance(nd))
            {
                continue;
            }

            final Element child = Element.class.cast(nd);
            if (WebBeansConstants.WEB_BEANS_XML_EXCLUDE.equalsIgnoreCase(child.getLocalName()))
            {
                final String name = getTrimmedAttribute(child, "name");
                final NodeList children = child.getChildNodes();
                boolean skip = false;
                for (int j = 0; j < children.getLength(); j++)
                {
                    final Node ndChild = children.item(j);
                    if (!Element.class.isInstance(ndChild))
                    {
                        continue;
                    }

                    final Element condition = Element.class.cast(ndChild);

                    final String localName = condition.getLocalName();
                    if (WebBeansConstants.WEB_BEANS_XML_IF_CLASS_AVAILABLE.equalsIgnoreCase(localName))
                    {
                        if (!isClassAvailable(loader, getTrimmedAttribute(condition, "name")))
                        {
                            skip = true;
                            break;
                        }
                    }
                    else if (WebBeansConstants.WEB_BEANS_XML_IF_CLASS_NOT_AVAILABLE.equalsIgnoreCase(localName))
                    {
                        if (isClassAvailable(loader, getTrimmedAttribute(condition, "name")))
                        {
                            skip = true;
                            break;
                        }
                    }
                    else if (WebBeansConstants.WEB_BEANS_XML_IF_SYSTEM_PROPERTY.equalsIgnoreCase(localName))
                    {
                        final String value = getTrimmedAttribute(condition, "value");
                        final String systProp = System.getProperty(getTrimmedAttribute(condition, "name"));
                        if ((value == null && systProp == null) || !value.equals(systProp))
                        {
                            skip = true;
                            break;
                        }
                    }
                }
                if (skip)
                {
                    continue;
                }
                if (name.endsWith(".*"))
                {
                    // package exclude without sub-packages
                    bdaInfo.addClassExclude(name.substring(0, name.length() - 2));
                }
                else if (name.endsWith(".**"))
                {
                    // package exclude WITH sub-packages
                    bdaInfo.addPackageExclude(name.substring(0, name.length() - 3));
                }
                else
                {
                    // a simple Class
                    bdaInfo.addClassExclude(name);
                }
            }
        }
    }

    private static boolean isClassAvailable(final ClassLoader loader, final String name)
    {
        try
        {
            // no Class.forName(name) since it doesn't attach the classloader loader to the class in some cases
            loader.loadClass(name);
            return true;
        }
        catch (final Throwable e) // NoClassDefFoundError or ClassNotFoundException
        {
            return false;
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
    protected Element getBeansRootElement(InputStream xmlStream) throws WebBeansException
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

    /**
     * @return the trimmed attribute value, or <code>null</code> if the attribute does not exist or the attribute is empty
     */
    protected String getTrimmedAttribute(Element element, String attributeName)
    {
        String val = element.getAttribute(attributeName);
        if (val != null)
        {
            val = val.trim();
            if (!val.isEmpty())
            {
                return val;
            }
        }
        return null;
    }


}
