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

import org.apache.webbeans.logger.WebBeansLogger;
import org.apache.webbeans.util.WebBeansConstants;
import org.apache.webbeans.util.WebBeansUtil;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Resolver for the web beans systemId's.
 */
public class WebBeansResolver implements EntityResolver
{
    /** Logger instance */
    private static WebBeansLogger logger = WebBeansLogger.getLogger(WebBeansResolver.class);

    /**
     * Resolve entity.
     */
    public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException
    {
        logger.debug("Resolving systemId with : [{0}]", systemId);

        if (systemId.equals(WebBeansConstants.WEB_BEANS_XML_SYSID))
        {
            InputStream stream = getClass().getClassLoader().getResourceAsStream("org/apache/webbeans/web-beans-1.0.xsd");

            if (stream != null)
            {
                logger.debug("Resolving is successful with systemId : [{0}]", systemId);
                return createInputSource(stream, publicId, systemId);
            }
        }

        else if (systemId.startsWith(WebBeansConstants.CLASSPATH_URI_SCHEMA))
        {
            String path = systemId.replaceFirst("classpath:", "");
            InputStream stream = WebBeansUtil.getCurrentClassLoader().getResourceAsStream(path);

            if (stream != null)
            {
                logger.debug("Resolving is successful with systemId : [{0}]", systemId);
                return createInputSource(stream, publicId, systemId);
            }
        }

        logger.debug("Resolving failed using default SAXResolver for systemId : [{0}]", systemId);
        return null;
    }

    /**
     * Creates the new input source.
     */
    private InputSource createInputSource(InputStream stream, String publicId, String systemId)
    {
        InputSource source = new InputSource();
        source.setPublicId(publicId);
        source.setSystemId(systemId);
        source.setByteStream(stream);

        return source;

    }

}
