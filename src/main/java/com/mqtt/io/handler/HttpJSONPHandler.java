package com.mqtt.io.handler;

import static io.netty.handler.codec.http.HttpHeaders.isKeepAlive;
import static io.netty.handler.codec.http.HttpHeaders.setContentLength;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpMethod.GET;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.Cookie;
import io.netty.handler.codec.http.CookieDecoder;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.ServerCookieEncoder;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.util.AttributeKey;
import io.netty.util.CharsetUtil;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;

public class HttpJSONPHandler {
	private static final Logger log = Logger.getLogger(HttpJSONPHandler.class
			.getName());
	private CustomWebSocketServerHandler webSocketServerHandler;
	private final AttributeKey<FullHttpRequest> key = AttributeKey
			.valueOf("req");

	public HttpJSONPHandler(CustomWebSocketServerHandler webSocketServerHandler) {
		this.webSocketServerHandler = webSocketServerHandler;
	}

	@SuppressWarnings("static-access")
	protected void handleHttpRequest(ChannelHandlerContext ctx,
			FullHttpRequest req) throws Exception {
		// Handle a bad request.
		if (!req.getDecoderResult().isSuccess()) {
			sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HTTP_1_1,
					BAD_REQUEST));
			return;
		}

		if (req.getUri().contains(this.webSocketServerHandler.WEBSOCKET_PATH)) {
			this.webSocketServerHandler.handshakeRequest(ctx, req);
			return;
		}

		// Allow only GET methods.
		if (req.getMethod() != GET) {
			sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HTTP_1_1,
					FORBIDDEN));
			return;
		}

		if ("/favicon.ico".equals(req.getUri())) {
			FullHttpResponse res = new DefaultFullHttpResponse(HTTP_1_1,
					NOT_FOUND);
			sendHttpResponse(ctx, req, res);
			return;
		}

		if (req.getUri().contains("/jsonp/connect")) {
			handleConnect(ctx, req);
			return;
		} else if (req.getUri().contains("/jsonp/subscribe")) {
			handleSubscrible(ctx, req);
			return;
		} else if (req.getUri().contains("/jsonp/polling")) { // 模拟ping以及获得请求
			handleWaitingMsg(ctx, req);
			return;
		} else if (req.getUri().contains("/jsonp/unsubscrible")) {
			handleUnsubscrible(ctx, req);
			return;
		} else if (req.getUri().contains("/jsonp/publish")) {
			handlePublish(ctx, req);
			return;
		} else { // invalid request
			sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HTTP_1_1,
					BAD_REQUEST));
		}

		// // Send the demo page and favicon.ico
		// ByteBuf content = Unpooled.wrappedBuffer("Hello World".getBytes());
		// FullHttpResponse res = new DefaultFullHttpResponse(HTTP_1_1, OK,
		// content);
		//
		// res.headers().set(CONTENT_TYPE, "text/html; charset=UTF-8");
		// setContentLength(res, content.readableBytes());
		//
		// sendHttpResponse(ctx, req, res);
	}

	private void handlePublish(ChannelHandlerContext ctx, FullHttpRequest req) {
		// TODO Auto-generated method stub

	}

	private void handleUnsubscrible(ChannelHandlerContext ctx,
			FullHttpRequest req) {
		// TODO Auto-generated method stub

	}

	public void handleTimeout(ChannelHandlerContext ctx) {
//		ByteBuf content = ctx
//				.channel()
//				.alloc()
//				.ioBuffer()
//				.writeBytes(
//						"\r\n\r\n<script>alert('hello!');</script>\r\n\r\n"
//								.getBytes());
		System.out.println("gose here ...");
		ctx.channel().close();
//		ByteBuf content = null;
//		try {
//			content = Unpooled.wrappedBuffer("\r\n\r\n<script>alert('hello!');</script>\r\n\r\n".getBytes("UTF-8"));
//		} catch (UnsupportedEncodingException e) {
//			e.printStackTrace();
//		}
//		if (ctx.channel().isOpen()) {
//			ctx.channel().writeAndFlush(content)
//					.addListener(ChannelFutureListener.CLOSE);
//		}
	}

	private void handleWaitingMsg(ChannelHandlerContext ctx, FullHttpRequest req) {
//		if (!checkJSessionId(req)) {
//			sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HTTP_1_1,
//					BAD_REQUEST));
//			return;
//		}

		ctx.attr(key).set(req);
		ctx.pipeline().addFirst(new ReadTimeoutHandler(5, TimeUnit.SECONDS));

		FullHttpResponse res = new DefaultFullHttpResponse(HTTP_1_1, OK);
		res.headers().add(CONTENT_TYPE, "text/html; charset=UTF-8");
		res.headers().add(HttpHeaders.Names.CONNECTION,
				HttpHeaders.Values.KEEP_ALIVE);
//		res.headers().add("X-XSS-Protection", "0");

//		if (req != null && req.headers().get("Origin") != null) {
//			res.headers().add("Access-Control-Allow-Origin",
//					req.headers().get("Origin"));
//			res.headers().add("Access-Control-Allow-Credentials", "true");
//		}

		ctx.channel().write(res);
		
		
		ByteBuf content = null;
		try {
			content = Unpooled.wrappedBuffer("\r\n\r\n<script>alert('hello!');</script>\r\n\r\n".getBytes("UTF-8"));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		ctx.channel().writeAndFlush(content);
		ctx.channel().close();

		// TODO 处理异常断开
	}

	private void handleSubscrible(ChannelHandlerContext ctx, FullHttpRequest req) {
		if (!checkJSessionId(req)) {
			sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HTTP_1_1,
					BAD_REQUEST));
			return;
		}

		String topic = getParameter(req, "topic");
		String qosStr = getParameter(req, "qos");
		if (StringUtils.isEmpty(topic)) {
			sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HTTP_1_1,
					BAD_REQUEST));
			return;
		}
		int qos = 0;
		if (StringUtils.isNumeric(qosStr)) {
			try {
				qos = Integer.parseInt(qosStr);
			} catch (Exception e) {
			}
		}
		if (qos > 2 || qos < 0) {
			sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HTTP_1_1,
					BAD_REQUEST));
			return;
		}

		log.info("topic = " + topic + " qos = " + qos);

		ByteBuf content = Unpooled.wrappedBuffer("{status:true}".getBytes());
		FullHttpResponse res = new DefaultFullHttpResponse(HTTP_1_1, OK,
				content);
		res.headers().set(CONTENT_TYPE, "text/html; charset=UTF-8");
		setContentLength(res, content.readableBytes());

		sendHttpResponse(ctx, req, res);
	}

	private void handleConnect(ChannelHandlerContext ctx, FullHttpRequest req) {
		ByteBuf content = Unpooled.wrappedBuffer("{status:true}".getBytes());
		FullHttpResponse res = new DefaultFullHttpResponse(HTTP_1_1, OK,
				content);
		String jsessionId = genJSessionId();
		res.headers().add("Set-Cookie",
				ServerCookieEncoder.encode("JSESSIONID", jsessionId));

		sessionMap.put(jsessionId, "");

		res.headers().set(CONTENT_TYPE, "text/html; charset=UTF-8");
		setContentLength(res, content.readableBytes());

		sendHttpResponse(ctx, req, res);
	}

	private String getParameter(FullHttpRequest req, String name) {
		QueryStringDecoder decoderQuery = new QueryStringDecoder(req.getUri());
		Map<String, List<String>> uriAttributes = decoderQuery.parameters();

		return uriAttributes.containsKey(name) ? uriAttributes.get(name).get(0)
				: null;
	}

	private final static ConcurrentHashMap<String, Object> sessionMap = new ConcurrentHashMap<String, Object>(
			100000, 0.9f, 256);

	private static String genJSessionId() {
		return UUID.randomUUID().toString();
	}

	private boolean checkJSessionId(FullHttpRequest req) {
		String jsessionId = getClientJSessionId(req);

		if (StringUtils.isBlank(jsessionId)) {
			return false;
		}

		return sessionMap.containsKey(jsessionId);
	}

	private String getClientJSessionId(FullHttpRequest req) {
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

	private static void sendHttpResponse(ChannelHandlerContext ctx,
			FullHttpRequest req, FullHttpResponse res) {
		// Generate an error page if response getStatus code is not OK (200).
		if (res.getStatus().code() != 200) {
			ByteBuf buf = Unpooled.copiedBuffer(res.getStatus().toString(),
					CharsetUtil.UTF_8);
			res.content().writeBytes(buf);
			buf.release();
			setContentLength(res, res.content().readableBytes());
		}

		// Send the response and close the connection if necessary.
		ChannelFuture f = ctx.channel().writeAndFlush(res);
		if (!isKeepAlive(req) || res.getStatus().code() != 200) {
			f.addListener(ChannelFutureListener.CLOSE);
		}
	}
}