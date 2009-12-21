package org.apache.webbeans.newtests.interceptors.beans;

import javax.enterprise.context.ApplicationScoped;

import org.apache.webbeans.test.component.intercept.webbeans.bindings.Transactional;

@Transactional
@ApplicationScoped
public class ApplicationScopedBean {

	private int j;

	public int getJ() {
		return j;
	}

	public void setJ(int j) {
		this.j = j;
	}
	
}
