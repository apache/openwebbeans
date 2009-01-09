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

    @Override
    public Object getAttribute(String name)
    {
        return attributes.get(name);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Enumeration getAttributeNames()
    {
        return attributes.keys();
    }

    @Override
    public ServletContext getContext(String uripath)
    {
        return this;
    }

    @Override
    public String getContextPath()
    {
        return "mockContextpath";
    }

    @Override
    public String getInitParameter(String name)
    {
        return null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Enumeration getInitParameterNames()
    {
        return new StringTokenizer(""); // 'standard' empty Enumeration
    }

    @Override
    public int getMajorVersion()
    {
        return 2;
    }

    @Override
    public String getMimeType(String file)
    {
        return null;
    }

    @Override
    public int getMinorVersion()
    {
        return 0;
    }

    @Override
    public RequestDispatcher getNamedDispatcher(String name)
    {
        return null;
    }

    @Override
    public String getRealPath(String path)
    {
        return "mockRealPath";
    }

    @Override
    public RequestDispatcher getRequestDispatcher(String path)
    {
        return null;
    }

    @Override
    public URL getResource(String path) throws MalformedURLException
    {
        return null;
    }

    @Override
    public InputStream getResourceAsStream(String path)
    {
        return null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Set getResourcePaths(String path)
    {
        return null;
    }

    @Override
    public String getServerInfo()
    {
        return "mockServer";
    }

    @Override
    public Servlet getServlet(String name) throws ServletException
    {
        return null;
    }

    @Override
    public String getServletContextName()
    {
        return null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Enumeration getServletNames()
    {
        return null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Enumeration getServlets()
    {
        return null;
    }

    @Override
    public void log(String msg)
    {
        // TODO
    }

    @Override
    public void log(Exception exception, String msg)
    {
        // TODO

    }

    @Override
    public void log(String message, Throwable throwable)
    {
        // TODO

    }

    @Override
    public void removeAttribute(String name)
    {
        attributes.remove(name);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setAttribute(String name, Object object)
    {
        attributes.put(name, object);
    }

}
