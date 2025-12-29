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
	public static IWebContext createWebContext(final Map<String, Object> mockVariables) {
		final ServletContext servletContext = createMockServletContext();
		final HttpServletRequest request = createMockHttpServletRequest(servletContext);
		final HttpServletResponse response = createMockHttpServletResponse();
		final IWebExchange webExchange = createMockWebExchange(request, response, servletContext);

		return new IWebContext() {
			@Override
			public Locale getLocale() {
				return Locale.getDefault();
			}

			@Override
			public boolean containsVariable(final String name) {
				return mockVariables.containsKey(name);
			}

			@Override
			public Object getVariable(final String name) {
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

	private static IWebExchange createMockWebExchange(final HttpServletRequest request,
			final HttpServletResponse response, final ServletContext servletContext) {
		final IWebRequest webRequest = createMockWebRequest(request);
		final IWebApplication webApplication = createMockWebApplication(servletContext);
		final Map<String, Object> attributes = new HashMap<>();

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
			public boolean containsAttribute(final String name) {
				return attributes.containsKey(name);
			}

			@Override
			public void setAttributeValue(final String name, final Object value) {
				attributes.put(name, value);
			}

			@Override
			public String transformURL(final String url) {
				return url;
			}

			@Override
			public Object getAttributeValue(final String name) {
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
			public void removeAttribute(final String name) {
				attributes.remove(name);
			}

			@Override
			public java.util.Map<String, Object> getAttributeMap() {
				return new HashMap<>(attributes);
			}
		};
	}

	private static IWebRequest createMockWebRequest(final HttpServletRequest request) {
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
			public String getHeaderValue(final String name) {
				return request.getHeader(name);
			}

			@Override
			public String[] getHeaderValues(final String name) {
				final Enumeration<String> headers = request.getHeaders(name);
				final List<String> result = new java.util.ArrayList<>();
				if (headers != null) {
					while (headers.hasMoreElements()) {
						result.add(headers.nextElement());
					}
				}
				return result.toArray(new String[0]);
			}

			@Override
			public Set<String> getAllHeaderNames() {
				final Enumeration<String> headerNames = request.getHeaderNames();
				final Set<String> result = new java.util.HashSet<>();
				if (headerNames != null) {
					while (headerNames.hasMoreElements()) {
						result.add(headerNames.nextElement());
					}
				}
				return result;
			}

			@Override
			public String getParameterValue(final String name) {
				return request.getParameter(name);
			}

			@Override
			public String[] getParameterValues(final String name) {
				return request.getParameterValues(name);
			}

			@Override
			public Set<String> getAllParameterNames() {
				return new java.util.HashSet<>(request.getParameterMap().keySet());
			}

			@Override
			public String getCookieValue(final String name) {
				final jakarta.servlet.http.Cookie[] cookies = request.getCookies();
				String result = null;
				if (cookies != null) {
					for (final jakarta.servlet.http.Cookie cookie : cookies) {
						if (name.equals(cookie.getName())) {
							result = cookie.getValue();
							break;
						}
					}
				}
				return result;
			}

			@Override
			public Set<String> getAllCookieNames() {
				final jakarta.servlet.http.Cookie[] cookies = request.getCookies();
				final Set<String> result = new java.util.HashSet<>();
				if (cookies != null) {
					for (final jakarta.servlet.http.Cookie cookie : cookies) {
						result.add(cookie.getName());
					}
				}
				return result;
			}

			@Override
			public boolean containsCookie(final String name) {
				final jakarta.servlet.http.Cookie[] cookies = request.getCookies();
				boolean result = false;
				if (cookies != null) {
					for (final jakarta.servlet.http.Cookie cookie : cookies) {
						if (name.equals(cookie.getName())) {
							result = true;
							break;
						}
					}
				}
				return result;
			}

			@Override
			public boolean containsParameter(final String name) {
				return request.getParameter(name) != null;
			}

			@Override
			public int getHeaderCount() {
				final Enumeration<String> headerNames = request.getHeaderNames();
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
			public boolean containsHeader(final String name) {
				return request.getHeader(name) != null;
			}

			@Override
			public java.util.Map<String, String[]> getHeaderMap() {
				final java.util.Map<String, String[]> map = new HashMap<>();
				final Enumeration<String> headerNames = request.getHeaderNames();
				if (headerNames != null) {
					while (headerNames.hasMoreElements()) {
						final String headerName = headerNames.nextElement();
						final Enumeration<String> headers = request.getHeaders(headerName);
						final java.util.List<String> values = new java.util.ArrayList<>();
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
				final java.util.Map<String, String[]> map = new HashMap<>();
				final jakarta.servlet.http.Cookie[] cookies = request.getCookies();
				if (cookies != null) {
					for (final jakarta.servlet.http.Cookie cookie : cookies) {
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
			public String[] getCookieValues(final String name) {
				final List<String> values = new java.util.ArrayList<>();
				final jakarta.servlet.http.Cookie[] cookies = request.getCookies();
				if (cookies != null) {
					for (final jakarta.servlet.http.Cookie cookie : cookies) {
						if (name.equals(cookie.getName())) {
							values.add(cookie.getValue());
						}
					}
				}
				return values.toArray(new String[0]);
			}
		};
	}

	private static IWebApplication createMockWebApplication(final ServletContext servletContext) {
		return new IWebApplication() {
			@Override
			public String getAttributeValue(final String name) {
				final Object attr = servletContext.getAttribute(name);
				return attr != null ? attr.toString() : null;
			}

			@Override
			public Set<String> getAllAttributeNames() {
				final Enumeration<String> names = servletContext.getAttributeNames();
				final Set<String> result = new java.util.HashSet<>();
				if (names != null) {
					while (names.hasMoreElements()) {
						result.add(names.nextElement());
					}
				}
				return result;
			}

			@Override
			public void setAttributeValue(final String name, final Object value) {
				servletContext.setAttribute(name, value);
			}

			@Override
			public void removeAttribute(final String name) {
				servletContext.removeAttribute(name);
			}

			@Override
			public boolean containsAttribute(final String name) {
				return servletContext.getAttribute(name) != null;
			}

			@Override
			public int getAttributeCount() {
				final Enumeration<String> names = servletContext.getAttributeNames();
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
			public boolean resourceExists(final String path) {
				boolean result = false;
				try {
					result = servletContext.getResource(path) != null;
				}
				catch (final java.net.MalformedURLException e) {
					// Return false on exception
				}
				return result;
			}

			@Override
			public java.io.InputStream getResourceAsStream(final String path) {
				return servletContext.getResourceAsStream(path);
			}

			@Override
			public java.util.Map<String, Object> getAttributeMap() {
				final java.util.Map<String, Object> map = new HashMap<>();
				final Enumeration<String> names = servletContext.getAttributeNames();
				if (names != null) {
					while (names.hasMoreElements()) {
						final String name = names.nextElement();
						final Object value = servletContext.getAttribute(name);
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
			public ServletContext getContext(final String uripath) {
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
			public String getMimeType(final String file) {
				return null;
			}

			@Override
			public java.util.Set<String> getResourcePaths(final String path) {
				return null;
			}

			@Override
			public java.net.URL getResource(final String path) {
				return null;
			}

			@Override
			public java.io.InputStream getResourceAsStream(final String path) {
				return null;
			}

			@Override
			public jakarta.servlet.RequestDispatcher getRequestDispatcher(final String path) {
				return null;
			}

			@Override
			public jakarta.servlet.RequestDispatcher getNamedDispatcher(final String name) {
				return null;
			}

			@Override
			public void log(final String msg) {
				// No-op for validation
			}

			@Override
			public void log(final String message, final Throwable throwable) {
				// No-op for validation
			}

			@Override
			public String getRealPath(final String path) {
				return null;
			}

			@Override
			public String getServerInfo() {
				return "MockServletContext/1.0";
			}

			@Override
			public String getInitParameter(final String name) {
				return null;
			}

			@Override
			public Enumeration<String> getInitParameterNames() {
				return java.util.Collections.emptyEnumeration();
			}

			@Override
			public boolean setInitParameter(final String name, final String value) {
				return false;
			}

			@Override
			public Object getAttribute(final String name) {
				return attributes.get(name);
			}

			@Override
			public Enumeration<String> getAttributeNames() {
				return java.util.Collections.enumeration(attributes.keySet());
			}

			@Override
			public void setAttribute(final String name, final Object object) {
				attributes.put(name, object);
			}

			@Override
			public void removeAttribute(final String name) {
				attributes.remove(name);
			}

			@Override
			public String getServletContextName() {
				return "MockServletContext";
			}

			@Override
			public jakarta.servlet.ServletRegistration.Dynamic addServlet(final String servletName,
					final jakarta.servlet.Servlet servlet) {
				return null;
			}

			@Override
			public jakarta.servlet.ServletRegistration.Dynamic addServlet(final String servletName,
					final String className) {
				return null;
			}

			@Override
			public jakarta.servlet.ServletRegistration.Dynamic addServlet(final String servletName,
					final Class<? extends jakarta.servlet.Servlet> servletClass) {
				return null;
			}

			@Override
			public <T extends jakarta.servlet.Servlet> T createServlet(final Class<T> clazz) {
				return null;
			}

			@Override
			public jakarta.servlet.ServletRegistration getServletRegistration(final String servletName) {
				return null;
			}

			@Override
			public java.util.Map<String, ? extends jakarta.servlet.ServletRegistration> getServletRegistrations() {
				return null;
			}

			@Override
			public jakarta.servlet.FilterRegistration.Dynamic addFilter(final String filterName,
					final jakarta.servlet.Filter filter) {
				return null;
			}

			@Override
			public jakarta.servlet.FilterRegistration.Dynamic addFilter(final String filterName,
					final String className) {
				return null;
			}

			@Override
			public jakarta.servlet.FilterRegistration.Dynamic addFilter(final String filterName,
					final Class<? extends jakarta.servlet.Filter> filterClass) {
				return null;
			}

			@Override
			public <T extends jakarta.servlet.Filter> T createFilter(final Class<T> clazz) {
				return null;
			}

			@Override
			public jakarta.servlet.FilterRegistration getFilterRegistration(final String filterName) {
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
					final java.util.Set<jakarta.servlet.SessionTrackingMode> sessionTrackingModes) {
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
			public void addListener(final String className) {
				// No-op
			}

			@Override
			public <T extends java.util.EventListener> void addListener(final T t) {
				// No-op
			}

			@Override
			public void addListener(final Class<? extends java.util.EventListener> listenerClass) {
				// No-op
			}

			@Override
			public <T extends java.util.EventListener> T createListener(final Class<T> clazz) {
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
			public void declareRoles(final String... roleNames) {
				// No-op
			}

			@Override
			public String getVirtualServerName() {
				return null;
			}

			@Override
			public jakarta.servlet.ServletRegistration.Dynamic addJspFile(final String servletName,
					final String jspFile) {
				return null;
			}

			@Override
			public int getSessionTimeout() {
				return 0;
			}

			@Override
			public void setSessionTimeout(final int sessionTimeout) {
				// No-op
			}

			@Override
			public String getRequestCharacterEncoding() {
				return null;
			}

			@Override
			public void setRequestCharacterEncoding(final String encoding) {
				// No-op
			}

			@Override
			public String getResponseCharacterEncoding() {
				return null;
			}

			@Override
			public void setResponseCharacterEncoding(final String encoding) {
				// No-op
			}
		};
	}

	private static HttpServletRequest createMockHttpServletRequest(final ServletContext servletContext) {
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
			public long getDateHeader(final String name) {
				return -1;
			}

			@Override
			public String getHeader(final String name) {
				return null;
			}

			@Override
			public Enumeration<String> getHeaders(final String name) {
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
			public boolean isUserInRole(final String role) {
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
			public jakarta.servlet.http.HttpSession getSession(final boolean create) {
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
			public boolean authenticate(final HttpServletResponse response) {
				return false;
			}

			@Override
			public void login(final String username, final String password) {
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
			public jakarta.servlet.http.Part getPart(final String name) {
				return null;
			}

			@Override
			public <T extends HttpUpgradeHandler> T upgrade(final Class<T> httpUpgradeHandlerClass) {
				return null;
			}

			@Override
			public Object getAttribute(final String name) {
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
			public void setCharacterEncoding(final String env) {
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
			public String getParameter(final String name) {
				return null;
			}

			@Override
			public Enumeration<String> getParameterNames() {
				return java.util.Collections.emptyEnumeration();
			}

			@Override
			public String[] getParameterValues(final String name) {
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
			public void setAttribute(final String name, final Object o) {
				attributes.put(name, o);
			}

			@Override
			public void removeAttribute(final String name) {
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
			public jakarta.servlet.RequestDispatcher getRequestDispatcher(final String path) {
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
			public jakarta.servlet.AsyncContext startAsync(final jakarta.servlet.ServletRequest servletRequest,
					final jakarta.servlet.ServletResponse servletResponse) {
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
			public void addCookie(final jakarta.servlet.http.Cookie cookie) {
				// No-op
			}

			@Override
			public boolean containsHeader(final String name) {
				return false;
			}

			@Override
			public String encodeURL(final String url) {
				return url;
			}

			@Override
			public String encodeRedirectURL(final String url) {
				return url;
			}

			@Override
			public void sendError(final int sc, final String msg) {
				// No-op
			}

			@Override
			public void sendError(final int sc) {
				// No-op
			}

			@Override
			public void sendRedirect(final String location) {
				// No-op
			}

			@Override
			public void setDateHeader(final String name, final long date) {
				// No-op
			}

			@Override
			public void addDateHeader(final String name, final long date) {
				// No-op
			}

			@Override
			public void setHeader(final String name, final String value) {
				// No-op
			}

			@Override
			public void addHeader(final String name, final String value) {
				// No-op
			}

			@Override
			public void setIntHeader(final String name, final int value) {
				// No-op
			}

			@Override
			public void addIntHeader(final String name, final int value) {
				// No-op
			}

			@Override
			public void setStatus(final int sc) {
				// No-op
			}

			@Override
			public int getStatus() {
				return 200;
			}

			@Override
			public String getHeader(final String name) {
				return null;
			}

			@Override
			public java.util.Collection<String> getHeaders(final String name) {
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
			public void setCharacterEncoding(final String charset) {
				// No-op
			}

			@Override
			public void setContentLength(final int len) {
				// No-op
			}

			@Override
			public void setContentLengthLong(final long length) {
				// No-op
			}

			@Override
			public void setContentType(final String type) {
				// No-op
			}

			@Override
			public void setBufferSize(final int size) {
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
			public void setLocale(final Locale loc) {
				// No-op
			}

			@Override
			public Locale getLocale() {
				return Locale.getDefault();
			}
		};
	}

}
