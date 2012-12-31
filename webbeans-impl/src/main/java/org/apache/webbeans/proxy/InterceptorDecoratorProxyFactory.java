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


import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import org.apache.webbeans.config.WebBeansContext;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;


/**
 * WORK IN PROGRESS
 *
 * Generate a dynamic subclass which has exactly 1 delegation point instance
 * which get's set in the Constructor of the proxy.
 * Any non-intercepted or decorated method will get delegated natively,
 * All intercepted and decorated methods will get invoked via an InvocationHandler chain.
 *
 * This factory will create and cache the proxy classes for a given type.
 *
 *
 */
//X TODO: clarify how serialisation works! The proxy classes might need to get created on deserialisation!
public class InterceptorDecoratorProxyFactory
{

    /** the name of the field which stores the proxied instance */
    public static final String FIELD_PROXIED_INSTANCE = "owbIntDecProxiedInstance";

    /** the name of the field which stores the Interceptor + Decorator stack InvocationHandler */
    public static final String FIELD_INVOCATION_HANDLER = "owbIntDecInvocationHandler";

    //X TODO add caching of created proxy classes. This is needed to prevent class loading clashes.
    //X a generated proxy cannot easily get redefined later!

    private WebBeansContext webBeansContext;

    public InterceptorDecoratorProxyFactory(WebBeansContext webBeansContext)
    {
        this.webBeansContext = webBeansContext;
    }

    public <T> T createProxyInstance(Class<T> proxyClass, T instance, InvocationHandler interceptorDecoratorStack)
            throws ProxyGenerationException
    {
        try
        {
            T proxy = proxyClass.newInstance();

            Field delegateField = proxy.getClass().getDeclaredField(FIELD_PROXIED_INSTANCE);
            delegateField.setAccessible(true);
            delegateField.set(proxy, instance);

            Field invocationHandlerField = proxy.getClass().getDeclaredField(FIELD_INVOCATION_HANDLER);
            invocationHandlerField.setAccessible(true);
            invocationHandlerField.set(proxy, interceptorDecoratorStack);

            return proxy;
        }
        catch (InstantiationException e)
        {
            throw new ProxyGenerationException(e);
        }
        catch (IllegalAccessException e)
        {
            throw new ProxyGenerationException(e);
        }
        catch (NoSuchFieldException e)
        {
            throw new ProxyGenerationException(e);
        }
    }


    /**
     * Create a decorator and interceptor proxy for the given type.
     *
     *
     * @param classLoader to use for creating the class in
     * @param classToProxy
     * @param interceptedMethods the list of intercepted or decorated methods
     * @param nonInterceptedMethods all methods which are <b>not</b> intercepted nor decorated and shall get delegated directly
     * @param <T>
     * @return the proxy class
     */
    public synchronized <T> Class<T> createProxyClass(ClassLoader classLoader, Class<T> classToProxy,
                                                      List<Method> interceptedMethods, List<Method> nonInterceptedMethods)
            throws ProxyGenerationException
    {
        String proxyClassName = classToProxy.getName() + "$OwbInterceptProxy";
        String proxyClassFileName = proxyClassName.replace('.', '/');

        final byte[] proxyBytes = generateProxy(classToProxy, proxyClassName, proxyClassFileName, interceptedMethods, nonInterceptedMethods);

        return defineAndLoadClass(classLoader, proxyClassName, proxyBytes);
    }


    private byte[] generateProxy(Class<?> classToProxy, String proxyClassName, String proxyClassFileName,
                                 List<Method> interceptedMethods, List<Method> nonInterceptedMethods)
            throws ProxyGenerationException
    {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        String classFileName = classToProxy.getName().replace('.', '/');

        String[] interfaceNames = new String[]{}; //X TODO there might be some more in the future

        cw.visit(Opcodes.V1_5, Opcodes.ACC_PUBLIC + Opcodes.ACC_SUPER + Opcodes.ACC_SYNTHETIC, proxyClassFileName, null, classFileName, interfaceNames);
        cw.visitSource(classFileName + ".java", null);

        createInstanceVariables(cw, classToProxy, classFileName);

        createConstructor(cw, proxyClassFileName, classToProxy, classFileName);


        delegateNonInterceptedMethods(cw, proxyClassFileName, classToProxy, classFileName, nonInterceptedMethods);



        //X TODO invoke all

        //X TODO continue;

        return cw.toByteArray();
    }

    private void createInstanceVariables(ClassWriter cw, Class<?> classToProxy, String classFileName)
    {
        // variable #1, the delegation point
        cw.visitField(Opcodes.ACC_FINAL | Opcodes.ACC_PRIVATE, FIELD_PROXIED_INSTANCE, Type.getDescriptor(classToProxy), null, null).visitEnd();

        // variable #2, the invocation handler
        cw.visitField(Opcodes.ACC_FINAL | Opcodes.ACC_PRIVATE, FIELD_INVOCATION_HANDLER, Type.getDescriptor(InvocationHandler.class), null, null).visitEnd();
    }

    /**
     * Each of our interceptor/decorator proxies has exactly 1 constructor
     * which invokes the super ct + sets the delegation field.
     *
     * //X TODO set delegation instance
     *
     * @param cw
     * @param classToProxy
     * @param classFileName
     * @throws ProxyGenerationException
     */
    private void createConstructor(ClassWriter cw, String proxyClassFileName, Class<?> classToProxy, String classFileName)
            throws ProxyGenerationException
    {
        try
        {
            Constructor superDefaultCt = classToProxy.getConstructor(null);

            final String descriptor = Type.getConstructorDescriptor(superDefaultCt);
            final MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", descriptor, null, null);
            mv.visitCode();
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, classFileName, "<init>", descriptor);

            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitInsn(Opcodes.ACONST_NULL);
            mv.visitFieldInsn(Opcodes.PUTFIELD, proxyClassFileName, FIELD_PROXIED_INSTANCE, Type.getDescriptor(classToProxy));

            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitInsn(Opcodes.ACONST_NULL);
            mv.visitFieldInsn(Opcodes.PUTFIELD, proxyClassFileName, FIELD_INVOCATION_HANDLER, Type.getDescriptor(InvocationHandler.class));

            mv.visitInsn(Opcodes.RETURN);
            mv.visitMaxs(-1, -1);
            mv.visitEnd();
        }
        catch (NoSuchMethodException e)
        {
            throw new ProxyGenerationException(e);
        }
    }

    /**
     * Directly delegate all non intercepted nor decorated methods to the internal instance.
     *
     * @param noninterceptedMethods all methods which are neither intercepted nor decorated
     */
    private static void delegateNonInterceptedMethods(ClassWriter cw, String proxyClassFileName, Class<?> classToProxy, String classFileName,
                                                      List<Method> noninterceptedMethods)
    {
        for (Method proxiedMethod : noninterceptedMethods)
        {
            String methodDescriptor = Type.getMethodDescriptor(proxiedMethod);

            //X TODO handle generic exception types?
            Class[] exceptionTypes = proxiedMethod.getExceptionTypes();
            String[] exceptionTypeNames = new String[exceptionTypes.length];
            for (int i = 0; i < exceptionTypes.length; i++)
            {
                exceptionTypeNames[i] = Type.getType(exceptionTypes[i]).getInternalName();
            }

            MethodVisitor mv = cw.visitMethod(proxiedMethod.getModifiers(), proxiedMethod.getName(), methodDescriptor, null, exceptionTypeNames);

            // fill method body
            mv.visitCode();

            // load the delegate variable
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitFieldInsn(Opcodes.GETFIELD, proxyClassFileName, FIELD_PROXIED_INSTANCE, Type.getDescriptor(classToProxy));

            int offset = 1;
            for (Class<?> aClass : proxiedMethod.getParameterTypes())
            {
                final Type type = Type.getType(aClass);
                mv.visitVarInsn(type.getOpcode(Opcodes.ILOAD), offset);
                offset += type.getSize();
            }

            final Type declaringClass = Type.getType(proxiedMethod.getDeclaringClass());
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, declaringClass.getInternalName(), proxiedMethod.getName(), methodDescriptor);

            final Type returnType = Type.getType(proxiedMethod.getReturnType());
            mv.visitInsn(returnType.getOpcode(Opcodes.IRETURN));

            mv.visitMaxs(-1, -1);

            mv.visitEnd();
        }
    }


    /**
     * The 'defineClass' method on the ClassLoader is protected, thus we need to invoke it via reflection.
     * @return the Class which got loaded in the classloader
     */
    private <T> Class<T> defineAndLoadClass(ClassLoader classLoader, String proxyName, byte[] proxyBytes)
            throws ProxyGenerationException
    {
        Class<?> clazz = classLoader.getClass();

        Method defineClassMethod = null;
        do
        {
            try
            {
                defineClassMethod = clazz.getDeclaredMethod("defineClass", String.class, byte[].class, int.class, int.class);
            }
            catch (NoSuchMethodException e)
            {
                // do nothing, we need to search the superclass
            }

            clazz = clazz.getSuperclass();
        } while (defineClassMethod == null && clazz != Object.class);

        if (defineClassMethod == null)
        {
            throw new ProxyGenerationException("could not find 'defineClass' method in the ClassLoader!");
        }

        defineClassMethod.setAccessible(true);
        try
        {
            Class<T> definedClass = (Class<T>) defineClassMethod.invoke(classLoader, proxyName, proxyBytes, 0, proxyBytes.length);

            try
            {
                Class<T> loadedClass = (Class<T>) classLoader.loadClass(definedClass.getName());
                return loadedClass;
            }
            catch (ClassNotFoundException e)
            {
                throw new ProxyGenerationException(e);
            }
        }
        catch (IllegalAccessException e)
        {
            throw new ProxyGenerationException(e);
        }
        catch (InvocationTargetException e)
        {
            throw new ProxyGenerationException(e);
        }
    }

}
