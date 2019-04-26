DO NOT EDIT - See: https://www.eclipse.org/jetty/documentation/current/startup-modules.html

[description]
Jetty setup to support Apache OpenWebBeans for CDI2 inside the webapp

[depend]
plus
deploy

[files]
maven://org.apache.openwebbeans/openwebbeans-spi/${apache-owb.version}|lib/apache-owb/openwebbeans-spi-${apache-owb.version}.jar
maven://org.apache.openwebbeans/openwebbeans-impl/${apache-owb.version}|lib/apache-owb/openwebbeans-impl-${apache-owb.version}.jar
maven://org.apache.openwebbeans/openwebbeans-web/${apache-owb.version}|lib/apache-owb/openwebbeans-web-${apache-owb.version}.jar
maven://org.apache.openwebbeans/openwebbeans-el22/${apache-owb.version}|lib/apache-owb/openwebbeans-el22-${apache-owb.version}.jar
maven://org.apache.openwebbeans/openwebbeans-jetty9/${apache-owb.version}|lib/apache-owb/openwebbeans-jetty9-${apache-owb.version}.jar
maven://org.apache.geronimo.specs/geronimo-jcdi_2.0_spec/${geronimo-cdi-spec.version}|lib/apache-owb/geronimo-jcdi_2.0_spec-${geronimo-cdi-spec.version}.jar
maven://org.apache.geronimo.specs/geronimo-atinject_1.0_spec/${geronimo-atinject-spec.version}|lib/apache-owb/geronimo-atinject_1.0_spec-${geronimo-atinject-spec.version}.jar
maven://org.apache.geronimo.specs/geronimo-interceptor_1.2_spec/${geronimo-interceptor-spec.version}|lib/apache-owb/geronimo-interceptor_1.2_spec-${geronimo-interceptor-spec.version}.jar
maven://org.apache.xbean/xbean-finder-shaded/${xbean.version}|lib/apache-owb/xbean-finder-shaded-${xbean.version}.jar
maven://org.apache.xbean/xbean-asm7-shaded/${xbean.version}|lib/apache-owb/xbean-asm7-shaded-${xbean.version}.jar

[lib]
lib/apache-owb/*.jar

[xml]
# Enable annotation scanning webapp configurations
etc/apache-owb.xml

[license]
Apache OpenWebBeans is an open source project hosted by the Apache Software Foundation and released under the Apache 2.0 license.
https://openwebbeans.apache.org/
http://www.apache.org/licenses/LICENSE-2.0.html

[ini]
apache-owb.version?=2.0.11
geronimo-cdi-spec.version=1.0
geronimo-atinject-spec.version=1.0
geronimo-interceptor-spec.version=1.0
xbean.version=4.13
