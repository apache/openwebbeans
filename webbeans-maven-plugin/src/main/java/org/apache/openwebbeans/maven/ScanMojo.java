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
package org.apache.openwebbeans.maven;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.DefaultDependencyResolutionRequest;
import org.apache.maven.project.DependencyResolutionException;
import org.apache.maven.project.DependencyResolutionRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.project.ProjectDependenciesResolver;
import org.apache.maven.repository.RepositorySystem;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilder;
import org.apache.xbean.finder.AnnotationFinder;
import org.apache.xbean.finder.archive.Archive;
import org.apache.xbean.finder.archive.ClasspathArchive;
import org.apache.xbean.finder.util.Files;
import org.eclipse.aether.graph.DependencyVisitor;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;

import static java.util.Arrays.asList;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.apache.maven.plugins.annotations.ResolutionScope.RUNTIME_PLUS_SYSTEM;

/**
 * Scan at build time the beans and generate an OWB configuration to use it.
 */
@Mojo(name = "scan", requiresDependencyResolution = RUNTIME_PLUS_SYSTEM)
public class ScanMojo extends AbstractMojo
{
    private static final String DEFAULT_DEP_EXCLUDES = "org.apache.johnzon:johnzon-," +
            "org.apache.xbean:," +
            "org.apache.openwebbeans:," +
            "org.apache.tomcat:tomcat-," +
            "org.apache.openjpa:openjpa," +
            "org.apache.geronimo.specs:," +
            "org.apache.commons:commons-," +
            "commons-.+:commons-.+," +
            "javax\\..+:.+," +
            "org.postgresql:," +
            "com.h2database:h2," +
            "org.checkerframework:," +
            "serp," +
            "slf4j-";

    @Parameter(property = "openwebbeans.skip", defaultValue = "false")
    private boolean skip;

    @Parameter(property = "openwebbeans.scanFolders", defaultValue = "false") // can move more than jars
    private boolean scanFolders;

    @Parameter(property = "openwebbeans.scopes", defaultValue = "compile,runtime")
    private Collection<String> scopes;

    @Parameter(property = "openwebbeans.libs")
    private Collection<String> libs;

    @Parameter(property = "openwebbeans.dependencies.includes")
    private Collection<String> dependenciesIncludes;

    @Parameter(property = "openwebbeans.dependencies.excludes", defaultValue = DEFAULT_DEP_EXCLUDES)
    private Collection<String> dependenciesExcludes;

    @Parameter(property = "openwebbeans.classes.excludes")
    private Collection<String> classesExcludes;

    @Parameter(defaultValue = "${project.build.outputDirectory}")
    private File outputDirectory;

    @Component
    private MavenProjectHelper projectHelper;

    @Component
    private RepositorySystem repositorySystem;

    @Component
    private ProjectDependenciesResolver dependenciesResolver;

    @Component
    private DependencyGraphBuilder graphBuilder;

    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;

    /**
     * Used to extend properties set by default.
     */
    @Parameter
    private Map<String, String> openwebbeansProperties;

    @Parameter(defaultValue = "CLASSES")
    private ScanMode mode;

    private Map<String, Pattern> patterns = new HashMap<>();

    @Override
    public void execute() throws MojoExecutionException
    {
        if (skip)
        {
            getLog().warn(getClass().getSimpleName() + " skipped");
            return;
        }

        final Collection<Path> files = new ArrayList<>();
        if (dependenciesExcludes.contains("{defaults}"))
        {
            dependenciesExcludes.remove("{defaults}");
            dependenciesExcludes.addAll(asList(DEFAULT_DEP_EXCLUDES.split(",")));
        }

        collectClasspath(files);
        if (!scanFolders)
        {
            files.removeIf(it -> java.nio.file.Files.isDirectory(it));
        }

        final List<URL> urls = files.stream().map(it ->
        {
            try
            {
                return it.toUri().toURL();
            }
            catch (final MalformedURLException e)
            {
                throw new IllegalStateException(e);
            }
        }).collect(toList());

        final Thread thread = Thread.currentThread();
        final ClassLoader oldLoader = thread.getContextClassLoader();
        final List<Scanned> scanned;
        try (final URLClassLoader loader = new URLClassLoader(urls.toArray(new URL[0])))
        {
            thread.setContextClassLoader(loader);
            scanned = urls.stream()
                    .map(url -> doScan(url, loader))
                    .collect(toList());
        }
        catch (final Exception e)
        {
            throw new MojoExecutionException(e.getMessage(), e);
        }
        finally
        {
            thread.setContextClassLoader(oldLoader);
        }

        final Properties properties = new Properties()
        {
            @Override // deterministic write, not elegant but simple, avoids to recode java.util.Properties.saveConvert
            public synchronized Enumeration<Object> keys()
            {
                final List<String> keys = new ArrayList<String>(List.class.cast(Collections.list(super.keys())));
                Collections.sort(keys);
                return Collections.enumeration(new ArrayList<>(keys));
            }

            @Override // same for java 11 and not 8
            public Set<Map.Entry<Object, Object>> entrySet()
            {
                return new LinkedHashSet<>(super.entrySet().stream()
                        .sorted(comparing(it -> String.valueOf(it.getKey())))
                        .collect(toList()));
            }
        };
        // meecrowave uses 1000 so let's override it
        // for now we don't override scanner since built-in ones are able to read these meta but later we could
        properties.setProperty("configuration.ordinal", "2000");
        switch (mode == null ? ScanMode.CLASSES : mode)
        {
            case CLASSES:
                if (!scanned.isEmpty())
                {
                    scanned.forEach(meta -> properties.setProperty(
                            "openwebbeans.buildtime.scanning." + meta.name + ".classes",
                            String.join(",", meta.classes)));
                }
                break;
            default:
                getLog().error("Unsupported scan mode: " + mode);
        }

        if (openwebbeansProperties != null) // easy way to customize a docker image or so let's support it
        {
            openwebbeansProperties.forEach(properties::setProperty);
        }

        final Path output = outputDirectory.toPath().resolve("META-INF/openwebbeans/openwebbeans.properties");
        try
        {
            java.nio.file.Files.createDirectories(output.getParent());
            try (final Writer writer = new BufferedWriter(java.nio.file.Files.newBufferedWriter(output))
            {
                @Override
                public void write(final String str) throws IOException
                {
                    if (!str.startsWith("#")) // skip date comment (and we don't care of other comments btw)
                    {
                        super.write(str);
                    }
                }
            })
            {
                // don't use store() to ensure it is reproducible (no date)
                properties.store(writer, "");
            }
        }
        catch (final IOException ioe)
        {
            throw new IllegalStateException(ioe);
        }
    }

    private Scanned doScan(final URL url, final ClassLoader loader)
    {
        final File file = Files.toFile(url);
        final Archive archive = ClasspathArchive.archive(loader, url);
        final Collection<String> classes = new AnnotationFinder(archive, false)
                .getAnnotatedClassNames().stream()
                .filter(it ->
                {
                    try
                    {
                        final Class<?> aClass = loader.loadClass(it);
                        return !aClass.isAnonymousClass() &&
                                !Modifier.isPrivate(aClass.getModifiers());
                    }
                    catch (final ClassNotFoundException | NoClassDefFoundError err)
                    {
                        return false;
                    }
                })
                .filter(it -> classesExcludes == null || classesExcludes.stream().noneMatch(it::startsWith))
                .sorted()
                .collect(toList());
        return new Scanned(file.getName(), classes);
    }

    protected void collectClasspath(final Collection<Path> files)
    {
        final Collection<String> includedArtifacts = project.getArtifacts().stream()
                .filter(this::isIncluded)
                .map(a ->
                {
                    files.add(a.getFile().toPath());
                    return a.getArtifactId();
                }).collect(toSet());
        libs.forEach(l ->
        {
            final boolean transitive = l.endsWith("?transitive");
            final String coords = transitive ? l.substring(0, l.length() - "?transitive".length()) : l;
            final String[] c = coords.split(":");
            if (c.length < 3 || c.length > 5)
            {
                throw new IllegalArgumentException("libs syntax is groupId:artifactId:version[:classifier][:type[?transitive]]");
            }
            if (!transitive)
            {
                files.add(resolve(c[0], c[1], c[2], c.length == 4 ? c[3] : ""));
            }
            else
                {
                addTransitiveDependencies(files, includedArtifacts, new Dependency()
                {{
                    setGroupId(c[0]);
                    setArtifactId(c[1]);
                    setVersion(c[2]);
                    if (c.length == 4 && !"-".equals(c[3]))
                    {
                        setClassifier(c[3]);
                    }
                    if (c.length == 5)
                    {
                        setType(c[4]);
                    }
                }});
            }
        });
    }

    private void addTransitiveDependencies(final Collection<Path> aggregator,
                                           final Collection<String> includedArtifacts, final Dependency dependency)
    {
        final DependencyResolutionRequest request = new DefaultDependencyResolutionRequest();
        request.setMavenProject(new MavenProject()
        {{
            getDependencies().add(dependency);
        }});
        try
        {
            dependenciesResolver
                    .resolve(request)
                    .getDependencyGraph()
                    .accept(new DependencyVisitor()
                    {
                        @Override
                        public boolean visitEnter(final org.eclipse.aether.graph.DependencyNode node)
                        {
                            return true;
                        }

                        @Override
                        public boolean visitLeave(final org.eclipse.aether.graph.DependencyNode node)
                        {
                            final org.eclipse.aether.artifact.Artifact artifact = node.getArtifact();
                            if (artifact != null && includedArtifacts.add(artifact.getArtifactId()))
                            {
                                aggregator.add(artifact.getFile().toPath());
                            }
                            return true;
                        }
                    });
        }
        catch (final DependencyResolutionException e)
        {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    private Path resolve(final String group, final String artifact, final String version, final String classifier)
    {
        final DefaultArtifact art = new DefaultArtifact(
                group, artifact, version, "compile", "jar", classifier, new DefaultArtifactHandler());
        final ArtifactResolutionRequest artifactRequest = new ArtifactResolutionRequest()
                .setArtifact(art);
        final ArtifactResolutionResult result = repositorySystem.resolve(artifactRequest);
        if (!result.isSuccess())
        {
            throw new IllegalStateException("Can't find " + art + ", please add it to the pom.");
        }
        return result.getArtifacts().iterator().next().getFile().toPath();
    }


    private boolean isIncluded(final Artifact a)
    {
        return (!((scopes == null &&
                !(Artifact.SCOPE_COMPILE.equals(a.getScope()) || Artifact.SCOPE_RUNTIME.equals(a.getScope())))
                || (scopes != null && !scopes.contains(a.getScope())))) &&
                isExplicitlyIncluded(a);
    }

    private boolean isExplicitlyIncluded(final Artifact art)
    {
        if (dependenciesExcludes.isEmpty() && dependenciesIncludes.isEmpty())
        {
            return true;
        }
        final String coord = art.getGroupId() + ':' + art.getArtifactId();
        final String artOnly = art.getArtifactId();
        if (!dependenciesIncludes.isEmpty() && dependenciesExcludes.isEmpty())
        {
            return dependenciesIncludes.stream()
                    .anyMatch(it -> compare(coord, artOnly, it));
        }
        if (dependenciesIncludes.isEmpty() && !dependenciesExcludes.isEmpty())
        {
            return dependenciesExcludes.stream()
                    .noneMatch(it -> compare(coord, artOnly, it));
        }
        final boolean forced = dependenciesIncludes.stream()
                .anyMatch(it -> compare(coord, artOnly, it));
        if (forced)
        {
            return true;
        }
        final boolean notExcluded = dependenciesExcludes.stream()
                .noneMatch(it -> compare(coord, artOnly, it));
        return notExcluded;
    }

    private boolean compare(final String coord, final String artOnly, final String conf)
    {
        return coord.startsWith(conf) || artOnly.startsWith(conf) || patterns.computeIfAbsent(conf, k ->
        {
            try
            {
                return Pattern.compile(k);
            }
            catch (final Exception e)
            {
                // whatever, ignore pattern
                return Pattern.compile("/\\\\");
            }
        }).matcher(coord).matches();
    }

    private static class Scanned
    {
        private final String name;
        private final Collection<String> classes;

        private Scanned(final String name, final Collection<String> classes)
        {
            this.name = name;
            this.classes = classes;
        }
    }

    // this will be used to add new mode like "FULLY_PRE_SCANNED"
    // where we wouldn't use the classloader but only the meta to scan
    public enum ScanMode
    {
        // means we map the list of classes per jar,
        // it enables to speed up scanning and keep extensibility of CDI
        CLASSES
    }
}
