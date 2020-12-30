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

import java.util.stream.Stream;

/**
 * We just extend the default AnnotationFinder to get Access to the original ClassInfo
 * for not having to call loadClass so often...
 */
public class OwbAnnotationFinder extends AnnotationFinder
{
    public OwbAnnotationFinder(Archive archive, boolean checkRuntimeAnnotation)
    {
        super(archive, checkRuntimeAnnotation);
    }

    public OwbAnnotationFinder(Archive archive)
    {
        super(archive);
    }

    public OwbAnnotationFinder(final Class<?>[] classes)
    {
        super(new ClassesArchive(/*empty since we want to read from reflection, not from resources*/));
        Stream.of(classes).forEach(c -> super.readClassDef(c));
    }

    public ClassInfo getClassInfo(String className)
    {
        return classInfos.get(className);
    }

}
