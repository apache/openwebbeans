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
package org.apache.webbeans.spi;

import java.net.URL;
import java.util.List;
import java.util.Set;

/**
 * This Service returns information about scanned beans.xml files.
 * The information needs to be available until {@link #release()}
 * gets invoked. This is usually at the end of the deployment phase.
 *
 * This SPI uses URLs as parameters because many virtual file
 * systems do not store their resources on a file system and also
 * are not able to move from the externalForm back to the original URL.
 */
public interface BeanArchiveService
{
    /**
     * Defines how CDI beans got discovered for each
     * JAR or ClassPath entry (aka BDA).
     */
    enum BeanDiscoveryMode
    {
        /**
         * Pick up all classes as CDI beans.
         * Classes with no 'bean defining annotations'
         * will get picked up as &#064;Dependent scoped beans.
         * This is basically the backward compatible mode to CDI-1.0.
         */
        ALL(10),

        /**
         * Only classes with a 'bean defining annotation' will get
         * picked up as CDI beans.
         * A 'bean defining annotation' is any CDI or atinject Scope annotation
         * as well as Stereotypes (the later only since CDI-1.2)
         */
        ANNOTATED(5),

        /**
         * Ignore all classes in this BDA when it comes to beans scanning.
         */
        NONE(2);

        /**
         * used for internal sorting. higher ordinal means more scanning
         */
        private final int ordinal;

        BeanDiscoveryMode(int ordinal)
        {
            this.ordinal = ordinal;
        }

        public int getOrdinal()
        {
            return ordinal;
        }

        public static BeanDiscoveryMode getByOrdinal(int ordinal)
        {
            for (BeanDiscoveryMode beanDiscoveryMode : values())
            {
                if (beanDiscoveryMode.getOrdinal() == ordinal)
                {
                    return beanDiscoveryMode;
                }
            }
            return null;
        }

        public static BeanDiscoveryMode max(BeanDiscoveryMode bdmA, BeanDiscoveryMode bdmB)
        {
            if (bdmA == null)
            {
                return bdmB;
            }
            if (bdmB == null)
            {
                return bdmA;
            }

            return getByOrdinal(Math.max(bdmA.getOrdinal(), bdmB.getOrdinal()));
        }
    }

    /**
     * Contains information about a single Bean Definition Archive (BDA).
     */
    interface BeanArchiveInformation
    {
        /**
         * @return the version string of the beans.xml file (if any), or <code>null</code> if not set
         */
        String getVersion();

        /**
         * @return the BeanDiscoveryMode used by this very BDA
         */
        BeanDiscoveryMode getBeanDiscoveryMode();

        /**
         * @return whether the given class is excluded from scanning or not
         */
        boolean isClassExcluded(String clazz);

        /**
         * @return whether the given package is excluded from scanning or not
         */
        boolean isPackageExcluded(String packageName);

        /**
         * @return the class name of the Interceptors defined in the beans.xml
         *          in a &lt;interceptors&gt;&lt;class&gt; section or an empty List.
         */
        List<String> getInterceptors();

        /**
         * @return the class name of Decorators defined in this beans.xml
         *          in a &lt;decorators&gt;&lt;class&gt; section or an empty List.
         */
        List<String> getDecorators();

        /**
         * @return the class name of the Alternatives defined in this beans.xml
         *          in a &lt;alternatives&gt;&lt;class&gt; section or an empty List.
         */
        List<String> getAlternativeClasses();

        /**
         * @return the class name of the Alternatives defined in this beans.xml
         *          in a &lt;alternatives&gt;&lt;stereotype&gt; section or an empty List.
         */
        List<String> getAlternativeStereotypes();


        List<String> getExcludedClasses();
        List<String> getExcludedPackages();
        List<String> getAllowProxyingClasses();
    }


    /**
     * The beanArchiveUrl might either point to a beans.xml file or the root of a JAR
     * or other ClassPath entry.  In case there is no beans.xml (implicit bean archive),
     * then we assume the 'default' behaviour of only scanning classes with
     * 'bean defining annotations'.
     * @return the {@link BeanArchiveInformation} of the given URL.
     * @see BeanDiscoveryMode
     */
    BeanArchiveInformation getBeanArchiveInformation(URL beanArchiveUrl);


    /**
     * This method is useful to later on know which JARs and ClassPath entries
     * did get scanned.
     * Please note that we use a special UrlSet internally which only holds
     * the externalForm as key and thus does no DNS resolving via the URL.
     * @return a Set of all registered Bean Archives.
     */
    Set<URL> getRegisteredBeanArchives();

    /**
     * Release the gathered information to free up memory.
     * This should get called at the end of the deployment phase.
     */
    void release();
}
