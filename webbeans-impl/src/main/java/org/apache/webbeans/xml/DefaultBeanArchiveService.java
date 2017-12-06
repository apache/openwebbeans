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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
import org.apache.xbean.finder.archive.FileArchive;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Please note that this implementation is not thread safe.
 */
public class DefaultBeanArchiveService implements BeanArchiveService
{
    private static final String WEB_INF_CLASSES = "WEB-INF/classes/";
    private static final String WEB_INF_CLASSES_MAVEN = "target/classes/";
    private static final String META_INF_BEANS_XML = "/META-INF/beans.xml";

    private static final Logger logger = WebBeansLoggerFacade.getLogger(BeanArchiveService.class);

    /**
     * Contains a map from the URL externalForm to the stored BeanArchiveInformation
     */
    private Map<String, BeanArchiveInformation> beanArchiveInformations = new HashMap<>();

    private UrlSet registeredBeanArchives = new UrlSet();


    @Override
    public BeanArchiveInformation getBeanArchiveInformation(URL beanArchiveUrl)
    {
        // Sometimes we need to decode the URL. See OWB-1220
        String beanArchiveLocation = FileArchive.decode(beanArchiveUrl.toExternalForm());

        BeanArchiveInformation bdaInfo = beanArchiveInformations.get(beanArchiveLocation);

        if (bdaInfo == null && !beanArchiveLocation.contains(".xml"))
        {
            // probably the beanArchiveUrl is a JAR classpath and not a beans.xml itself
            // in this case we need to look whether we have a corresponding beans.xml already scanned

            String strippedBeanArchiveUrl = stripProtocol(beanArchiveLocation);
            strippedBeanArchiveUrl = stripTrailingSlash(strippedBeanArchiveUrl);

            for (Map.Entry<String, BeanArchiveInformation> entry : beanArchiveInformations.entrySet())
            {
                // we have to 'normalise' both values and remove the beans.xml string to be compareable
                String entryUrl = stripProtocol(entry.getKey());
                if (entryUrl.length(   ) > META_INF_BEANS_XML.length() &&
                    entryUrl.substring(entryUrl.length() - META_INF_BEANS_XML.length()).equalsIgnoreCase(META_INF_BEANS_XML))
                {
                    entryUrl = entryUrl.substring(0, entryUrl.length() - META_INF_BEANS_XML.length());
                    entryUrl = stripTrailingSlash(entryUrl);
                }
                if (entryUrl.equals(strippedBeanArchiveUrl))
                {
                    bdaInfo = entry.getValue();
                    break;
                }
            }

        }

        if (bdaInfo == null
                && (beanArchiveLocation.contains(WEB_INF_CLASSES) || beanArchiveLocation.contains(WEB_INF_CLASSES_MAVEN)))
        {
            // this is a very special case for beans.xml in a WAR file
            // in this case we need to merge the 2 BDAs from WEB-INF/classes/META-INF/beans.xml and WEB-INF/beans.xml
            // this requires the WEB-INF/beans.xml being parsed first (which is usually the case)

            // first we read the BeanArchiveInformation from the WEB-INF/classes directory.
            bdaInfo = readBeansXml(beanArchiveUrl, beanArchiveLocation);

            // next we merge in the BDAInfo from WEB-INF/beans.xml
            bdaInfo = mergeWithWebInfBeansXml(bdaInfo);
            beanArchiveInformations.put(beanArchiveLocation, bdaInfo);
            registeredBeanArchives.add(beanArchiveUrl);
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

    /**
     * Merge the BDA info from webInfClasses with the one from WEB-INF/beans.xml
     */
    private BeanArchiveInformation mergeWithWebInfBeansXml(BeanArchiveInformation bdaWebClasses)
    {
        BeanArchiveInformation bdaWebInf = null;
        for (Map.Entry<String, BeanArchiveInformation> entry : beanArchiveInformations.entrySet())
        {
            if (entry.getKey().endsWith("WEB-INF/beans.xml"))
            {
                bdaWebInf = entry.getValue();
                break;
            }
        }

        if (bdaWebInf == null)
        {
            // no merge needed
            return bdaWebClasses;
        }

        // means we need to merge them
        DefaultBeanArchiveInformation mergedBdaInfo = new DefaultBeanArchiveInformation(bdaWebClasses.getBdaUrl());

        mergedBdaInfo.setBeanDiscoveryMode(BeanDiscoveryMode.max(bdaWebClasses.getBeanDiscoveryMode(), bdaWebInf.getBeanDiscoveryMode()));

        mergedBdaInfo.setVersion(bdaWebClasses.getVersion() != null ? bdaWebClasses.getVersion() : bdaWebInf.getVersion());

        mergedBdaInfo.setExcludedClasses(mergeLists(bdaWebClasses.getExcludedClasses(), bdaWebInf.getExcludedClasses()));
        mergedBdaInfo.setExcludedPackages(mergeLists(bdaWebClasses.getExcludedPackages(), bdaWebInf.getExcludedPackages()));

        mergedBdaInfo.setInterceptors(mergeLists(bdaWebClasses.getInterceptors(), bdaWebInf.getInterceptors()));
        mergedBdaInfo.setDecorators(mergeLists(bdaWebClasses.getDecorators(), bdaWebInf.getDecorators()));
        mergedBdaInfo.getAlternativeClasses().addAll(mergeLists(bdaWebClasses.getAlternativeClasses(), bdaWebInf.getAlternativeClasses()));
        mergedBdaInfo.getAlternativeStereotypes().addAll(mergeLists(bdaWebClasses.getAlternativeStereotypes(), bdaWebInf.getAlternativeStereotypes()));

        // we do NOT merge the allowProxying as they just stack up anyway

        return mergedBdaInfo;
    }

    private List<String> mergeLists(List<String> list1, List<String> list2)
    {
        if (list1 == null || list1.isEmpty())
        {
            return list2;
        }

        if (list2 == null)
        {
            return null;
        }

        List<String> mergedList = new ArrayList<>(list1);

        for (String val : list2)
        {
            if (!mergedList.contains(val))
            {
                mergedList.add(val);
            }
        }

        return mergedList;
    }

    @Override
    public Set<URL> getRegisteredBeanArchives()
    {
        return registeredBeanArchives;
    }

    /**
     * This method exists for extensibility reasons.
     */
    protected DefaultBeanArchiveInformation createBeanArchiveInformation(String bdaUrl)
    {
        return new DefaultBeanArchiveInformation(bdaUrl);
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
            DefaultBeanArchiveInformation bdaInfo = createBeanArchiveInformation(beansXmlLocation);
            bdaInfo.setBeanDiscoveryMode(BeanDiscoveryMode.ANNOTATED);
            return bdaInfo;
        }


        InputStream xmlStream = null;
        try
        {
            xmlStream = beansXmlUrl.openStream();

            return readBeansXml(xmlStream, beansXmlUrl.toExternalForm());

        }
        catch (Exception e)
        {
            throw new WebBeansDeploymentException("Error while parsing the beans.xml file " + beansXmlLocation, e);
        }
        finally
        {
            try
            {
                if (xmlStream != null)
                {
                    xmlStream.close();
                }
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

    private String stripTrailingSlash(String urlPath)
    {
        while (urlPath.endsWith("/") || urlPath.endsWith("!") || urlPath.endsWith("\\"))
        {
            urlPath = urlPath.substring(0, urlPath.length() - 1);
        }
        return urlPath;
    }




    /**
     * Read the information from the given beans.xml and fill it into a
     * BeanArchiveInformation instance.
     */
    protected BeanArchiveInformation readBeansXml(InputStream xmlStreamIn, String beansXmlLocation) throws IOException
    {
        DefaultBeanArchiveInformation bdaInfo = createBeanArchiveInformation(beansXmlLocation);

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
                        throw new WebBeansConfigurationException("beans.xml must have a <beans> root element, but has: " + webBeansRoot.getLocalName() +
                                                                 " in " + beansXmlLocation);
                    }

                    bdaInfo.setVersion(getTrimmedAttribute(webBeansRoot, "version"));

                    String beanDiscoveryMode = getTrimmedAttribute(webBeansRoot, "bean-discovery-mode");
                    bdaInfo.setBeanDiscoveryMode(beanDiscoveryMode != null ? BeanDiscoveryMode.valueOf(beanDiscoveryMode.toUpperCase()) : null);

                    readBeanChildren(bdaInfo, webBeansRoot, beansXmlLocation);
                }


                if (bdaInfo.getVersion() != null && !"1.0".equals(bdaInfo.getVersion()) && bdaInfo.getBeanDiscoveryMode() == null)
                {
                    throw new WebBeansConfigurationException("beans.xml with version 1.1 and higher must declare a bean-discovery-mode! url=" + beansXmlLocation);
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

    private void readBeanChildren(DefaultBeanArchiveInformation bdaInfo, Element webBeansRoot, String beansXmlLocation)
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
            else if (WebBeansConstants.WEB_BEANS_XML_ALLOW_PROXYING_ELEMENT.equalsIgnoreCase(child.getLocalName()))
            {
                fillAllowProxying(bdaInfo, child);
            }
            else if (WebBeansConstants.WEB_BEANS_XML_SCOPED_BEANS_ONLY_ELEMENT.equalsIgnoreCase(child.getLocalName()))
            {
                logger.log(Level.FINE, "trimmed bean archive detected: " + beansXmlLocation);
                bdaInfo.setBeanDiscoveryMode(BeanDiscoveryMode.TRIM);
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
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        NodeList childNodes = scanElement.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++)
        {
            Node nd = childNodes.item(i);
            if (!Element.class.isInstance(nd))
            {
                continue;
            }

            Element child = Element.class.cast(nd);
            if (WebBeansConstants.WEB_BEANS_XML_EXCLUDE.equalsIgnoreCase(child.getLocalName()))
            {
                String name = getTrimmedAttribute(child, "name");
                NodeList children = child.getChildNodes();
                boolean skip = false;
                for (int j = 0; j < children.getLength(); j++)
                {
                    Node ndChild = children.item(j);
                    if (!Element.class.isInstance(ndChild))
                    {
                        continue;
                    }

                    Element condition = Element.class.cast(ndChild);

                    String localName = condition.getLocalName();
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
                        String value = getTrimmedAttribute(condition, "value");
                        String systProp = System.getProperty(getTrimmedAttribute(condition, "name"));
                        if ((value == null && systProp == null) || !(value != null && value.equals(systProp)))
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

    private void fillAllowProxying(DefaultBeanArchiveInformation bdaInfo, Element allowProxyingElement)
    {
        ElementIterator elit = new ElementIterator(allowProxyingElement);
        while (elit.hasNext())
        {
            Element child = elit.next();
            if (WebBeansConstants.WEB_BEANS_XML_CLASS.equalsIgnoreCase(child.getLocalName()))
            {
                String clazz = child.getTextContent().trim();
                if (clazz.isEmpty())
                {
                    throw new WebBeansConfigurationException("allowProxying <class> element must not be empty!");
                }
                bdaInfo.getAllowProxyingClasses().add(clazz);
            }
        }
    }



    private static boolean isClassAvailable(ClassLoader loader, String name)
    {
        try
        {
            // no Class.forName(name) since it doesn't attach the classloader loader to the class in some cases
            loader.loadClass(name);
            return true;
        }
        catch (Throwable e) // NoClassDefFoundError or ClassNotFoundException
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
