package io.mqtt.handler;

import static io.netty.handler.codec.http.HttpHeaders.isKeepAlive;
import static io.netty.handler.codec.http.HttpHeaders.setContentLength;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpMethod.GET;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpResponseStatus.UNAUTHORIZED;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import io.mqtt.handler.entity.BlankHttpChannelEntity;
import io.mqtt.handler.entity.HttpChannelEntity;
import io.mqtt.tool.MemPool;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.Cookie;
import io.netty.handler.codec.http.CookieDecoder;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.ServerCookieEncoder;
import io.netty.handler.timeout.ReadTimeoutException;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.util.AttributeKey;
import io.netty.util.CharsetUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.meqantt.message.Message;
import org.meqantt.message.PublishMessage;

public class HttpJsonpRequestHandler extends
		SimpleChannelInboundHandler<FullHttpRequest> {
	private static final InternalLogger logger = InternalLoggerFactory
			.getInstance(HttpJsonpRequestHandler.class);

	private final static String HEADER_CONTENT_TYPE = "text/javascript; charset=UTF-8";
	private final static String TEMPLATE = "%s(%s);";

	private final static ConcurrentHashMap<String, HttpChannelEntity> sessionMap = new ConcurrentHashMap<String, HttpChannelEntity>(
			100000, 0.9f, 256);

	// Global Identiter/全局唯一
	private static final AttributeKey<HttpRequest> key = AttributeKey
			.valueOf("req");

	private String websocketUri, selfUri;

	public HttpJsonpRequestHandler(String websocketUri, String selfUri) {
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

		if (!req.getUri().startsWith(this.selfUri)) {
			sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HTTP_1_1,
					BAD_REQUEST));
			return;
		}

		handleHttpRequest(ctx, req);
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

		if (req.getUri().contains("/jsonp/connect")) {
			handleConnect(ctx, req);
		} else if (req.getUri().contains("/jsonp/subscribe")) {
			handleSubscrible(ctx, req);
		} else if (req.getUri().contains("/jsonp/polling")) {
			handleWaitingMsg(ctx, req);
		} else if (req.getUri().contains("/jsonp/unsubscrible")) {
			handleUnsubscrible(ctx, req);
		} else if (req.getUri().contains("/jsonp/publish")) {
			// HttpResponseStatus
			sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HTTP_1_1,
					HttpResponseStatus.FORBIDDEN));
		} else { // invalid request
			sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HTTP_1_1,
					BAD_REQUEST));
		}
	}

	private void handleUnsubscrible(ChannelHandlerContext ctx, HttpRequest req) {
		// TODO code here...
	}

	public void handleTimeout(ChannelHandlerContext ctx) {
		HttpRequest req = ctx.attr(key).get();
		String sessionId = getClientJSessionId(req);
		HttpChannelEntity httpChannelEntity = sessionMap.get(sessionId);
		httpChannelEntity.setCtx(null);
		// empty json
		ByteBuf content = Unpooled.copiedBuffer("{}", CharsetUtil.UTF_8);
		ctx.writeAndFlush(content).addListener(ChannelFutureListener.CLOSE);
	}

	private void handleWaitingMsg(ChannelHandlerContext ctx, HttpRequest req) {
		if (!checkJSessionId(req)) {
			sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HTTP_1_1,
					UNAUTHORIZED));
			return;
		}
		ctx.attr(key).set(req);

		HttpResponse res = new DefaultHttpResponse(HTTP_1_1, OK);
		res.headers().set(CONTENT_TYPE, HEADER_CONTENT_TYPE);
		res.headers().set(HttpHeaders.Names.CONNECTION,
				HttpHeaders.Values.KEEP_ALIVE);
		res.headers().set("X-XSS-Protection", "0");

		if (req != null && req.headers().get("Origin") != null) {
			res.headers().set("Access-Control-Allow-Origin",
					req.headers().get("Origin"));
			res.headers().set("Access-Control-Allow-Credentials", "true");
		}

		ctx.write(res);

		String sessionId = getClientJSessionId(req);
		HttpChannelEntity httpChannelEntity = sessionMap.get(sessionId);
		Queue<Message> queue = httpChannelEntity.getQueue();

		Message message = queue.poll();

		if (message != null && message instanceof PublishMessage) {
			logger.debug("message is not null!");
			PublishMessage publishMessage = (PublishMessage) message;
			doWriteBody(ctx, publishMessage);

			return;
		}

		httpChannelEntity.setCtx(ctx);

		String timeoutStr = getParameter(req, "timeout");
		int timeout = 0;
		if (StringUtils.isNumeric(timeoutStr)) {
			try {
				timeout = Integer.parseInt(timeoutStr);
			} catch (Exception e) {
			}
		}
		if (timeout <= 0) {
			timeout = 299;
		}

		logger.debug("going to set ReadTimeoutHandler now ...");
		ctx.pipeline().addFirst(
				new ReadTimeoutHandler(timeout, TimeUnit.SECONDS));
	}

	public static void doWriteBody(ChannelHandlerContext ctx,
			PublishMessage publishMessage) {
		ByteBuf content = ctx.alloc().directBuffer();
		HttpRequest req = ctx.attr(key).get();
		content.writeBytes(getTargetFormatMessage(req,
				publishMessage.getDataAsString()).getBytes(CharsetUtil.UTF_8));
		ctx.writeAndFlush(content).addListener(ChannelFutureListener.CLOSE);
	}

	private void handleSubscrible(ChannelHandlerContext ctx, HttpRequest req) {
		if (!checkJSessionId(req)) {
			sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HTTP_1_1,
					UNAUTHORIZED));
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

		String sessionId = getClientJSessionId(req);

		HttpChannelEntity httpChannelEntity = sessionMap.get(sessionId);

		if (httpChannelEntity instanceof BlankHttpChannelEntity) {
			httpChannelEntity = new HttpChannelEntity(sessionId);
			sessionMap.put(sessionId, httpChannelEntity);
		}

		MemPool.putTopic(httpChannelEntity, topic);
		logger.debug("topic = " + topic + " qos = " + qos);

		ByteBuf content = ctx.alloc().directBuffer()
				.writeBytes("{status:true}".getBytes());
		FullHttpResponse res = new DefaultFullHttpResponse(HTTP_1_1, OK,
				content);
		res.headers().set(CONTENT_TYPE, HEADER_CONTENT_TYPE);
		setContentLength(res, content.readableBytes());

		sendHttpResponse(ctx, req, res);
	}

	private void handleConnect(ChannelHandlerContext ctx, HttpRequest req) {
		ByteBuf content = ctx.alloc().directBuffer()
				.writeBytes("{status:true}".getBytes());
		FullHttpResponse res = new DefaultFullHttpResponse(HTTP_1_1, OK,
				content);

		String sessionId = getClientJSessionId(req);
		if (!checkJSessionId(sessionId)) {
			sessionId = genJSessionId();
			res.headers().add("Set-Cookie",
					ServerCookieEncoder.encode("JSESSIONID", sessionId));

			sessionMap.put(sessionId, BlankHttpChannelEntity.BLANK);
		}
		res.headers().set(CONTENT_TYPE, HEADER_CONTENT_TYPE);
		setContentLength(res, content.readableBytes());

		sendHttpResponse(ctx, req, res);
	}

	private String getParameter(HttpRequest req, String name) {
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
}