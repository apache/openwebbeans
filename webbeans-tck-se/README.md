<!--
    Licensed to the Apache Software Foundation (ASF) under one or more
    contributor license agreements. See the NOTICE file distributed with
    this work for additional information regarding copyright ownership.
    The ASF licenses this file to You under the Apache License, Version
    2.0 (the "License"); you may not use this file except in compliance
    with the License. You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0 Unless required by
    applicable law or agreed to in writing, software distributed under the
    License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
    CONDITIONS OF ANY KIND, either express or implied. See the License for
    the specific language governing permissions and limitations under the
    License.
-->
# Jakarta CDI SE TCK runner

Runs the SE part of the Jakarta Contexts and Dependency Injection TCK
(`org.jboss.cdi.tck.tests.se.*`) against OpenWebBeans.

    mvn test -pl webbeans-tck-se

The specification this is checked against is
[Jakarta Contexts and Dependency Injection 4.1](https://jakarta.ee/specifications/cdi/4.1/)
(part of Jakarta EE 11); section numbers below refer to the
[4.1 specification document](https://jakarta.ee/specifications/cdi/4.1/jakarta-cdi-spec-4.1.html).

## Why this is a separate module

The other TCK suites live in `webbeans-tck` and run in the in-VM
`owb-arquillian-standalone` container. The SE tests cannot: they bootstrap
`SeContainerInitializer` themselves and build their deployment with
`org.jboss.arquillian.container.se.api.ClassPath`, which only the Arquillian
*SE managed* container understands. That container forks a fresh JVM per
deployment and talks to it over JMX.

Arquillian discovers `DeployableContainer` implementations through the
`ServiceLoader` and aborts with

    Multiple service implementations found for interface ...DeployableContainer

as soon as two of them are on one classpath. That happens during bootstrap,
before `arquillian.launch` or the `arquillian.xml` container qualifier are
consulted, so the two containers cannot be separated by configuration - they
need separate classpaths, i.e. separate modules.

## How the forked JVM is set up

* `maven-dependency-plugin` copies all dependencies into the staging directory
  (`target/se-libs`). `ManagedSEDeployableContainer` puts every `*.jar` in that
  directory on the classpath of the JVM it forks.
* That directory is defined once, by the `owb.se.tck.librariesPath` property in
  `pom.xml`. The two files that also have to name it -
  `arquillian.xml` (`librariesPath`) and `META-INF/cdi-tck.properties`
  (`org.jboss.cdi.tck.libraryDirectory`) - get it via test resource filtering,
  so the path is not repeated as a literal anywhere. Filtering expands it to an
  absolute path, which the SE container handles fine (it does
  `new File(librariesPath)`).
* `maven-antrun-plugin` additionally zips this module's own compiled
  `src/main/resources` into `target/se-libs/openwebbeans-tck-se-config.jar`.
  That is the only way to get
  `META-INF/openwebbeans/openwebbeans.properties` in front of OWB inside the
  forked JVM - `src/test/resources` is never on that JVM's classpath, and a
  `-D` system property does not work either because
  `OpenWebBeansConfiguration.overrideWithGlobalSettings()` only consults
  `System.getProperties()` for keys that already exist in a configuration file.
* `container-se-managed` is old (1.0.2.Final, 2017) and still depends on
  Arquillian 1.1.15.Final. Those transitive artifacts are excluded in `pom.xml`
  and re-declared at `${arquillian.version}`, otherwise the run fails with
  `NoClassDefFoundError` on SPI types that only exist in current Arquillian.
