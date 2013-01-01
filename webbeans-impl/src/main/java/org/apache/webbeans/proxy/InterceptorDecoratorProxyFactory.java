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
import java.lang.reflect.Modifier;
import java.util.List;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
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

    public InterceptorDecoratorProxyFactory()
    {
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

        String[] interfaceNames = new String[]{Type.getInternalName(OwbProxy.class)};

        cw.visit(Opcodes.V1_5, Opcodes.ACC_PUBLIC + Opcodes.ACC_SUPER + Opcodes.ACC_SYNTHETIC, proxyClassFileName, null, classFileName, interfaceNames);
        cw.visitSource(classFileName + ".java", null);

        createInstanceVariables(cw, classToProxy, classFileName);

        createConstructor(cw, proxyClassFileName, classToProxy, classFileName);


        if (nonInterceptedMethods != null)
        {
            delegateNonInterceptedMethods(cw, proxyClassFileName, classToProxy, nonInterceptedMethods);
        }

        if (interceptedMethods != null)
        {
            delegateInterceptedMethods(cw, proxyClassFileName, classToProxy, interceptedMethods);
        }

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
    private void delegateNonInterceptedMethods(ClassWriter cw, String proxyClassFileName, Class<?> classToProxy, List<Method> noninterceptedMethods)
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

    private boolean unproxyableMethod(Method delegatedMethod)
    {
        int modifiers = delegatedMethod.getModifiers();

        //X TODO how to deal with native functions?
        return (modifiers & (Modifier.PRIVATE | Modifier.ABSTRACT | Modifier.STATIC | Modifier.FINAL | Modifier.NATIVE)) > 0 ||
               "finalize".equals(delegatedMethod.getName());
    }

    private void delegateInterceptedMethods(ClassWriter cw, String proxyClassFileName, Class<?> classToProxy, List<Method> interceptedMethods)
            throws ProxyGenerationException
    {
        for (Method proxiedMethod : interceptedMethods)
        {
            generateInvocationHandlerMethod(cw, proxiedMethod, proxyClassFileName);
        }
    }

    private void generateInvocationHandlerMethod(ClassWriter cw, Method method, String proxyName)
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

        // invoke getMethod() with the method name and the array of types
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Class", "getDeclaredMethod",
                "(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;");

        // store the returned method for later
        mv.visitVarInsn(Opcodes.ASTORE, length);

        // the following code generates bytecode equivalent to:
        // return ((<returntype>) invocationHandler.invoke(this, method, new Object[] { <function arguments }))[.<primitive>Value()];

        final Label l4 = new Label();
        mv.visitLabel(l4);
        mv.visitVarInsn(Opcodes.ALOAD, 0);

        // get the invocationHandler field from this class
        mv.visitFieldInsn(Opcodes.GETFIELD, proxyName, FIELD_INVOCATION_HANDLER, "Ljava/lang/reflect/InvocationHandler;");

        // we want to pass "this" in as the first parameter
        mv.visitVarInsn(Opcodes.ALOAD, 0);

        // and the method we fetched earlier
        mv.visitVarInsn(Opcodes.ALOAD, length);

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
        mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/lang/reflect/InvocationHandler", "invoke",
                "(Ljava/lang/Object;Ljava/lang/reflect/Method;[Ljava/lang/Object;)Ljava/lang/Object;");

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
     * Returns the name of the Java method to call to get the primitive value from an Object - e.g. intValue for java.lang.Integer
     *
     * @param type Type whose primitive method we want to lookup
     * @return The name of the method to use
     */
    private String getPrimitiveMethod(final Class<?> type)
    {
        if (Integer.TYPE.equals(type))
        {
            return "intValue";
        }
        else if (Boolean.TYPE.equals(type))
        {
            return "booleanValue";
        }
        else if (Character.TYPE.equals(type))
        {
            return "charValue";
        }
        else if (Byte.TYPE.equals(type))
        {
            return "byteValue";
        }
        else if (Short.TYPE.equals(type))
        {
            return "shortValue";
        }
        else if (Float.TYPE.equals(type))
        {
            return "floatValue";
        }
        else if (Long.TYPE.equals(type))
        {
            return "longValue";
        }
        else if (Double.TYPE.equals(type))
        {
            return "doubleValue";
        }

        throw new IllegalStateException("Type: " + type.getCanonicalName() + " is not a primitive type");
    }



    private void generateReturn(MethodVisitor mv, Method delegatedMethod)
    {
        final Class<?> returnType = delegatedMethod.getReturnType();
        mv.visitInsn(getReturnInsn(returnType));
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

    /**
     * @return the wrapper type for a primitive, e.g. java.lang.Integer for int
     */
    private String getWrapperType(final Class<?> type)
    {
        if (Integer.TYPE.equals(type))
        {
            return Integer.class.getCanonicalName().replace('.', '/');
        }
        else if (Boolean.TYPE.equals(type))
        {
            return Boolean.class.getCanonicalName().replace('.', '/');
        }
        else if (Character.TYPE.equals(type))
        {
            return Character.class.getCanonicalName().replace('.', '/');
        }
        else if (Byte.TYPE.equals(type))
        {
            return Byte.class.getCanonicalName().replace('.', '/');
        }
        else if (Short.TYPE.equals(type))
        {
            return Short.class.getCanonicalName().replace('.', '/');
        }
        else if (Float.TYPE.equals(type))
        {
            return Float.class.getCanonicalName().replace('.', '/');
        }
        else if (Long.TYPE.equals(type))
        {
            return Long.class.getCanonicalName().replace('.', '/');
        }
        else if (Double.TYPE.equals(type))
        {
            return Double.class.getCanonicalName().replace('.', '/');
        }
        else if (Void.TYPE.equals(type))
        {
            return Void.class.getCanonicalName().replace('.', '/');
        }

        throw new IllegalStateException("Type: " + type.getCanonicalName() + " is not a primitive type");
    }

    /**
     * Returns the appropriate bytecode instruction to load a value from a variable to the stack
     *
     * @param type Type to load
     * @return Bytecode instruction to use
     */
    private int getVarInsn(final Class<?> type)
    {
        if (type.isPrimitive())
        {
            if (Integer.TYPE.equals(type))
            {
                return Opcodes.ILOAD;
            }
            else if (Boolean.TYPE.equals(type))
            {
                return Opcodes.ILOAD;
            }
            else if (Character.TYPE.equals(type))
            {
                return Opcodes.ILOAD;
            }
            else if (Byte.TYPE.equals(type))
            {
                return Opcodes.ILOAD;
            }
            else if (Short.TYPE.equals(type))
            {
                return Opcodes.ILOAD;
            }
            else if (Float.TYPE.equals(type))
            {
                return Opcodes.FLOAD;
            }
            else if (Long.TYPE.equals(type))
            {
                return Opcodes.LLOAD;
            }
            else if (Double.TYPE.equals(type))
            {
                return Opcodes.DLOAD;
            }
        }

        throw new IllegalStateException("Type: " + type.getCanonicalName() + " is not a primitive type");
    }

    /**
     * Invokes the most appropriate bytecode instruction to put a number on the stack
     *
     * @param mv
     * @param i
     */
    private void pushIntOntoStack(final MethodVisitor mv, final int i)
    {
        if (i == 0)
        {
            mv.visitInsn(Opcodes.ICONST_0);
        }
        else if (i == 1)
        {
            mv.visitInsn(Opcodes.ICONST_1);
        }
        else if (i == 2)
        {
            mv.visitInsn(Opcodes.ICONST_2);
        }
        else if (i == 3)
        {
            mv.visitInsn(Opcodes.ICONST_3);
        }
        else if (i == 4)
        {
            mv.visitInsn(Opcodes.ICONST_4);
        }
        else if (i == 5)
        {
            mv.visitInsn(Opcodes.ICONST_5);
        }
        else if (i > 5 && i <= 255)
        {
            mv.visitIntInsn(Opcodes.BIPUSH, i);
        }
        else
        {
            mv.visitIntInsn(Opcodes.SIPUSH, i);
        }
    }


    /**
     * Gets the appropriate bytecode instruction for RETURN, according to what type we need to return
     *
     * @param type Type the needs to be returned
     * @return The matching bytecode instruction
     */
    private int getReturnInsn(final Class<?> type)
    {
        if (type.isPrimitive())
        {
            if (Void.TYPE.equals(type))
            {
                return Opcodes.RETURN;
            }
            if (Integer.TYPE.equals(type))
            {
                return Opcodes.IRETURN;
            }
            else if (Boolean.TYPE.equals(type))
            {
                return Opcodes.IRETURN;
            }
            else if (Character.TYPE.equals(type))
            {
                return Opcodes.IRETURN;
            }
            else if (Byte.TYPE.equals(type))
            {
                return Opcodes.IRETURN;
            }
            else if (Short.TYPE.equals(type))
            {
                return Opcodes.IRETURN;
            }
            else if (Float.TYPE.equals(type))
            {
                return Opcodes.FRETURN;
            }
            else if (Long.TYPE.equals(type))
            {
                return Opcodes.LRETURN;
            }
            else if (Double.TYPE.equals(type))
            {
                return Opcodes.DRETURN;
            }
        }

        return Opcodes.ARETURN;
    }

    /**
     * Gets the string to use for CHECKCAST instruction, returning the correct value for any type, including primitives and arrays
     *
     * @param returnType The type to cast to with CHECKCAST
     * @return CHECKCAST parameter
     */
    private String getCastType(final Class<?> returnType)
    {
        if (returnType.isPrimitive())
        {
            return getWrapperType(returnType);
        }
        else
        {
            return Type.getInternalName(returnType);
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
