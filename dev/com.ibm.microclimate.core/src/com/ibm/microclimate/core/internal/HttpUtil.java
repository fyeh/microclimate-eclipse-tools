/*******************************************************************************
 * Copyright (c) 2018, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.microclimate.core.internal;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;

/**
 * Static utilities to allow easy HTTP communication, and make diagnosing and handling errors a bit easier.
 */
public class HttpUtil {

	private HttpUtil() {}

	public static class HttpResult {
		public final int responseCode;
		public final boolean isGoodResponse;

		// Can be null
		public final String response;
		// Can be null
		public final String error;
		
		private final Map<String, List<String>> headerFields;

		public HttpResult(HttpURLConnection connection) throws IOException {
			responseCode = connection.getResponseCode();
			isGoodResponse = responseCode > 199 && responseCode < 300;
			
			headerFields = isGoodResponse ? connection.getHeaderFields() : null;

			// Read error first because sometimes if there is an error, connection.getInputStream() throws an exception
			InputStream eis = connection.getErrorStream();
			if (eis != null) {
				error = MCUtil.readAllFromStream(eis);
			}
			else {
				error = null;
			}

			if (!isGoodResponse) {
				MCLogger.logError("Received bad response code " + responseCode + " from "
						+ connection.getURL() + " - Error:\n" + error);
			}

			InputStream is = connection.getInputStream();
			if (is != null) {
				response = MCUtil.readAllFromStream(is);
			}
			else {
				response = null;
			}
		}
		
		public String getHeader(String key) {
			if (headerFields == null) {
				return null;
			}
			List<String> list = headerFields.get(key);
			if (list == null || list.isEmpty()) {
				return null;
			}
			return list.get(0);
		}
	}
	
	public static HttpResult get(URI uri) throws IOException {
		return get(uri, null, 5000);
	}

	public static HttpResult get(URI uri, Map<String, String> requestProperties, int timeout) throws IOException {
		return get(uri, requestProperties, null, timeout);
	}
	
	public static HttpResult get(URI baseUri, Map<String, String> requestProperties, Map<String, Object> params, int timeout) throws IOException {
		HttpURLConnection connection = null;

		try {
			URI uri = addParams(baseUri, params);
			connection = (HttpURLConnection) uri.toURL().openConnection();
			connection.setRequestMethod("GET");
			connection.setReadTimeout(timeout);
			addRequestProperties(connection, requestProperties);
			
			return new HttpResult(connection);
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
	}
	
	public static HttpResult post(URI uri, JSONObject payload) throws IOException {
		return post(uri, null, payload);
	}

	public static HttpResult post(URI uri, Map<String, String> requestProperties, JSONObject payload) throws IOException {
		HttpURLConnection connection = null;

		MCLogger.log("POST " + payload.toString() + " TO " + uri);
		try {
			connection = (HttpURLConnection) uri.toURL().openConnection();

			connection.setRequestMethod("POST");
			connection.setRequestProperty("Content-Type", "application/json");
			connection.setDoOutput(true);
			addRequestProperties(connection, requestProperties);
			
			DataOutputStream payloadStream = new DataOutputStream(connection.getOutputStream());
			payloadStream.write(payload.toString().getBytes());

			return new HttpResult(connection);
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
	}
	
	public static HttpResult post(URI uri, Map<String, String> requestProperties, Map<String, Object> params) throws IOException {
        String paramStr = getParamString(params);
		byte[] postData = paramStr.getBytes(Charset.forName("UTF-8"));
        HttpURLConnection connection = null;
        try {
        	connection = (HttpURLConnection) uri.toURL().openConnection();
        	connection.setRequestMethod("POST");
        	connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        	connection.setRequestProperty("Content-Length", String.valueOf(postData.length));
        	addRequestProperties(connection, requestProperties);
        	connection.setDoOutput(true);
        	DataOutputStream payloadStream = new DataOutputStream(connection.getOutputStream());
			payloadStream.write(postData);

			return new HttpResult(connection);
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
	}
	
	public static HttpResult put(URI uri) throws IOException {
		HttpURLConnection connection = null;

		MCLogger.log("PUT " + uri);
		try {
			connection = (HttpURLConnection) uri.toURL().openConnection();

			connection.setRequestMethod("PUT");

			return new HttpResult(connection);
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
	}
	
	public static HttpResult head(URI uri) throws IOException {
		HttpURLConnection connection = null;

		MCLogger.log("HEAD " + uri);
		try {
			connection = (HttpURLConnection) uri.toURL().openConnection();

			connection.setRequestMethod("HEAD");

			return new HttpResult(connection);
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
	}
	
	public static HttpResult delete(URI uri) throws IOException {
		HttpURLConnection connection = null;

		MCLogger.log("DELETE " + uri);
		try {
			connection = (HttpURLConnection) uri.toURL().openConnection();

			connection.setRequestMethod("DELETE");

			return new HttpResult(connection);
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
	}
	
	private static void addRequestProperties(HttpURLConnection connection, Map<String, String> requestProperties) {
		if (requestProperties != null) {
			for (Map.Entry<String, String> prop : requestProperties.entrySet()) {
				  connection.setRequestProperty(prop.getKey(), prop.getValue());
			}
		}
	}

    private static URI addParams(URI uri, Map<String, Object> params) throws IOException {
    	try {
	    	if (params == null || params.isEmpty()) {
	    		return uri;
	    	}
	        StringBuilder data = new StringBuilder();
	        for (Map.Entry<String, Object> param : params.entrySet()) {
	            if (data.length() != 0)
	                data.append('&');
	            data.append(URLEncoder.encode(param.getKey(), "UTF-8"));
	            data.append('=');
	            data.append(URLEncoder.encode(String.valueOf(param.getValue()), "UTF-8"));
	        }
	        return new URI(uri.getScheme(), uri.getAuthority(), uri.getPath(), data.toString(), uri.getFragment());
    	} catch (Exception e) {
    		String msg = "Adding parameters to the URI failed";
    		MCLogger.logError(msg, e);
    		throw new IOException(msg, e);
    	}
    }
    
    private static String getParamString(Map<String, Object> params) throws UnsupportedEncodingException {
        StringBuilder postData = new StringBuilder();
        for (Map.Entry<String, Object> param : params.entrySet()) {
            if (postData.length() != 0)
                postData.append('&');
            postData.append(URLEncoder.encode(param.getKey(), "UTF-8"));
            postData.append('=');
            postData.append(URLEncoder.encode(String.valueOf(param.getValue()), "UTF-8"));
        }
        return postData.toString();
    }


}
