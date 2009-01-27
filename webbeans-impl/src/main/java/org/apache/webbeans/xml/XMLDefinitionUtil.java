/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.apache.webbeans.xml;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.annotation.Named;
import javax.annotation.NonBinding;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.context.ScopeType;
import javax.decorator.Decorator;
import javax.inject.DeploymentType;
import javax.inject.Specializes;
import javax.inject.UnsatisfiedDependencyException;
import javax.inject.manager.Bean;
import javax.inject.manager.Interceptor;
import javax.interceptor.AroundInvoke;

import org.apache.webbeans.WebBeansConstants;
import org.apache.webbeans.component.AbstractComponent;
import org.apache.webbeans.component.xml.XMLComponentImpl;
import org.apache.webbeans.component.xml.XMLProducerComponentImpl;
import org.apache.webbeans.container.InjectionResolver;
import org.apache.webbeans.decorator.WebBeansDecoratorConfig;
import org.apache.webbeans.event.NotificationManager;
import org.apache.webbeans.event.TransactionalObserverType;
import org.apache.webbeans.event.xml.BeanObserverXMLImpl;
import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.exception.definition.NonexistentFieldException;
import org.apache.webbeans.exception.definition.NonexistentTypeException;
import org.apache.webbeans.inject.xml.XMLInjectionPointModel;
import org.apache.webbeans.intercept.InterceptorData;
import org.apache.webbeans.intercept.InterceptorUtil;
import org.apache.webbeans.intercept.WebBeansInterceptorConfig;
import org.apache.webbeans.intercept.webbeans.WebBeansInterceptor;
import org.apache.webbeans.proxy.JavassistProxyFactory;
import org.apache.webbeans.util.AnnotationUtil;
import org.apache.webbeans.util.Asserts;
import org.apache.webbeans.util.ClassUtil;
import org.apache.webbeans.util.WebBeansUtil;
import org.dom4j.Element;

@SuppressWarnings("unchecked")
public final class XMLDefinitionUtil
{
    private XMLDefinitionUtil()
    {

    }

    /**
     * Checks the conditions for simple webbeans class defined in the XML file.
     * 
     * @param clazz simple webbeans class declared in XML
     * @throws WebBeansConfigurationException if check is fail
     */
    public static void checkSimpleWebBeansInXML(Class<?> clazz, Element webBeanDecleration, String errorMessage) throws WebBeansConfigurationException
    {
        Asserts.nullCheckForClass(clazz);
        if (errorMessage == null)
        {
            errorMessage = "XML defined simple webbeans failed. ";
        }

        int modifier = clazz.getModifiers();

        if (ClassUtil.isParametrized(clazz))
        {
            throw new WebBeansConfigurationException(errorMessage + "Simple WebBeans component implementation class : " + clazz.getName() + " can not be parametrized type");
        }

        if (!ClassUtil.isStatic(modifier) && ClassUtil.isInnerClazz(clazz))
        {
            throw new WebBeansConfigurationException(errorMessage + "Simple WebBeans component implementation class : " + clazz.getName() + " can not be non-static inner class");
        }

        if (clazz.isAnnotationPresent(javax.interceptor.Interceptor.class))
        {
            boolean found = XMLUtil.isElementChildExistWithWebBeansNameSpace(webBeanDecleration, WebBeansConstants.WEB_BEANS_XML_INTERCEPTOR_ELEMENT);
            if (!found)
            {
                throw new WebBeansConfigurationException(errorMessage + "Simple WebBeans component implementation class : " + clazz.getName() + " must be declared as <Interceptor> element in the XML");
            }
        }

        if (clazz.isAnnotationPresent(Decorator.class))
        {
            boolean found = XMLUtil.isElementChildExistWithWebBeansNameSpace(webBeanDecleration, WebBeansConstants.WEB_BEANS_XML_DECORATOR_ELEMENT);
            if (!found)
            {
                throw new WebBeansConfigurationException(errorMessage + "Simple WebBeans component implementation class : " + clazz.getName() + " must be declared as <Decorator> element in the XML");
            }
        }

    }

    public static void checkTypeMetaDataClasses(List<Class<? extends Annotation>> typeSet, String errorMessage)
    {
        if (typeSet != null && !typeSet.isEmpty())
        {
            Iterator<Class<? extends Annotation>> it = typeSet.iterator();
            while (it.hasNext())
            {
                Class<? extends Annotation> clazz = it.next();
                if (clazz.isAnnotationPresent(DeploymentType.class) || clazz.isAnnotationPresent(ScopeType.class) || AnnotationUtil.isBindingAnnotation(clazz) || AnnotationUtil.isInterceptorBindingAnnotation(clazz) || AnnotationUtil.isStereoTypeAnnotation(clazz) || clazz.equals(Named.class) || clazz.equals(Specializes.class) || clazz.equals(javax.interceptor.Interceptor.class) || clazz.equals(Decorator.class))
                {
                    continue;
                }
                else
                {
                    throw new WebBeansConfigurationException(errorMessage + " TypeLevelMeta data configuration is failed because of the class : " + clazz.getName() + " is not applicable type");
                }
            }
        }

    }

    /**
     * Gets applicable annotation class for given defineType parameter from the
     * given annotation set.
     * 
     * @param component webbeans component
     * @param annotationSet type-level metadata annotation set
     * @param defineType annotation type class
     * @param errorMessage error message for the operation
     * @return applicable annotation class for given defineType parameter from
     *         the given set
     */
    public static <T> Class<? extends Annotation> defineXMLTypeMetaData(AbstractComponent<T> component, List<Class<? extends Annotation>> annotationSet, Class<? extends Annotation> defineType, String errorMessage)
    {
        // Found annotation for given defineType parameter
        Class<? extends Annotation> metaType = null;

        Iterator<Class<? extends Annotation>> it = annotationSet.iterator();
        boolean found = false;
        while (it.hasNext())
        {
            Class<? extends Annotation> temp = it.next();
            if (temp.isAnnotationPresent(defineType))
            {
                if (found)
                {
                    throw new WebBeansConfigurationException(errorMessage);
                }
                else
                {
                    metaType = temp;
                    found = true;
                }
            }
        }

        return metaType;
    }

    public static <T> boolean defineXMLBindingType(AbstractComponent<T> component, List<Class<? extends Annotation>> annotationSet, List<Element> annotationElementList, String errorMessage)
    {
        Iterator<Class<? extends Annotation>> it = annotationSet.iterator();
        boolean found = false;
        int i = 0;
        while (it.hasNext())
        {
            Class<? extends Annotation> temp = it.next();
            if (AnnotationUtil.isBindingAnnotation(temp))
            {
                Method[] methods = temp.getDeclaredMethods();

                for (Method method : methods)
                {
                    Class<?> clazz = method.getReturnType();
                    if (clazz.isArray() || clazz.isAnnotation())
                    {
                        if (!AnnotationUtil.isAnnotationExist(method.getAnnotations(), NonBinding.class))
                        {
                            throw new WebBeansConfigurationException(errorMessage + "WebBeans definition class : " + component.getReturnType().getName() + " @BindingType : " + temp.getName() + " must have @NonBinding valued members for its array-valued and annotation valued members");
                        }
                    }
                }

                if (!found)
                {
                    found = true;
                }

                component.addBindingType(XMLUtil.getXMLDefinedAnnotationMember(annotationElementList.get(i), temp, errorMessage));
            }

            i++;
        }

        return found;
    }

    public static <T> void defineXMLClassLevelInterceptorType(XMLComponentImpl<T> component, List<Class<? extends Annotation>> annotationSet, List<Element> annotationElementList, String errorMessage)
    {
        Iterator<Class<? extends Annotation>> it = annotationSet.iterator();

        Set<Annotation> bindingTypeSet = new HashSet<Annotation>();
        int i = 0;
        while (it.hasNext())
        {
            Class<? extends Annotation> temp = it.next();
            if (AnnotationUtil.isInterceptorBindingAnnotation(temp))
            {
                bindingTypeSet.add(XMLUtil.getXMLDefinedAnnotationMember(annotationElementList.get(i), temp, errorMessage));
            }

            i++;
        }

        Set<Annotation> stereoTypesSet = component.getStereoTypes();
        Annotation[] stereoTypes = new Annotation[stereoTypesSet.size()];
        stereoTypes = stereoTypesSet.toArray(stereoTypes);
        for (Annotation stero : stereoTypes)
        {
            if (AnnotationUtil.isInterceptorBindingMetaAnnotationExist(stero.annotationType().getDeclaredAnnotations()))
            {
                Annotation[] steroInterceptorBindings = AnnotationUtil.getInterceptorBindingMetaAnnotations(stero.annotationType().getDeclaredAnnotations());

                for (Annotation ann : steroInterceptorBindings)
                {
                    bindingTypeSet.add(ann);
                }
            }
        }

        Annotation[] anns = new Annotation[bindingTypeSet.size()];
        anns = bindingTypeSet.toArray(anns);

        Set<Interceptor> set = WebBeansInterceptorConfig.findDeployedWebBeansInterceptor(anns);

        WebBeansInterceptorConfig.addComponentInterceptors(set, component.getInterceptorStack());

    }

    public static <T> void defineXMLMethodLevelInterceptorType(XMLComponentImpl<T> component, Method interceptorMethod, Element interceptorMethodElement, String errorMessage)
    {
        List<Element> bindingTypes = interceptorMethodElement.elements();
        Set<Annotation> bindingTypesSet = new HashSet<Annotation>();
        for (Element bindingType : bindingTypes)
        {
            Class<? extends Annotation> annot = (Class<? extends Annotation>) XMLUtil.getElementJavaType(bindingType);
            Annotation bindingAnnot = XMLUtil.getXMLDefinedAnnotationMember(bindingType, annot, errorMessage);

            bindingTypesSet.add(bindingAnnot);
        }

        Annotation[] result = new Annotation[bindingTypesSet.size()];
        result = bindingTypesSet.toArray(result);

        Set<Interceptor> setInterceptors = WebBeansInterceptorConfig.findDeployedWebBeansInterceptor(result);
        Iterator<Interceptor> it = setInterceptors.iterator();

        List<InterceptorData> stack = component.getInterceptorStack();
        while (it.hasNext())
        {
            WebBeansInterceptor interceptor = (WebBeansInterceptor) it.next();

            WebBeansUtil.configureInterceptorMethods(interceptor, interceptor.getClazz(), AroundInvoke.class, false, true, stack, interceptorMethod, true);
            WebBeansUtil.configureInterceptorMethods(interceptor, interceptor.getClazz(), PostConstruct.class, false, true, stack, interceptorMethod, true);
            WebBeansUtil.configureInterceptorMethods(interceptor, interceptor.getClazz(), PreDestroy.class, false, true, stack, interceptorMethod, true);
        }

    }

    /**
     * Configures the webbeans component stereotype.
     * 
     * @param component webbeans component
     * @param annotationSet set of type-level metadata annotation set
     */
    public static <T> void defineXMLStereoType(AbstractComponent<T> component, List<Class<? extends Annotation>> annotationSet)
    {
        Iterator<Class<? extends Annotation>> it = annotationSet.iterator();
        while (it.hasNext())
        {
            Class<? extends Annotation> temp = it.next();
            if (AnnotationUtil.isStereoTypeAnnotation(temp))
            {
                component.addStereoType(JavassistProxyFactory.createNewAnnotationProxy(temp));
            }
        }
    }

    public static <T> boolean defineXMLName(AbstractComponent<T> component, List<Class<? extends Annotation>> annotationSet)
    {
        Iterator<Class<? extends Annotation>> it = annotationSet.iterator();
        while (it.hasNext())
        {
            Class<? extends Annotation> temp = it.next();
            if (temp.equals(Named.class))
            {
                return true;
            }
        }

        return false;
    }

    public static <T> void defineXMLSpecializes(XMLComponentImpl<T> component, List<Class<? extends Annotation>> annotationSet)
    {
        Iterator<Class<? extends Annotation>> it = annotationSet.iterator();
        while (it.hasNext())
        {
            Class<? extends Annotation> temp = it.next();
            if (temp.equals(Specializes.class))
            {
                XMLSpecializesManager.getInstance().addXMLSpecializeClass(temp);
            }
        }
    }

    public static <T> void defineXMLInterceptors(XMLComponentImpl<T> component, List<Class<? extends Annotation>> annotationSet, List<Element> annotationElementList, String errorMessage)
    {
        Iterator<Class<? extends Annotation>> it = annotationSet.iterator();
        boolean found = false;

        Set<Annotation> interceptorBindingTypes = new HashSet<Annotation>();
        int i = 0;
        while (it.hasNext())
        {
            Class<? extends Annotation> temp = it.next();
            if (temp.equals(javax.interceptor.Interceptor.class))
            {
                if (found)
                {
                    throw new WebBeansConfigurationException(errorMessage + "More than one <Interceptor> element exist for class : " + component.getReturnType().getName());
                }
                else
                {
                    found = true;
                }
            }
            else if (AnnotationUtil.isInterceptorBindingAnnotation(temp))
            {
                Element annotationElement = annotationElementList.get(i);
                Annotation bindingAnnotation = XMLUtil.getXMLDefinedAnnotationMember(annotationElement, temp, errorMessage);
                interceptorBindingTypes.add(bindingAnnotation);
            }

            i++;
        }

        if (interceptorBindingTypes.size() == 0)
        {
            throw new WebBeansConfigurationException(errorMessage + "<Interceptor> decleration must have one interceptor binding type for class : " + component.getReturnType().getName());
        }

        Annotation[] anns = new Annotation[interceptorBindingTypes.size()];
        anns = interceptorBindingTypes.toArray(anns);
        InterceptorUtil.checkLifecycleConditions(component.getReturnType(), anns, errorMessage + "Lifecycle interceptor : " + component.getReturnType().getName() + " interceptor binding type must be defined as @Target{TYPE}");

        WebBeansInterceptorConfig.configureInterceptorClass((XMLComponentImpl<Object>) component, anns);
    }

    public static <T> void defineXMLDecorators(XMLComponentImpl<T> component, List<Class<? extends Annotation>> annotationSet, Element decoratorDecleration, String errorMessage)
    {
        Iterator<Class<? extends Annotation>> it = annotationSet.iterator();
        boolean found = false;
        while (it.hasNext())
        {
            Class<? extends Annotation> temp = it.next();
            if (temp.equals(Decorator.class))
            {
                if (found)
                {
                    throw new WebBeansConfigurationException(errorMessage + "More than one <Decorator> element exist");
                }
                else
                {
                    found = true;
                }
            }
        }

        if (found)
        {
            List<Element> childs = decoratorDecleration.elements();
            for (Element child : childs)
            {
                if (XMLUtil.getElementNameSpace(child).equals(XMLUtil.getElementNameSpace(decoratorDecleration)) && XMLUtil.isElementHasDecoratesChild(child))
                {
                    Field field = ClassUtil.getFieldWithName(component.getReturnType(), child.getName());
                    if (field == null)
                    {
                        throw new NonexistentFieldException(errorMessage + "Field with name : " + child.getName() + " not found in the decorator class : " + component.getReturnType().getName());
                    }

                    Element decorates = child.element(WebBeansConstants.WEB_BEANS_XML_DECORATES_ELEMENT);
                    Element type = (Element) decorates.elements().get(0);

                    Class<?> apType = XMLUtil.getElementJavaType(type);

                    if (!field.getType().isAssignableFrom(apType))
                    {
                        throw new WebBeansConfigurationException(errorMessage + "Field name : " + field.getName() + " xml defined class type must be assignable to the field actual class type");
                    }

                    XMLInjectionPointModel model = XMLUtil.getInjectionPointModel(type, errorMessage);

                    WebBeansDecoratorConfig.configureXMLDecoratorClass((AbstractComponent<Object>) component, model);
                }
                else
                {
                    throw new WebBeansConfigurationException(errorMessage + "Delegate decleration must defined exactly one child element with name Decorates");
                }
            }
        }
    }

    /**
     * Returns newly created and configures xml webbeans producer component.
     * 
     * @param component webbeans component that defines producer method
     * @param producesMethod producer method
     * @param producerMethodElement produce method xml element
     * @param errorMessage error message
     * @return newly created and configures xml webbeans producer component.
     * @see XMLProducerComponentImpl
     */
    public static <T> XMLProducerComponentImpl<T> defineXMLProducerMethod(WebBeansXMLConfigurator configurator, XMLComponentImpl<T> component, Method producesMethod, Element producerMethodElement, String errorMessage)
    {
        boolean producesDefined = false;
        List<Element> childElements = producerMethodElement.elements();
        Class<T> type = null;
        Element typeElement = null;
        Element arrayElement = null;
        List<Class<? extends Annotation>> memberLevelMetaData = new ArrayList<Class<? extends Annotation>>();
        List<Element> memberLevelElement = new ArrayList<Element>();
        List<XMLInjectionPointModel> injectedParameters = new ArrayList<XMLInjectionPointModel>();

        for (Element childElement : childElements)
        {
            if (XMLUtil.isElementInWebBeansNameSpaceWithName(childElement, WebBeansConstants.WEB_BEANS_XML_PRODUCES_ELEMENT))
            {
                if (producesDefined == false)
                {
                    producesDefined = true;
                }
                else
                {
                    throw new WebBeansConfigurationException(errorMessage + "More than one <Produces> element is defined");
                }

                List<Element> producesElementChilds = childElement.elements();
                boolean definedType = false;

                for (Element producesElementChild : producesElementChilds)
                {
                    if (producesElementChild.getName().equals(WebBeansConstants.WEB_BEANS_XML_ARRAY_ELEMENT))
                    {
                        arrayElement = producesElementChild;
                        definedType = true;
                    }
                    else
                    {
                        type = (Class<T>) XMLUtil.getElementJavaType(producesElementChild);
                        if (type == null)
                        {
                            throw new NonexistentTypeException(errorMessage + "Java type : " + XMLUtil.getElementJavaClassName(producesElementChild) + " does not exist in the <Produces> element child");
                        }

                        else if (type.isAnnotation())
                        {
                            memberLevelMetaData.add((Class<? extends Annotation>) type);
                            memberLevelElement.add(producesElementChild);
                        }
                        else
                        {
                            if (!type.isAssignableFrom(producesMethod.getReturnType()) && !producesMethod.getReturnType().isAssignableFrom(type))
                            {
                                throw new WebBeansConfigurationException(errorMessage + "Defined returned method type is not compatible for producer method name : " + producesMethod.getName() + " in class : " + component.getReturnType().getName());
                            }

                            if (definedType)
                            {
                                throw new WebBeansConfigurationException(errorMessage + "More than one Java type in the <Produces> element");
                            }
                            else
                            {
                                typeElement = producesElementChild;
                                definedType = true;
                            }
                        }
                    }
                }

                if (!definedType)
                {
                    throw new WebBeansConfigurationException(errorMessage + "<Produces> element must define at least one java type child");
                }
            }
            else
            {
                XMLInjectionPointModel injectionPointModel = XMLUtil.getInjectionPointModel(childElement, errorMessage);
                injectedParameters.add(injectionPointModel);

            }
        }

        XMLProducerComponentImpl<T> producerComponentImpl = configureProduerMethod(component, producesMethod, injectedParameters, type, arrayElement, typeElement, errorMessage);

        configureProducerTypeLevelMetaData(configurator, producerComponentImpl, producesMethod, producerMethodElement, memberLevelMetaData, memberLevelElement, component, errorMessage);

        return producerComponentImpl;
    }

    /**
     * Configures and returns the newly created producer method webbeans
     * component.
     * 
     * @param parentComponent producer method webbeans parent component
     * @param producesMethod producer method
     * @param injectedParameters injected parameters of the producer method
     * @param type java class type of the producer method return type, null if
     *            the return type is defined as an Array element.
     * @param arrayElement if the return type is array type, this parameter is
     *            the Array element definition
     * @param typeElement if the return type is a Java type, this parameter is
     *            the Java type element
     * @param errorMessage error message
     * @return new xml defines producer method component
     * @see XMLProducerComponentImpl
     */
    private static <T> XMLProducerComponentImpl<T> configureProduerMethod(AbstractComponent<?> parentComponent, Method producesMethod, List<XMLInjectionPointModel> injectedParameters, Class<T> type, Element arrayElement, Element typeElement, String errorMessage)
    {
        /* New producer webbeans component */
        XMLProducerComponentImpl<T> producerComponentImpl = new XMLProducerComponentImpl<T>(parentComponent, type);

        /* Check return type is the array type */
        if (arrayElement != null)
        {
            /* Configures array type */
            XMLUtil.defineXMLProducerApiTypeFromArrayElement(producerComponentImpl, arrayElement, errorMessage);
        }
        /* Return type is java type */
        else
        {
            /* Configures the java api types and actual type parameters */
            XMLInjectionPointModel model = XMLUtil.getInjectionPointModel(typeElement, errorMessage);

            producerComponentImpl.setActualTypeArguments(model.getActualTypeArguments());
            producerComponentImpl.addApiType(model.getInjectionClassType());

            if (model.getInjectionClassType().isPrimitive())
            {
                producerComponentImpl.setNullable(false);
            }
        }

        producerComponentImpl.addApiType(Object.class);

        /* Set creator method */
        producerComponentImpl.setCreatorMethod(producesMethod);

        /* Configures producer method injected parameters */
        for (XMLInjectionPointModel injectionPointModel : injectedParameters)
        {
            producerComponentImpl.addProducerMethodInjectionPointModel(injectionPointModel);
        }

        return producerComponentImpl;

    }

    /**
     * Configures xml defined producer method webbeans type level metadatas.
     * 
     * @param producerComponentImpl xml webbeans producer component
     * @param producesMethod producer method
     * @param producerMethodElement producer method xml element
     * @param memberLevelMetaData member level annotations
     * @param memberLevelElement member level xml elements
     * @param component parent component that defines producer method
     * @param errorMessage error message
     * @return type level metadata configured webbeans
     * @see XMLProducerComponentImpl
     */
    private static <T> XMLProducerComponentImpl<T> configureProducerTypeLevelMetaData(WebBeansXMLConfigurator configurator, XMLProducerComponentImpl<T> producerComponentImpl, Method producesMethod, Element producerMethodElement, List<Class<? extends Annotation>> memberLevelMetaData, List<Element> memberLevelElement, XMLComponentImpl<T> component, String errorMessage)
    {

        for (Class<? extends Annotation> memberLevelMetaDataClass : memberLevelMetaData)
        {
            if (!memberLevelMetaDataClass.isAnnotationPresent(DeploymentType.class) && !memberLevelMetaDataClass.isAnnotationPresent(ScopeType.class) && !AnnotationUtil.isStereoTypeAnnotation(memberLevelMetaDataClass) && !memberLevelMetaDataClass.equals(Named.class))
            {
                throw new WebBeansConfigurationException(errorMessage + "Defined annotations for producer method name : " + producesMethod.getName() + " in class : " + component.getReturnType().getName() + " is not correct");
            }
        }

        configurator.configureProducerTypeLevelMetaData(producerComponentImpl, memberLevelMetaData, memberLevelElement, producerMethodElement);

        return producerComponentImpl;
    }

    /**
     * Configures the disposal method of the webbeans component using the xml
     * configuration.
     * 
     * @param component producer method webbeans component
     * @param disposalMethod disposal method defined in the xml
     * @param disposalMethodElement disposal method xml element
     * @param errorMessage error message used in exceptions
     * @throws WebBeansConfigurationException if more than one Disposal element
     *             exist for the given disposal method element
     * @throws UnsatisfiedDependencyException if no producer method found for
     *             given disposal method
     */
    public static <T> void defineXMLDisposalMethod(XMLComponentImpl<T> component, Method disposalMethod, Element disposalMethodElement, String errorMessage)
    {
        /* Disposal method element childs */
        List<Element> disposalChildElements = disposalMethodElement.elements();

        /* Multiple <Disposes> element control parameter */
        boolean disposalDefined = false;

        /* Other parameter elements other than @Disposes */
        List<Element> otherParameterElements = new ArrayList<Element>();

        XMLProducerComponentImpl<?> producerComponent = null;

        for (Element childElement : disposalChildElements)
        {
            if (XMLUtil.isElementInWebBeansNameSpaceWithName(childElement, WebBeansConstants.WEB_BEANS_XML_DISPOSES_ELEMENT))
            {
                if (disposalDefined == false)
                {
                    disposalDefined = true;
                }
                else
                {
                    throw new WebBeansConfigurationException(errorMessage + "More than one <Disposal> element is defined for defining disposal method : " + disposalMethod.getName());
                }

                Element typeElement = (Element) childElement.elements().get(0);

                /* Find disposal method model */
                XMLInjectionPointModel model = XMLUtil.getInjectionPointModel(typeElement, errorMessage);

                /* Binding types for disposal method */
                Set<Annotation> bindingTypes = model.getBindingTypes();
                Annotation[] bindingAnns = new Annotation[bindingTypes.size()];
                bindingAnns = bindingTypes.toArray(bindingAnns);

                Set<Bean<T>> set = InjectionResolver.getInstance().implResolveByType(model.getInjectionClassType(), model.getActualTypeArguments(), bindingAnns);
                producerComponent = (XMLProducerComponentImpl<?>) set.iterator().next();

                if (producerComponent == null)
                {
                    throw new UnsatisfiedDependencyException(errorMessage + "Producer method component of the disposal method : " + disposalMethod.getName() + "is not found");
                }

                producerComponent.setDisposalMethod(disposalMethod);

            }
            /* Disposal method parameter other than @Disposes */
            else
            {
                otherParameterElements.add(childElement);
            }
        }// end of for childs

        /* Add other params injection point models */
        for (Element otherElement : otherParameterElements)
        {
            XMLInjectionPointModel injectionPointParamModel = XMLUtil.getInjectionPointModel(otherElement, errorMessage);
            producerComponent.addDisposalMethodInjectionPointModel(injectionPointParamModel);
        }
    }

    public static <T, K> void defineXMLObservesMethod(XMLComponentImpl<T> component, Method observesMethod, Element observesMethodElement, String errorMessage)
    {
        component.addObservableMethod(observesMethod);

        /* Observes method element childs */
        List<Element> observesChildElements = observesMethodElement.elements();

        /* Other parameter elements other than @Observes */
        List<Element> otherParameterElements = new ArrayList<Element>();

        BeanObserverXMLImpl<K> beanObserver = new BeanObserverXMLImpl<K>(component, observesMethod, false, TransactionalObserverType.NONE);

        Class<K> eventType = null;

        for (Element childElement : observesChildElements)
        {
            if (XMLUtil.isElementInWebBeansNameSpaceWithName(childElement, WebBeansConstants.WEB_BEANS_XML_OBSERVES_ELEMENT))
            {
                Element typeElement = (Element) childElement.elements().get(0);

                eventType = (Class<K>) XMLUtil.getElementJavaType(typeElement);

                /* Find observes method model */
                XMLInjectionPointModel model = XMLUtil.getInjectionPointModel(typeElement, errorMessage);

                /* Binding types for disposal method */
                Set<Annotation> bindingTypes = model.getBindingTypes();
                Annotation[] bindingAnns = new Annotation[bindingTypes.size()];
                bindingAnns = bindingTypes.toArray(bindingAnns);

                beanObserver.addXMLInjectionObservesParameter(model);

                NotificationManager.getInstance().addObserver(beanObserver, eventType, bindingAnns);

            }
            /* Disposal method parameter other than @Disposes */
            else
            {
                otherParameterElements.add(childElement);
            }
        }// end of for childs

        /* Add other params injection point models */
        for (Element otherElement : otherParameterElements)
        {
            XMLInjectionPointModel injectionPointParamModel = XMLUtil.getInjectionPointModel(otherElement, errorMessage);
            beanObserver.addXMLInjectionObservesParameter(injectionPointParamModel);
        }
    }
}