package org.apache.webbeans.test.interceptors.owb1441;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.InvocationContext;

public class WatchInterceptor {

  public WatchInterceptor(String totallyUselessParamJustToNotHaveADefaultCt) {

  }

  private static boolean observed = false;

  @AroundInvoke
  public Object invoke(InvocationContext context) throws Exception
  {
    System.out.println("I am watching you " + context.getMethod());
    observed = true;

    return context.proceed();
  }

  public static boolean isObserved()
  {
    boolean wasObserved = observed;
    observed = false;
    return wasObserved;
  }

}
