package org.apache.webbeans.newtests.interceptors.beans;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

@RequestScoped
public class RequestScopedBean {

	private int i=0;
	private @Inject ApplicationScopedBean myService;

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
