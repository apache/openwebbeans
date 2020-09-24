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
maven://org.apache.geronimo.specs/geronimo-jcdi_2.0_spec/${geronimo-cdi.version}|lib/apache-owb/geronimo-jcdi_2.0_spec-${geronimo-cdi.version}.jar
maven://org.apache.geronimo.specs/geronimo-atinject_1.0_spec/${geronimo-atinject.version}|lib/apache-owb/geronimo-atinject_1.0_spec-${geronimo-atinject.version}.jar
maven://org.apache.geronimo.specs/geronimo-interceptor_1.2_spec/${geronimo-interceptor.version}|lib/apache-owb/geronimo-interceptor_1.2_spec-${geronimo-interceptor.version}.jar
maven://org.apache.xbean/xbean-finder-shaded/${xbean.version}|lib/apache-owb/xbean-finder-shaded-${xbean.version}.jar
maven://org.apache.xbean/xbean-asm9-shaded/${xbean.version}|lib/apache-owb/xbean-asm9-shaded-${xbean.version}.jar

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
apache-owb.version?=@project.version@
geronimo-cdi.version=@geronimo_cdi.version@
geronimo-atinject.version=@geronimo_atinject.version@
geronimo-interceptor.version=@geronimo_interceptor.version@
xbean.version=@xbean.version@
