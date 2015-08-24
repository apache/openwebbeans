@echo off
REM -----------------------------------------------------------------------------
REM Licensed to the Apache Software Foundation (ASF) under one
REM or more contributor license agreements.  See the NOTICE file
REM distributed with this work for additional information
REM regarding copyright ownership.  The ASF licenses this file
REM to you under the Apache License, Version 2.0 (the
REM "License"); you may not use this file except in compliance
REM with the License.  You may obtain a copy of the License at
REM
REM http://www.apache.org/licenses/LICENSE-2.0
REM
REM Unless required by applicable law or agreed to in writing,
REM software distributed under the License is distributed on an
REM "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
REM KIND, either express or implied.  See the License for the
REM specific language governing permissions and limitations
REM under the License.
REM -----------------------------------------------------------------------------

REM -----------------------------------------------------------------------------
REM Install script for Apache OpenWebBeans to Tomcat7 and higher
REM
REM This script ALSO works with Tomcat8 and Tomcat9!
REM
REM usage example:
REM First unzip the openwebbeans binary distribution file.
REM From within the unzipped folder start this script:
REM $> install_owb_tomcat7.bat c:\opt\apache-tomcat-8.0.23
REM instead of passing the parameter you can also set the
REM CATALINA_HOME environment variable
REM -----------------------------------------------------------------------------


REM -----------------------------------------------------------------------------
REM set environment variables
REM -----------------------------------------------------------------------------

if defined %1 GOTO noParam
    SET OWB_CATALINA_HOME=%CATALINA_HOME%

:noParam

if defined CATALINA_HOME GOTO catalina_defined
    SET OWB_CATALINA_HOME=%1

if defined OWB_CATALINA_HOME GOTO catalina_defined
    echo USAGE: %0 tomcat_install_dir
    GOTO end

:catalina_defined


if exist %OWB_CATALINA_HOME%\lib\catalina.jar GOTO okCatalina
    echo "ERROR: CATALINA_HOME or first parameter doesn't point to a valid tomcat installation!"
    GOTO end

:okCatalina

echo "OWB_INSTALLER: installing Apache OpenWebBeans to %OWB_CATALINA_HOME%"

REM -----------------------------------------------------------------------------
REM first ersase all leftovers from a previous install
REM -----------------------------------------------------------------------------
echo "OWB_INSTALLER: erase old OpenWebBeans Artifacts from Tomcat installation"

del %OWB_CATALINA_HOME%\lib\openwebbeans*.jar
del %OWB_CATALINA_HOME%\lib\xbean-asm*.jar
del %OWB_CATALINA_HOME%\lib\xbean-finder*.jar
del %OWB_CATALINA_HOME%\lib\geronimo-annotation*.jar
del %OWB_CATALINA_HOME%\lib\geronimo-interceptor*.jar
del %OWB_CATALINA_HOME%\lib\geronimo-jcdi*.jar
del %OWB_CATALINA_HOME%\lib\geronimo-atinject*.jar


REM -----------------------------------------------------------------------------
REM next we copy the openwebbeans libraries into the tomcat lib dir
REM -----------------------------------------------------------------------------
echo "OWB_INSTALLER: copying OpenWebBeans Artifacts to Tomcat installation"

copy openwebbeans-impl-*.jar %OWB_CATALINA_HOME%\lib\
copy xbean-asm*.jar %OWB_CATALINA_HOME%\lib\
copy xbean-finder*.jar %OWB_CATALINA_HOME%\lib\

copy api\geronimo-atinject_*.jar %OWB_CATALINA_HOME%\lib\
copy api\geronimo-jcdi_*.jar %OWB_CATALINA_HOME%\lib\
copy api\geronimo-interceptor_*.jar %OWB_CATALINA_HOME%\lib\
copy api\geronimo-annotation_*.jar %OWB_CATALINA_HOME%\lib\

copy spi\openwebbeans-spi-*.jar %OWB_CATALINA_HOME%\lib\

copy plugins\openwebbeans-web-*.jar %OWB_CATALINA_HOME%\lib\
copy plugins\openwebbeans-el22-*.jar %OWB_CATALINA_HOME%\lib\
copy plugins\openwebbeans-tomcat7-*.jar %OWB_CATALINA_HOME%\lib\
copy plugins\openwebbeans-jsf-*.jar %OWB_CATALINA_HOME%\lib\openwebbeans-jsf.jar.disabled


REM -----------------------------------------------------------------------------
REM as last step we add the OWB tomcat7 listener
REM -----------------------------------------------------------------------------
echo ...
echo "OWB_INSTALLER: add OpenWebBeans ContextLifecycleListener to" %CATALINA_HOME%\conf\context.xml

echo ATTENTION: THIS STEP NEEDS TO BE DONE MANUALLY UNDER WINDOWS!
echo change your tomcats conf\context.xml to contain the following entry in the "<Context>" element:
echo "<Listener className="org.apache.webbeans.web.tomcat7.ContextLifecycleListener" />"


:end
set OWB_CATALINA_HOME=
