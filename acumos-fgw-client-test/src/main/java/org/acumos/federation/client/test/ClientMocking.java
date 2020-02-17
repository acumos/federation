/*-
 * ===============LICENSE_START=======================================================
 * Acumos
 * ===================================================================================
 * Copyright (C) 2019 AT&T Intellectual Property & Tech Mahindra. All rights reserved.
 * ===================================================================================
 * This Acumos software file is distributed by AT&T and Tech Mahindra
 * under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * This file is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ===============LICENSE_END=========================================================
 */
package org.acumos.federation.client.test;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Base64;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.HttpResponse;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.apache.http.protocol.HttpContext;
import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import org.acumos.federation.client.config.ClientConfig;
import org.acumos.federation.client.config.TlsConfig;
import org.acumos.federation.client.config.BasicAuthConfig;

/**
 * Simulate responses from Federation Gateway or other RestTemplate based clients.
 */

public class ClientMocking implements Answer<HttpResponse> {
	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	/**
	 * Replace single quotes with double quotes.
	 * Makes it easy to write JSON strings.
	 *
	 * @param s String with single quotes.
	 * @return String replacing single quotes with double quotes.
	 */
	public static String xq(String s) {
		return s.replaceAll("'", "\"");
	}

	/**
	 * Create a ClientConfig appropriate for use with the included
	 * test certificate resources.
	 * Name is typically acumosa, acumosb, or acumosc.
	 *
	 * @param name Base name for test certificate resource file.
	 * @return ClientConfig set up for 2-way TLS using test certificate.
	 */
	public static ClientConfig getConfig(String name) {
		ClientConfig ret = new ClientConfig();
		TlsConfig tls = new TlsConfig();
		tls.setKeyStore("classpath:" + name + ".pkcs12");
		tls.setKeyStoreType("PKCS12");
		tls.setKeyStorePassword(name);
		tls.setTrustStore("classpath:acumosTrustStore.jks");
		tls.setTrustStorePassword("acumos");
		ret.setSsl(tls);
		BasicAuthConfig creds = new BasicAuthConfig();
		creds.setUsername(name);
		creds.setPassword(name);
		ret.setCreds(creds);
		return ret;
	}

	private static class BasicCloseableHttpResponse extends BasicHttpResponse implements CloseableHttpResponse {
		public BasicCloseableHttpResponse(StatusLine line) {
			super(line);
		}

		@Override
		public void close() throws IOException {
			// no op
		}
	}

	/**
	 * Information on request being processed
	 */
	public static class RequestInfo {
		private HttpUriRequest req;
		private String line;

		/**
		 * Collect information on a request.
		 * @param req The request to examine.
		 */
		public RequestInfo(HttpUriRequest req) {
			this.req = req;
			line = getMethod() + " " + getPath() + (req.getURI().getQuery() == null ? "": ("?" + req.getURI().getQuery()));
		}

		/**
		 * Get the method.
		 *
		 * @return The HTTP method of the request.
		 */
		public String getMethod() {
			return req.getMethod();
		}

		/**
		 * Get the method and URI
		 *
		 * @return The method and URI of the request, including any query string.
		 */
		public String getLine() {
			return line;
		}

		/**
		 * Get the path component of the URI
		 *
		 * @return The URI excluding the query string.
		 */
		public String getPath() {
			return req.getURI().getPath();
		}

		/**
		 * Check whether the named header is missing.
		 *
		 * @param name attribute name to assert
		 * @return true if the header is absent.
		 */
		public boolean lacksHeader(String name) {
			return req.getFirstHeader(name) == null;
		}

		/**
		 * Check whether the header has the specified header value.
		 *
		 * @param name attribute name to assert
		 * @param value value of attribute to assert
		 * @return true if the header is missing or has the wrong value.
		 */
		public boolean lacksHeaderValue(String name, String value) {
			return req.getFirstHeader(name) == null || !value.equals(req.getFirstHeader(name).getValue());
		}
	}

	private static class Response {
		private Predicate<RequestInfo> matcher;
		private byte[] body;
		private Consumer<RequestInfo> action;
		private int code = 200;
		private String message = "OK";
		private ContentType contentType = ContentType.APPLICATION_JSON;
	}

	private static Predicate<RequestInfo> s2p(String line) {
		return r -> r.getLine().equals(line);
	}

	private ArrayList<Response> responses;
	private HttpClient client;

	/**
	 * Create a responder to handle requests.
	 *
	 * @throws IOException when there is no http answer
	 */

	public ClientMocking() throws IOException {
		responses = new ArrayList<>();
		client = mock(HttpClient.class);
		when(client.execute(any(HttpUriRequest.class), any(HttpContext.class))).thenAnswer(this);
	}

	/**
	 * Redirect the template to this responder.
	 *
	 * @param template The RestTemplate to redirect.
	 * @return This responder.
	 */
	public ClientMocking applyTo(RestTemplate template) {
		template.setRequestFactory(new HttpComponentsClientHttpRequestFactory(client));
		return this;
	}

	/**
	 * Set the identified RestTemplate field to use this responder.
	 *
	 * @param object The object containing the RestTemplate.
	 * @param fieldName The name of the RestTemplate field in the object.
	 * @return This responder.
	 */
	public ClientMocking applyTo(Object object, String fieldName) {
		return applyTo((RestTemplate)ReflectionTestUtils.getField(object, fieldName));
	}

	/**
	 * Set the restTemplate field of an object to use this responder.
	 *
	 * Typically, the object will be a FederationClient or a GatewayClient.
	 * @param object The object with the restTemplate field.
	 * @return This responder.
	 */
	public ClientMocking applyTo(Object object) {
		return applyTo(object, "restTemplate");
	}

	@Override
	public HttpResponse answer(InvocationOnMock invocation) throws Throwable {
		RequestInfo info = new RequestInfo((HttpUriRequest)invocation.getArguments()[0]);
		for (Response r: responses) {
			if (r.matcher.test(info)) {
				if (r.action != null) {
					r.action.accept(info);
				}
				BasicCloseableHttpResponse ret = new BasicCloseableHttpResponse(new BasicStatusLine(new ProtocolVersion("HTTP", 1, 1), r.code, r.message));
				if (r.body != null) {
					ret.setEntity(new ByteArrayEntity(r.body, r.contentType));
					ret.addHeader("Content-Length", String.valueOf(r.body.length));
					if (r.body.length != 0) {
						ret.addHeader("Content-Type", r.contentType.toString());
					}
				}
				if (log.isInfoEnabled()) {
					log.info("Mock client response to {} is {}", info.getLine(), (r.body == null? "null": new String(r.body)));
				}
				return ret;
			}
		}
		throw new IOException("Mock unhandled " + info.getLine());
	}

	/**
	 * Handle the specified requests.
	 *
	 * @param matcher A predicate for matching requests.
	 * @param body The response body to return.
	 * @param type The content type of the body.
	 * @param action An action to perform, whenever the request is handled.
	 * @return This responder.
	 */
	public ClientMocking on(Predicate<RequestInfo> matcher, byte[] body, ContentType type, Consumer<RequestInfo> action) {
		Response r = new Response();
		r.matcher = matcher;
		r.body = body;
		r.contentType = type;
		r.action = action;
		responses.add(r);
		return this;
	}

	/**
	 * Handle the specified request.
	 *
	 * @param line The HTTP method and URI to handle.
	 * @param body The response body to return.
	 * @param type The content type of the body.
	 * @param action An action to perform, whenever the request is handled.
	 * @return This responder.
	 */
	public ClientMocking on(String line, String body, ContentType type, Consumer<RequestInfo> action) {
		return on(s2p(line), body.getBytes(), type, action);
	}

	/**
	 * Handle the specified request.
	 *
	 * @param line The HTTP method and URI to handle.
	 * @param body The response body to return, as a JSON string.
	 * @param action An action to perform, whenever the request is handled.
	 * @return This responder.
	 */
	public ClientMocking on(String line, String body, Consumer<RequestInfo> action) {
		return on(line, body, ContentType.APPLICATION_JSON, action);
	}

	/**
	 * Handle the specified request.
	 *
	 * @param line The HTTP method and URI to handle.
	 * @param body The response body to return, as a JSON string.
	 * @return This responder.
	 */
	public ClientMocking on(String line, String body) {
		return on(line, body, ContentType.APPLICATION_JSON);
	}

	/**
	 * Handle the specified request.
	 *
	 * @param line The HTTP method and URI to handle.
	 * @param body The response body to return, as a JSON string.
	 * @param type The content type of the body.
	 * @return This responder.
	 */
	public ClientMocking on(String line, String body, ContentType type) {
		return on(line, body, type, null);
	}

	/**
	 * Fail the specified requests.
	 *
	 * @param matcher A predicate for matching requests.
	 * @param code The HTTP error code to return.
	 * @param message The HTTP error status message to return.
	 * @return This responder.
	 */
	public ClientMocking errorOn(Predicate<RequestInfo> matcher, int code, String message) {
		Response r = new Response();
		r.matcher = matcher;
		r.code = code;
		r.message = message;
		responses.add(r);
		return this;
	}

	/**
	 * Fail the specified request.
	 *
	 * @param line The HTTP method and URI to handle.
	 * @param code The HTTP error code to return.
	 * @param message The HTTP error status message to return.
	 * @return This responder.
	 */
	public ClientMocking errorOn(String line, int code, String message) {
		return errorOn(s2p(line), code, message);
	}

	/**
	 * Fail the request if no auth header
	 *
	 * @param code The HTTP error code to return.
	 * @param message The HTTP error status message to return.
	 * @return This responder.
	 */
	public ClientMocking errorOnNoAuth(int code, String message) {
		return errorOn(ri -> ri.lacksHeader("Authorization"), code, message);
	}

	/**
	 * Fail the request if wrong auth header
	 *
	 * @param username The expected user name.
	 * @param password The expected password.
	 * @param code The HTTP error code to return.
	 * @param message The HTTP error status message to return.
	 * @return This responder.
	 */
	public ClientMocking errorOnBadAuth(String username, String password, int code, String message) {
		String authhdr = "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes());
		return errorOn(ri -> ri.lacksHeaderValue("Authorization", authhdr), code, message);
	}
}
