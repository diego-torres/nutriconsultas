package com.nutriconsultas.validation.template;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import jakarta.servlet.ServletConnection;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpUpgradeHandler;

import org.thymeleaf.context.IWebContext;
import org.thymeleaf.web.IWebApplication;
import org.thymeleaf.web.IWebExchange;
import org.thymeleaf.web.IWebRequest;

/**
 * Factory for creating IWebContext instances for template validation. This encapsulates
 * all the mock servlet objects needed for Thymeleaf template processing.
 */
public class WebContextFactory {

	/**
	 * Creates an IWebContext for template validation with the provided mock model
	 * variables.
	 * @param mockVariables the mock model variables to include in the context
	 * @return an IWebContext instance
	 */
	public static IWebContext createWebContext(Map<String, Object> mockVariables) {
		ServletContext servletContext = createMockServletContext();
		HttpServletRequest request = createMockHttpServletRequest(servletContext);
		HttpServletResponse response = createMockHttpServletResponse();
		IWebExchange webExchange = createMockWebExchange(request, response, servletContext);

		return new IWebContext() {
			@Override
			public Locale getLocale() {
				return Locale.getDefault();
			}

			@Override
			public boolean containsVariable(String name) {
				return mockVariables.containsKey(name);
			}

			@Override
			public Object getVariable(String name) {
				return mockVariables.get(name);
			}

			@Override
			public java.util.Set<String> getVariableNames() {
				return mockVariables.keySet();
			}

			@Override
			public IWebExchange getExchange() {
				return webExchange;
			}
		};
	}

	private static IWebExchange createMockWebExchange(HttpServletRequest request, HttpServletResponse response,
			ServletContext servletContext) {
		IWebRequest webRequest = createMockWebRequest(request);
		IWebApplication webApplication = createMockWebApplication(servletContext);
		Map<String, Object> attributes = new HashMap<>();

		return new IWebExchange() {
			@Override
			public IWebRequest getRequest() {
				return webRequest;
			}

			@Override
			public IWebApplication getApplication() {
				return webApplication;
			}

			@Override
			public String getCharacterEncoding() {
				return request.getCharacterEncoding() != null ? request.getCharacterEncoding() : "UTF-8";
			}

			@Override
			public java.security.Principal getPrincipal() {
				return request.getUserPrincipal();
			}

			@Override
			public String getContentType() {
				return request.getContentType();
			}

			@Override
			public Locale getLocale() {
				return request.getLocale() != null ? request.getLocale() : Locale.getDefault();
			}

			@Override
			public boolean containsAttribute(String name) {
				return attributes.containsKey(name);
			}

			@Override
			public void setAttributeValue(String name, Object value) {
				attributes.put(name, value);
			}

			@Override
			public String transformURL(String url) {
				return url;
			}

			@Override
			public Object getAttributeValue(String name) {
				return attributes.get(name);
			}

			@Override
			public int getAttributeCount() {
				return attributes.size();
			}

			@Override
			public org.thymeleaf.web.IWebSession getSession() {
				return null;
			}

			@Override
			public java.util.Set<String> getAllAttributeNames() {
				return attributes.keySet();
			}

			@Override
			public void removeAttribute(String name) {
				attributes.remove(name);
			}

			@Override
			public java.util.Map<String, Object> getAttributeMap() {
				return new HashMap<>(attributes);
			}
		};
	}

	private static IWebRequest createMockWebRequest(HttpServletRequest request) {
		return new IWebRequest() {
			@Override
			public String getMethod() {
				return request.getMethod();
			}

			@Override
			public String getScheme() {
				return request.getScheme();
			}

			@Override
			public String getServerName() {
				return request.getServerName();
			}

			@Override
			public Integer getServerPort() {
				return request.getServerPort();
			}

			@Override
			public String getPathWithinApplication() {
				return request.getRequestURI();
			}

			@Override
			public String getApplicationPath() {
				return request.getContextPath();
			}

			@Override
			public String getQueryString() {
				return request.getQueryString();
			}

			@Override
			public String getHeaderValue(String name) {
				return request.getHeader(name);
			}

			@Override
			public String[] getHeaderValues(String name) {
				Enumeration<String> headers = request.getHeaders(name);
				List<String> result = new java.util.ArrayList<>();
				if (headers != null) {
					while (headers.hasMoreElements()) {
						result.add(headers.nextElement());
					}
				}
				return result.toArray(new String[0]);
			}

			@Override
			public Set<String> getAllHeaderNames() {
				Enumeration<String> headerNames = request.getHeaderNames();
				Set<String> result = new java.util.HashSet<>();
				if (headerNames != null) {
					while (headerNames.hasMoreElements()) {
						result.add(headerNames.nextElement());
					}
				}
				return result;
			}

			@Override
			public String getParameterValue(String name) {
				return request.getParameter(name);
			}

			@Override
			public String[] getParameterValues(String name) {
				return request.getParameterValues(name);
			}

			@Override
			public Set<String> getAllParameterNames() {
				return new java.util.HashSet<>(request.getParameterMap().keySet());
			}

			@Override
			public String getCookieValue(String name) {
				jakarta.servlet.http.Cookie[] cookies = request.getCookies();
				if (cookies != null) {
					for (jakarta.servlet.http.Cookie cookie : cookies) {
						if (name.equals(cookie.getName())) {
							return cookie.getValue();
						}
					}
				}
				return null;
			}

			@Override
			public Set<String> getAllCookieNames() {
				jakarta.servlet.http.Cookie[] cookies = request.getCookies();
				Set<String> result = new java.util.HashSet<>();
				if (cookies != null) {
					for (jakarta.servlet.http.Cookie cookie : cookies) {
						result.add(cookie.getName());
					}
				}
				return result;
			}

			@Override
			public boolean containsCookie(String name) {
				jakarta.servlet.http.Cookie[] cookies = request.getCookies();
				if (cookies != null) {
					for (jakarta.servlet.http.Cookie cookie : cookies) {
						if (name.equals(cookie.getName())) {
							return true;
						}
					}
				}
				return false;
			}

			@Override
			public boolean containsParameter(String name) {
				return request.getParameter(name) != null;
			}

			@Override
			public int getHeaderCount() {
				Enumeration<String> headerNames = request.getHeaderNames();
				int count = 0;
				if (headerNames != null) {
					while (headerNames.hasMoreElements()) {
						headerNames.nextElement();
						count++;
					}
				}
				return count;
			}

			@Override
			public boolean containsHeader(String name) {
				return request.getHeader(name) != null;
			}

			@Override
			public java.util.Map<String, String[]> getHeaderMap() {
				java.util.Map<String, String[]> map = new HashMap<>();
				Enumeration<String> headerNames = request.getHeaderNames();
				if (headerNames != null) {
					while (headerNames.hasMoreElements()) {
						String headerName = headerNames.nextElement();
						Enumeration<String> headers = request.getHeaders(headerName);
						java.util.List<String> values = new java.util.ArrayList<>();
						if (headers != null) {
							while (headers.hasMoreElements()) {
								values.add(headers.nextElement());
							}
						}
						map.put(headerName, values.toArray(new String[0]));
					}
				}
				return map;
			}

			@Override
			public int getParameterCount() {
				return request.getParameterMap().size();
			}

			@Override
			public int getCookieCount() {
				jakarta.servlet.http.Cookie[] cookies = request.getCookies();
				return cookies != null ? cookies.length : 0;
			}

			@Override
			public java.util.Map<String, String[]> getCookieMap() {
				java.util.Map<String, String[]> map = new HashMap<>();
				jakarta.servlet.http.Cookie[] cookies = request.getCookies();
				if (cookies != null) {
					for (jakarta.servlet.http.Cookie cookie : cookies) {
						map.put(cookie.getName(), new String[] { cookie.getValue() });
					}
				}
				return map;
			}

			@Override
			public java.util.Map<String, String[]> getParameterMap() {
				return request.getParameterMap();
			}

			@Override
			public String[] getCookieValues(String name) {
				List<String> values = new java.util.ArrayList<>();
				jakarta.servlet.http.Cookie[] cookies = request.getCookies();
				if (cookies != null) {
					for (jakarta.servlet.http.Cookie cookie : cookies) {
						if (name.equals(cookie.getName())) {
							values.add(cookie.getValue());
						}
					}
				}
				return values.toArray(new String[0]);
			}
		};
	}

	private static IWebApplication createMockWebApplication(ServletContext servletContext) {
		return new IWebApplication() {
			@Override
			public String getAttributeValue(String name) {
				Object attr = servletContext.getAttribute(name);
				return attr != null ? attr.toString() : null;
			}

			@Override
			public Set<String> getAllAttributeNames() {
				Enumeration<String> names = servletContext.getAttributeNames();
				Set<String> result = new java.util.HashSet<>();
				if (names != null) {
					while (names.hasMoreElements()) {
						result.add(names.nextElement());
					}
				}
				return result;
			}

			@Override
			public void setAttributeValue(String name, Object value) {
				servletContext.setAttribute(name, value);
			}

			@Override
			public void removeAttribute(String name) {
				servletContext.removeAttribute(name);
			}

			@Override
			public boolean containsAttribute(String name) {
				return servletContext.getAttribute(name) != null;
			}

			@Override
			public int getAttributeCount() {
				Enumeration<String> names = servletContext.getAttributeNames();
				int count = 0;
				if (names != null) {
					while (names.hasMoreElements()) {
						names.nextElement();
						count++;
					}
				}
				return count;
			}

			@Override
			public boolean resourceExists(String path) {
				try {
					return servletContext.getResource(path) != null;
				}
				catch (java.net.MalformedURLException e) {
					return false;
				}
			}

			@Override
			public java.io.InputStream getResourceAsStream(String path) {
				return servletContext.getResourceAsStream(path);
			}

			@Override
			public java.util.Map<String, Object> getAttributeMap() {
				java.util.Map<String, Object> map = new HashMap<>();
				Enumeration<String> names = servletContext.getAttributeNames();
				if (names != null) {
					while (names.hasMoreElements()) {
						String name = names.nextElement();
						Object value = servletContext.getAttribute(name);
						map.put(name, value);
					}
				}
				return map;
			}
		};
	}

	private static ServletContext createMockServletContext() {
		return new ServletContext() {
			private final Map<String, Object> attributes = new HashMap<>();

			@Override
			public String getContextPath() {
				return "";
			}

			@Override
			public ServletContext getContext(String uripath) {
				return this;
			}

			@Override
			public int getMajorVersion() {
				return 6;
			}

			@Override
			public int getMinorVersion() {
				return 0;
			}

			@Override
			public int getEffectiveMajorVersion() {
				return 6;
			}

			@Override
			public int getEffectiveMinorVersion() {
				return 0;
			}

			@Override
			public String getMimeType(String file) {
				return null;
			}

			@Override
			public java.util.Set<String> getResourcePaths(String path) {
				return null;
			}

			@Override
			public java.net.URL getResource(String path) {
				return null;
			}

			@Override
			public java.io.InputStream getResourceAsStream(String path) {
				return null;
			}

			@Override
			public jakarta.servlet.RequestDispatcher getRequestDispatcher(String path) {
				return null;
			}

			@Override
			public jakarta.servlet.RequestDispatcher getNamedDispatcher(String name) {
				return null;
			}

			@Override
			public void log(String msg) {
				// No-op for validation
			}

			@Override
			public void log(String message, Throwable throwable) {
				// No-op for validation
			}

			@Override
			public String getRealPath(String path) {
				return null;
			}

			@Override
			public String getServerInfo() {
				return "MockServletContext/1.0";
			}

			@Override
			public String getInitParameter(String name) {
				return null;
			}

			@Override
			public Enumeration<String> getInitParameterNames() {
				return java.util.Collections.emptyEnumeration();
			}

			@Override
			public boolean setInitParameter(String name, String value) {
				return false;
			}

			@Override
			public Object getAttribute(String name) {
				return attributes.get(name);
			}

			@Override
			public Enumeration<String> getAttributeNames() {
				return java.util.Collections.enumeration(attributes.keySet());
			}

			@Override
			public void setAttribute(String name, Object object) {
				attributes.put(name, object);
			}

			@Override
			public void removeAttribute(String name) {
				attributes.remove(name);
			}

			@Override
			public String getServletContextName() {
				return "MockServletContext";
			}

			@Override
			public jakarta.servlet.ServletRegistration.Dynamic addServlet(String servletName,
					jakarta.servlet.Servlet servlet) {
				return null;
			}

			@Override
			public jakarta.servlet.ServletRegistration.Dynamic addServlet(String servletName, String className) {
				return null;
			}

			@Override
			public jakarta.servlet.ServletRegistration.Dynamic addServlet(String servletName,
					Class<? extends jakarta.servlet.Servlet> servletClass) {
				return null;
			}

			@Override
			public <T extends jakarta.servlet.Servlet> T createServlet(Class<T> clazz) {
				return null;
			}

			@Override
			public jakarta.servlet.ServletRegistration getServletRegistration(String servletName) {
				return null;
			}

			@Override
			public java.util.Map<String, ? extends jakarta.servlet.ServletRegistration> getServletRegistrations() {
				return null;
			}

			@Override
			public jakarta.servlet.FilterRegistration.Dynamic addFilter(String filterName,
					jakarta.servlet.Filter filter) {
				return null;
			}

			@Override
			public jakarta.servlet.FilterRegistration.Dynamic addFilter(String filterName, String className) {
				return null;
			}

			@Override
			public jakarta.servlet.FilterRegistration.Dynamic addFilter(String filterName,
					Class<? extends jakarta.servlet.Filter> filterClass) {
				return null;
			}

			@Override
			public <T extends jakarta.servlet.Filter> T createFilter(Class<T> clazz) {
				return null;
			}

			@Override
			public jakarta.servlet.FilterRegistration getFilterRegistration(String filterName) {
				return null;
			}

			@Override
			public java.util.Map<String, ? extends jakarta.servlet.FilterRegistration> getFilterRegistrations() {
				return null;
			}

			@Override
			public jakarta.servlet.SessionCookieConfig getSessionCookieConfig() {
				return null;
			}

			@Override
			public void setSessionTrackingModes(
					java.util.Set<jakarta.servlet.SessionTrackingMode> sessionTrackingModes) {
				// No-op
			}

			@Override
			public java.util.Set<jakarta.servlet.SessionTrackingMode> getDefaultSessionTrackingModes() {
				return null;
			}

			@Override
			public java.util.Set<jakarta.servlet.SessionTrackingMode> getEffectiveSessionTrackingModes() {
				return null;
			}

			@Override
			public void addListener(String className) {
				// No-op
			}

			@Override
			public <T extends java.util.EventListener> void addListener(T t) {
				// No-op
			}

			@Override
			public void addListener(Class<? extends java.util.EventListener> listenerClass) {
				// No-op
			}

			@Override
			public <T extends java.util.EventListener> T createListener(Class<T> clazz) {
				return null;
			}

			@Override
			public jakarta.servlet.descriptor.JspConfigDescriptor getJspConfigDescriptor() {
				return null;
			}

			@Override
			public ClassLoader getClassLoader() {
				return Thread.currentThread().getContextClassLoader();
			}

			@Override
			public void declareRoles(String... roleNames) {
				// No-op
			}

			@Override
			public String getVirtualServerName() {
				return null;
			}

			@Override
			public jakarta.servlet.ServletRegistration.Dynamic addJspFile(String servletName, String jspFile) {
				return null;
			}

			@Override
			public int getSessionTimeout() {
				return 0;
			}

			@Override
			public void setSessionTimeout(int sessionTimeout) {
				// No-op
			}

			@Override
			public String getRequestCharacterEncoding() {
				return null;
			}

			@Override
			public void setRequestCharacterEncoding(String encoding) {
				// No-op
			}

			@Override
			public String getResponseCharacterEncoding() {
				return null;
			}

			@Override
			public void setResponseCharacterEncoding(String encoding) {
				// No-op
			}
		};
	}

	private static HttpServletRequest createMockHttpServletRequest(ServletContext servletContext) {
		return new HttpServletRequest() {
			private final Map<String, Object> attributes = new HashMap<>();

			@Override
			public String getAuthType() {
				return null;
			}

			@Override
			public jakarta.servlet.http.Cookie[] getCookies() {
				return new jakarta.servlet.http.Cookie[0];
			}

			@Override
			public long getDateHeader(String name) {
				return -1;
			}

			@Override
			public String getHeader(String name) {
				return null;
			}

			@Override
			public Enumeration<String> getHeaders(String name) {
				return java.util.Collections.emptyEnumeration();
			}

			@Override
			public Enumeration<String> getHeaderNames() {
				return java.util.Collections.emptyEnumeration();
			}

			@Override
			public int getIntHeader(String name) {
				return -1;
			}

			@Override
			public String getMethod() {
				return "GET";
			}

			@Override
			public String getPathInfo() {
				return null;
			}

			@Override
			public String getPathTranslated() {
				return null;
			}

			@Override
			public String getContextPath() {
				return "";
			}

			@Override
			public String getQueryString() {
				return null;
			}

			@Override
			public String getRemoteUser() {
				return null;
			}

			@Override
			public boolean isUserInRole(String role) {
				return false;
			}

			@Override
			public java.security.Principal getUserPrincipal() {
				return null;
			}

			@Override
			public String getRequestedSessionId() {
				return null;
			}

			@Override
			public String getRequestURI() {
				return "/";
			}

			@Override
			public StringBuffer getRequestURL() {
				return new StringBuffer("http://localhost/");
			}

			@Override
			public String getServletPath() {
				return "/";
			}

			@Override
			public jakarta.servlet.http.HttpSession getSession(boolean create) {
				return null;
			}

			@Override
			public jakarta.servlet.http.HttpSession getSession() {
				return null;
			}

			@Override
			public String changeSessionId() {
				return null;
			}

			@Override
			public boolean isRequestedSessionIdValid() {
				return false;
			}

			@Override
			public boolean isRequestedSessionIdFromCookie() {
				return false;
			}

			@Override
			public boolean isRequestedSessionIdFromURL() {
				return false;
			}

			@Override
			public boolean authenticate(HttpServletResponse response) {
				return false;
			}

			@Override
			public void login(String username, String password) {
				// No-op
			}

			@Override
			public void logout() {
				// No-op
			}

			@Override
			public java.util.Collection<jakarta.servlet.http.Part> getParts() {
				return null;
			}

			@Override
			public jakarta.servlet.http.Part getPart(String name) {
				return null;
			}

			@Override
			public <T extends HttpUpgradeHandler> T upgrade(Class<T> httpUpgradeHandlerClass) {
				return null;
			}

			@Override
			public Object getAttribute(String name) {
				return attributes.get(name);
			}

			@Override
			public Enumeration<String> getAttributeNames() {
				return java.util.Collections.enumeration(attributes.keySet());
			}

			@Override
			public String getCharacterEncoding() {
				return "UTF-8";
			}

			@Override
			public void setCharacterEncoding(String env) {
				// No-op
			}

			@Override
			public int getContentLength() {
				return 0;
			}

			@Override
			public long getContentLengthLong() {
				return 0;
			}

			@Override
			public String getContentType() {
				return null;
			}

			@Override
			public jakarta.servlet.ServletInputStream getInputStream() {
				return null;
			}

			@Override
			public String getParameter(String name) {
				return null;
			}

			@Override
			public Enumeration<String> getParameterNames() {
				return java.util.Collections.emptyEnumeration();
			}

			@Override
			public String[] getParameterValues(String name) {
				return null;
			}

			@Override
			public Map<String, String[]> getParameterMap() {
				return new HashMap<>();
			}

			@Override
			public String getProtocol() {
				return "HTTP/1.1";
			}

			@Override
			public String getScheme() {
				return "http";
			}

			@Override
			public String getServerName() {
				return "localhost";
			}

			@Override
			public int getServerPort() {
				return 80;
			}

			@Override
			public java.io.BufferedReader getReader() {
				return null;
			}

			@Override
			public String getRemoteAddr() {
				return "127.0.0.1";
			}

			@Override
			public String getRemoteHost() {
				return "localhost";
			}

			@Override
			public void setAttribute(String name, Object o) {
				attributes.put(name, o);
			}

			@Override
			public void removeAttribute(String name) {
				attributes.remove(name);
			}

			@Override
			public Locale getLocale() {
				return Locale.getDefault();
			}

			@Override
			public Enumeration<Locale> getLocales() {
				return java.util.Collections.enumeration(java.util.List.of(Locale.getDefault()));
			}

			@Override
			public boolean isSecure() {
				return false;
			}

			@Override
			public jakarta.servlet.RequestDispatcher getRequestDispatcher(String path) {
				return null;
			}

			@Override
			public int getRemotePort() {
				return 0;
			}

			@Override
			public String getRequestId() {
				return null;
			}

			@Override
			public String getProtocolRequestId() {
				return null;
			}

			@Override
			public ServletConnection getServletConnection() {
				return null;
			}

			@Override
			public String getLocalName() {
				return "localhost";
			}

			@Override
			public String getLocalAddr() {
				return "127.0.0.1";
			}

			@Override
			public int getLocalPort() {
				return 80;
			}

			@Override
			public ServletContext getServletContext() {
				return servletContext;
			}

			@Override
			public jakarta.servlet.AsyncContext startAsync() {
				return null;
			}

			@Override
			public jakarta.servlet.AsyncContext startAsync(jakarta.servlet.ServletRequest servletRequest,
					jakarta.servlet.ServletResponse servletResponse) {
				return null;
			}

			@Override
			public boolean isAsyncStarted() {
				return false;
			}

			@Override
			public boolean isAsyncSupported() {
				return false;
			}

			@Override
			public jakarta.servlet.AsyncContext getAsyncContext() {
				return null;
			}

			@Override
			public jakarta.servlet.DispatcherType getDispatcherType() {
				return jakarta.servlet.DispatcherType.REQUEST;
			}
		};
	}

	private static HttpServletResponse createMockHttpServletResponse() {
		return new HttpServletResponse() {
			@Override
			public void addCookie(jakarta.servlet.http.Cookie cookie) {
				// No-op
			}

			@Override
			public boolean containsHeader(String name) {
				return false;
			}

			@Override
			public String encodeURL(String url) {
				return url;
			}

			@Override
			public String encodeRedirectURL(String url) {
				return url;
			}

			@Override
			public void sendError(int sc, String msg) {
				// No-op
			}

			@Override
			public void sendError(int sc) {
				// No-op
			}

			@Override
			public void sendRedirect(String location) {
				// No-op
			}

			@Override
			public void setDateHeader(String name, long date) {
				// No-op
			}

			@Override
			public void addDateHeader(String name, long date) {
				// No-op
			}

			@Override
			public void setHeader(String name, String value) {
				// No-op
			}

			@Override
			public void addHeader(String name, String value) {
				// No-op
			}

			@Override
			public void setIntHeader(String name, int value) {
				// No-op
			}

			@Override
			public void addIntHeader(String name, int value) {
				// No-op
			}

			@Override
			public void setStatus(int sc) {
				// No-op
			}

			@Override
			public int getStatus() {
				return 200;
			}

			@Override
			public String getHeader(String name) {
				return null;
			}

			@Override
			public java.util.Collection<String> getHeaders(String name) {
				return new java.util.ArrayList<>();
			}

			@Override
			public java.util.Collection<String> getHeaderNames() {
				return new java.util.ArrayList<>();
			}

			@Override
			public String getCharacterEncoding() {
				return "UTF-8";
			}

			@Override
			public String getContentType() {
				return null;
			}

			@Override
			public jakarta.servlet.ServletOutputStream getOutputStream() {
				return null;
			}

			@Override
			public java.io.PrintWriter getWriter() {
				return null;
			}

			@Override
			public void setCharacterEncoding(String charset) {
				// No-op
			}

			@Override
			public void setContentLength(int len) {
				// No-op
			}

			@Override
			public void setContentLengthLong(long length) {
				// No-op
			}

			@Override
			public void setContentType(String type) {
				// No-op
			}

			@Override
			public void setBufferSize(int size) {
				// No-op
			}

			@Override
			public int getBufferSize() {
				return 0;
			}

			@Override
			public void flushBuffer() {
				// No-op
			}

			@Override
			public void resetBuffer() {
				// No-op
			}

			@Override
			public boolean isCommitted() {
				return false;
			}

			@Override
			public void reset() {
				// No-op
			}

			@Override
			public void setLocale(Locale loc) {
				// No-op
			}

			@Override
			public Locale getLocale() {
				return Locale.getDefault();
			}
		};
	}

}
