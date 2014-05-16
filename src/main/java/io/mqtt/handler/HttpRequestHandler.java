package io.mqtt.handler;

import static io.netty.handler.codec.http.HttpHeaders.isKeepAlive;
import static io.netty.handler.codec.http.HttpHeaders.setContentLength;
import static io.netty.handler.codec.http.HttpMethod.GET;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import io.mqtt.handler.entity.HttpChannelEntity;
import io.mqtt.handler.http.HttpDefaultTransport;
import io.mqtt.handler.http.HttpJsonpTransport;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.Cookie;
import io.netty.handler.codec.http.CookieDecoder;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.timeout.ReadTimeoutException;
import io.netty.util.AttributeKey;
import io.netty.util.CharsetUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.meqantt.message.PublishMessage;

public class HttpRequestHandler extends
		SimpleChannelInboundHandler<FullHttpRequest> {
	private static final InternalLogger logger = InternalLoggerFactory
			.getInstance(HttpRequestHandler.class);

	private final static String HEADER_CONTENT_TYPE = "text/javascript; charset=UTF-8";
	private final static String TEMPLATE = "%s(%s);";

	private final static ConcurrentHashMap<String, HttpChannelEntity> sessionMap = new ConcurrentHashMap<String, HttpChannelEntity>(
			100000, 0.9f, 256);

	// Global Identiter/全局唯一
	private static final AttributeKey<HttpRequest> key = AttributeKey
			.valueOf("req");

	private String websocketUri, selfUri;
	
	private static Map<String, HttpDefaultTransport> transportMap = new HashMap<String, HttpDefaultTransport>(1);
	static{
		transportMap.put(HttpJsonpTransport.PREFIX, new HttpJsonpTransport());
	}

	public HttpRequestHandler(String websocketUri, String selfUri) {
		this.websocketUri = websocketUri;
		this.selfUri = selfUri;
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest req)
			throws Exception {
		if (!req.getDecoderResult().isSuccess()) {
			sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HTTP_1_1,
					BAD_REQUEST));
			return;
		}

		if (req.getUri().equalsIgnoreCase(this.websocketUri)) {
			ctx.fireChannelRead(req.retain());
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

				for(String prefix : transportMap.keySet()){
				if (req.getUri().startsWith(prefix)) {
					HttpDefaultTransport transport = transportMap.get(prefix);
					transport.handleRequest(ctx, req);
				} 
				}
				// invalid request
					sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HTTP_1_1,
							BAD_REQUEST));
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
			throws Exception {
		if (cause instanceof ReadTimeoutException) {
			handleTimeout(ctx);
		} else {
			cause.printStackTrace();
			ctx.close();
		}
	}

	protected void handleHttpRequest(ChannelHandlerContext ctx,
			FullHttpRequest req) throws Exception {
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

		if (req.getUri().startsWith(selfUri)) {
			handleConnect(ctx, req);
		} else { // invalid request
			sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HTTP_1_1,
					BAD_REQUEST));
		}
	}

	public static void doWriteBody(ChannelHandlerContext ctx,
			PublishMessage publishMessage) {
		ByteBuf content = ctx.alloc().directBuffer();
		HttpRequest req = ctx.attr(key).get();
		content.writeBytes(getTargetFormatMessage(req,
				publishMessage.getDataAsString()).getBytes(CharsetUtil.UTF_8));
		ctx.writeAndFlush(content).addListener(ChannelFutureListener.CLOSE);
	}
	
	private static String getParameter(HttpRequest req, String name) {
		QueryStringDecoder decoderQuery = new QueryStringDecoder(req.getUri());
		Map<String, List<String>> uriAttributes = decoderQuery.parameters();

		return uriAttributes.containsKey(name) ? uriAttributes.get(name).get(0)
				: null;
	}

	private static String genJSessionId() {
		return UUID.randomUUID().toString();
	}

	private boolean checkJSessionId(HttpRequest req) {
		String jsessionId = getClientJSessionId(req);

		if (jsessionId == null) {
			return false;
		}

		return sessionMap.containsKey(jsessionId);
	}

	private boolean checkJSessionId(String sessionId) {
		if (sessionId == null) {
			return false;
		}

		return sessionMap.containsKey(sessionId);
	}

	private String getClientJSessionId(HttpRequest req) {
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
			HttpRequest req, FullHttpResponse res) {
		// Generate an error page if response getStatus code is not OK (200).
		if (res.getStatus().code() != 200) {
			ByteBuf buf = Unpooled.copiedBuffer(res.getStatus().toString(),
					CharsetUtil.UTF_8);
			res.content().writeBytes(buf);
			buf.release();
			setContentLength(res, res.content().readableBytes());
		}

		// Send the response and close the connection if necessary.
		ChannelFuture f = ctx.writeAndFlush(res);
		if (!isKeepAlive(req) || res.getStatus().code() != 200) {
			f.addListener(ChannelFutureListener.CLOSE);
		}
	}

	private static String getTargetFormatMessage(HttpRequest req,
			String jsonMessage) {
		String callbackparam = getParameter(req, "jsoncallback");
		if (callbackparam == null) {
			callbackparam = "jsoncallback";
		}

		logger.debug("format json message : "
				+ String.format(TEMPLATE, callbackparam, jsonMessage));

		return String.format(TEMPLATE, callbackparam, jsonMessage);
	}
	
	private static Runnable runnable = new Runnable(String sessionId) {
		private String sessionId;
		{
			this.sessionId = sessionId;
		}
		
		@Override
		public void run() {
			
		}
	};
}