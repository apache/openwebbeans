<!--
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
-->
<%@ page import="java.util.Date" %>
<%@ page import="jakarta.enterprise.inject.spi.CDI" %>
<%@ page import="org.superbiz.beans.RequestScopedBean" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    System.out.println("entering target.jsp page");
    CDI.current().select(RequestScopedBean.class).get().ping();
%>
<html>
<head>
    <title>target page</title>
</head>
<body>
All fine!
</body>
</html>
<%
    System.out.println("leaving target.jsp page");
%>
