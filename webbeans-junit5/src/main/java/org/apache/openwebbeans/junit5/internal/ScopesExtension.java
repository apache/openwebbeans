/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.openwebbeans.junit5.internal;

import org.apache.openwebbeans.junit5.Scopes;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.spi.ContextsService;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.util.AnnotationUtils;

import javax.enterprise.context.ConversationScoped;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.context.SessionScoped;
import javax.enterprise.context.spi.Context;
import java.lang.annotation.Annotation;
import java.util.stream.Stream;

public class ScopesExtension implements BeforeAllCallback, AfterAllCallback, BeforeEachCallback, AfterEachCallback
{
    private Runnable[] classCallbacks;
    private Runnable[] methodCallbacks;

    @Override
    public void afterAll(final ExtensionContext context)
    {
        stop(classCallbacks);
    }

    @Override
    public void afterEach(final ExtensionContext context)
    {
        stop(methodCallbacks);
    }

    @Override
    public void beforeAll(final ExtensionContext context)
    {
        classCallbacks = start(context, true);
    }

    @Override
    public void beforeEach(final ExtensionContext context)
    {
        methodCallbacks = start(context, false);
    }

    private Runnable[] start(final ExtensionContext context, final boolean canVetoScopes)
    {
        final Class<?>[] scopes = AnnotationUtils.findAnnotation(context.getElement(), Scopes.class)
                .map(Scopes::value)
                .orElse(null);
        if (scopes == null || scopes.length == 0)
        {
            return null;
        }
        final WebBeansContext webBeansContext = WebBeansContext.currentInstance();
        final ContextsService contextsService = webBeansContext.getContextsService();
        if (canVetoScopes)
        {
            stopIfNeeded(scopes, contextsService, RequestScoped.class);
            stopIfNeeded(scopes, contextsService, SessionScoped.class);
            if (webBeansContext.getOpenWebBeansConfiguration().supportsConversation())
            {
                stopIfNeeded(scopes, contextsService, ConversationScoped.class);
            }
        }
        return Stream.of(scopes)
                .map(scope -> {
                    // todo: endParam support, not needed in standalone but can be in web?
                    final Class<? extends Annotation> scopeAnnot = (Class<? extends Annotation>) scope;
                    contextsService.startContext(scopeAnnot, null);
                    return (Runnable) () -> contextsService.endContext(scopeAnnot, null);
                })
                .toArray(Runnable[]::new);
    }

    private void stopIfNeeded(final Class<?>[] scopes, final ContextsService contextsService, final Class<? extends Annotation> scope)
    {
        if (Stream.of(scopes).noneMatch(it -> it == scope))
        {
            final Context currentReqScope = contextsService.getCurrentContext(scope);
            if (currentReqScope != null && currentReqScope.isActive())
            {
                contextsService.endContext(scope, null);
            }
        }
    }

    private void stop(final Runnable[] destroyers)
    {
        if (destroyers != null)
        {
            Stream.of(destroyers).forEach(Runnable::run);
        }
    }
}
