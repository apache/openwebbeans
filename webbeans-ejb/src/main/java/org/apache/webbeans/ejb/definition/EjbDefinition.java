/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.webbeans.ejb.definition;

import java.lang.annotation.Annotation;

import org.apache.webbeans.config.DefinitionUtil;
import org.apache.webbeans.ejb.component.EjbComponentImpl;
import org.apache.webbeans.ejb.util.EjbDefinitionUtility;
import org.apache.webbeans.ejb.util.EjbUtility;
import org.apache.webbeans.ejb.util.EjbValidator;
import org.apache.webbeans.util.WebBeansUtil;

/**
 * @version $Rev$ $Date$
 */
public final class EjbDefinition
{
    private EjbDefinition()
    {
        
    }

    public static <T> EjbComponentImpl<T> defineEjbBean(Class<T> ejbClass)
    {
        EjbValidator.validateDecoratorOrInterceptor(ejbClass);
        
        EjbComponentImpl<T> ejbComponent = new EjbComponentImpl<T>(ejbClass);
        ejbComponent.setEjbType(EjbUtility.getEjbTypeForAnnotatedClass(ejbClass));
        
        Annotation[] ejbDeclaredAnnotations = ejbClass.getDeclaredAnnotations();
        
        DefinitionUtil.defineStereoTypes(ejbComponent, ejbDeclaredAnnotations);
        
        Class<? extends Annotation> deploymentType = DefinitionUtil.defineDeploymentType(ejbComponent, ejbDeclaredAnnotations, "There are more than one @DeploymentType annotation in the ejb webbean class : " + ejbClass.getName());

        // Check if the deployment type is enabled.
        if (!WebBeansUtil.isDeploymentTypeEnabled(deploymentType))
        {
            return null;
        }
        
        
        EjbDefinitionUtility.defineApiType(ejbComponent);
        DefinitionUtil.defineScopeType(ejbComponent, ejbDeclaredAnnotations, "Ejb webbean implementation class : " + ejbClass.getName() + " stereotypes must declare same @ScopeType annotation");
        
        EjbValidator.validateEjbScopeType(ejbComponent);
        
        
        return ejbComponent;
    }
}
