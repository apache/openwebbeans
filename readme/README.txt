-------------------------------
Apache OpenWebBeans README
-------------------------------

Welcome!

Thanks for downloading and using Apache OpenWebBeans.
In short OWB
This document is a "Getting Started Guide" for the latest release OWB.

--------------------------------
What is Apache OpenWebBeans?
--------------------------------
OpenWebBeans is an Apache License V 2.0 licensed implementation of the JSR-365,
Contexts and Dependency Injection 2.0 specification.

CDI-2.0 is backward compatible to JSR-346 CDI-1.2 and JSR-299 CDI-1.0.

Our project's web page can be found at:
https://openwebbeans.apache.org

We also support the Jakarta EE specifications by providing shaded libraries.

The latest Java Version we support is Java-16.
The minimum Java Version is Java-8.


--------------------------------
OpenWebBeans 2.0.19 Release Features
--------------------------------

- The latest OWB release supports the following features
-----------------------------------
* Managed Beans Support
* Producer Method Support
* Producer Field Support
* Java EE Resource Injection Support
* Inheritance, Stereotype Inheritances
* Specialization Support
* Event Support
* Decorator and Interceptor Support
* Lookup and Dependency Injection Support
* Java EE Plugin Support (via ServetContextListener interface)
* Portable Integration Support
* Passivation Capability of Beans
* @Alternative support
* OSGi environment support with an own plugable bundle ClassPath scanner
* plugable SecurityManager integration doubles speed if no SecurityManager is being used
* support for direct CDI usage in tomcat-8, tomcat-9 and other Servlet environments



Noteable differences to CDI spec behaviour
--------------------------------------------

In a few special cases Apache OpenWebBeans might behave a little bit different than
other CDI implementations. This is to some degree caused by the JSR-299 spec being
not clear about some special topics so we needed to interpret the wording on our own.
This mainly concerns the area of section 5 and 12.1 Bean Archives (BDA) which doesn't work
out when it comes to OSGi containers and likes.
In Apache OpenWebBeans, a settings configured in a beans.xml file of a jar is not
only effective for this very bean archive but for the whole BeanManager in control
of the Application. This is especially the case for <alternatives>, <decorators> and
<interceptors>! An Alternative, Interceptor or Decorator enabled in one BDA is active
for the whole Application.


-------------------------------------------
Release Notes - OpenWebBeans - Version 2.0.19
-------------------------------------------
Bug
    [OWB-1349] - Respect configuration that is made via BeforeBeanDiscovery#configureQualifier

Task
    [OWB-1350] - upgrade to xbean-asm9-shaded for Java16 support
    [OWB-1351] - update various dependencies


-------------------------------------------
Release Notes - OpenWebBeans - Version 2.0.18
-------------------------------------------

Sub-task
    [OWB-1346] - prevent scanning of generated proxies

Bug
    [OWB-1281] - java.lang.UnsatisfiedLinkError in scanner stops application deployment
    [OWB-1328] - NPE in AbstractMetaDataFactory
    [OWB-1332] - BeansDeployer#packageVetoCache does not work for negative hits
    [OWB-1333] - [junit5] @Cdi#onStart not working
    [OWB-1341] - Event bus: IN_PROGRESS phase should not be sent to transactionService
    [OWB-1342] - Improve startup performance
    [OWB-1344] - Ensure creating annotatedtype is thread safe at runtime

Task
    [OWB-1327] - Run TCK for jakarta packaging
    [OWB-1329] - Remove openwebbeans-maven module
    [OWB-1330] - Junit5 parameter resolver companion for @Cdi
    [OWB-1331] - Create ajunit5 @Scopes extension to be able to control a bit more the started scopes
    [OWB-1343] - Add a property to skip @Vetoed check on packages
    [OWB-1345] - Upgrade gradle shadow plugin support to v6.0.0
    [OWB-1347] - upgrade to apache-parent 23



-------------------------------------------
Release Notes - OpenWebBeans - Version 2.0.17
-------------------------------------------
Bug
    [OWB-1214] - Package annotation access is fragile

Task
    [OWB-1322] - SLF4J integration workaround for log4j2-slf4j implementation which can fail in NPE on java >= 9
    [OWB-1323] - Upgrade to asm8
    [OWB-1324] - Support maven shade 3.2.3
    [OWB-1325] - Provide a spy flavor of ClassDefiningService
    [OWB-1326] - Bean#isNullable is ignored since CDI-1.1.


-------------------------------------------
Release Notes - OpenWebBeans - Version 2.0.8
-------------------------------------------

Bug

    [OWB-1257] - Conditional exclusion of beans in beans.xml does not honor system property
    [OWB-1263] - Generic observers not called correctly
    [OWB-1264] - Observers method throws NoClassDefFoundError for optional classes
    [OWB-1269] - TomcatSecurityService principal is not the contextual one out of request scope beans

Improvement

    [OWB-1261] - Upgrade ASM to version 7
    [OWB-1265] - [perf] cache AnnotationManager#getRepeatableMethod
    [OWB-1266] - [perf] InjectionResolver cache can be activated earlier

Task

    [OWB-1268] - Upgrade to xbean 4.12


-------------------------------------------
Release Notes - OpenWebBeans - Version 2.0.7
-------------------------------------------

Bug

    [OWB-1247] - Update to XBean Asm 6 Shaded 4.9
    [OWB-1248] - defineClass used which is not supported by java 11
    [OWB-1251] - event.fireAsync hangs when there is no observer

Improvement

    [OWB-1249] - org.apache.webbeans.config.OpenWebBeansConfiguration#overrideWithGlobalSettings environment overriding is not supported
    [OWB-1250] - Reduce the log level of anymous classes message when it cant be loaded
    [OWB-1252] - WebContextsService lazyStartRequestContext fails on first access.
    [OWB-1253] - Improve performance of BeforeDestroyed and Initialized Literals
    [OWB-1254] - destroying the Session doesn't fire BeforeDestroyed(SessionScoped.class) in our WebContextsService
    [OWB-1255] - update to apache-parent-21 for sha512


-------------------------------------------
Release Notes - OpenWebBeans - Version 2.0.6
-------------------------------------------

Bug

    [OWB-1199] - CDISeScannerService.autoScanning should be true by default
    [OWB-1203] - GProcessSyntheticBean not handled correctly for extensions leading to incorrect ProcessBean behavior
    [OWB-1204] - Interceptor and Decorator ignored in annotated mode if not decorated with @Dependent
    [OWB-1205] - We should not fire ProcessInjectionPoint for CDI Extension Observers
    [OWB-1207] - Inconsistent behavior of the instance behind CDI.current()
    [OWB-1209] - Custom bean with isAlternative()=true should not be automatically enabled
    [OWB-1210] - Providing an own alternative implementation of Provider<T> might disable some Instance<T> resolving
    [OWB-1211] - OWB is not firing BeforeDestroyed on contexts
    [OWB-1213] - producer of URI or other classes with private ct blow up with a NPE

Task

    [OWB-1086] - initial setup for cdi-2.0


-------------------------------------------
Release Notes - OpenWebBeans - Version 2.0.0
-------------------------------------------
Sub-task

    [OWB-1185] - implement Annotated#getAnnotations
    [OWB-1186] - update logic for bootstrapping-events
    [OWB-1187] - implement configurators
    [OWB-1188] - implement async events
    [OWB-1189] - add new parts to the event-api
    [OWB-1190] - implement java-se support
    [OWB-1192] - update logic for Instance
    [OWB-1193] - implement InterceptionFactory

Bug

    [OWB-1183] - OWB-Arquillian does not supports implicit bean discovery mode
    [OWB-1184] - arquillian connector doesn't support BDAs
    [OWB-1196] - Signed classes can't be proxied: java.lang.SecurityException: class "com.Foo$$OwbNormalScopeProxy0"'s signer information does not match signer information of other classes in the same package

Improvement

    [OWB-1135] - Remove duplication for openwebbeans/Messages
    [OWB-1195] - do a codestyle analysis check and apply fidings before releasing OWB-2.0.0

Task

    [OWB-1087] - fix failing integration tests with java 8
    [OWB-1182] - Implement the CDI-2.0 API


-------------------------------------------
Release Notes - OpenWebBeans - Version 1.7.3
-------------------------------------------

Bug

    [OWB-1172] - getResources doesn't work in OWB Arquillian container for WebArchives
    [OWB-1173] - InjectionPoint with no ownerBean fails on serialisation
    [OWB-1175] - Duplicate registration of ServletContextBean
    [OWB-1177] - producer should check runtime instance for Serializable constraint and not returned type

Task

    [OWB-1178] - Upgrade Arquillian to 1.1.13 and ShrinkWrap to 1.2.6


-------------------------------------------
Release Notes - OpenWebBeans - Version 1.7.2
-------------------------------------------

Bug

    [OWB-1170] - normal scoped proxy creation not thread safe


-------------------------------------------
Release Notes - OpenWebBeans - Version 1.7.1
-------------------------------------------

Bug

    [OWB-1150] - OSGI bundle version ranges too restrictive
    [OWB-1154] - Synchronization on a string literal
    [OWB-1157] - when user does a getReference of a programmatically registered bean without looking it up it can lead to multiple bean handling
    [OWB-1158] - Web lifecycle not registering Servlet Context as bean
    [OWB-1159] - HttpServletRequest not available
    [OWB-1160] - DefaultArchiveService migh mix up BDA urls if the containing folder is also a cp entry
    [OWB-1162] - CDI.current().select(X.class).select(SomeQualifier.LITERAL) doesn't work
    [OWB-1163] - NPE in BeforeBeanDiscovery#addAnnotatedType if id is null
    [OWB-1164] - Third Party Beans do not include Any qualifier if not included in bean impl

Improvement

    [OWB-1151] - extend our default scan-excludes
    [OWB-1152] - Improve compatibility and handling with Java9
    [OWB-1153] - improve tomcat-plugin performance
    [OWB-1156] - implement support for <trim/> feature of CDI-2.0

Task

    [OWB-1090] - remove jsf12 module for owb-2.0




-------------------------------------------
Release Notes - OpenWebBeans - Version 1.7.0
-------------------------------------------

Bug

    [OWB-1115] - Wrong version listed on download page
    [OWB-1121] - IllegalStateException in case of duplicated classes
    [OWB-1122] - InjectionPoint.getQualifiers does not return the list of selected qualifiers with programmatic lookup
    [OWB-1123] - NPE at NormalScopeProxyFactory.createNormalScopeProxy during deserialization
    [OWB-1124] - Lazy start on SessionContext NPE on no active RequestContext
    [OWB-1125] - Please delete old releases from mirroring system
    [OWB-1129] - isAfterBeanDiscoveryFired() doesn't handle before/during/after event states as required
    [OWB-1130] - ContainerCtrlTckTest fails
    [OWB-1131] - Typo in exception message
    [OWB-1136] - Can't access SessionScoped beans inside @Observers @Initialized(SessionScoped)
    [OWB-1138] - PassivationCapable bean id is not unique: PRODUCERMETHOD#class
    [OWB-1139] - JSP misses CDI ELResolver
    [OWB-1140] - Caused by: javax.enterprise.inject.UnsatisfiedResolutionException: Api type [xxx] is not found with the qualifiers
    [OWB-1149] - ContextsService doesn't re-attach mocked sessions correctly

Dependency upgrade

    [OWB-1134] - Upgrade plugins to support Java 8: maven-bundle-plugin and maven-checkstyle-plugin

New Feature

    [OWB-1141] - Maven Shade transformer for openwebbeans.properties
    [OWB-1142] - Gradle ShadowJar Transformer for openwebbeans.properties

Task

    [OWB-1089] - OpenWebBeansJsfPlugin is pretty much empty
    [OWB-1127] - Upgrade to XBean 4.5
    [OWB-1143] - upgrade to apache-parent-18, plugins and dependencies
    [OWB-1144] - Drop JSF-1.2 module for OWB-1.7.x
    [OWB-1145] - Drop EL-1.0 module for OWB-1.7
    [OWB-1146] - Remove tomcat6 module for OWB-1.7
    [OWB-1147] - Remove webbeans-jee5-ejb-resource module for OWB-1.7
    [OWB-1148] - remove the webbeans-doc module




-------------------------------------------
Release Notes - OpenWebBeans - Version 1.6.3
-------------------------------------------

Bug

    [OWB-1095] - Beans not fully initialized in AfterBeanDiscovery or InjectionPoint validation to early?
    [OWB-1100] - blacklist org.codehaus.groovy.runtime.,org.apache.commons.collections.functors.,org.apache.xalan in OwbCustomObjectInputStream
    [OWB-1102] - ProcessInjectionPoint observer is resolved for declared injection points with parent class
    [OWB-1103] - ProxyGenerationException caused by NoClassDefFoundError in OpenWebBeans OSGi
    [OWB-1105] - add an exclude list config for classes which have final methods but shall considered proxyable nonetheless
    [OWB-1111] - firing a during-transaction event fails if the tx is already rolled back or not active
    [OWB-1112] - CDI Producer method
    [OWB-1113] - Exceptions during Extension initialization get swallowed on tomcat9
    [OWB-1116] - @Specializes rules must take ProcessBeanAttributes#veto() into consideration
    [OWB-1117] - Beanmanager createInjectionTarget shouldnt validate injection points during extension boot
    [OWB-1118] - Producer Fields don't work with Interceptors

Improvement

    [OWB-1094] - Move bean scanning excludes to openwebbeans.properties
    [OWB-1098] - Iteration instead of recursion
    [OWB-1104] - add bootstrap jar to scan-excludes list
    [OWB-1106] - Scanning mode: Only jars with beans.xml (Like eg CDI 1.0)
    [OWB-1107] - Scanning mode: Like Annotated but send PAT allowing Extensions to add beans
    [OWB-1108] - properly destroy SPI services which implement Closeable

New Feature

    [OWB-1110] - implement exclude mechanism to suppress UnproxyableResolutionException for some classes

Task

    [OWB-1034] - re-visit BeanCacheKey#getQualifierHashCode
    [OWB-1091] - bootstrap upgrade for our site
    [OWB-1093] - WebApp beans scanning broken with maven and jetty
    [OWB-1096] - Enable > CDI1.0 unit tests


-------------------------------------------
Release Notes - OpenWebBeans - Version 1.6.2
-------------------------------------------

Bug

    [OWB-948] - Type of @New bean does not respect parameter
    [OWB-1084] - destory session event can used the wrong payload



-------------------------------------------
Release Notes - OpenWebBeans - Version 1.6.1
-------------------------------------------

Bug

    [OWB-1082] - WebContext BeanManager not longer serializable
    [OWB-1083] - WebContextsService errors when servlet session invalidated during request lifecycle

Improvement

    [OWB-1081] - check Reception.IF_EXISTS in case of an inactive context



-------------------------------------------
Release Notes - OpenWebBeans - Version 1.6.0
-------------------------------------------

Bug

    [OWB-626] - Conversation Scope isn't accessible after RENDER_RESPONSE phase
    [OWB-654] - manual lookups of beans with generics fail
    [OWB-758] - session backed session context
    [OWB-771] - Invocation​ContextImpl cleans target field if occurs an exception
    [OWB-844] - [PERFORMANCE] OWB conversation.ConversationImpl.isTransient() needs improvement
    [OWB-900] - Documentation Links are broken
    [OWB-906] - Check failover for custom scopes -> e.g. JSF 2.2 ViewScope
    [OWB-907] - ClassUtil fails with ArrayIndexOutOfBoundsException if WildcardType.getUpperBound() returns empty array
    [OWB-933] - @Delegate with constructor injection fails
    [OWB-986] - CreationalContextImpl.toString throws NullPointerException
    [OWB-989] - Clean info issue on Sonar
    [OWB-1035] - WebBeansELResolver broken in case of multiple beans
    [OWB-1040] - Lifecycle events fired during bean discovery do not follow specification sequence
    [OWB-1047] - Guess sample currently not working due to missing javax.annotation.Priority
    [OWB-1056] - interceptor and annotations spec jars missing in distribution
    [OWB-1059] - empty beans.xml file should result in BeanDiscoveryMode.ALL
    [OWB-1061] - Surplus and missing @Initialized and @Destroyed events
    [OWB-1062] - JspFactory.getDefault() returns null
    [OWB-1064] - Split ApplicationContext destroyal for custom beans and CDI internal beans like Extensions
    [OWB-1065] - Incorrect matching of parameterized events
    [OWB-1066] - Stack overflow on firing parameterized event
    [OWB-1067] - Download pages must link to KEYS file and describe how to use sigs
    [OWB-1069] - Propagate SessionContext to end of request only if a manual Session.invalidate() was called
    [OWB-1071] - WEB-INF/beans.xml is partly broken
    [OWB-1076] - remove sample sources from our binary distribution
    [OWB-1077] - create install scripts for our binary distribution

Improvement

    [OWB-609] - refactor conversation handling
    [OWB-762] - improve error message for "duplicated" configs
    [OWB-786] - available implementations of SecurityService need an improved error-handling
    [OWB-798] - expensive check in EventUtil#checkEventBindings
    [OWB-821] - reduce the number of string creations
    [OWB-851] - improve registration of default ee-beans
    [OWB-915] - re-visit tomcat modules
    [OWB-1044] - Cache whether @Initialized and @Destroyed get used at all in an app
    [OWB-1048] - Store @SessionScoped beans in real HttpSession if available
    [OWB-1049] - Remove FailOver service and related handling
    [OWB-1050] - Store the Map<conversationId, conversationContexts> in the SessionContext
    [OWB-1051] - Use RequestScopedBeanInterceptorHandler by default
    [OWB-1055] - Review splitting WebBeansConfigurationListener in Begin and End listeners
    [OWB-1073] - improve OpenWebBeansConfiguration lookup handling
    [OWB-1075] - improve annotation check
    [OWB-1080] - Use the JVMs java version for our generated proxies

New Feature

    [OWB-1070] - eager session creation configuration
    [OWB-1072] - CDI 1.2 spec requires a "CDI Conversation Filter" which eagerly touches the Conversation

Task

    [OWB-1060] - upgrade to latest apache-parent
    [OWB-1068] - Remove ContextsService#activateContext and #deactivateContext
    [OWB-1079] - upgrade ASM to 5.0.4

Test

    [OWB-1052] - improve our test coverage for the owb-web module
    [OWB-1053] - improve event performance



-------------------------------------------
Release Notes - OpenWebBeans - Version 1.5.0
-------------------------------------------


Bug

    [OWB-642] - Method WebBeansUtil.configureProducerMethodSpecializations is unreliable
    [OWB-679] - StereoTypeManager is completely unused -> remove or fix.
    [OWB-737] - @specializes @alternative child class disables super class even when not enabled when using stereotypes
    [OWB-745] - Fixes usage of java.lang.Class and java.lang.reflect.Method
    [OWB-746] - Having a Decorator with a constructor- or method-injection-point for the delegate leads to an exception if no corresponding field is available
    [OWB-747] - Cleanup OwbBean interface
    [OWB-799] - arquillian adapter uses archive:// url which can be slow because of host resolution
    [OWB-809] - addBeans does not invalidate resolvedComponents cache in InjectionResolver
    [OWB-810] - (public or protected) Observer methods don't get intercepted in the first firing of the event
    [OWB-811] - (public or protected) Observer methods don't get intercepted in the first firing of the event
    [OWB-816] - OWB does not correctly serialize/deserialize InstanceImpl
    [OWB-853] - CLONE - "Could NOT lazily initialize session context because of null RequestContext" always occur on session expire
    [OWB-860] - Type variable not bound in generic bean type
    [OWB-873] - NoSuchMethodException if no constructor with an empty parameter list is present AND the bean has @Decorator beans
    [OWB-910] - @PreDestroy causes ContextNotActiveException
    [OWB-912] - Weird Behavior with @Specializes and @Inject @Any
    [OWB-913] - o.a.webbeans.web.tomcat.TomcatInstanceManager should implement the new method in o.a.tomcat.InstanceManager introduced with Tomcat 7.0.47
    [OWB-922] - Instance#select() rules are wrong
    [OWB-929] - implicit filter in InstanceImpl#iterator
    [OWB-930] - NotificationManager#disableOverriddenObservers removes wrong observers
    [OWB-934] - Contextual#destroy not invoked for custom implementations
    [OWB-938] - NoClassDefFoundError not caught when checking if a bean is a managed one or not in WebBeanUtils (isValidManagedBean)
    [OWB-943] - pick up @NormalScope scopes, interceptors and decorators in implicit bean archives
    [OWB-945] - Decorators are not applied on methods which use parameterized type from super interface
    [OWB-950] - rework ConcurrentHashMap usages to avoid java 8 issues
    [OWB-954] - implement AfterTypeDiscovery
    [OWB-955] - rework AlternativesManager to fit CDI 1.1 needs.
    [OWB-957] - interceptor proxies blow up if you have more than 127 methods in a class
    [OWB-958] - our @Specializes code is based on classes and not on AnnotatedTypes
    [OWB-960] - Proxying varargs methods not working
    [OWB-965] - Create index to tomcat-sample
    [OWB-966] - upgrade Arquillian to 1.1.2.Final
    [OWB-967] - implement ProcessSyntheticAnnotatedType
    [OWB-968] - GenericUtils create infinite loop when parsing Enums
    [OWB-969] - BeanManager#createAnnotatedType should not reflect changes done by Extensions
    [OWB-970] - AbstractDecoratorInjectionTarget doesn't use right classloader to proxy classes (uses container classloader)
    [OWB-971] - Interceptors and Decorators must not define producer nor disposal methods
    [OWB-973] - Download page hash and sig links don't work
    [OWB-974] - All beans handed over to passivating contexts need to be serializable
    [OWB-975] - our Arquillian connector starts the ApplicationContext twice
    [OWB-976] - move cdi-1.1 BeanManager featurs from AbstractBeanManager to BeanManagerImpl
    [OWB-979] - method are considered overriden if java rules are respected but annotations are the same too
    [OWB-983] - sample/tomcat7-sample doesn't run with 'mvn tomcat7:run'
    [OWB-985] - support tomcat 7.0.54
    [OWB-988] - Move PrincipalBean from owb-ee-common to owb-impl
    [OWB-992] - Decorator generic delegate attribute must be same with decorated type
    [OWB-993] - OwbTypeVariableImpl needs to implements new methods of JDK-1.8 TypeVariable
    [OWB-994] - Container must throw DefinitionException on Bean definition errors
    [OWB-995] - BeanManager must throw Exception if some methods are called before LifecycleEvents
    [OWB-996] - implement @WithAnnotations
    [OWB-997] - implement @WithAnnotations
    [OWB-1001] - Container must fire ProcessInjectionPoint
    [OWB-1002] - Container must detected definition errors for Decorators
    [OWB-1003] - Decorators and Interceptors can not be applied to BeanManager
    [OWB-1005] - Container must throw DefinitionException if a Decorator or Interceptor has Producer or Observer methods
    [OWB-1006] - A Decorator must implement at least one interface
    [OWB-1007] - DefinitionError must be detected if @Decorated is injected into a bean instance other than a decorator
    [OWB-1008] - Decorators must not declare abstract methods which are not declared in implemented interfaces
    [OWB-1009] - adapt validation of interceptors to specification 1.2
    [OWB-1010] - Interceptor bindings of bean class replaces Interceptor bindings in stereotypes
    [OWB-1011] - Intercepted Beans must be proxyable
    [OWB-1012] - adapt validation of producers as specified in spec 1.2
    [OWB-1013] - Race condition in Instance injection
    [OWB-1014] - Validate injection of bean metadata in Producers
    [OWB-1015] - Container must detect DeplyomentError for Unproxyable bean types
    [OWB-1016] - java.net.MalformedURLException: WEB-INF/beans.xml
    [OWB-1019] - implement passivation check rules as defined in CDI-140 and CDI-153
    [OWB-1022] - InjectionPoint validated to early?
    [OWB-1023] - We should throw an IllegalArgumentException if BeanManager#validate parameter is null
    [OWB-1024] - DecoratorBeanBuilder : Number of TypeArguments must match - Decorated Type: 2 Delegate Type: 1
    [OWB-1030] - Lookup of InjectionPoint does not work
    [OWB-1037] - CLONE - NormalScoped ASM proxies broken in some cases for partial beans
    [OWB-1039] - improve exception handling during deployment
    [OWB-1041] - Session id changes in tomcat integration are not propagated to session context manager
    [OWB-1042] - dependent producer resolution needs to consider raw types
    [OWB-1045] - WebContextsService#destroySession
    [OWB-1046] - Starting transient conversation fails with jetty

Improvement

    [OWB-652] - Introduce HierarchicBeanManager
    [OWB-755] - Move the instance creation into Producer.produce
    [OWB-763] - move our remaining tests from TestContext to AbstractUnitTest
    [OWB-820] - cleanup of el resolvers
    [OWB-932] - skip validation of the cdi-api
    [OWB-937] - unify startup detection
    [OWB-972] - Define an Application Boundary SPI
    [OWB-987] - improve getReference type handling
    [OWB-1004] - Enable repeating qualifiers with binding attributes
    [OWB-1020] - evaluate class interceptors and use them if there is no special method interceptor
    [OWB-1031] - improve error message if injection point validation fails

New Feature

    [OWB-752] - upgrade jcdi to CDI-1.1
    [OWB-814] - Implement CDI-268
    [OWB-815] - Implement EventMetadata
    [OWB-928] - implement CDI-1.1 beans.xml scanning
    [OWB-931] - NormalScopeProxyFactory classloader usage
    [OWB-942] - CLONE owb-1.2- Signal or handle differently final methods
    [OWB-977] - ProcessBeanAttributes
    [OWB-978] - @Vetoed
    [OWB-980] - Support @WithAnnotations in ProcessAnnotatedType event
    [OWB-1021] - simple stackoverflow protection for TypeVariable
    [OWB-1029] - Use OWB on GoogleAppEngine

Task

    [OWB-770] - Implement new Lifecycle events
    [OWB-782] - Create AnnotatedTypeService SPI interface
    [OWB-826] - Implement CDI-58
    [OWB-843] - Implement utility-methods in BeanManager
    [OWB-847] - cleanup of SubclassProxyFactory
    [OWB-925] - move OWB trunk to CDI-1.1
    [OWB-926] - retire the OWB-CdiTest project
    [OWB-927] - retire the OWB-CdiTest project
    [OWB-936] - Check proxy generation works well with java 7
    [OWB-944] - upgrade to asm5
    [OWB-951] - Implement AlterableContext
    [OWB-1025] - add handling for @Initialized and @Destroyed




Older Releases:

-------------------------------------------
Release Notes - OpenWebBeans - Version 1.2.1
-------------------------------------------

Bug

    [OWB-626] - Conversation Scope isn't accessible after RENDER_RESPONSE phase
    [OWB-642] - Method WebBeansUtil.configureProducerMethodSpecializations is unreliable
    [OWB-675] - Alternative resolving does not take Qualifiers into consideration
    [OWB-654] - manual lookups of beans with generics fail
    [OWB-679] - StereoTypeManager is completely unused -> remove or fix.
    [OWB-745] - Fixes usage of java.lang.Class and java.lang.reflect.Method
    [OWB-774] - missing deployment hints
    [OWB-812] - study how to resolve the classloader to create a proxy
    [OWB-870] - @Observer event is being received twice
    [OWB-872] - Listening to AfterBeanDiscovery suppresses @Decorator registration as a side effect
    [OWB-874] - AfterBeanDiscovery event should not be fired if there is no extension module registered
    [OWB-876] - basic handling of virtual resources in arquillian adapter
    [OWB-877] - no need to cast parent type in AnnotatedTypeImpl
    [OWB-878] - OpenWebBeans does not correctly handle generics
    [OWB-881] - interception of bean using constructor injection is not supported
    [OWB-885] - ProducerMethodBeansBuilder ejb handling is too linked to openejb
    [OWB-886] - @Specializes appears to be broken
    [OWB-887] - Ambigious resolution with this two producers: Map<X,Y> and Map<X,Z>
    [OWB-888] - Provider to an EJB does not work
    [OWB-889] - intercepted/decorated beans are not serializable even if the whole stack is
    [OWB-890] - InjectionResolver#checkInjectionPoints throw NPE for some 3rd party beans
    [OWB-891] - dynamically removing @Alternative does not work
    [OWB-893] - OpenWebBeans 1.2.1 fails when injecting generic value holder
    [OWB-895] - BeforeBeanDiscovery.addAnnotatedType() calls does not work as expected when owb jars are deployed outside WEB-INF/lib folder
    [OWB-896] - Delegate InjectionPoints need different bean resolving rules
    [OWB-897] - Interceptors do not work on processed injection targets
    [OWB-898] - ClassFormatError in ASM engine
    [OWB-901] - missing type erasure handling for ParameterizedType
    [OWB-902] - NPE when Bean#getBeanClass is null
    [OWB-904] - VerifyError on Interceptor usage
    [OWB-905] - Remove @Ingored from DefaultOwbFailOverTest#restoreConversationContexts
    [OWB-908] - Proxy creation fails if a method throws an Exception which is an inner class
    [OWB-909] - ConversationBean needs to implement PassivationCapable
    [OWB-911] - SelfInterceptorBean not serialized correctly in DefaultInterceptorHandler

Documentation

	[OWB-783] - No documentation available in the project webpage

Improvement

    [OWB-652] - Introduce HierarchicBeanManager
    [OWB-762] - improve error message for "duplicated" configs
    [OWB-763] - move our remaining tests from TestContext to AbstractUnitTest
    [OWB-786] - available implementations of SecurityService need an improved error-handling
    [OWB-820] - cleanup of el resolvers
    [OWB-821] - reduce the number of string creations
    [OWB-880] - OpenWebBeans Arquillian Container rely on bad Archive instance assumptions

New Feature

    [OWB-814] - Implement CDI-268
    [OWB-879] - allow to configure owb properties in owb arquillian adapter
    [OWB-882] - AbstractUnitTest should support to inject bean instances in test class
    [OWB-883] - basic @AroundConstruct implementation
    [OWB-884] - basic @Priority support

Task

    [OWB-782] - Create AnnotatedTypeService SPI interface
    [OWB-854] - cleanup of jdk 1.5 specific parts
    [OWB-866] - use xbean asm4 shade
    [OWB-867] - a class without a "CDI" constructor is not always an issue so log it with info level only when mandatory to not pollute logs

-------------------------------------------
Release Notes - OpenWebBeans - Version 1.2.0
-------------------------------------------

Bug

    [OWB-151] - @Dependent beans not interceptable
    [OWB-187] - Interceptors with lifecycle and @AroundInvoke permitted to have bindingtypes containing method-level annotations
    [OWB-306] - overrridden @AroundInvoke and lifecycle interceptors are still run
    [OWB-392] - AbstractInjectable#dependentInstanceOfProducerMethods must not be static nor public
    [OWB-423] - OpenWebBeansEjbInterceptor is LATE in establishing the request context for an EJB
    [OWB-468] - Make BeansDeployer.deployFromClassPath(ScannerService) resilient to ClassNotFoundException and NoClassDefFoundError's
    [OWB-497] - Don't break deployment if java can't read all the annotations
    [OWB-513] - proxies should be inactive after a container shutdown
    [OWB-549] - Security review needed for ClassUtil
    [OWB-556] - bean with interceptor + @PreDestroy causes a NullPointerException
    [OWB-568] - Decorater for generic Interfaces does not work
    [OWB-569] - OpenWebBeans uses the Java Reflection API to discover program element types and annotations in addition to the AnnotatedType
    [OWB-570] - Interceptors must support retry
    [OWB-572] - OwbParametrizedTypeImpl s equals method is broken
    [OWB-575] - ResourceProxyHandler.invoke should unwrap and throw the underlying cause of the InvocationTargetException
    [OWB-665] - invocation order of @PostConstruct interceptors
    [OWB-714] - EmptyStackException when accessing an instance that is created by a producer method that has an InjectionPoint as parameter.
    [OWB-722] - @Typed not respected for beans using generics
    [OWB-728] - AbstractProducer stores CreationalContext
    [OWB-729] - review CreationalContext in Interceptor and Decorator creation
    [OWB-730] - remove InjectionTargetWrapper
    [OWB-733] - CLONE - ClassLoader leak in WebBeansUtil.isScopeTypeNormalCache
    [OWB-739] - CLONE - Ambiguous producer methods and fields are not detected due to bug in AbstractProducerBean equals and hashCode
    [OWB-740] - NPE while removing dependent beans
    [OWB-748] - Implement CDI-132
    [OWB-749] - Move Interceptor stuff to InterceptorManager
    [OWB-750] - OWB annotationlitteral use instanceof to implement equals and not annotationType()
    [OWB-754] - fix PassivationCapable detection
    [OWB-759] - Decorator position not well managed if the decorator is abstract
    [OWB-764] - constructor with multiple InjectionPoints cause Exception
    [OWB-767] - DefaultBDABeansXmlScanner synchronizes without visibility guarantees
    [OWB-768] - add support for proxying protected methods in NormalScopedProxyFactory
    [OWB-769] - Serialisation support for our new InterceptorDecoratorProxies
    [OWB-775] - error on shutdown doesn't cleanup WebBeansFinder map
    [OWB-776] - private and protected producer methods do not properly de-reference the contextual instance
    [OWB-777] - InjectableMethod must unwrap normalscoping proxies
    [OWB-778] - InterceptorBean must unwrap InjectionTargetExceptions
    [OWB-784] - WebContextsService bypasses spi contract of ContextsService
    [OWB-788] - deployment error when a @Specializes bean is disabled via an Extension
    [OWB-791] - only one generic is handled for injections of ManagedBeans
    [OWB-793] - "Ambigious" typo
    [OWB-794] - AbstractOwbBean#toString doesn't reflect ParameterizedTypes
    [OWB-801] - null instance shouldn't be destroyed (copy)
    [OWB-802] - #annotationType() of javax.enterprise:cdi-api not compatible with AbstractAnnotationLiteral
    [OWB-803] - Forever loop when abstract decorator does not implement invoked method
    [OWB-805] - CLONE - @Alternative is ignored when using the Provider interface.
    [OWB-806] - CLONE - Overloaded EJB Observer methods fail to deploy
    [OWB-807] - performance issue with owb-arquillian-standalone
    [OWB-813] - CLONE - #annotationType() of javax.enterprise:cdi-api not compatible with AbstractAnnotationLiteral
    [OWB-817] - the jsf module(s) are bound to a specific el version
    [OWB-818] - StandaloneResourceInjectionService can lead to injection of 'null'
    [OWB-822] - ejb should be tested first and not after having defined a managed bean
    [OWB-823] - EJBs support ee injections
    [OWB-824] - annotated field events should get the associated annotatedfield
    [OWB-825] - don't fire the same PAT event for a bean intercepted by itself (ejb interceptor style)
    [OWB-828] - broken proxies in case of bridge methods
    [OWB-829] - generate our proxies with 2 $$
    [OWB-831] - filtering producers by annotated type is too strict (java type should be enough) since it prevents extensions to add producers
    [OWB-832] - ejb producers should use ejb view methods
    [OWB-833] - disposal injection points shouldn't be listed in getInjectionPoints() method
    [OWB-834] - self interceptors have lifecycle methods
    [OWB-835] - even resource should be serializable
    [OWB-836] - auto interception ignored when no other reason to proxy the class
    [OWB-837] - lifecycle interceptors can't catch exceptions
    [OWB-838] - putting bean dependent instances first in dependent objects
    [OWB-839] - interceptor lifecycle methods (@postConstruct,...) should be ignored for method interceptors
    [OWB-840] - don't create decorators if the asked instance if the delegate
    [OWB-842] - lazy mode for @New on ejbs
    [OWB-845] - @Disposes not validated
    [OWB-848] - deserialization of normal-scoped proxies for PassivationCapable beans fails
    [OWB-849] - org.apache.webbeans.portable.ProviderBasedProxyProducer uses the wrong classloader
    [OWB-850] - all provider based producer can't be proxied -> handling dependent scope
    [OWB-855] - NPE when no default constructor is found
    [OWB-856] - Currently in OpenWebBeans one can add exactly one AnnotatedType
    [OWB-857] - NullPointerException on passivation
    [OWB-858] - AnnotatedTypeImpl not thread safe
    [OWB-861] - Decorator building fails when decorator has inheritance
    [OWB-862] - allow independent bootstrapping

Improvement

    [OWB-479] - detect loops in producer beans vs. producer method parameters at deployment time
    [OWB-488] - move WebBeansConfigurationException messages to message bundles
    [OWB-551] - Reduce static synchronized hashmap usage even further
    [OWB-603] - Incorporate enhanced BeanArchive handling of the preliminary CDI-1.1 specification
    [OWB-632] - re-visit WebBeansUtil#isPassivationCapable
    [OWB-715] - Remove EL22 implementation from Core to Own Project
    [OWB-717] - Remove Failover implementation from Web to Own Project
    [OWB-727] - introduce a static INSTANCE for DefaultLiteral, AnyLiteral, etc
    [OWB-735] - remove CreationalContextWrapper
    [OWB-744] - change the name of all Bean<T> implementations to reflect this fact
    [OWB-753] - remove lazy Bean initialisation
    [OWB-765] - Implement BeanAttributes
    [OWB-766] - Use CreationalContextImpl to pass info about InjectionPoint, Event, etc
    [OWB-772] - typo in exception message wrt passivation capable dependencies
    [OWB-779] - ScannerService using xbean
    [OWB-797] - AnnotationManager checks the same annotations again
    [OWB-819] - check injectable reference for normal-scoped beans
    [OWB-852] - Improve getContextsService performance

New Feature

    [OWB-321] - Conversation beans could not be populated to non-faces request by a JSF redirect navigation rule
    [OWB-495] - create a jetty integration plugin
    [OWB-606] - create bundles for standard use cases like JSF-webapp, standalone, etc
    [OWB-671] - Automatically register FailOverFilter if failover is activated
    [OWB-710] - openwebbeans-arquillian container
    [OWB-756] - implement BeanAttributes and ProcessBeanAttritubes
    [OWB-830] - Implement Instance#destory

Task

    [OWB-692] - remove ContextsFactory
    [OWB-726] - upgrade trunk (OWB-1.2.x) to target java 1.6
    [OWB-781] - Remove all Javassist and Scannotation related parts from OWB
    [OWB-859] - Remove checkstyle double-checked locking rule

Wish

    [OWB-344] - implement Decorators and Interceptors as subclassing



-------------------------------------------
Release Notes - OpenWebBeans - Version 1.1.8
-------------------------------------------

Bug

    [OWB-700] - ProcessInjectionTarget.setInjectionTarget() has no effect when trying to post process beans
    [OWB-723] - Interceptors doesn't work for beans got from stereotype annotated producer methods
    [OWB-736] - NPE while removing dependent beans
    [OWB-742] - @Alternative is ignored when using the Provider interface.
    [OWB-743] - Overloaded EJB Observer methods fail to deploy
    [OWB-751] - OWB annotation litterals implementation uses only instanceof in equals implementation
    [OWB-760] - CLONE - Decorator position not well managed if the decorator is abstract
    [OWB-761] - position in DelegateHandler shouldn't be static
    [OWB-785] - WebContextsService isn't compatible with actor-frameworks
    [OWB-790] - owb-1.1.x - deployment error when a @Specializes bean is disabled via an Extension

New Feature

    [OWB-789] - owb-1.1.x - openwebbeans-arquillian container


-------------------------------------------
Release Notes - OpenWebBeans - Version 1.1.7
-------------------------------------------

Bug

    [OWB-711] - Specialization does not deactivate Observer methods in parent class
    [OWB-713] - Static observer methods of superclasses are not called when a method with the same name and parameters exists in the subclass
    [OWB-718] - Decorator can't call two methods of the delegate object in the same method
    [OWB-719] - @Named qualifier is not adhering to CDI spec default naming conventions
    [OWB-720] - injections using generic not well managed
    [OWB-722] - @Typed not respected for beans using generics
    [OWB-724] - Ambiguous producer methods and fields are not detected due to bug in AbstractProducerBean equals and hashCode
    [OWB-725] - Beans containing a producerMethod are using the wrong CreationalContext
    [OWB-732] - ClassLoader leak in WebBeansUtil.isScopeTypeNormalCache
    [OWB-734] - CLONE - AbstractProducer stores CreationalContext

Task

    [OWB-712] - Enable console output for checkstyle
    [OWB-721] - create a branch for OWB-1.1.x maintenance and switch trunk to 1.2.0-SNAPSHOT

-------------------------------------------
Release Notes - OpenWebBeans - Version 1.1.6
-------------------------------------------

Bug

    [OWB-694] - Misleading exception message "Wrong termination object"
    [OWB-696] - check for unproxyable API types should get moved to the validateBeans phase
    [OWB-697] - Non-Static Loggers leads to NonSerizializableException
    [OWB-698] - InjectableBeanManager not serializable
    [OWB-703] - getBeans cache key algorithm must be unique
    [OWB-707] - tomcat-sample and tomcat7-sample are just broken.
    [OWB-708] - PrincipalBean doesn't get found
    [OWB-709] - webbeans-tomcat6 must honour WEB-INF/classes/META-INF/beans.xml

Improvement

    [OWB-695] - Cause missing in AnnotationDB$CrossReferenceException
    [OWB-701] - Support ASM for Bean Proxies
    [OWB-702] - Add serialization unit tests to openwebbeans-web to catch future regressions
    [OWB-704] - use method filter in javassist proxies instead of "manual" filtering



-------------------------------------------
Release Notes - OpenWebBeans - Version 1.1.5
-------------------------------------------

Bug

    * [OWB-498] - Java EE Resource Injections for CDI Interceptors & Decorators
    * [OWB-605] - tomcat plugins must register WebBeansConfigurationListener as first Listener
    * [OWB-663] - Maven dependencies between impl and spi
    * [OWB-666] - invalid check in AnnotationManager#checkStereoTypeClass
    * [OWB-667] - Bean queries have strange behavior (difference between Open Web Beans and the Reference Implementation)
    * [OWB-669] - Bean fail over is not in sync with specs
    * [OWB-672] - Decorators creates Stackoverflow or NPE if under heavy load
    * [OWB-673] - injecttarget are not updated after ProcessInjectionTarget event
    * [OWB-676] - CdiTest OwbContainer fails if WebContainerLifecycle is being used
    * [OWB-677] - improve getBeans cache key algorithm
    * [OWB-680] - drop unused getInstance() methods from our services
    * [OWB-681] - remove deprecated methods from BeanManagerImpl
    * [OWB-682] - get rid of OWB InterceptorType and usejavax.enterprise.inject.spi.InterceptionType instead
    * [OWB-683] - remove obsolete Methods from our Bean implementations and handlers
    * [OWB-685] - OwbApplicationFactory doesn't set Application
    * [OWB-686] - OWBApplicationFactory wrappedApp should be consistent between getter and setter and volatile
    * [OWB-687] - clean up non-static loggers
    * [OWB-688] - fix non-serializable fields in Serializable classes
    * [OWB-689] - OWBInjector should be stateless
    * [OWB-690] - WebContextsService cleanup should get reworked
    * [OWB-693] - webbeans-web shall not declare StandaloneResourceInjectionService

Improvement

    * [OWB-674] - rewrite owb logger api
    * [OWB-684] - move from ancient 'ContextTypes' definition to standadrd scope annotations

Task

    * [OWB-678] - Check for unused classes and remove them
    * [OWB-691] - Sonar cleanup before the 1.1.5 release


-------------------------------------------
Release Notes - OpenWebBeans - Version 1.1.4
-------------------------------------------

Bug

    * [OWB-567] - Lookup of Provider results in NullPointerException on get()
    * [OWB-574] - NewBean doesn't support EJBs
    * [OWB-580] - ObserverMethodImpl needs support for EJB's whose Bean Types do not include the EJB class
    * [OWB-602] - OpenWebBeans OpenEJB integration OSGi bundle declares the wrong version for the javax.transaction package
    * [OWB-617] - NullPointerException in InstanceBean#createInstance
    * [OWB-628] - Event injection doesn't work in observer methods.
    * [OWB-633] - define stereotypes & thirdparty
    * [OWB-634] - @Interceptors added by extension ignored
    * [OWB-636] - Samples point to parent pom file that doesn't exist
    * [OWB-643] - it isn't possible to add/remove @Alternative during the bootstrapping process
    * [OWB-644] - wrong config entry for LoaderService
    * [OWB-645] - InjectionPoint is null when using @Produces. NullPointerException caused by CDI container providing a null InjectionPoint into a producer method.
    * [OWB-646] - Failover does not work (again)
    * [OWB-648] - regression on tck org.jboss.jsr299.tck.tests.lookup.injectionpoint.InjectableReferenceTest
    * [OWB-649] - exceptions in EJB's are wrapped in InvocationTargetException
    * [OWB-655] - CDI doesn´t inject stateless EJB by abstract class.
    * [OWB-658] - BeanManager.getBeans(Type, Annotation...) can not be used to query all known beans
    * [OWB-659] - An annotated interface class is being seen as a manage bean.
    * [OWB-660] - WebBeansContext #activateContext for SessionScoped.class doesn't set the ThreadLocal

Improvement

    * [OWB-596] - Provide info about injetion point for "Passivation capable beans must satisfy passivation capable dependencies ..."
    * [OWB-604] - more details for exceptions during bootstrapping
    * [OWB-635] - support callbacks (@PostContrcut, @PreDestroy) in Extensions
    * [OWB-637] - [perf] ELContextStore.destroyDependents() creates unnecessary HashMap$KeyIterator instances
    * [OWB-638] - [PERF] Avoid unnecessary AbstractList$Itr instances
    * [OWB-639] - [perf] InjectionResolver.getBeanCacheKey creates many StringBuilder instances
    * [OWB-641] - the jee5-ejb-resource module should support @EJB(mappedName)
    * [OWB-647] - [PERF] Avoid unnecessary StringBuilder instances - improve checkNullInstance, checkScopeType, ... methods
    * [OWB-653] - remove @ViewScoped support
    * [OWB-657] - review startup performance
    * [OWB-661] - hashCode, equals and toString() of our built in Qualifier Literals should be implemented ourselfs

Task

    * [OWB-656] - remove webbeans-openejb
    * [OWB-662] - remove obsolete class WebBeansAnnotation



-------------------------------------------
Release Notes - OpenWebBeans - Version 1.1.3
-------------------------------------------

Bug

    * [OWB-515] - interceptors don't support inheritance without an overridden method annotated with @AroundInvoke
    * [OWB-565] - missing check for producer methods
    * [OWB-625] - BeanManager.resolve throw java.util.NoSuchElementException with an empty set parameter
    * [OWB-629] - NoClassDefFoundError for optional dependencies
    * [OWB-630] - AmbiguousResolutionException thrown for Decorators that Decorate multiple beans where any of those beans are passivation capable.
    * [OWB-631] - openwebbeans-resource misses openwebbeans.properties

Improvement

    * [OWB-475] - support for optional beans
    * [OWB-627] - Automatically destroy @Dependent contextual instances created with Instance<T>


-------------------------------------------
Release Notes - OpenWebBeans - Version 1.1.2
-------------------------------------------

Bug

    * [OWB-562] - non-enabled alternative beans with passivating scope fail validation during deployment
    * [OWB-589] - " ... requires a passivation capable dependency ..." for producer method with return type String and non serializable injected dependency
    * [OWB-597] - StackOverFlow when injecting product in same bean where @Produces is placed
    * [OWB-615] - remove @Overrides for interfaces to be java5 compatible
    * [OWB-616] - javax.el.ExpressionFactory has final methods! CDI doesn't allow that. - Test on final **PRIVATE** methods too?
    * [OWB-618] - we sometimes invoke a dispose method without having created the bean upfront
    * [OWB-619] - @New beans must only exist if there is at least one injection point for them
    * [OWB-620] - any disabled bean of passivating scope will wrongly be detected as 'not passivatable'
    * [OWB-622] - beanmanager injection in afterBeanDiscovery method parameter
    * [OWB-624] - AnnotatedTypes registered in BeforeBeanDiscovery might get processed twice

Improvement

    * [OWB-623] - Relax check on @AroundInvoke Interceptors 'throws Exception'

New Feature

    * [OWB-621] - Alternative configuration method for buggy container or pre servlet api 2.5 container


-------------------------------------------
Release Notes - OpenWebBeans - Version 1.1.1
-------------------------------------------

Bug

    * [OWB-406] - BaseEjbBean.removedStatefulInstance used by multiple instances/EjbBeanProxyHandlers
    * [OWB-447] - unnecessary contextual/non-contextual distinction in OpenWebBeansEJBIntercpetor
    * [OWB-449] - EJB interceptor has incorrect/unnecessary use of business method checks
    * [OWB-483] - Problem with mulitple custom interceptors and passivation
    * [OWB-512] - ApplicationContext and SingletonContext in WebContextsService
    * [OWB-554] - DelegateHandler wraps Beans exceptions
    * [OWB-558] - PassivationCapable bean id's for Producer Fields do not take into account generics
    * [OWB-561] - Multiple contexts with the same Scope are not handled properly -- causing tck failures
    * [OWB-563] - producers of passivating beans fail when the declared return type is not serializable but the actual return type is
    * [OWB-566] - ProcessInjectionTarget event gets fired too early
    * [OWB-571] - fix site build under maven3 and upgrade logo
    * [OWB-573] - Invalid checking of Interceptor serialization capabilities for non-Passivation capable EJBs
    * [OWB-576] - FileNotFoundException on WebSphere
    * [OWB-577] - FileNotFoundException on WebSphere
    * [OWB-578] - Allow DI for OpenWebBeansConfiguration properties
    * [OWB-579] - check for non-proxyiable methods should exclude synthetic methods
    * [OWB-581] - Decorator interface check needs configurable exclusions
    * [OWB-584] - check for declared name consistency for specializes beans is wrong
    * [OWB-585] - ProcessSessionBean doesn't deal with generic type quite right (CDITCK-215)
    * [OWB-586] - Interceptors added by portable extensions don't work
    * [OWB-587] - Use business interface for producer and disposer methods of Session beans
    * [OWB-588] - PrincipalBean is misspelled
    * [OWB-590] - Seam Persistence does not work with OWB - AfterBeanDiscovery.addBean will be ignored
    * [OWB-591] - EJB @Specializes inheritance
    * [OWB-593] - Interceptor binding added on an interceptor class at ProcessAnnotatedType phase is not considered
    * [OWB-595] - Use case "Faces Request Generates Non-Faces Response" locks conversation forever (-> BusyConversationException)
    * [OWB-598] - InjectionResolver crashes with a NPE when injecting a method parameter
    * [OWB-599] - move getBeanXmls() back to Set<URL>
    * [OWB-600] - cache information about non intercepted methdos in ProxyHandlers
    * [OWB-601] - WebContextsService only works if ServletContext is given
    * [OWB-608] - openwebbeans-el10 plugin misses openwebbeans.properties
    * [OWB-614] - add LICENSE and NOTICE files to all our samples

Improvement

    * [OWB-555] - ClassUtil methods contain spelling, camelcase, etc., type errors
    * [OWB-557] - #setAccessible(false) isn't needed
    * [OWB-560] - upgrade the TCK to 1.0.4.SP1
    * [OWB-564] - CdiTestOpenWebBeansContainer - check if a std.-context is active before destroying it
    * [OWB-582] - Support for Java 1.5 (needed for WebSphere 6.1)
    * [OWB-583] - Support for Servlet API 2.4 (needed for WebSphere 6.1)
    * [OWB-594] - create a configurable mapping Scope->ProxyMethodHandlerImplementation
    * [OWB-607] - upgrade our samples to newest available dependencies
    * [OWB-610] - upgrade to apache parent pom 10
    * [OWB-611] - adding ASF trademark documentation to our official site build
    * [OWB-612] - upgrade various maven plugins
    * [OWB-613] - Exclude Samples WARs Publishing with Maven

Task

    * [OWB-592] - EJB Specialization utility method



-------------------------------------------
Release Notes - OpenWebBeans - Version 1.1.0
-------------------------------------------

Bug

    * [OWB-295] - resolve bugs in Javassist Proxy
    * [OWB-417] - BaseEjbBean.destroyComponentInstance() should call direct container remove API, not call an @Remove annotated method
    * [OWB-422] - Support needed for PrePassivate, PostActivate, and AroundTimeout via EJBInterceptor.
    * [OWB-444] - Using Static Loggers in Shared ClassLoader
    * [OWB-452] - set active flag to false then context is destroyed
    * [OWB-456] - When multiple interceptors are defined for a bean OWB does NOT correctly remove the overridden base Interceptors
    * [OWB-469] - JSR299TCK: Security Error / Passivation errors during readObject
    * [OWB-470] - OWBInjector does not work correctly for EJB Beans
    * [OWB-471] - Possible StackOverflowException from defineProducerMethods in WebBeansAnnotatedTypeUtil
    * [OWB-473] - bundles that use javasissist to proxy their contents need to import some javassist packages
    * [OWB-474] - InjectionTargetBean#injectSuperResources is missing
    * [OWB-477] - Two instances of using ObjectInputStream that may not have visibility into application classloader
    * [OWB-480] - Avoid a couple NPEs
    * [OWB-482] - Small issues
    * [OWB-486] - ResourceBean tries to proxy final classes before testing them for being final
    * [OWB-489] - AnnotatedTypes added with BeforeBeanDiscovery.addAnnotatedType method are ignored
    * [OWB-490] - ProcessObserverMethod Type parameters are inverted (CDITCK-174)
    * [OWB-491] - Decorators init needs to scan superclasses for more interfaces. cf CDITCK-178
    * [OWB-492] - events don't get sent to private @Observes methods
    * [OWB-493] - ProcessProducerMethod and ProcessProducerField type parameters are reversed in filtering (?) CDITCK-168
    * [OWB-494] - Subclasses with non-overriden observer methods not recognized as beans with observer methods
    * [OWB-496] - Don't replace the ProxyFactory classloaderProvider without the intention to do so
    * [OWB-499] - WEB-INF/beans.xml of a war will not activate Bean Archive behaviour
    * [OWB-502] - Only cache the ContextService once, in the SingletonService
    * [OWB-504] - OwbApplicationFactory getWrapped should return wrapped application factory
    * [OWB-505] - OwbApplicationFactory should not be installed by default
    * [OWB-509] - Unwrap InvocationTargetException in ResourceProxyHandler.invokie
    * [OWB-510] - return null instead of an unusable proxy if a resource is missing
    * [OWB-511] - Delegate actualInstance serialization behavior in ResourceProxyHandler
    * [OWB-514] - Leak in ELContextStore
    * [OWB-519] - broken wls support
    * [OWB-521] - ProducerMethodBean could theoretically produce a NPE
    * [OWB-522] - Missing updateTimeout in one of begin methods for conversation
    * [OWB-523] - @SessionScoped bean failover does not work
    * [OWB-524] - OWB classpath scanning of non-jars doesn't work if the classpath contains spaces
    * [OWB-527] - JspFactory.getDefaultFactory() is synchronized, We can cache the return value to improve performance
    * [OWB-529] - lazy initialized class members should be volatile
    * [OWB-530] - multi massive execution of our Interceptor test unveils a concurrency problem
    * [OWB-531] - cleanup WebBeansELResolver#getValue
    * [OWB-533] - concurrency bottleneck due to use of our logger
    * [OWB-534] - Injection of @PersistenceContext does not work with abstract/base classes
    * [OWB-541] - replace WeakHashMap with a standard one in InterceptorHandler
    * [OWB-542] - Disposer is called twice on Dependent beans when injected into a managed object that is called from a JSP
    * [OWB-543] - get rid of checked Exceptions in our SPI
    * [OWB-545] - Cleanup our SecurityManager integration
    * [OWB-546] - @Typed gets ignored if we use a AnnotatedType from an Extension @Observing ProcessAnnotatedType
    * [OWB-547] - WebContextsService throws NPE on asynchronous app startup
    * [OWB-548] - missing null check in DefaultContextsService#stopApplicationContext
    * [OWB-550] - duplicated observer methods in case of @Specializes

Improvement

    * [OWB-209] - remove all <repositories> from our poms
    * [OWB-254] - suppress initialising contextual handling for configurable URIs.
    * [OWB-335] - implement a sample for @ViewScoped in reservation
    * [OWB-393] - remove old XML configuration code
    * [OWB-448] - More changes for decorator and interceptor passivation support
    * [OWB-461] - source code quality
    * [OWB-472] - archive centric beans.xml enabling
    * [OWB-478] - make OWB build maven-3 aware
    * [OWB-485] - AmbiguousResolutionException doesn't print details about the injection point
    * [OWB-500] - improved app-server support
    * [OWB-503] - Reduce static synchronized hashmap usage
    * [OWB-506] - Upgrade our samples to Apache MyFaces-2.0.3 and OpenJPA-2.0.1
    * [OWB-507] - our samples should be prepared for EE as default
    * [OWB-508] - Dependent scope proxies are needed to wrap the build-in beans returned from the services if they are not serializable yet
    * [OWB-516] - Get TCK standalone 1.0.4 CR2
    * [OWB-517] - Conversation Log Improvement
    * [OWB-518] - log all bean-archive markers
    * [OWB-520] - spi for the webbeans-jee5-ejb-resource plugin
    * [OWB-525] - create a findbugs filter file in our build-tools resource
    * [OWB-526] - remove usage of java.net.URLs from ScannerService and drop scannotation
    * [OWB-535] - free ScannerService resources once we don't need it anymore
    * [OWB-536] - revisit our Logger usage in our Bean impls
    * [OWB-537] - clear() AnnotatedElementFactory after the deployment
    * [OWB-539] - fill AnnotatedTypeImpl lazily
    * [OWB-540] - StandaloneResourceInjectionService should cache info about classes which don't contain EE resource injection points
    * [OWB-544] - improve BeanManager#getContext performance

New Feature

    * [OWB-433] - add a configuration flag for switching to a lenient lifecycle interceptor checking
    * [OWB-501] - owb ee 5 resource integration
    * [OWB-528] - Use ApplicationWrapper as parent of OwbApplication in JSF 2 plugin
    * [OWB-532] - create a new BeanManager#isInUse()
    * [OWB-538] - lazy loading of not explicitly marked (via annotation or registered by extension) Dependent beans

Question

    * [OWB-383] - static fields in CreationalContext

TCK Challenge

    * [OWB-484] - Running TCK 1.0.1 Final and Respective Corrections

Task

    * [OWB-19] - Geronimo Integration
    * [OWB-428] - implementation of equals and hashCode for AbstractOwbBean
    * [OWB-453] - add a flag to disable context activation in EJB interceptor




-------------------------------------------
Release Notes - OpenWebBeans - Version 1.0.0
-------------------------------------------

Bug

    * [OWB-318] - multiple methods with same EJB @interceptors(foo.class) in same bean class get multiple interceptor instances
    * [OWB-384] - OWB needs to call 299-defined PrePassivate, PostActivate, and AroundTimeout interceptors for EJBs
    * [OWB-422] - Support needed for PrePassivate, PostActivate, and AroundTimeout via EJBInterceptor.
    * [OWB-429] - OpenWebBeansEjbPlugin Class Hierarchy
    * [OWB-438] - Cached Normal Scoped Proxy instances
    * [OWB-439] - EjbPlugin session bean proxy creation thread safe problem
    * [OWB-445] - we must not use javassist ProxyFactory#setHandler(MethodHandler)
    * [OWB-446] - EJB lifecycle callbacks not stacked correctly
    * [OWB-450] - NullPointerException in DependentScopedBeanInterceptorHandler when it has a NullCreationalContext (normally from a EE component).
    * [OWB-454] - ClassUtil.callInstanceMethod() doesn't propogate original exception
    * [OWB-455] - IllegalArgument method calling remove method of EJB during destroy
    * [OWB-456] - When multiple interceptors are defined for a bean OWB does NOT correctly remove the overriden base Interceptors
    * [OWB-457] - we must not create a SessionContext for static resource reqeusts
    * [OWB-460] - fix owb-openejb and owb-ejb artifactIds
    * [OWB-464] - InjectionPointImpl using wrong class loader during serialize/deserialize, dropping qualifiers, and omiting qualifier values.
    * [OWB-466] - Ensure removal of all ThreadLocal values

Improvement

    * [OWB-177] - Handling of InterceptionType#POST_ACTIVATE, PRE_PASSIVATE and AROUND_TIMEOUT is missing
    * [OWB-407] - detailed information about exceptions
    * [OWB-451] - Allow InterceptorUtil#callAroundInvokes to propogate a callers 'creational context key'
    * [OWB-459] - upgrade to newer library versions
    * [OWB-463] - EjbDefinitionUtility.defineEjbBeanProxy() should be able to create proxies for no-interface local beans
    * [OWB-465] - enhance EJB common code for crude @LocalBean support

TCK Challenge

    * [OWB-394] - Any idea why our BeforeBeanDiscovery.addInterceptorBinding() has different signature?

Task

    * [OWB-453] - add a flag to disable context activation in EJB interceptor
    * [OWB-462] - Refactor AnnotationUtil.hasAnnotationMember()


-------------------------------------------
Release Notes - OpenWebBeans - Version 1.0.0-alpha-2
-------------------------------------------
Bug

    * [OWB-303] - upgrade Javassist to a newer version
    * [OWB-338] - our internal SessionContext and ConversationContext must support Session serialization
    * [OWB-385] - implement passivation of managed beans in ServletContextListener
    * [OWB-401] - ELContextStore not cleaned up for some JSP EL lookups
    * [OWB-402] - OpenWebBeansJsfPlugin does not recognize @ManagedBean
    * [OWB-404] - Contexts must not get stored in a static Map in BeanManager
    * [OWB-405] - AnnotatedElementFactory must not use static cache maps
    * [OWB-408] - NPE in WebBeansELResolver
    * [OWB-415] - EjbBeanProxyHandler for dependent ejb must save ejb instance
    * [OWB-416] - BaseEJBBean.destroyComponentInstance() tries to call multiple remove methods
    * [OWB-418] - EjbBeanProxyHandler must be 1:1 with proxy instances for dependent SFSB
    * [OWB-419] - fix @Dependent handling in ELResolver
    * [OWB-420] - SingletonContext is mapped to @ConversationScoped
    * [OWB-421] - defined-in-class EJB lifecycle callbacks masked by our Interceptor
    * [OWB-426] - Tweak EJBPlugin to work with Standalone Tests
    * [OWB-431] - Generic Type Inheritance not resolved correctly
    * [OWB-434] - ThreadLocal<SingletonContext> doen't get cleaned up
    * [OWB-436] - AbstractContext bean instance creation is not thread safe
    * [OWB-437] - Improve AbstractContext synchronization
    * [OWB-440] - WebBeansDecoratorConfig.getDecoratorStack always returns new Decorators
    * [OWB-442] - our EJB proxies are broken when multiple local interfaces are used on a single class
    * [OWB-443] - Normal-scoped EJB not removed by container during Contextual.destroy()

Improvement

    * [OWB-57] - cleanup problems found by maven-findbugs-plugin
    * [OWB-195] - Give warning to the developer related with non- portable operations
    * [OWB-409] - create a name for OWB our faces-config.xml
    * [OWB-410] - lazy initialisation of ejbInterceptors
    * [OWB-411] - cache calls to isNormalScope()
    * [OWB-412] - Allow container specific extensions to WebConfigurationListener access to the lifecycle
    * [OWB-413] - cache calls to ClassUtils#getObjectMethodNames()
    * [OWB-414] - improve Interceptor performance
    * [OWB-425] - improve performance of owb-el-resolver
    * [OWB-427] - improve read-performance of AbstractOwbBean
    * [OWB-430] - improve performance of WebBeansPhaseListener
    * [OWB-432] - Create Singleton Service SPI
    * [OWB-441] - new configuration properties mechanism

Sub-task

    * [OWB-193] - If an interceptor or decorator has any scope other than @Dependent, non-portable behavior results.
    * [OWB-194] - If an interceptor or decorator has a name, non-portable behavior results.
    * [OWB-196] - If an interceptor or decorator is an alternative, non-portable behavior results.
    * [OWB-197] - If a stereotype declares any other qualifier an- notation, non-portable behavior results.
    * [OWB-198] - If a stereotype is annotated @Typed, non-portable behavior results.


TCK Challenge

    * [OWB-424] - Adding Document for How to Configure and Run TCK (standalone and web profile TCKs)


-------------------------------------------
Release Notes - OpenWebBeans - Version 1.0.0-alpha-1
-------------------------------------------
Bug

    * [OWB-216] - Update pom.xml svn links
    * [OWB-231] - exception using abstract decorators
    * [OWB-245] - Using parameterized type varaibles fails for Producer Method injection
    * [OWB-259] - Implement spec 11.5.5. ProcessModule event
    * [OWB-289] - Owb return 2 beans for Indirect specialized producer beans
    * [OWB-302] - InjectionPoint injections (both method and field based) in Decorators result in null
    * [OWB-312] - Add dopriv's to allow OWB to function with java 2 security enabled
    * [OWB-317] - creationalContext in InvocationContextImpl is always null
    * [OWB-318] - multiple methods with same EJB @interceptors(foo.class) in same bean class get multiple interceptor instances
    * [OWB-327] - annotating an Interceptor with @ApplictionScoped leads to OutOfMemory
    * [OWB-329] - Interceptor instances get created each time the interceptor gets called
    * [OWB-332] - Destroy Depdent Of Producer Method Beans when Invocation Completes
    * [OWB-333] - InjectionTarget and Producer Handling
    * [OWB-334] - cid is missing when using redirect for a jsf 2.0 application
    * [OWB-336] - injected BeanManager must be Serializable
    * [OWB-337] - events must not get broadcasted to beans which have no active Context
    * [OWB-339] - Injecting Non-Contextual Beans Causes NPE in WebBeansUtil
    * [OWB-340] - BeanManagerImpl.createInjectionTarget() Throws Exception When No Constructor Found
    * [OWB-341] - CreationalContext#incompleteInstance should get cleaned after create()
    * [OWB-342] - InterceptorHandler crashes with NullPointerException after deserialisation
    * [OWB-343] - upgrade JPA spec from 1.0-PFD2 to 1.0 final revision
    * [OWB-345] - Remove duplicate dependencies
    * [OWB-351] - OWB picks up @SessionScoped contextual instances from expired sessions
    * [OWB-352] - Thread Safety Problem in our InterceptorHandlers, aka proxies
    * [OWB-353] - NPE in removeDependents@CreationalContextImpl
    * [OWB-354] - WebContextService may throw NPE in tiered classloading environmemt
    * [OWB-357] - WebbeansFinder should index first on ClassLoader, not singleton type
    * [OWB-359] - ownerCreationalContext sometimes causes NPE in InterceptorDataImpl.createNewInstance()
    * [OWB-361] - underlying EJB method not actually in our interceptors stack
    * [OWB-362] - InvocationTargetException when invoking Stateless SessionBean
    * [OWB-363] - Intermittent bug with ApplicationScope disposers not being called
    * [OWB-366] - ContextNotActiveException fired from AppScope/NormalScopedBeanInterceptorHandler when a proxied object finalized
    * [OWB-368] - The 299 spec (that I have) uses receive=IF_EXISTS but OWB uses notifyObserver=IF_EXISTS.
    * [OWB-369] - Static ContextsService in ContextFactory causes wrong webContextService used for multiple applications
    * [OWB-370] - Intransient Conversation context get rdestroyed randomly by destroyWithRespectToTimout
    * [OWB-371] - no lifecycle interceptors for non-contextual EJB
    * [OWB-372] - creational context not cleaned up for non-contextual EJB interceptions
    * [OWB-373] - build crashes with missing artifact error
    * [OWB-374] - migrate jsf2sample from sun to MyFaces
    * [OWB-376] - [patch] Guess example broken with Jetty plugin 6.x due to EL 2.2
    * [OWB-377] - revise logging
    * [OWB-378] - ejb at bottom of decorator stack doesn't handle changed method
    * [OWB-380] - NormalScopedBeanInterceptorHandler throws NPE when handling 3rd party Contexts
    * [OWB-381] - NPE thrown from AbstractInjectable if dependent producer returns null
    * [OWB-382] - injecting a @Dependent bean into a passivatation scoped bean causes a NonSerializableException
    * [OWB-387] - DependentContext Interceptor Double Call for PostConstruct
    * [OWB-390] - fix broken links in our 'site'
    * [OWB-396] - fix poms to work with maven 3
    * [OWB-398] - DelegateHandler cached too agressively
    * [OWB-399] - Proxy objects could not be correctly deserialized by using javassist 3.11. we need to update to 3.12
    * [OWB-400] - starting OWB as part of an EAR in geronimo causes a exception due to missing 'bundle' protocol

Improvement

    * [OWB-116] - Update Business Method Definition
    * [OWB-118] - Supports Decorators for Other Delegate Injections
    * [OWB-136] - fix 'broken' license headers in our java files
    * [OWB-170] - Address findbug issues in webbeans-impl
    * [OWB-183] - Improve webbeans-doc module to get a documentation more user friendly
    * [OWB-214] - get rid of javax.transaction.Transaction dependency in webbeans-impl
    * [OWB-237] - NoSuchElementException when WebBeansConfigurationListener is absent
    * [OWB-275] - remove unused imports and cleanup code
    * [OWB-286] - java.lang.NoClassDefFoundError: javax/validation/Validator
    * [OWB-313] - create caching strategies for resolving Bean<T> for BeanManager and EL invocations
    * [OWB-314] - cache resolved instances in NormalScopedBeanMethodHandlers of @ApplicationScoped beans
    * [OWB-315] - cache resolved instances in NormalScopedBeanMethodHandlers of @SessionScoped beans
    * [OWB-319] - Strange logging when writing non-Serializable SessionScoped bean
    * [OWB-320] - Remove Java EE Dependencies from WebBeans Core
    * [OWB-322] - Create new EJB project and separate common EJB classes from OpenEJB plugin
    * [OWB-325] - Relocate SPI Classes to SPI Module. Change JSR299, JSR330 as optional pom dependency.
    * [OWB-326] - improve producer tests
    * [OWB-328] - improve logger performance
    * [OWB-330] - reduce BeanManagerImpl#getManager() calls inside the same functions
    * [OWB-331] - Cache Interceptor & Decorator Stack oon Interceptor Handler
    * [OWB-346] - Make EJB samples running
    * [OWB-347] - Using InjectableBeanManager in TCK
    * [OWB-349] - ignore exception during type hierarchy scan
    * [OWB-350] - Support Interceptor for non-contextual EJBs
    * [OWB-355] - OpenEjbBean should look for @Remove methods
    * [OWB-356] - EjbPlugin only looks for DeployementInfo once, so new deployed application won't be discovered
    * [OWB-358] - provide property to skip injection in @PostConstruct of OpenWebBeansEjbInterceptor
    * [OWB-360] - Add BeanManager to a ServletContext attribute
    * [OWB-364] - Reduce the amount of info level logging
    * [OWB-365] - make injection optional in OWBEJBInterceptor
    * [OWB-375] - Performance: OWB logging performs operations when logging disabled.
    * [OWB-379] - upgrade to final atinject-spec artifact
    * [OWB-386] - upgarde CDI TCK to 1.0.2.CR1
    * [OWB-389] - atinject-tck upgrade to final 1.0 release
    * [OWB-397] - Add helper method and some debug to WebBeansFinder

New Feature

    * [OWB-316] - Implement a generic TestContainer for CDI implementations
    * [OWB-323] - Provide methods to pass classloader into ServiceLoader and WebBeansFinder for use in tiered classloader situations
    * [OWB-324] - Add Tomcat Plugin
    * [OWB-348] - Adding Interceptor and Decorator Support for EJB Beans
    * [OWB-395] - OpenWebBeans Tomcat 7 Support

TCK Challenge

    * [OWB-388] - Pass TCK 1.0.2 CR1 Web Profile

Task

    * [OWB-6] - Scope passivation
    * [OWB-14] - Update WebBeans Lifecycle for Servlet Beans
    * [OWB-46] - Injection into non-contextual objects
    * [OWB-204] - Update Samples for JSF2 Usage
    * [OWB-220] - Update site.xml links and bread crumbs to point to non-incubator.
    * [OWB-310] - Drop dom4j and use jre builtin xml parsers for processing beans.xml
    * [OWB-391] - create a owb-build-tools project to maintain project specific checkstyle rules, etc.

Test

    * [OWB-56] - Integrate the official JSR-299 TCK test suite
    * [OWB-222] - Update website download link, and fix relative URL translation
    * [OWB-367] - Add a unit test for IF_EXISTS


----------------------------------------------
Required Platform
----------------------------------------------
Java Version : Java SE >= 1.6


---------------------------------------------
How to Configure OpenWebBeans
---------------------------------------------

This section explains a content of the distribution bundle, OWB plugins and its
dependent libraries.

---------------------------------------------
1.1.6 Distribution Content
---------------------------------------------
There are several jars in the OpenWebBeans 1.0.0 distribution;

 - openwebbeans-impl-1.1.6.jar     --> Includes Core Dependency Injection Service.
 - openwebbeans-ejb-1.1.6.jar      --> EJB Plugin(Supports EJBs in OpenEJB embedded in Tomcat).
 - openwebbeans-jms-1.1.6.jar      --> JMS Plugin(Supports injection of JMS related artifacts,i.e, ConnectionFactory, Session, Connection etc.)
 - openwebbeans-jsf-1.1.6.jar      --> JSF-2.0 Plugin(JSF Conversation Scoped Support).
 - openwebbeans-jsf12-1.1.6.jar    --> JSF-1.2 Plugin(JSF Conversation Scoped Support).
 - openwebbeans-resource-1.1.6.jar --> Java EE Resource Injection for Web Projects (Includes @PersistenceContext,@PersistenceUnit
                                          and @Resource injection into the Managed Beans. @Resource injections use java:/comp/env of the
                                          Web application component. @PersistenceContext is based on extended EntityManager.
 - openwebbeans-spi-1.1.6.jar      --> OpenWebBeans Server Provider Interfaces. They are implemented by runtime environments that would
                                          like to use OpenWebBeans as a JSR-299 implementation.
 - samples                            --> Includes source code of the samples. Samples are mavenized project  therefore you can easily build and run
                                          them from your environment that has maven runtime.
 - openwebbeans-osgi-1.1.6.jar     --> ClassPath ScannerService SPI implementation for OSGI environments like Apache Geronimo-3
 - openwebbeans-web-1.1.6.jar      --> Basic Servlet integration
 - openwebbeans-tomcat6-1.1.6.jar  --> Support for deeper integration into Apache Tomcat-6
 - openwebbeans-tomcat7-1.1.6.jar  --> Support for deeper integration into Apache Tomcat-7




------------------------------------------
How OWB Plugins Work
------------------------------------------

OpenWebBeans has been developed with a plugin architecture. The Core dependency injection service
is provided with openwebbeans-impl. If you need further service functionality,
you have to add respective plugin jars into the application classpath. OpenWebBeans
uses the Java SE 6.0 java.util.ServiceLoader mechanism to pickup plugins at runtime.
If you run under Java SE 5.0, an similar hand written implementation will be used.
Please do not confuse OWB plugins with portable Extensions! OWB plugins are for
internal use only whereas portable CDI Extensions will run on any JSR-299 container.

Current Plugins:
---------------------
Look at "1.1.6 Distribution Content" above.

------------------------------------------
Dependent Libraries
------------------------------------------

Third Party jars:
-----------------
They are necessary at runtime in the Core Implementation.

javassist : Version 3.12.0.GA
scannotation : Version 1.0.2 (if not running in an OSGi environment like Apache Geronimo-3)

Java EE APIs jars(Container Provider Libraries) :
-------------------------------------------------
Generally full Java EE servers provides these jars. But web containers like Tomcat or Jetty
do not contain some of them, such as JPA, JSF, Validation API etc. So, if you do not want to bundle
these libraries within your application classpath, you have to include these libraries in your
server common classpath.

jcdi-api (JSR-299 Specification API)
atinject-api (JSR-330 Specification API)
servlet-2.5 or servlet 3.0 (Servlet Specification API)
ejb-3.1 (EJB Specification API)
el-2.2 (Expression Langauge Specification API)
jsf-2.0 (Java Server Faces API)
jsr-250 (Annotation API)
interceptor-1.1 (Interceptor API)
jta-1.1 (Java Transaction API)
jsp.2.1 or jsp-2.2 (Java Server Pages API)
jpa-2.0 (Java Persistence API)
jaxws-2.1 or jaxws-2.2 (Java Web Service API)
jms-1.1 or jms-1.2 (Java Messaging Service API)
validation (Validation Specification)

Dependencies of OpenWebBeans Maven Modules&Plugins
--------------------------------------------------

openwebbeans-impl :
------------------
Third party        : javassist, scannotation, openwebbeans-spi
Container Provided : jcdi-api, at-inject, servlet, el, jsr-250, interceptor, jta, jsp, validation

openwebbeans-ejb:
-----------------
Third party        : openwebbeans-impl and its dependencies
Container Provided : OpenWebBeans EJB plugin is based on OpenEJB in Tomcat. Therefore, if you install OpenEJB
                     within Tomcat correctly, there is no need to add any additional libraries. Look at the
                     OpenEJB in Tomcat configuration section.

openwebbeans-jms:
-----------------
Third party        : openwebbeans-impl and its dependencies
Container Provided : jms

openwebbeans-jsf:
-----------------
Third party        : openwebbeans-impl and its dependencies
Container Provided : jsf

NOTE : We are trying to decrease dependent libraries of the our core, i.e, openwebbeans-impl.
At 1.1.6, dependent third party libraries will be decreased. We have a plan to create profile
plugins, therefore each profile plugin provides its own dependent libraries. For example, in
fully Java EE Profile Plugin, Transaction API is supported but this will not be the case
for Java Web Profile Plugin or Java SE Profile Plugin. Stay Tuned!

Currently, as you have seen above, openwebbeans-impl depends on some Java EE/Runtime
provided libraries (servlet, jsp, el etc). In the future, with OpenWebBeans profiling support,
openwebbeans-impl will not depend on any Java EE APIs. Those APIs will be provided
by OpenWebBeans profiles/plugins that openwebbeans-impl will be used. Therefore,
you will able to use OpenWebBeans in your own runtime environment easily by writing
your own plugins and contributing it to OpenWebBeans :)

------------------------------------------
Library Configuration
------------------------------------------
To run openwebbeans applications in the Java EE based application server,
you could add the JSR-299 API and JSR-330 API into the server common classpath, and
implementation, plugins and dependent jars into your "WEB-INF/lib" directory
of the Java EE Web Application.

In this release, we can not support the OpenWebBeans as an integrated
functionality of the Java EE Application Servers. So, you have to manage the
configuration of the OpenWebBeans within your application's "web.xml" file. A sample "web.xml"
file can be found in the "config" directory. To use EJB functionality, you also have to
add OWB specific interceptor into your EJB beans. Look at the EJB section of this readme
for further details.

---------------------------------------------
OpenWebBeans Properties File
---------------------------------------------
OpenWebBeans uses a default configuration file to configure some of its
properties. Default configuration files are embedded into OWB implementation
jar files. Instead of opening the jars file and changing configuration properties, simply add
an "openwebbeans.properties" file into a "META-INF/openwebbeans" folder of your application
classpath. This will override the values from the default configuration.
You can specify a property 'configuraion.ordinal' in this file to define the overlay order.
A properties file with higher 'configuration.ordinal' value will applied later and thus
have a higher precedence. If you don't specify a 'configuration.ordinal' a value of 100 is assumed;
This allows to have multiple openwebbeans.properties files e.g. a common one in an EAR lib
(with configuration.ordinal=100) and more specific ones for each WebApp in your EAR (with a
configuration.ordinal of e.g. 101).

Each plugin developer can provide their own SPI implementation class and own configuration values. If you would like
to use those implementation classes or configuration values, you have to override the default configuration file as explained
in the above paragraph, i.e, putting "openwebbeans.properties" file into "META-INF/openwebbeans" folder of your application.
It is recommended to use a 'configuration.ordinal' between 50 and 99 for custom SPI implementations.

Below are OpenWebBeans' default configuration properties from our openwebbeans-impl.jar file and our plugins such as
our OpenEJB plugin.

Override default value of ResourceInjectionService
-------------------------------------------------
org.apache.webbeans.spi.ResourceInjectionService=org.apache.webbeans.ejb.resource.OpenEjbResourceInjectionService

OpenWebBeans uses the "OpenEjbResourceInjectionService" class to inject resources into the managed bean instances.

Configuration Names and Their Default Values :

- "org.apache.webbeans.spi.ContainerLifecycle"
   Description : Implementation of org.apache.webbeans.spi.ContainerLifecycle. All magic starts from here.
   Values      : org.apache.webbeans.lifecycle.DefaultLifecycle, OR CUSTOM
   Default     : org.apache.webbeans.lifecycle.DefaultLifecycle

- "org.apache.webbeans.spi.JNDIService"
   Description  : Configures JNDI provider implementation.
   Values       : org.apache.webbeans.spi.se.DefaultJndiService OR CUSTOM
   Default      : org.apache.webbeans.spi.se.DefaultJndiService

- "org.apache.webbeans.spi.conversation.ConversationService"
   Description  : Implementation of conversation.
   Values       : org.apache.webbeans.spi.conversation.jsf.DefaultConversationService OR CUSTOM
   Default      : org.apache.webbeans.spi.conversation.jsf.DefaultConversationService

- "org.apache.webbeans.spi.ScannerService"
   Description  : Default implementation of org.apache.webbeans.spi.ScannerService. It is used for scanning application deployment
                  for finding bean classes and configuration files.
   Values       : org.apache.webbeans.spi.ee.deployer.DefaultScannerService OR CUSTOM
   Default      : org.apache.webbeans.spi.ee.deployer.DefaultScannerService

- "org.apache.webbeans.spi.SecurityService"
   Description   : Implementation of org.apache.webbeans.spi.SecurityService. It is used for getting current "Principal".
   Values        : org.apache.webbeans.spi.se.DefaultSecurityService or CUSTOM
   Default       : org.apache.webbeans.spi.se.DefaultSecurityService

- "org.apache.webbeans.spi.ValidatorService"
   Description   : Implementation of org.apache.webbeans.spi.ValidatorService. It is used for getting "ValidatorFactory" and "Validator".
   Values        : org.apache.webbeans.spi.se.DefaultValidatorService or CUSTOM
   Default       : org.apache.webbeans.spi.se.DefaultValidatorService

- "org.apache.webbeans.spi.TransactionService"
   Description   : Implementation of org.apache.webbeans.spi.TransactionService. It is used for getting "TransactionManager" and "Transaction".
   Values        : org.apache.webbeans.spi.se.DefaultTransactionService or CUSTOM
   Default       : org.apache.webbeans.spi.se.DefaultTransactionService

- "org.apache.webbeans.spi.ResourceInjectionService"
   Description   : Implementation of org.apache.webbeans.spi.ResourceInjectionService. It is used for injection Java EE enviroment resource into the
                   Managed Bean instances.
   Values        : org.apache.webbeans.se.DefaultResourceInjectionService or CUSTOM
   Default       : org.apache.webbeans.se.DefaultResourceInjectionService

- "org.apache.webbeans.spi.JNDIService.jmsConnectionFactoryJndi"
   Description   : Configures JMS ConnectionFactory object jndi name
   Values        : Server specific JNDI name
   Default       : ConnectionFactory

- "org.apache.webbeans.spi.deployer.useEjbMetaDataDiscoveryService"
   Description   : Use EJB functionality or not. If use OpenEJB configures to true
   Values        : false, true
   Default       : false

- "org.apache.webbeans.spi.FailOverService"
   Description   : Implementation of the org.apache.webbeans.spi.FailOverService. It is used for enabling passivation/failover beans.
   Values        : org.apache.webbeans.web.failover.DefaultOwbFailOverService or CUSTOM
   Default       : org.apache.webbeans.web.failover.DefaultOwbFailOverService

- "org.apache.webbeans.web.failover.issupportfailover"
   Description   : Support failover of beans or not
   Values        : false, true
   Default       : false

- "org.apache.webbeans.web.failover.issupportpassivation"
   Description   : Support passivation of beans or not
   Values        : false, true
   Default       : false

- "org.apache.webbeans.forceNoCheckedExceptions"
   Description   : The interceptors spec defines that @PostConstruct & Co must not throw checked Exceptions. Setting this configuration to 'false' disables this check.
   Values        : false, true
   Default       : true

- "org.apache.webbeans.spi.SecurityService"
   Description   : Service to provide methods which must be guarded via doPrivileged blocks.
   Values        : org.apache.webbeans.corespi.security.SimpleSercurityService or org.apache.webbeans.corespi.security.ManagedSecurityService
   Default       : org.apache.webbeans.corespi.security.SimpleSercurityService


---------------------------------------------
How to Run Samples
---------------------------------------------

In this release, there are several sample applications located in the "/samples" directory
of the distribution. You can run those samples via simple maven command.

1) "Guess Application" : Simple usage of the OWB + JSF.
It can be run in the jetty web container via maven jetty plugin from source.
Look at "Compile and Run Samples via Jetty&Tomcat Plugin" section.

2) "Hotel Reservation Application" : Show usage of JSF + JPA + OWB
It can be run in the jetty web container via maven jetty plugin from source.
Look at "Compile and Run Samples via Jetty&Tomcat Plugin" section.

3) "JMS Injection Sample" : Show JMS injections. JMS injection currently uses
   ConnectionFactory as JMS connection factory jndi name. You can change this
   via configuration file. Look above explanation for how to configure JMS jndi. Also,
   JMS injection requires to use of a JMS provider. Generally Java EE servers contains
   default JMS provider. It can be run on JBoss and Geronimo. It uses Queue with jndi_name = "queue/A".
   So you have to create a queue destination in your JMS provider with name "queue/A" to run example.
   If you want to change queue jndi name, then look at source and change it from "WEB-INF/beans.xml" file.

4) "Conversation Sample" : Shows usage of JSF conversations.
It can be run in the jetty web container via maven jetty plugin from source.
Look at "Compile and Run Samples via Jetty&Tomcat Plugin" section.

5) "JSF2 Sample" : Shows usage of JSF2 Ajax.
It can be run in the jetty web container via maven jetty plugin from source.
Look at "Compile and Run Samples via Jetty&Tomcat Plugin" section. It requires
to use JSF2 runtime.


6) "Standalone Sample" : Shows usage of OpenWebBeans in Standalone Swing Application.
Look at "OpenWebBeans in Java SE" section.

Configuring and Running the Applications:
--------------------------------------------
See section Compile and Run Samples via Tomcat Plugin.

--------------------------------------------
Maven Install and Package From the Source
--------------------------------------------

Maven Version : Apache Maven 3.2.1 or later

First you have to download the "source" version of the OpenWebBeans project that
contains the all source code of the OpenWebBeans.

To install the Maven artifacts of the project from the source, Maven must be installed
in your runtime. After Maven installation, just run the following command in the top level
directory that contains the main "pom.xml" :

> mvn clean install

This command will install all the Maven artifacts into your local Maven repository.

If you wish to package all artifacts of the project, just run the following command
in in the top level directory that contains the main "pom.xml" :

> mvn clean package

This command will package the project artifacts from the source and put these artifacts into the each modules
respective "target" directory.

-------------------------------------------
Compile and Run Samples via Tomcat&Jetty Plugin
-------------------------------------------
This section shows how to run samples in Jetty or OpenEJB Embedded Tomcat.

------------------------------------------
Samples Run within Jetty Plugin
------------------------------------------
You can compile and run "guess","jsf2","conversation-sample" and "reservation "samples via maven Jetty plugin.
Go to the source bundle "samples/" directory. In the "guess/" or "reservation/" directory, run
the following maven commands. It will start up maven Jetty container. It bundles all of the
required jars into the WEB-INF/lib folder. You are not required to add any jar to the classpath.

Samples : Guess and Reservation
------------------------------
Go to the source folder of projects and run

> mvn clean install -Pjetty
> mvn jetty:run -Pjetty

Guess URL               : http://localhost:8080/guess
Reservation URL         : http://localhost:8080/reservation

Samples : Conversation Sample and JSF2
-------------------------------------
Go to the source folder of projects and run

>mvn clean install
>mvn jetty:run

Conversation Sample URL : http://localhost:8080/conversation-sample
JSF2 Sample URL         : http://localhost:8080/jsf2sample

------------------------------------------
Samples Run within Tomcat Plugin
------------------------------------------
OpenEJB samples are run with Maven Tomcat Plugin.

Tomcat Plugin uses http://localhost:8080/manager application to deploy war file
into your embeddable EJB Tomcat container. There must be an tomcat-users.xml
file in the "conf" directory of the server that contains manager role and username.

>Start Tomcat server if not started
>mvn tomcat:deploy

Ejb Sample URL    : http://localhost:8080/ejb-sample
Ejb Telephone URL : http://localhost:8080/ejb-telephone

Example tomcat-users.xml file
------------------------------------------
<tomcat-users>
<role rolename="manager"/>
<user username="admin" password="" roles="manager"/>
</tomcat-users>

-----------------------------------------
Deploy JMS Sample
-----------------------------------------
Simple drops jms-sample.war file into your application deploy location.

JMS Sample Example URL        : Hit the url http://localhost:8080/jms-sample/sender.jsf for sending JMS messages
                                Hit the url http://localhost:8080/jms-sample/receiver.jsf for receiving JMS messages

-----------------------------------------
OpenWebBeans in Java SE
----------------------------------------
OpenWebBeans can be used in Java SE environments such as Java Swing
applications. A Standalone Sample is provided to show how to use OpenWebBeans
in Java SE.

Go to the source directory of the standalone sample:
>mvn clean package;
>cd target;
>jar -xvf standalone-sample.jar
>java -jar standalone-sample-1.1.6-SNAPSHOT.jar
>Enjoy :)

-----------------------------------------------
OpenWebBeans User and Development Mailing Lists
-----------------------------------------------
Please mail to the user mailing list with any questions or advice
about the OpenWebBeans.

User Mailing List : [users@openwebbeans.apache.org]

You can also join the discussions happening in the dev list

Dev Mailing List  : [dev@openwebbeans.apache.org]

-------------------------------------------
OpenWebBeans JIRA Page
-------------------------------------------
Please logs bugs into the "https://issues.apache.org/jira/browse/OWB".

------------------------------------------
OpenWebBeans Wiki and Blog Page
-----------------------------------------
Wiki: http://cwiki.apache.org/OWB/
Introduction to OpenWebBeans : http://cwiki.apache.org/OWB/introduction-to-openwebbeans.html
Blog : http://blogs.apache.org/owb

-----------------------------------------
OpenWebBeans Web Page
----------------------------------------
You can reach the OpenWebBeans web page at
http://openwebbeans.apache.org
---------------------------------------

Enjoy!

Your Apache OpenWebBeans Team

