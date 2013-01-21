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
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.util.ExceptionUtil;
import org.apache.xbean.asm.ClassWriter;
import org.apache.xbean.asm.Label;
import org.apache.xbean.asm.MethodVisitor;
import org.apache.xbean.asm.Opcodes;
import org.apache.xbean.asm.Type;


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
 */
public class InterceptorDecoratorProxyFactory extends AbstractProxyFactory
{

    /** the name of the field which stores the proxied instance */
    public static final String FIELD_PROXIED_INSTANCE = "owbIntDecProxiedInstance";

    /** the name of the field which stores the Interceptor + Decorator stack InterceptorHandler */
    public static final String FIELD_INTERCEPTOR_HANDLER = "owbIntDecHandler";

    /** the name of the field which stores the Method[] of all intercepted methods */
    public static final String FIELD_INTERCEPTED_METHODS = "owbIntDecMethods";

    //X TODO add caching of created proxy classes. This is needed to prevent class loading clashes.
    //X a generated proxy cannot easily get redefined later!


    public InterceptorDecoratorProxyFactory(WebBeansContext webBeansContext)
    {
        super(webBeansContext);
    }

    public <T> T createProxyInstance(Class<? extends T> proxyClass, T instance, InterceptorHandler interceptorDecoratorStack)
            throws ProxyGenerationException
    {
        try
        {
            T proxy = unsafeNewInstance(proxyClass);

            Field delegateField = proxy.getClass().getDeclaredField(FIELD_PROXIED_INSTANCE);
            delegateField.setAccessible(true);
            delegateField.set(proxy, instance);

            Field invocationHandlerField = proxy.getClass().getDeclaredField(FIELD_INTERCEPTOR_HANDLER);
            invocationHandlerField.setAccessible(true);
            invocationHandlerField.set(proxy, interceptorDecoratorStack);

            return proxy;
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
     * @return the internal instance which gets proxied.
     */
    public <T> T getInternalInstance(T proxyInstance)
    {
        try
        {
            Field internalInstanceField = proxyInstance.getClass().getDeclaredField(FIELD_PROXIED_INSTANCE);
            internalInstanceField.setAccessible(true);
            return (T) internalInstanceField.get(proxyInstance);
        }
        catch (Exception e)
        {
            ExceptionUtil.throwAsRuntimeException(e);
        }
        return null;
    }

    /**
     * @return the internal instance which gets proxied.
     */
    public InterceptorHandler getInterceptorHandler(OwbInterceptorProxy proxyInstance)
    {
        try
        {
            Field internalInstanceField = proxyInstance.getClass().getDeclaredField(FIELD_INTERCEPTOR_HANDLER);
            internalInstanceField.setAccessible(true);
            return (InterceptorHandler) internalInstanceField.get(proxyInstance);
        }
        catch (Exception e)
        {
            ExceptionUtil.throwAsRuntimeException(e);
        }
        return null;
    }

    @Override
    protected void createSerialisation(ClassWriter cw, String proxyClassFileName, Class<?> classToProxy, String classFileName)
    {
        // nothing to do ;)
    }

    /**
     * <p>Create a decorator and interceptor proxy for the given type. A single instance
     * of such a proxy class has exactly one single internal instance.</p>
     *
     * <p>There are 3 different kind of methods:
     * <ol>
     *     <li>
     *         private methods - they do not get proxied at all! If you like to invoke a private method,
     *         then you can use {@link #getInternalInstance(Object)} and use reflection on it.
     *     </li>
     *     <li>
     *         non-proxied methods - all methods which do not have a business interceptor nor decorator
     *         will get delegated to the internal instance without invoking any InterceptorHandler nor
     *         doing reflection. Just plain java bytecode will get generated!
     *     </li>
     *     <li>
     *         proxied methods - all calls to such a proxied method will get forwarded to the
     *         InterceptorHandler which got set for this instance.
     *     </li>
     * </ol>
     * </p>
     *
     *
     * @param classLoader to use for creating the class in
     * @param classToProxy the class for which a subclass will get generated
     * @param interceptedMethods the list of intercepted or decorated business methods.
     * @param nonInterceptedMethods all methods which are <b>not</b> intercepted nor decorated and shall get delegated directly
     * @param <T>
     * @return the proxy class
     * //X TODO for serialisation reasons this probably needs the Bean it serves.
     */
    public synchronized <T> Class<T> createProxyClass(ClassLoader classLoader, Class<T> classToProxy,
                                                      Method[] interceptedMethods, Method[] nonInterceptedMethods)
            throws ProxyGenerationException
    {
        String proxyClassName = getUnusedProxyClassName(classLoader, classToProxy.getName() + "$OwbInterceptProxy");


        Class<T> clazz = createProxyClass(classLoader, proxyClassName, classToProxy, interceptedMethods, nonInterceptedMethods);

        try
        {
            Field interceptedMethodsField = clazz.getDeclaredField(FIELD_INTERCEPTED_METHODS);
            interceptedMethodsField.setAccessible(true);
            interceptedMethodsField.set(null, interceptedMethods);
        }
        catch (Exception e)
        {
            throw new ProxyGenerationException(e);
        }

        return clazz;
    }

    @Override
    protected Class getMarkerInterface()
    {
        return OwbInterceptorProxy.class;
    }

    protected void createInstanceVariables(ClassWriter cw, Class<?> classToProxy, String classFileName)
    {
        // variable #1, the delegation point
        cw.visitField(Opcodes.ACC_PRIVATE,
                FIELD_PROXIED_INSTANCE, Type.getDescriptor(classToProxy), null, null).visitEnd();

        // variable #2, the invocation handler
        cw.visitField(Opcodes.ACC_PRIVATE,
                FIELD_INTERCEPTOR_HANDLER, Type.getDescriptor(InterceptorHandler.class), null, null).visitEnd();

        // variable #3, the Method[] of all intercepted methods.
        cw.visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC,
                FIELD_INTERCEPTED_METHODS, Type.getDescriptor(Method[].class), null, null).visitEnd();
    }

    /**
     * Each of our interceptor/decorator proxies has exactly 1 constructor
     * which invokes the super ct + sets the delegation field.
     *
     * @param cw
     * @param classToProxy
     * @param classFileName
     * @throws ProxyGenerationException
     */
    protected void createConstructor(ClassWriter cw, String proxyClassFileName, Class<?> classToProxy, String classFileName)
            throws ProxyGenerationException
    {
        try
        {
            Constructor superDefaultCt = classToProxy.getDeclaredConstructor(null);

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
            mv.visitFieldInsn(Opcodes.PUTFIELD, proxyClassFileName, FIELD_INTERCEPTOR_HANDLER, Type.getDescriptor(InterceptorHandler.class));

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
    protected void delegateNonInterceptedMethods(ClassWriter cw, String proxyClassFileName, Class<?> classToProxy, Method[] noninterceptedMethods)
    {
        for (Method delegatedMethod : noninterceptedMethods)
        {
            if (unproxyableMethod(delegatedMethod))
            {
                continue;
            }

            String methodDescriptor = Type.getMethodDescriptor(delegatedMethod);

            //X TODO handle generic exception types?
            Class[] exceptionTypes = delegatedMethod.getExceptionTypes();
            String[] exceptionTypeNames = new String[exceptionTypes.length];
            for (int i = 0; i < exceptionTypes.length; i++)
            {
                exceptionTypeNames[i] = Type.getType(exceptionTypes[i]).getInternalName();
            }

            int targetModifiers = delegatedMethod.getModifiers() & (Modifier.PROTECTED | Modifier.PUBLIC);

            MethodVisitor mv = cw.visitMethod(targetModifiers, delegatedMethod.getName(), methodDescriptor, null, exceptionTypeNames);

            // fill method body
            mv.visitCode();

            // load the delegate variable
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitFieldInsn(Opcodes.GETFIELD, proxyClassFileName, FIELD_PROXIED_INSTANCE, Type.getDescriptor(classToProxy));

            int offset = 1;
            for (Class<?> aClass : delegatedMethod.getParameterTypes())
            {
                final Type type = Type.getType(aClass);
                mv.visitVarInsn(type.getOpcode(Opcodes.ILOAD), offset);
                offset += type.getSize();
            }

            final Type declaringClass = Type.getType(delegatedMethod.getDeclaringClass());
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, declaringClass.getInternalName(), delegatedMethod.getName(), methodDescriptor);

            generateReturn(mv, delegatedMethod);

            mv.visitMaxs(-1, -1);

            mv.visitEnd();
        }
    }

    protected void delegateInterceptedMethods(ClassWriter cw, String proxyClassFileName, Class<?> classToProxy, Method[] interceptedMethods)
            throws ProxyGenerationException
    {
        for (int i = 0; i < interceptedMethods.length; i++)
        {
            Method proxiedMethod = interceptedMethods[i];
            generateInterceptorHandledMethod(cw, proxiedMethod, i, classToProxy, proxyClassFileName);
        }
    }

    private void generateInterceptorHandledMethod(ClassWriter cw, Method method, int methodIndex, Class<?> classToProxy, String proxyClassFileName)
            throws ProxyGenerationException
    {
        if ("<init>".equals(method.getName()))
        {
            return;
        }

        final Class<?> returnType = method.getReturnType();
        final Class<?>[] parameterTypes = method.getParameterTypes();
        final Class<?>[] exceptionTypes = method.getExceptionTypes();
        final int modifiers = method.getModifiers();

        if (Modifier.isFinal(modifiers) || Modifier.isStatic(modifiers))
        {
            throw new WebBeansConfigurationException("It's not possible to proxy a final or static method: " + classToProxy.getName() +
                                                     " " + method.getName());
        }

        // push the method definition
        int modifier = modifiers & (Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED);

        MethodVisitor mv = cw.visitMethod(modifier, method.getName(), Type.getMethodDescriptor(method), null, null);
        mv.visitCode();

        // push try/catch block, to catch declared exceptions, and to catch java.lang.Throwable
        final Label l0 = new Label();
        final Label l1 = new Label();
        final Label l2 = new Label();

        if (exceptionTypes.length > 0)
        {
            mv.visitTryCatchBlock(l0, l1, l2, "java/lang/reflect/InvocationTargetException");
        }

        // push try code
        mv.visitLabel(l0);
        final String classNameToOverride = method.getDeclaringClass().getName().replace('.', '/');
        mv.visitLdcInsn(Type.getType("L" + classNameToOverride + ";"));

        // the following code generates the bytecode for this line of Java:
        // Method method = <proxy>.class.getMethod("add", new Class[] { <array of function argument classes> });

        // get the method name to invoke, and push to stack
        mv.visitLdcInsn(method.getName());

        // create the Class[]
        createArrayDefinition(mv, parameterTypes.length, Class.class);

        int length = 1;

        // push parameters into array
        for (int i = 0; i < parameterTypes.length; i++)
        {
            // keep copy of array on stack
            mv.visitInsn(Opcodes.DUP);

            final Class<?> parameterType = parameterTypes[i];

            // push number onto stack
            pushIntOntoStack(mv, i);

            if (parameterType.isPrimitive())
            {
                String wrapperType = getWrapperType(parameterType);
                mv.visitFieldInsn(Opcodes.GETSTATIC, wrapperType, "TYPE", "Ljava/lang/Class;");
            }
            else
            {
                mv.visitLdcInsn(Type.getType(parameterType));
            }

            mv.visitInsn(Opcodes.AASTORE);

            if (Long.TYPE.equals(parameterType) || Double.TYPE.equals(parameterType))
            {
                length += 2;
            }
            else
            {
                length++;
            }
        }

        // the following code generates bytecode equivalent to:
        // return ((<returntype>) invocationHandler.invoke(this, {methodIndex}, new Object[] { <function arguments }))[.<primitive>Value()];

        final Label l4 = new Label();
        mv.visitLabel(l4);
        mv.visitVarInsn(Opcodes.ALOAD, 0);

        // get the invocationHandler field from this class
        mv.visitFieldInsn(Opcodes.GETFIELD, proxyClassFileName, FIELD_INTERCEPTOR_HANDLER, Type.getDescriptor(InterceptorHandler.class));

        // add the Method from the static array as first parameter
        mv.visitFieldInsn(Opcodes.GETSTATIC, proxyClassFileName, FIELD_INTERCEPTED_METHODS, Type.getDescriptor(Method[].class));

        // push the methodIndex of the current method
        mv.visitIntInsn(Opcodes.BIPUSH, methodIndex);

        // and now load the Method from the array
        mv.visitInsn(Opcodes.AALOAD);

        // need to construct the array of objects passed in
        // create the Object[]
        createArrayDefinition(mv, parameterTypes.length, Object.class);

        int index = 1;
        // push parameters into array
        for (int i = 0; i < parameterTypes.length; i++)
        {
            // keep copy of array on stack
            mv.visitInsn(Opcodes.DUP);

            final Class<?> parameterType = parameterTypes[i];

            // push number onto stack
            pushIntOntoStack(mv, i);

            if (parameterType.isPrimitive())
            {
                String wrapperType = getWrapperType(parameterType);
                mv.visitVarInsn(getVarInsn(parameterType), index);

                mv.visitMethodInsn(Opcodes.INVOKESTATIC, wrapperType, "valueOf",
                        "(" + Type.getDescriptor(parameterType) + ")L" + wrapperType + ";");
                mv.visitInsn(Opcodes.AASTORE);

                if (Long.TYPE.equals(parameterType) || Double.TYPE.equals(parameterType))
                {
                    index += 2;
                }
                else
                {
                    index++;
                }
            }
            else
            {
                mv.visitVarInsn(Opcodes.ALOAD, index);
                mv.visitInsn(Opcodes.AASTORE);
                index++;
            }
        }

        // invoke the invocationHandler
        mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(InterceptorHandler.class), "invoke",
                "(Ljava/lang/reflect/Method;[Ljava/lang/Object;)Ljava/lang/Object;");

        // cast the result
        mv.visitTypeInsn(Opcodes.CHECKCAST, getCastType(returnType));

        if (returnType.isPrimitive() && (!Void.TYPE.equals(returnType)))
        {
            // get the primitive value
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, getWrapperType(returnType), getPrimitiveMethod(returnType),
                    "()" + Type.getDescriptor(returnType));
        }

        // push return
        mv.visitLabel(l1);
        if (!Void.TYPE.equals(returnType))
        {
            mv.visitInsn(getReturnInsn(returnType));
        }
        else
        {
            mv.visitInsn(Opcodes.POP);
            mv.visitInsn(Opcodes.RETURN);
        }

        // catch InvocationTargetException
        if (exceptionTypes.length > 0)
        {
            mv.visitLabel(l2);
            mv.visitVarInsn(Opcodes.ASTORE, length);

            final Label l5 = new Label();
            mv.visitLabel(l5);

            for (int i = 0; i < exceptionTypes.length; i++)
            {
                final Class<?> exceptionType = exceptionTypes[i];

                mv.visitLdcInsn(Type.getType("L" + exceptionType.getCanonicalName().replace('.', '/') + ";"));
                mv.visitVarInsn(Opcodes.ALOAD, length);
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/reflect/InvocationTargetException", "getCause",
                        "()Ljava/lang/Throwable;");
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Object", "getClass", "()Ljava/lang/Class;");
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Object", "equals", "(Ljava/lang/Object;)Z");

                final Label l6 = new Label();
                mv.visitJumpInsn(Opcodes.IFEQ, l6);

                final Label l7 = new Label();
                mv.visitLabel(l7);

                mv.visitVarInsn(Opcodes.ALOAD, length);
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/reflect/InvocationTargetException", "getCause",
                        "()Ljava/lang/Throwable;");
                mv.visitTypeInsn(Opcodes.CHECKCAST, exceptionType.getCanonicalName().replace('.', '/'));
                mv.visitInsn(Opcodes.ATHROW);
                mv.visitLabel(l6);

                if (i == (exceptionTypes.length - 1))
                {
                    mv.visitTypeInsn(Opcodes.NEW, "java/lang/reflect/UndeclaredThrowableException");
                    mv.visitInsn(Opcodes.DUP);
                    mv.visitVarInsn(Opcodes.ALOAD, length);
                    mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/reflect/UndeclaredThrowableException", "<init>",
                            "(Ljava/lang/Throwable;)V");
                    mv.visitInsn(Opcodes.ATHROW);
                }
            }
        }

        // finish this method
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }


    /**
     * pushes an array of the specified size to the method visitor. The generated bytecode will leave
     * the new array at the top of the stack.
     *
     * @param mv   MethodVisitor to use
     * @param size Size of the array to create
     * @param type Type of array to create
     * @throws ProxyGenerationException
     */
    private void createArrayDefinition(final MethodVisitor mv, final int size, final Class<?> type)
            throws ProxyGenerationException
    {
        // create a new array of java.lang.class (2)

        if (size < 0)
        {
            throw new ProxyGenerationException("Array size cannot be less than zero");
        }

        pushIntOntoStack(mv, size);

        mv.visitTypeInsn(Opcodes.ANEWARRAY, type.getCanonicalName().replace('.', '/'));
    }


}
