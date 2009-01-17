package org.apache.webbeans.test.mock;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Set;
import java.util.StringTokenizer;

import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

/**
 * Implement the ServletContext interface for testing.
 */
public class MockServletContext implements ServletContext
{

    @SuppressWarnings("unchecked")
    private Hashtable attributes = new Hashtable();

    public Object getAttribute(String name)
    {
        return attributes.get(name);
    }

    @SuppressWarnings("unchecked")
    public Enumeration getAttributeNames()
    {
        return attributes.keys();
    }

    public ServletContext getContext(String uripath)
    {
        return this;
    }

    public String getContextPath()
    {
        return "mockContextpath";
    }

    public String getInitParameter(String name)
    {
        return null;
    }

    @SuppressWarnings("unchecked")
    public Enumeration getInitParameterNames()
    {
        return new StringTokenizer(""); // 'standard' empty Enumeration
    }

    public int getMajorVersion()
    {
        return 2;
    }

    public String getMimeType(String file)
    {
        return null;
    }

    public int getMinorVersion()
    {
        return 0;
    }

    public RequestDispatcher getNamedDispatcher(String name)
    {
        return null;
    }

    public String getRealPath(String path)
    {
        return "mockRealPath";
    }

    public RequestDispatcher getRequestDispatcher(String path)
    {
        return null;
    }

    public URL getResource(String path) throws MalformedURLException
    {
        return null;
    }

    public InputStream getResourceAsStream(String path)
    {
        return null;
    }

    @SuppressWarnings("unchecked")
    public Set getResourcePaths(String path)
    {
        return null;
    }

    public String getServerInfo()
    {
        return "mockServer";
    }

    public Servlet getServlet(String name) throws ServletException
    {
        return null;
    }

    public String getServletContextName()
    {
        return null;
    }

    @SuppressWarnings("unchecked")
    public Enumeration getServletNames()
    {
        return null;
    }

    @SuppressWarnings("unchecked")
    public Enumeration getServlets()
    {
        return null;
    }

    public void log(String msg)
    {
        // TODO
    }

    public void log(Exception exception, String msg)
    {
        // TODO

    }

    public void log(String message, Throwable throwable)
    {
        // TODO

    }

    public void removeAttribute(String name)
    {
        attributes.remove(name);
    }

    @SuppressWarnings("unchecked")
    public void setAttribute(String name, Object object)
    {
        attributes.put(name, object);
    }

}
