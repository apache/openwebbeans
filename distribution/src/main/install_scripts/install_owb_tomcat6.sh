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
# Install script for Apache OpenWebBeans to Tomcat6
#
# usage example:
# First unzip the openwebbeans binary distribution file.
# From within the unzipped folder start this script:
# $> ./install_owb_tomcat7.sh /opt/apache-tomcat-8.0.23
# instead of passing the parameter you can also set the
# CATALINA_HOME environment variable
# -----------------------------------------------------------------------------


# -----------------------------------------------------------------------------
# set environment variables
# -----------------------------------------------------------------------------
if [ -z "$CATALINA_HOME" ]; then
    export CATALINA_HOME="$1"
fi

if [ -z "$CATALINA_HOME" ]; then
    echo "USAGE: $0 tomcat_install_dir"
    exit -1
fi


if [ ! -f "$CATALINA_HOME/lib/catalina.jar" ]; then
    echo "ERROR: CATALINA_HOME or first parameter doesn't point to a valid tomcat installation!"
    exit -1
fi

echo "OWB_INSTALLER: installing Apache OpenWebBeans to $CATALINA_HOME"

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
cp plugins/openwebbeans-tomcat6-*.jar "$CATALINA_HOME"/lib/
cp plugins/openwebbeans-jsf-*.jar "$CATALINA_HOME"/lib/openwebbeans-jsf.jar.disabled


# -----------------------------------------------------------------------------
# as last step we add the OWB tomcat(6) listener
# -----------------------------------------------------------------------------
echo "OWB_INSTALLER: add OpenWebBeans ContextLifecycleListener to ${CATALINA_HOME}/conf/context.xml"

if grep -q "tomcat.ContextLifecycleListener" "${CATALINA_HOME}"/conf/context.xml ; then
    echo "OpenWebBeans context already in place"
else
    sed -i -- 's/<Context>/<Context>\
    <Listener className="org.apache.webbeans.web.tomcat.ContextLifecycleListener" \/>/g' "${CATALINA_HOME}"/conf/context.xml
fi
