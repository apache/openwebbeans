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
package org.apache.webbeans.proxy;

import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Provider;
import java.io.ObjectStreamException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.webbeans.component.OwbBean;
import org.apache.webbeans.config.OpenWebBeansConfiguration;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.exception.ProxyGenerationException;
import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.intercept.NormalScopedBeanInterceptorHandler;
import org.apache.webbeans.util.ClassUtil;
import org.apache.webbeans.util.ExceptionUtil;
import org.apache.webbeans.util.WebBeansUtil;
import org.apache.xbean.asm6.ClassWriter;
import org.apache.xbean.asm6.MethodVisitor;
import org.apache.xbean.asm6.Opcodes;
import org.apache.xbean.asm6.Type;

/**
 * This factory creates proxies which delegate the
 * method invocations 1:1 to an instance which gets
 * resolved via a {@link javax.inject.Provider}.
 */
public class NormalScopeProxyFactory extends AbstractProxyFactory
{
    /** the name of the field which stores the {@link Provider} for the Contextual Instance */
    public static final String FIELD_INSTANCE_PROVIDER = "owbContextualInstanceProvider";

    /** the Method[] for all protected methods. We need to invoke them via reflection. */
    public static final String FIELD_PROTECTED_METHODS = "owbProtectedMethods";

    /**
     * Caches the proxy classes for each bean.
     * We need this to prevent filling up the ClassLoaders by
     */
    private ConcurrentMap<Bean<?>, Class<?>> cachedProxyClasses = new ConcurrentHashMap<Bean<?>, Class<?>>();


    public NormalScopeProxyFactory(WebBeansContext webBeansContext)
    {
        super(webBeansContext);
    }

    @Override
    protected Class getMarkerInterface()
    {
        return OwbNormalScopeProxy.class;
    }


    public static <T> T unwrapInstance(T proxyInstance)
    {
        if (proxyInstance instanceof OwbNormalScopeProxy)
        {
            try
            {
                Field internalInstanceField = proxyInstance.getClass().getDeclaredField(FIELD_INSTANCE_PROVIDER);
                internalInstanceField.setAccessible(true);
                Provider<T> provider = (Provider<T>) internalInstanceField.get(proxyInstance);
                return provider.get();
            }
            catch (Exception e)
            {
                throw ExceptionUtil.throwAsRuntimeException(e);
            }
        }

        return proxyInstance;
    }

    /**
     * @return the internal instance which gets proxied.
     */
    public Provider getInstanceProvider(OwbNormalScopeProxy proxyInstance)
    {
        try
        {
            Field internalInstanceField = proxyInstance.getClass().getDeclaredField(FIELD_INSTANCE_PROVIDER);
            internalInstanceField.setAccessible(true);
            return (Provider) internalInstanceField.get(proxyInstance);
        }
        catch (Exception e)
        {
            throw ExceptionUtil.throwAsRuntimeException(e);
        }
    }

    public <T> T createNormalScopeProxy(Bean<T> bean)
    {
        final ClassLoader classLoader;
        if (bean.getBeanClass() != null)
        {
            classLoader = getProxyClassLoader(bean.getBeanClass());
        }
        else if (OwbBean.class.isInstance(bean) && OwbBean.class.cast(bean).getReturnType() != null)
        {
            classLoader = getProxyClassLoader(OwbBean.class.cast(bean).getReturnType());
        }
        else
        {
            classLoader = WebBeansUtil.getCurrentClassLoader();
        }

        Class<T> classToProxy;
        if (bean instanceof OwbBean)
        {
            classToProxy = ((OwbBean<T>) bean).getReturnType();
        }
        else
        {
            // TODO: that might be wrong sometimes
            classToProxy = (Class<T>) bean.getBeanClass();
        }

        Class<? extends T> proxyClass = (Class<? extends T>) cachedProxyClasses.get(bean);

        if (proxyClass == null)
        {
            proxyClass = createProxyClass(bean, classLoader, classToProxy);
        }

        return createProxyInstance(proxyClass, getInstanceProvider(classLoader, bean));
    }

    public Provider getInstanceProvider(ClassLoader classLoader, Bean<?> bean)
    {
        String scopeClassName = bean.getScope().getName();
        Class<? extends Provider> instanceProviderClass = null;
        String proxyMappingConfigKey = OpenWebBeansConfiguration.PROXY_MAPPING_PREFIX + scopeClassName;
        String className = webBeansContext.getOpenWebBeansConfiguration().getProperty(proxyMappingConfigKey);
        if (className == null || NormalScopedBeanInterceptorHandler.class.getName().equals(className))
        {
            return new NormalScopedBeanInterceptorHandler(webBeansContext.getBeanManagerImpl(), bean);
        }

        try
        {
            instanceProviderClass = (Class<? extends Provider>) Class.forName(className, true, classLoader);
            Constructor<?> ct = instanceProviderClass.getConstructor(BeanManager.class, Bean.class);
            return (Provider) ct.newInstance(webBeansContext.getBeanManagerImpl(), bean);
        }
        catch (NoSuchMethodException nsme)
        {
            throw new WebBeansConfigurationException("Configured InterceptorHandler " + className +" has wrong constructor" , nsme);
        }
        catch (ClassNotFoundException e)
        {
            throw new WebBeansConfigurationException("Configured InterceptorHandler " + className +" cannot be found", e);
        }
        catch (InvocationTargetException e)
        {
            throw new WebBeansConfigurationException("Configured InterceptorHandler " + className +" creation exception", e);
        }
        catch (InstantiationException e)
        {
            throw new WebBeansConfigurationException("Configured InterceptorHandler " + className +" creation exception", e);
        }
        catch (IllegalAccessException e)
        {
            throw new WebBeansConfigurationException("Configured InterceptorHandler " + className +" creation exception", e);
        }
    }

    public synchronized <T> Class<T> createProxyClass(Bean<T> bean, ClassLoader classLoader, Class<T> classToProxy)
    {
        Class<T> proxyClass = (Class<T>) cachedProxyClasses.get(bean);

        if (proxyClass == null)
        {
            proxyClass = createProxyClass(classLoader, classToProxy);
            cachedProxyClasses.putIfAbsent(bean, proxyClass);
        }

        return proxyClass;
    }

    @Override
    protected void createSerialisation(ClassWriter cw, String proxyClassFileName, Class<?> classToProxy, String classFileName)
    {
        String[] exceptionTypeNames = {Type.getType(ObjectStreamException.class).getInternalName()};
        MethodVisitor mv = cw.visitMethod(Modifier.PUBLIC, "writeReplace", "()Ljava/lang/Object;", null, exceptionTypeNames);

        // fill method body
        mv.visitCode();

        // load the contextual instance Provider
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitFieldInsn(Opcodes.GETFIELD, proxyClassFileName, FIELD_INSTANCE_PROVIDER, Type.getDescriptor(Provider.class));

        mv.visitInsn(Opcodes.ARETURN);

        mv.visitMaxs(-1, -1);
        mv.visitEnd();
    }

    /**
     * @param classLoader to use for creating the class in
     * @param classToProxy the class for which a subclass will get generated
     * @param <T>
     * @return the proxy class
     */
    public <T> Class<T> createProxyClass(ClassLoader classLoader, Class<T> classToProxy)
            throws ProxyGenerationException
    {
        String proxyClassName = getUnusedProxyClassName(
                classLoader,
                (classToProxy.getSigners() != null ? getSignedClassProxyName(classToProxy) : classToProxy.getName()) + "$$OwbNormalScopeProxy");

        Method[] nonInterceptedMethods;
        Method[] interceptedMethods = null;
        if (classToProxy.isInterface())
        {
            nonInterceptedMethods = classToProxy.getMethods();
        }
        else
        {
            List<Method> methods = new ArrayList<Method>();
            List<Method> protectedMethods = new ArrayList<Method>();


            for (Method method : ClassUtil.getNonPrivateMethods(classToProxy, true))
            {
                if (unproxyableMethod(method))
                {
                    continue;
                }
                if (Modifier.isProtected(method.getModifiers()))
                {
                    protectedMethods.add(method);
                }
                else
                {
                    methods.add(method);
                }
            }

            nonInterceptedMethods = methods.toArray(new Method[methods.size()]);
            interceptedMethods = protectedMethods.toArray(new Method[protectedMethods.size()]);
        }

        Class<T> clazz = createProxyClass(classLoader, proxyClassName, classToProxy, interceptedMethods, nonInterceptedMethods);

        if (interceptedMethods != null && interceptedMethods.length > 0)
        {
            try
            {
                Field protectedMethodsField = clazz.getDeclaredField(FIELD_PROTECTED_METHODS);
                protectedMethodsField.setAccessible(true);
                protectedMethodsField.set(null, interceptedMethods);
            }
            catch (Exception e)
            {
                throw new ProxyGenerationException(e);
            }
        }
        return clazz;
    }

    public <T> T createProxyInstance(Class<T> proxyClass, Provider provider)
            throws ProxyGenerationException
    {
        try
        {
            T proxy = unsafe.unsafeNewInstance(proxyClass);

            Field delegateField = proxy.getClass().getDeclaredField(FIELD_INSTANCE_PROVIDER);
            delegateField.setAccessible(true);
            delegateField.set(proxy, provider);

            return proxy;
        }
        catch (Exception e)
        {
            throw new ProxyGenerationException(e);
        }
    }


    @Override
    protected void createConstructor(ClassWriter cw, String proxyClassFileName, Class<?> classToProxy, String classFileName, Constructor<?> ignored)
            throws ProxyGenerationException
    {
        try
        {
            Constructor superDefaultCt;
            String parentClassFileName;
            if (classToProxy.isInterface())
            {
                parentClassFileName = Type.getInternalName(Object.class);
                superDefaultCt = Object.class.getConstructor(null);
            }
            else
            {
                parentClassFileName = classFileName;
                superDefaultCt = classToProxy.getDeclaredConstructor(null);
            }

            final String descriptor = Type.getConstructorDescriptor(superDefaultCt);
            final MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", descriptor, null, null);
            mv.visitCode();
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, parentClassFileName, "<init>", descriptor, false);

            // the instance provider field
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitInsn(Opcodes.ACONST_NULL);
            mv.visitFieldInsn(Opcodes.PUTFIELD, proxyClassFileName, FIELD_INSTANCE_PROVIDER, Type.getDescriptor(Provider.class));

            mv.visitInsn(Opcodes.RETURN);
            mv.visitMaxs(-1, -1);
            mv.visitEnd();
        }
        catch (NoSuchMethodException e)
        {
            throw new ProxyGenerationException(e);
        }
    }

    @Override
    protected void createInstanceVariables(ClassWriter cw, Class<?> classToProxy, String classFileName)
    {
        // variable #1, the Provider<?> for the Contextual Instance
        cw.visitField(Opcodes.ACC_PRIVATE,
                FIELD_INSTANCE_PROVIDER, Type.getDescriptor(Provider.class), null, null).visitEnd();

        // variable #2, the Method[] for all protected methods
        cw.visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC,
                FIELD_PROTECTED_METHODS, Type.getDescriptor(Method[].class), null, null).visitEnd();
    }

    /**
     * In the NormalScope proxying case this is used for all the protected methods
     * as they need to get invoked via reflection.
     */
    @Override
    protected void delegateInterceptedMethods(ClassLoader classLoader, ClassWriter cw, String proxyClassFileName,
                                              Class<?> classToProxy, Method[] interceptedMethods)
            throws ProxyGenerationException
    {
        if (interceptedMethods == null)
        {
            return;
        }

        for (int i = 0; i < interceptedMethods.length; i++)
        {
            Method proxiedMethod = interceptedMethods[i];
            generateDelegationMethod(cw, proxiedMethod, i, classToProxy, proxyClassFileName);
        }
    }

    @Override
    protected void delegateNonInterceptedMethods(ClassLoader classLoader, ClassWriter cw, String proxyClassFileName,
                                                 Class<?> classToProxy, Method[] noninterceptedMethods)
            throws ProxyGenerationException
    {

        for (Method delegatedMethod : noninterceptedMethods)
        {
            String methodDescriptor = Type.getMethodDescriptor(delegatedMethod);

            //X TODO handle generic exception types?
            Class[] exceptionTypes = delegatedMethod.getExceptionTypes();
            String[] exceptionTypeNames = new String[exceptionTypes.length];
            for (int i = 0; i < exceptionTypes.length; i++)
            {
                exceptionTypeNames[i] = Type.getType(exceptionTypes[i]).getInternalName();
            }

            int targetModifiers = delegatedMethod.getModifiers() & (Modifier.PROTECTED | Modifier.PUBLIC | MODIFIER_VARARGS);

            MethodVisitor mv = cw.visitMethod(targetModifiers, delegatedMethod.getName(), methodDescriptor, null, exceptionTypeNames);

            // fill method body
            mv.visitCode();

            // load the contextual instance Provider
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitFieldInsn(Opcodes.GETFIELD, proxyClassFileName, FIELD_INSTANCE_PROVIDER, Type.getDescriptor(Provider.class));

            // invoke the get() method on the Provider
            mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(Provider.class), "get", "()Ljava/lang/Object;", true);

            // and convert the Object to the target class type
            mv.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(classToProxy));


            // now calculate the parameters
            int offset = 1;
            for (Class<?> aClass : delegatedMethod.getParameterTypes())
            {
                final Type type = Type.getType(aClass);
                mv.visitVarInsn(type.getOpcode(Opcodes.ILOAD), offset);
                offset += type.getSize();
            }

            // and finally invoke the target method on the provided Contextual Instance
            final Type declaringClass = Type.getType(delegatedMethod.getDeclaringClass());
            boolean interfaceMethod = Modifier.isInterface(delegatedMethod.getDeclaringClass().getModifiers());
            mv.visitMethodInsn(interfaceMethod ? Opcodes.INVOKEINTERFACE : Opcodes.INVOKEVIRTUAL,
                               declaringClass.getInternalName(), delegatedMethod.getName(), methodDescriptor, interfaceMethod);

            generateReturn(mv, delegatedMethod);

            mv.visitMaxs(-1, -1);

            mv.visitEnd();
        }

    }

    private void generateDelegationMethod(ClassWriter cw, Method method, int methodIndex, Class<?> classToProxy, String proxyClassFileName)
    {
        final Class<?> returnType = method.getReturnType();
        final Class<?>[] parameterTypes = method.getParameterTypes();
        final int modifiers = method.getModifiers();

        // push the method definition
        int modifier = modifiers & (Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED | Opcodes.ACC_VARARGS);

        MethodVisitor mv = cw.visitMethod(modifier, method.getName(), Type.getMethodDescriptor(method), null, null);
        mv.visitCode();


        mv.visitVarInsn(Opcodes.ALOAD, 0);

        // add the Method from the static array as first parameter
        mv.visitFieldInsn(Opcodes.GETSTATIC, proxyClassFileName, FIELD_PROTECTED_METHODS, Type.getDescriptor(Method[].class));

        // push the methodIndex of the current method
        mv.visitIntInsn(Opcodes.BIPUSH, methodIndex);

        // and now load the Method from the array
        mv.visitInsn(Opcodes.AALOAD);


        // now invoke the get() on the contextual instance Provider<T>
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitFieldInsn(Opcodes.GETFIELD, proxyClassFileName, FIELD_INSTANCE_PROVIDER, Type.getDescriptor(Provider.class));

        // invoke the get() method on the Provider
        mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(Provider.class), "get", "()Ljava/lang/Object;", true);


        // prepare the parameter array as Object[] and store it on the stack
        pushMethodParameterArray(mv, parameterTypes);


        // this invokes NormalScopeProxyFactory.delegateProtectedMethod
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(NormalScopeProxyFactory.class), "delegateProtectedMethod",
                "(Ljava/lang/reflect/Method;Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;", false);

        // cast the result
        mv.visitTypeInsn(Opcodes.CHECKCAST, getCastType(returnType));


        if (returnType.isPrimitive() && (!Void.TYPE.equals(returnType)))
        {
            // get the primitive value
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, getWrapperType(returnType), getPrimitiveMethod(returnType),
                    "()" + Type.getDescriptor(returnType), false);
        }

        mv.visitInsn(getReturnInsn(returnType));

        // finish this method
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }



    /**
     * This method get invoked via generated ASM code.
     * It delegates to the underlying protected Method so we don't need to do
     * all the reflection stuff in our generated bytecode.
     * This is needed as we cannot invoke instanceProvider.get().targetMethod() directly
     * if targetMethod is protected. Please see Java LangSpec 6.6.2 about the complex
     * rules for calling 'protected' methods.
     *
     * @see #generateDelegationMethod(org.apache.xbean.asm6.ClassWriter, java.lang.reflect.Method, int, Class, String)
     */
    @SuppressWarnings("unused")
    public static Object delegateProtectedMethod(Method method, Object instance, Object[] params)
    {
        try
        {
            if (!method.isAccessible())
            {
                method.setAccessible(true);
            }
            return method.invoke(instance, params);
        }
        catch (InvocationTargetException ite)
        {
            throw ExceptionUtil.throwAsRuntimeException(ite.getCause());
        }
        catch (Exception e)
        {
            throw ExceptionUtil.throwAsRuntimeException(e);
        }
    }

}
