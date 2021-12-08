#!/usr/bin/env bash
# -----------------------------------------------------------------------------
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
# -----------------------------------------------------------------------------

# -----------------------------------------------------------------------------
# Install script for Apache OpenWebBeans and Apache MyFaces to Tomcat7 and higher
#
# This script ALSO works with Tomcat8 and Tomcat9!
#
# usage example:
# Download the openwebbeans binary distribution from
#  https://openwebbeans.apache.org/download.html
# Also download the Apache MyFaces binary distribution from
#  http://myfaces.apache.org/download.html
#
# Then unzip the openwebbeans binary distribution file
# and change into it. This is where you find THIS script ;)
#
# From within the unzipped folder start this script:
# $> ./install_owb_tomcat7.sh somelocation/myfaces-core-assembly-2.2.8-bin.zip /opt/apache-tomcat-8.0.23
#
# The first parameter is the downloaded MyFaces binary zip file, the second parameter
# is the installation base directory of your tomcat installation.
#
# -----------------------------------------------------------------------------


# -----------------------------------------------------------------------------
# set environment variables
# -----------------------------------------------------------------------------
if [ -z "$CATALINA_HOME" ]; then
    export CATALINA_HOME="$2"
fi

if [ -z "$CATALINA_HOME" ]; then
    echo "USAGE: $0 myfaces-core-assembly-x.x.x-bin.zip tomcat_install_dir"
    exit -1
fi


if [ ! -f "$CATALINA_HOME/lib/catalina.jar" ]; then
    echo "ERROR: CATALINA_HOME or second parameter doesn't point to a valid tomcat installation!"
    exit -1
fi

if [ ! -f "$1" ]; then
    echo "ERROR: MyFaces bundle parameter missing. Download the zip at http://myfaces.apache.org/download.html"
    exit -1
fi


echo "OWB_INSTALLER: installing Apache OpenWebBeans and MyFaces to $CATALINA_HOME"

# -----------------------------------------------------------------------------
# first ersase all leftovers from a previous install
# -----------------------------------------------------------------------------
echo "OWB_INSTALLER: erase old OpenWebBeans Artifacts from Tomcat installation"

rm -f "$CATALINA_HOME"/lib/openwebbeans*.jar
rm -f "$CATALINA_HOME"/lib/xbean-asm*.jar
rm -f "$CATALINA_HOME"/lib/xbean-finder*.jar
rm -f "$CATALINA_HOME"/lib/geronimo-annotation*.jar
rm -f "$CATALINA_HOME"/lib/geronimo-interceptor*.jar
rm -f "$CATALINA_HOME"/lib/geronimo-jcdi*.jar
rm -f "$CATALINA_HOME"/lib/geronimo-atinject*.jar
rm -f "$CATALINA_HOME"/lib/myfaces-api-*.jar
rm -f "$CATALINA_HOME"/lib/myfaces-impl-*.jar
rm -f "$CATALINA_HOME"/lib/lib/commons-beanutils-*.jar
rm -f "$CATALINA_HOME"/lib/lib/commons-codec-*.jar
rm -f "$CATALINA_HOME"/lib/lib/commons-collections-*.jar
rm -f "$CATALINA_HOME"/lib/lib/commons-digester-*.jar
rm -f "$CATALINA_HOME"/lib/lib/commons-logging-*.jar


# -----------------------------------------------------------------------------
# next we copy the openwebbeans libraries into the tomcat lib dir
# -----------------------------------------------------------------------------
echo "OWB_INSTALLER: copying OpenWebBeans Artifacts to Tomcat installation"

cp openwebbeans-impl-*.jar "$CATALINA_HOME"/lib/
cp xbean-asm*.jar "$CATALINA_HOME"/lib/
cp xbean-finder*.jar "$CATALINA_HOME"/lib/

cp api/geronimo-atinject_*.jar "$CATALINA_HOME"/lib/
cp api/geronimo-jcdi_*.jar "$CATALINA_HOME"/lib/
cp api/geronimo-interceptor_*.jar "$CATALINA_HOME"/lib/
cp api/geronimo-annotation_*.jar "$CATALINA_HOME"/lib/

cp spi/openwebbeans-spi-*.jar "$CATALINA_HOME"/lib/

cp plugins/openwebbeans-web-*.jar "$CATALINA_HOME"/lib/
cp plugins/openwebbeans-el22-*.jar "$CATALINA_HOME"/lib/
cp plugins/openwebbeans-tomcat7-*.jar "$CATALINA_HOME"/lib/
cp plugins/openwebbeans-jsf-*.jar "$CATALINA_HOME"/lib/

# -----------------------------------------------------------------------------
# then unzip the myfaces bundle and copy the files to the tomcat lib dir
# -----------------------------------------------------------------------------
rm -rf myfaces_zip
mkdir myfaces_zip
cd myfaces_zip
echo "Unzipping MyFaces bundle $1"
unzip $1
cd myfaces-core-*

cp lib/myfaces-api-*.jar "$CATALINA_HOME"/lib/
cp lib/myfaces-impl-*.jar "$CATALINA_HOME"/lib/
cp lib/commons-beanutils-*.jar "$CATALINA_HOME"/lib/
cp lib/commons-codec-*.jar "$CATALINA_HOME"/lib/
cp lib/commons-collections-*.jar "$CATALINA_HOME"/lib/
cp lib/commons-digester-*.jar "$CATALINA_HOME"/lib/
cp lib/commons-logging-*.jar "$CATALINA_HOME"/lib/


# -----------------------------------------------------------------------------
# as last step we add the OWB tomcat7 listener
# -----------------------------------------------------------------------------
echo "OWB_INSTALLER: add OpenWebBeans ContextLifecycleListener to ${CATALINA_HOME}/conf/context.xml"

if grep -q "tomcat7.ContextLifecycleListener" "${CATALINA_HOME}"/conf/context.xml ; then
    echo "OpenWebBeans context already in place"
else
    sed -i -- 's/<Context>/<Context>\
    <Listener className="org.apache.webbeans.web.tomcat7.ContextLifecycleListener" \/>/g' "${CATALINA_HOME}"/conf/context.xml
fi
