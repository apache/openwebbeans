<div align="center">
<img src="https://openwebbeans.apache.org/resources/images/logo.png" width="300" />
</div>
<br>

![Maven Central](https://img.shields.io/maven-central/v/org.apache.openwebbeans/openwebbeans-impl)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Build Status](https://github.com/apache/openwebbeans/workflows/CI/badge.svg)](https://github.com/apache/openwebbeans/actions)
[![Build Status ASF](https://ci-builds.apache.org/buildStatus/icon?job=OpenWebBeans%2FOpenWebBeans+CI+%28main%29&subject=ASF-Build)](https://ci-builds.apache.org/job/OpenWebBeans/job/OpenWebBeans%20CI%20%28main%29/)

Apache's implementation of the Contexts and Dependency Injection (CDI) / Jakarta CDI specification

## Branches

### main
Jakarta CDI 4.1 implementation (Jakarta EE 11)  
Requires Java 17+

### owb_4.0.x
![owb_4.0.x](https://img.shields.io/maven-central/v/org.apache.openwebbeans/openwebbeans-impl?versionPrefix=4.0&color=cyan)  
Jakarta CDI 4.0 implementation  
Requires Java 11+

### owb_2.0.x
![owb_2.0.x](https://img.shields.io/maven-central/v/org.apache.openwebbeans/openwebbeans-impl?versionPrefix=2.0&color=cyan)  
CDI 2.0 (JSR-365) implementation  
Requires Java 8+.

### owb_1.7.x
![owb_1.7.x](https://img.shields.io/maven-central/v/org.apache.openwebbeans/openwebbeans-impl?versionPrefix=1.7&color=cyan)  
CDI 1.2 (JSR-346) implementation  
Requires Java 7+.

### owb_1.2.x
![owb_1.2.x](https://img.shields.io/maven-central/v/org.apache.openwebbeans/openwebbeans-impl?versionPrefix=1.2&color=cyan)  
CDI 1.0 (JSR-299) implementation  
Requires Java 5+.

## Minimum Requirements (main)

- Java 17+
- CDI 4.1 (Jakarta CDI / Jakarta EE 11)
- TCK compliant

## Installation

```
mvn clean install
```

## Usage

### Core Dependency

```xml
<dependency>
    <groupId>org.apache.openwebbeans</groupId>
    <artifactId>openwebbeans-impl</artifactId>
    <version>${owb.version}</version>
</dependency>
<dependency>
    <groupId>org.apache.openwebbeans</groupId>
    <artifactId>openwebbeans-spi</artifactId>
    <version>${owb.version}</version>
</dependency>
```

### Web / Servlet Integration

```xml
<dependency>
    <groupId>org.apache.openwebbeans</groupId>
    <artifactId>openwebbeans-web</artifactId>
    <version>${owb.version}</version>
</dependency>
```

### JSF Integration

```xml
<dependency>
    <groupId>org.apache.openwebbeans</groupId>
    <artifactId>openwebbeans-jsf</artifactId>
    <version>${owb.version}</version>
</dependency>
```

### Tomcat Integration

```xml
<dependency>
    <groupId>org.apache.openwebbeans</groupId>
    <artifactId>openwebbeans-tomcat</artifactId>
    <version>${owb.version}</version>
</dependency>
```

### Arquillian Integration (Testing)

```xml
<dependency>
    <groupId>org.apache.openwebbeans</groupId>
    <artifactId>openwebbeans-arquillian</artifactId>
    <version>${owb.version}</version>
    <scope>test</scope>
</dependency>
```

### JUnit 5 Integration (Testing)

```xml
<dependency>
    <groupId>org.apache.openwebbeans</groupId>
    <artifactId>openwebbeans-junit5</artifactId>
    <version>${owb.version}</version>
    <scope>test</scope>
</dependency>
```

## Modules

OWB is modularly built — a full CDI container in under 1 MB total. Available modules include:

- `openwebbeans-impl` — core CDI container implementation
- `openwebbeans-spi` — SPI interfaces
- `openwebbeans-web` — Servlet / web container integration
- `openwebbeans-jsf` — Jakarta Faces integration
- `openwebbeans-tomcat` — Apache Tomcat integration
- `openwebbeans-jetty9` — Jetty 9 integration
- `openwebbeans-ejb` — EJB integration
- `openwebbeans-jms` — JMS integration
- `openwebbeans-el22` — EL 2.2+ integration
- `openwebbeans-osgi` — OSGi environment support
- `openwebbeans-se` — Java SE standalone support
- `openwebbeans-slf4j` — SLF4J logging bridge
- `openwebbeans-junit5` — JUnit 5 test support
- `openwebbeans-arquillian` — Arquillian test support
- `openwebbeans-gradle` — Gradle build support

## Java SE Usage

OWB can run as a standalone CDI container in plain Java SE applications (Swing, JavaFX, Eclipse RCP, etc.) without any application server or servlet container.

## More Information

Please visit https://openwebbeans.apache.org for full documentation, examples, and release notes.

Issue tracker: https://issues.apache.org/jira/browse/OWB
