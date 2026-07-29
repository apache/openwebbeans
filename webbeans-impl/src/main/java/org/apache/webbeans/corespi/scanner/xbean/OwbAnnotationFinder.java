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
package org.apache.webbeans.corespi.scanner.xbean;

import org.apache.xbean.finder.AnnotationFinder;
import org.apache.xbean.finder.archive.Archive;
import org.apache.xbean.finder.archive.ClassesArchive;

import org.apache.xbean.finder.util.SingleLinkedList;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * We just extend the default AnnotationFinder to get Access to the original ClassInfo
 * for not having to call loadClass so often...
 */
public class OwbAnnotationFinder extends AnnotationFinder
{
    public OwbAnnotationFinder(CdiArchive archive, boolean checkRuntimeAnnotation)
    {
        super(archive, checkRuntimeAnnotation);
    }

    public OwbAnnotationFinder(Archive archive)
    {
        super(archive);
    }

    public CdiArchive getCdiArchive()
    {
        return (CdiArchive) getArchive();
    }

    public OwbAnnotationFinder(final Class<?>[] classes)
    {
        super(new ClassesArchive(/*empty since we want to read from reflection, not from resources*/));
        try
        {
            final Field linking = AnnotationFinder.class.getDeclaredField("linking");
            if (!linking.isAccessible())
            {
                linking.setAccessible(true);
            }
            linking.set(this, true);
        }
        catch (final Exception e)
        {
            // ignore, will not affect all cases
        }
        Stream.of(classes).forEach(super::readClassDef);
    }

    public ClassInfo getClassInfo(String className)
    {
        return classInfos.get(className);
    }

    /**
     * Absorbs another finder which scanned a disjoint slice of the same bean archive.
     * Must be called before any find/link usage.
     */
    public void merge(final OwbAnnotationFinder other)
    {
        classInfos.putAll(other.classInfos);
        originalInfos.putAll(other.originalInfos);
        for (final Map.Entry<String, List<Info>> entry : other.annotated.entrySet())
        {
            // no addAll, values can be xbean SingleLinkedList
            final List<Info> target = annotated.computeIfAbsent(entry.getKey(), k -> new SingleLinkedList<>());
            for (final Info info : entry.getValue())
            {
                target.add(info);
            }
        }
        other.getCdiArchive().classesByUrl().forEach((url, foundClasses) ->
        {
            final CdiArchive.FoundClasses base = getCdiArchive().classesByUrl().get(url);
            if (base != null)
            {
                base.getClassNames().addAll(foundClasses.getClassNames());
            }
        });
    }
}
