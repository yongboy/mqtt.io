package io.mqtt.handler.http;

import io.mqtt.tool.MemPool;
import io.netty.handler.codec.http.Cookie;
import io.netty.handler.codec.http.CookieDecoder;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.DefaultHttpDataFactory;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import io.netty.handler.codec.http.multipart.InterfaceHttpData.HttpDataType;
import io.netty.util.AttributeKey;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class HttpSessionStore {
	// Global Identiter/全局唯一
	public static final AttributeKey<HttpRequest> key = AttributeKey
			.valueOf("req");

	public static String getParameter(HttpRequest req, String name) {
		QueryStringDecoder decoderQuery = new QueryStringDecoder(req.getUri());
		Map<String, List<String>> uriAttributes = decoderQuery.parameters();

		return uriAttributes.containsKey(name) ? uriAttributes.get(name).get(0)
				: null;
	}
	
	public static String getPostParameter(HttpRequest req, String name) {
		HttpPostRequestDecoder decoder = new HttpPostRequestDecoder(
				new DefaultHttpDataFactory(false), req);

		InterfaceHttpData data = decoder.getBodyHttpData(name);
		if (data.getHttpDataType() == HttpDataType.Attribute) {
			Attribute attribute = (Attribute) data;
			String value = null;
			try {
				value = attribute.getValue();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return value;
		}

		return null;
	}

	public static String genJSessionId() {
		return UUID.randomUUID().toString();
	}

	public static boolean checkJSessionId(HttpRequest req) {
		String jsessionId = getClientSessionId(req);

		if (jsessionId == null) {
			return false;
		}

		return MemPool.checkClientID(jsessionId);
	}

	public static boolean checkJSessionId(String sessionId) {
		if (sessionId == null) {
			return false;
		}

		return MemPool.checkClientID(sessionId);
	}

	public static String getClientSessionId(HttpRequest req) {
		Set<Cookie> cookies;
		String value = req.headers().get(HttpHeaders.Names.COOKIE);
		if (value == null) {
			return null;
		} else {
			cookies = CookieDecoder.decode(value);
		}

		for (Cookie cookie : cookies) {
			if (cookie.getName().contains("JSESSIONID")) {
				return cookie.getValue();
			}
		}

		return null;
	}
}
