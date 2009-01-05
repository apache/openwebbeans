--------------------------------
What is OpenWebBeans?
--------------------------------
OpenWebBeans is an ASL-License implementation of the JSR-299, WebBeans Specification.

Project web page could be found at the URL : 
http://incubator.apache.org/projects/openwebbeans.html

--------------------------------
OpenWebBeans M1 Release Content
--------------------------------

- M1 Release Supports the followings
-----------------------------------
* Simple WebBeans Support
* Producer Method Support
* Event Support
* Decorator and Interceptor Support
* Experimental XML Configuration Support
* Lookup and Dependency Injection Support
* Java EE Plugin Support (via ServetContextListener interface)

- M1 Release Does not Supports the followings
--------------------------------------------
* Enterprise WebBeans Support
* JMS WebBeans Support
* Producer Field Support
* Servlet Injection Support
* Inheritance, Stereotype Inheritance and Realization Support
* Common Annotations Support
* Full Support for Validation Checks
* Passivation Scope and Serialization Operations
* Full Support for XML Configuration
* Java EE Container Integration Support (SPI)

---------------------------------------------
How to Configure The OpenWebBeans
---------------------------------------------

There are two important jars for OpenWebBeans;

 - webbeans-api-1.0.0-SNAPSHOT.jar
 - webbeans-impl-1.0.0-SNAPSHOT.jar

There are also a dependent libraries. These dependent library jars
are located in the directory "/lib/thirdparty". 

Java EE APIs jars used by the project are located in the directory
"lib/javaee" folder. You could put necessary Java EE jars into the 
server classpath if the server is not contains these jars already. 

To run openwebbeans applications in the Java EE based application server, 
you could add OpenWebBeans API, Implementation and dependent jars into 
the common classpath of the Java EE Application Server or your WEB-INF/lib 
directory of the Java EE Web Application.

In this release, we can not support the OpenWebBeans as an integrated
functionality of the Java EE Application Servers. So, you have to manage the
configuration of the OpenWebBeans within your "web.xml" file. A sample web.xml
file can be found in the "config" directory.

---------------------------------------------
How to Run The Samples
---------------------------------------------

In this release, there is a sample application called "Login and Guess". It is
located in the "/samples" directory of the root folder. In the distribution, there are
binary and source versions of the samples project could be found. Name of the binary 
file is the "samples/guess.war", you can deploy it into the any Java EE web container. 

Before this, you have to configure OpenWebBeans runtime, 
see "How to Configure OpenWebBeans.

--------------------------------------------
Build and Package  From the Source
--------------------------------------------
To install the Maven artifacts of the project from the source, Maven must be installed
in your runtime. After Maven installation, just run the following command in the src/ folder : 

> mvn clean install

This command will install all the Maven artifacts in your local Maven respository.

If you wish to build all the artifacts of the project, just run the following command
in the src/ folder : 

> mvn clean package -Ddistribute=true

This command will package the project artifacts of the project from the source and put it into the "src/distribution/target/"
directory.

-------------------------------------------
OpenWebBeans User Mail List
-------------------------------------------

Please mail to the user mailing list about any questions or advice
about the OpenWebBeans.

User Mailing List : [openwebbeans-users@incubator.apache.org]

-------------------------------------------
OpenWebBeans JIRA Page
-------------------------------------------

Please logs the bugs into the "https://issues.apache.org/jira/browse/OWB".
