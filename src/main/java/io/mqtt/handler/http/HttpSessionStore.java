package io.mqtt.handler.http;

import io.mqtt.handler.entity.HttpJsonpChannelEntity;
import io.netty.handler.codec.http.Cookie;
import io.netty.handler.codec.http.CookieDecoder;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.util.AttributeKey;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class HttpSessionStore {
	public final static ConcurrentHashMap<String, HttpJsonpChannelEntity> sessionMap = new ConcurrentHashMap<String, HttpJsonpChannelEntity>(
			100000, 0.9f, 256);
	
	// Global Identiter/全局唯一
	public static final AttributeKey<HttpRequest> key = AttributeKey
			.valueOf("req");

	public static String getParameter(HttpRequest req, String name) {
		QueryStringDecoder decoderQuery = new QueryStringDecoder(req.getUri());
		Map<String, List<String>> uriAttributes = decoderQuery.parameters();

		return uriAttributes.containsKey(name) ? uriAttributes.get(name).get(0)
				: null;
	}

	public static String genJSessionId() {
		return UUID.randomUUID().toString();
	}

	public static boolean checkJSessionId(HttpRequest req) {
		String jsessionId = getClientJSessionId(req);

		if (jsessionId == null) {
			return false;
		}

		return sessionMap.containsKey(jsessionId);
	}

	public static boolean checkJSessionId(String sessionId) {
		if (sessionId == null) {
			return false;
		}

		return sessionMap.containsKey(sessionId);
	}

	public static String getClientJSessionId(HttpRequest req) {
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
