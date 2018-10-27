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


import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.exception.ProxyGenerationException;
import org.apache.webbeans.exception.WebBeansConfigurationException;
import org.apache.webbeans.logger.WebBeansLoggerFacade;
import org.apache.webbeans.util.Asserts;
import org.apache.webbeans.util.ExceptionUtil;
import org.apache.xbean.asm6.ClassWriter;
import org.apache.xbean.asm6.Label;
import org.apache.xbean.asm6.MethodVisitor;
import org.apache.xbean.asm6.Opcodes;
import org.apache.xbean.asm6.Type;

import javax.enterprise.inject.spi.Bean;
import java.io.ObjectStreamException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;


/**
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
    private static final Logger logger = WebBeansLoggerFacade.getLogger(InterceptorDecoratorProxyFactory.class);


    /** the name of the field which stores the proxied instance */
    public static final String FIELD_PROXIED_INSTANCE = "owbIntDecProxiedInstance";

    /** the name of the field which stores the Interceptor + Decorator stack InterceptorHandler */
    public static final String FIELD_INTERCEPTOR_HANDLER = "owbIntDecHandler";

    /** the name of the field which stores the Method[] of all intercepted methods */
    public static final String FIELD_INTERCEPTED_METHODS = "owbIntDecMethods";

    /**
     * Caches the proxy classes for each bean.
     * We need this to prevent filling up the ClassLoaders by
     */
    private ConcurrentMap<Bean<?>, Class<?>> cachedProxyClasses = new ConcurrentHashMap<Bean<?>, Class<?>>();


    public InterceptorDecoratorProxyFactory(WebBeansContext webBeansContext)
    {
        super(webBeansContext);
    }

    public <T> T createProxyInstance(Class<? extends T> proxyClass, T instance, InterceptorHandler interceptorDecoratorStack)
            throws ProxyGenerationException
    {
        Asserts.assertNotNull(instance);

        try
        {
            T proxy = unsafe.unsafeNewInstance(proxyClass);

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
    public static <T> T unwrapInstance(T proxyInstance)
    {
        try
        {
            if (proxyInstance instanceof OwbInterceptorProxy)
            {
                Field internalInstanceField = proxyInstance.getClass().getDeclaredField(FIELD_PROXIED_INSTANCE);
                internalInstanceField.setAccessible(true);
                return (T) internalInstanceField.get(proxyInstance);
            }
            else
            {
                return proxyInstance;
            }
        }
        catch (Exception e)
        {
            throw ExceptionUtil.throwAsRuntimeException(e);
        }
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
            throw ExceptionUtil.throwAsRuntimeException(e);
        }
    }

    /**
     * <p>Create a decorator and interceptor proxy for the given type. A single instance
     * of such a proxy class has exactly one single internal instance.</p>
     *
     * <p>There are 3 different kind of methods:
     * <ol>
     *     <li>
     *         private methods - they do not get proxied at all! If you like to invoke a private method,
     *         then you can use {@link #unwrapInstance(Object)} and use reflection on it.
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
     * @param bean the bean the proxy serves for. Needed for caching and serialisation.
     * @param classLoader to use for creating the class
     * @param classToProxy the class for which a subclass will get generated
     * @param interceptedMethods the list of intercepted or decorated business methods.
     * @param nonInterceptedMethods all methods which are <b>not</b> intercepted nor decorated and shall get delegated directly
     * @param <T>
     * @return the proxy class
     */
    public synchronized <T> Class<T> createProxyClass(Bean<T> bean, ClassLoader classLoader, Class<T> classToProxy,
                                                      Method[] interceptedMethods, Method[] nonInterceptedMethods)
            throws ProxyGenerationException
    {
        String proxyClassName = getUnusedProxyClassName(
                classLoader,
                (classToProxy.getSigners() != null ? getSignedClassProxyName(classToProxy) : classToProxy.getName()) + "$$OwbInterceptProxy");


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

        cachedProxyClasses.put(bean, clazz);

        return clazz;
    }

    public <T> Class<T> getCachedProxyClass(Bean<T> bean)
    {
        return (Class<T>) cachedProxyClasses.get(bean);
    }

    @Override
    protected Class getMarkerInterface()
    {
        return OwbInterceptorProxy.class;
    }

    @Override
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

    @Override
    protected void createSerialisation(ClassWriter cw, String proxyClassFileName, Class<?> classToProxy, String classFileName)
    {
        String[] exceptionTypeNames = {Type.getType(ObjectStreamException.class).getInternalName()};
        MethodVisitor mv = cw.visitMethod(Modifier.PUBLIC, "writeReplace", "()Ljava/lang/Object;", null, exceptionTypeNames);

        // fill method body
        mv.visitCode();

        // load the contextual instance Provider
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitFieldInsn(Opcodes.GETFIELD, proxyClassFileName, FIELD_INTERCEPTOR_HANDLER, Type.getDescriptor(InterceptorHandler.class));

        mv.visitInsn(Opcodes.ARETURN);

        mv.visitMaxs(-1, -1);
        mv.visitEnd();
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
    @Override
    protected void createConstructor(ClassWriter cw, String proxyClassFileName, Class<?> classToProxy, String classFileName, Constructor<?> ignored)
            throws ProxyGenerationException
    {
        Constructor superDefaultCt;
        String parentClassFileName = classFileName;
        String descriptor = "()V";

        try
        {
            if (classToProxy.isInterface())
            {
                parentClassFileName = Type.getInternalName(Object.class);
                superDefaultCt = Object.class.getConstructor(null);
                descriptor = Type.getConstructorDescriptor(superDefaultCt);
            }
        }
        catch (NoSuchMethodException nsme)
        {
            // no worries
        }

        // was: final String descriptor = Type.getConstructorDescriptor(classToProxy.getDeclaredConstructor());
        // but we need to get a default constructor even if the bean uses constructor injection
        final MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", descriptor, null, null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, parentClassFileName, "<init>", descriptor, false);

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

    /**
     * Directly delegate all non intercepted nor decorated methods to the internal instance.
     *
     * @param noninterceptedMethods all methods which are neither intercepted nor decorated
     */
    @Override
    protected void delegateNonInterceptedMethods(ClassLoader classLoader, ClassWriter cw, String proxyClassFileName, Class<?> classToProxy, Method[] noninterceptedMethods)
    {
        for (Method delegatedMethod : noninterceptedMethods)
        {
            if (unproxyableMethod(delegatedMethod))
            {
                continue;
            }

            final int modifiers = delegatedMethod.getModifiers();
            if (Modifier.isProtected(modifiers)
                && !delegatedMethod.getDeclaringClass().getPackage().getName()
                .equals(classToProxy.getPackage().getName()))
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

            int targetModifiers = modifiers & (Modifier.PROTECTED | Modifier.PUBLIC | MODIFIER_VARARGS);

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
            boolean isItf = delegatedMethod.getDeclaringClass().isInterface();
            mv.visitMethodInsn(
                    isItf ? Opcodes.INVOKEINTERFACE : Opcodes.INVOKEVIRTUAL, declaringClass.getInternalName(),
                    delegatedMethod.getName(), methodDescriptor, isItf);

            generateReturn(mv, delegatedMethod);

            mv.visitMaxs(-1, -1);

            mv.visitEnd();
        }
    }

    @Override
    protected void delegateInterceptedMethods(ClassLoader classLoader, ClassWriter cw, String proxyClassFileName, Class<?> classToProxy, Method[] interceptedMethods)
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
        int modifier = modifiers & (Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED | Opcodes.ACC_VARARGS);

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
        if (methodIndex <128)
        {
            mv.visitIntInsn(Opcodes.BIPUSH, methodIndex);
        }
        else if (methodIndex < 32267)
        {
            // for methods > 127 we need to push a short number as index
            mv.visitIntInsn(Opcodes.SIPUSH, methodIndex);
        }
        else
        {
            throw new ProxyGenerationException("Sorry, we only support Classes with 2^15 methods...");
        }

        // and now load the Method from the array
        mv.visitInsn(Opcodes.AALOAD);


        // prepare the parameter array as Object[] and store it on the stack
        pushMethodParameterArray(mv, parameterTypes);


        // invoke the invocationHandler
        mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(InterceptorHandler.class), "invoke",
                "(Ljava/lang/reflect/Method;[Ljava/lang/Object;)Ljava/lang/Object;", true);

        // cast the result
        mv.visitTypeInsn(Opcodes.CHECKCAST, getCastType(returnType));

        if (returnType.isPrimitive() && (!Void.TYPE.equals(returnType)))
        {
            // get the primitive value
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, getWrapperType(returnType), getPrimitiveMethod(returnType),
                    "()" + Type.getDescriptor(returnType), false);
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
                        "()Ljava/lang/Throwable;", false);
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Object", "getClass", "()Ljava/lang/Class;", false);
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Object", "equals", "(Ljava/lang/Object;)Z", false);

                final Label l6 = new Label();
                mv.visitJumpInsn(Opcodes.IFEQ, l6);

                final Label l7 = new Label();
                mv.visitLabel(l7);

                mv.visitVarInsn(Opcodes.ALOAD, length);
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/reflect/InvocationTargetException", "getCause",
                        "()Ljava/lang/Throwable;", false);
                mv.visitTypeInsn(Opcodes.CHECKCAST, getCastType(exceptionType));
                mv.visitInsn(Opcodes.ATHROW);
                mv.visitLabel(l6);

                if (i == (exceptionTypes.length - 1))
                {
                    mv.visitTypeInsn(Opcodes.NEW, "java/lang/reflect/UndeclaredThrowableException");
                    mv.visitInsn(Opcodes.DUP);
                    mv.visitVarInsn(Opcodes.ALOAD, length);
                    mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/reflect/UndeclaredThrowableException", "<init>",
                            "(Ljava/lang/Throwable;)V", false);
                    mv.visitInsn(Opcodes.ATHROW);
                }
            }
        }

        // finish this method
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }


}
