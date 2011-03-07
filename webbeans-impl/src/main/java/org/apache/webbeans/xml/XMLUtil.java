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

import org.apache.webbeans.config.OWBLogConst;
import org.apache.webbeans.exception.WebBeansException;
import org.apache.webbeans.logger.WebBeansLogger;
import org.apache.webbeans.util.Asserts;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Element;


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


}

