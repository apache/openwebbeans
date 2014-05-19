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
package org.apache.webbeans.inject;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Priority;
import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.spi.Bean;

import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.util.AnnotationUtil;

/**
 * This class has 2 responsibilities.
 *
 * 1.) to collect information about configured &#064;Alternatives at boot time
 *
 * 2.) answer if a class is an enabled Alternative.
 * This is needed for {@link org.apache.webbeans.container.BeanManagerImpl#resolve(java.util.Set)}
 *
 * The boot order for 1.) is to first register all the alternatives and stereotypes
 * from the XML.
 * After that the AnnotatedType scanning is performed and all &#064;Alternatives with
 * &#064;Priority get added as well. We will also add classes which have an Alternative stereotype.
 *
 * After the AnnotatedTypes got scanned we have to fire the {@link javax.enterprise.inject.spi.AfterTypeDiscovery}
 * event with the collected <i>effective</i> alternative classes sorted by their priority.
 * Any extension can re-order the alternatives which then form the base of the resolve() handling
 * at runtime.
 */
public class AlternativesManager
{

    private final WebBeansContext webBeansContext;

    /**
     * All the stereotypes which are configured via XML &lt;class&gt;
     */
    private final Set<Class<?>> configuredAlternatives = new HashSet<Class<?>>();

    /**
     * All Stereotypes which are declared as &#064;Alternative in a beans.xml.
     * Please note that &#064;Priority on a stereotype does <b>not</b> automatically enable it!
     */
    private final Set<Class<? extends Annotation>> configuredStereotypeAlternatives = new HashSet<Class<? extends Annotation>>();

    /**
     * All the stereotypes which are either configured via XML &lt;class&gt; or
     * have a &#064;Priority annotation.
     * key: the class
     * value: the priority. Alternatives from beans.xml have -1 as they are lowest prio.
     */
    private final List<PriorityAlternative> priorityAlternatives = new ArrayList<PriorityAlternative>();

    private List<Class<?>> prioritizedAlternatives = null;



    public AlternativesManager(WebBeansContext webBeansContext)
    {
        this.webBeansContext = webBeansContext;
    }



    /**
     * This methods gets called while scanning the various beans.xml files.
     * It registers a &lt;stereotype&gt; alternative.
     */
    public void addXmlStereoTypeAlternative(Class<?> alternative)
    {                
        if(Annotation.class.isAssignableFrom(alternative))
        {
            Class<? extends Annotation> stereo = (Class<? extends Annotation>)alternative;
            boolean ok = false;
            if(webBeansContext.getAnnotationManager().isStereoTypeAnnotation(stereo))
            {
                if(AnnotationUtil.hasClassAnnotation(stereo, Alternative.class))
                {
                    ok = true;

                    configuredStereotypeAlternatives.add(stereo);
                }
            }
            
            if(!ok)
            {
                throw new WebBeansConfigurationException("Given stereotype class : " + alternative.getName() + " is not annotated with @Alternative" );
            }
        }
        else
        {
            throw new WebBeansConfigurationException("Given stereotype class : " + alternative.getName() + " is not an annotation" );
        }        
    }

    /**
     * This methods gets called while scanning the various beans.xml files.
     * It registers a &lt;class&gt; alternative.
     */
    public void addXmlClazzAlternative(Class<?> alternative)
    {
        if(AnnotationUtil.hasClassAnnotation(alternative, Alternative.class))
        {
            configuredAlternatives.add(alternative);
        }
        else
        {
            throw new WebBeansConfigurationException("Given class : " + alternative.getName() + " is not annotated with @Alternative" );
        }
    }

    /**
     * This method is used to add Alternatives which have a &#064;Priority annotation.
     * This is performed after the ProcessAnnotatedType events got fired.
     */
    public void addPriorityClazzAlternative(Class<?> clazz, Priority priority)
    {
        priorityAlternatives.add(new PriorityAlternative(clazz, priority.value()));
    }

    /**
     * Alternatives get ordered by their priority and as lowest priority all
     * the alternatives added via XML get added.
     * @return the list of sorted alternatives
     */
    public List<Class<?>> getPrioritizedAlternatives()
    {
        if (prioritizedAlternatives == null)
        {
            Collections.sort(priorityAlternatives);

            prioritizedAlternatives = new ArrayList<Class<?>>(priorityAlternatives.size());

            for (PriorityAlternative priorityAlternative : priorityAlternatives)
            {
                // add in reverse order
                prioritizedAlternatives.add(priorityAlternative.clazz);
            }
        }

        return prioritizedAlternatives;
    }


    /**
     * @return <code>true</code> if the given bean is a configured alternative
     */
    public boolean isAlternative(Bean<?> bean)
    {
        return isAlternative(bean.getBeanClass(), bean.getStereotypes());
    }

    /**
     * @return <code>true</code> if the given bean is a configured alternative
     */
    public boolean isAlternative(Class<?> beanType, Set<Class<? extends Annotation>> stereotypes)
    {
        if(configuredAlternatives.contains(beanType) ||
           prioritizedAlternatives.contains(beanType))
        {
            return true;
        }
        
        for(Class<? extends Annotation> ann : stereotypes)
        {
            if(configuredStereotypeAlternatives.contains(ann))
            {
                return true;
            }
        }


        return false;
    }
    
    public void clear()
    {
        configuredAlternatives.clear();
        configuredStereotypeAlternatives.clear();
        priorityAlternatives.clear();

        prioritizedAlternatives = null;
    }

    private static class PriorityAlternative implements Comparable<PriorityAlternative>
    {
        private final int priority;
        private final Class<?> clazz;

        public PriorityAlternative(Class<?> clazz, int priority)
        {
            this.clazz = clazz;
            this.priority = priority;
        }

        @Override
        public int compareTo(PriorityAlternative o)
        {
            if (priority != o.priority)
            {
                // sort descending
                return (priority > o.priority) ? -1 : ((priority == o.priority) ? 0 : 1);
            }

            // we additionally sort according to the class name to at least
            // prevent randomness if 2 classes have the same ordinal.
            // see CDI-437 for more info about why it's broken in CDI-1.1.
            return clazz.getName().compareTo(o.clazz.getName());
        }
    }
}
