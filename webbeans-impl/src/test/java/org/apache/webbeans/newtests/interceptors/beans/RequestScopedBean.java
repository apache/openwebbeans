package org.apache.webbeans.newtests.interceptors.beans;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

@RequestScoped
public class RequestScopedBean {

	private int i=0;
	private @Inject ApplicationScopedBean myService;

	/** we need this trick, since the injected beans itself are only proxies... */
	public RequestScopedBean getInstance() {
		return this;
	}
	
	public int getI() {
		return i;
	}

	public void setI(int i) {
		this.i = i;
	}

	public ApplicationScopedBean getMyService() {
		return myService;
	}

	public void setMyService(ApplicationScopedBean myService) {
		this.myService = myService;
	}
	
	
}
