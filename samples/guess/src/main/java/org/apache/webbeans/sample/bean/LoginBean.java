/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.webbeans.sample.bean;

import java.lang.annotation.Annotation;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.context.SessionScoped;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Current;
import javax.enterprise.inject.Named;
import javax.enterprise.inject.Produces;
import javax.event.Fires;
import javax.faces.application.FacesMessage;

import org.apache.webbeans.sample.bindings.LoggedInUser;
import org.apache.webbeans.sample.dependent.LoginCheck;
import org.apache.webbeans.sample.event.LoggedInEvent;
import org.apache.webbeans.sample.model.User;
import org.apache.webbeans.sample.util.FacesMessageUtil;

@RequestScoped
@Named
public class LoginBean
{
	private User user = null;
	
	private String userName;
	
	private String password;
	
	private @Current LoginCheck loginCheck;
	
	private @Current FacesMessageUtil messageUtil;
	
	private @Fires Event<LoggedInEvent> event;
	
	public LoginBean()
	{
		
	}
	
	public String login()
	{
		if(loginCheck.checkLogin(this.userName, this.password))
		{
			user = new User();
			user.setUserName(this.userName);
			user.setPassword(this.password);
			
			event.fire(new LoggedInEvent(this.user), new Annotation[0]);
			
			return "loginSuccess";
		}
		else
		{
			messageUtil.addMessage(FacesMessage.SEVERITY_ERROR, "Login Failed", "Login failed");
			return null;
		}
	}
	
	public void afterLoggedIn(@Observes LoggedInEvent event)
	{
		messageUtil.addMessage(FacesMessage.SEVERITY_INFO, "Login Successfull", "Login Successfull");
	}
	
	
	@Produces @SessionScoped @LoggedInUser @Named(value="currentUser")
	public User getLoggedInUser()
	{
		return this.user;
	}

	/**
	 * @return the userName
	 */
	public String getUserName()
	{
		return userName;
	}

	/**
	 * @param userName the userName to set
	 */
	public void setUserName(String userName)
	{
		this.userName = userName;
	}

	/**
	 * @return the password
	 */
	public String getPassword()
	{
		return password;
	}

	/**
	 * @param password the password to set
	 */
	public void setPassword(String password)
	{
		this.password = password;
	}
	
	
}
