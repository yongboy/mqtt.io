package io.mqtt.handler.http;

import static io.netty.handler.codec.http.HttpHeaders.isKeepAlive;
import static io.netty.handler.codec.http.HttpHeaders.setContentLength;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpResponseStatus.UNAUTHORIZED;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import io.mqtt.handler.entity.HttpChannelEntity;
import io.mqtt.handler.entity.HttpJsonpChannelEntity;
import io.mqtt.tool.MemPool;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.ServerCookieEncoder;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.util.CharsetUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.Queue;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.meqantt.message.Message;
import org.meqantt.message.PublishMessage;

public class HttpJsonpTransport extends HttpTransport {
	private static final InternalLogger logger = InternalLoggerFactory
			.getInstance(HttpJsonpTransport.class);

	public static final String PREFIX = "/jsonp/";

	private final static String HEADER_CONTENT_TYPE = "text/javascript; charset=UTF-8";
	private final static String TEMPLATE = "%s(%s);";

	@Override
	public void handleRequest(ChannelHandlerContext ctx, FullHttpRequest req)
			throws Exception {
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

	@Override
	public void handleTimeout(ChannelHandlerContext ctx) {
		HttpRequest req = ctx.attr(HttpSessionStore.key).get();
		String sessionId = HttpSessionStore.getClientJSessionId(req);
		HttpJsonpChannelEntity httpChannelEntity = HttpSessionStore.sessionMap
				.get(sessionId);
		httpChannelEntity.setCtx(null);
		// empty json
		ByteBuf content = Unpooled.copiedBuffer("{}", CharsetUtil.UTF_8);
		ctx.writeAndFlush(content).addListener(ChannelFutureListener.CLOSE);
	}

	private void handleWaitingMsg(ChannelHandlerContext ctx, HttpRequest req) {
		if (!HttpSessionStore.checkJSessionId(req)) {
			sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HTTP_1_1,
					UNAUTHORIZED));
			return;
		}
		ctx.attr(HttpSessionStore.key).set(req);

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

		String sessionId = HttpSessionStore.getClientJSessionId(req);
		HttpJsonpChannelEntity httpChannelEntity = HttpSessionStore.sessionMap
				.get(sessionId);
		Queue<Message> queue = httpChannelEntity.getQueue();

		Message message = queue.poll();

		if (message != null && message instanceof PublishMessage) {
			logger.debug("message is not null!");
			PublishMessage publishMessage = (PublishMessage) message;
			doWriteBody(ctx, publishMessage);

			return;
		}

		httpChannelEntity.setCtx(ctx);

		String timeoutStr = HttpSessionStore.getParameter(req, "timeout");
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
		HttpRequest req = ctx.attr(HttpSessionStore.key).get();
		content.writeBytes(getTargetFormatMessage(req,
				publishMessage.getDataAsString()).getBytes(CharsetUtil.UTF_8));
		ctx.writeAndFlush(content).addListener(ChannelFutureListener.CLOSE);
	}

	private void handleSubscrible(ChannelHandlerContext ctx, HttpRequest req) {
		if (!HttpSessionStore.checkJSessionId(req)) {
			sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HTTP_1_1,
					UNAUTHORIZED));
			return;
		}

		String topic = HttpSessionStore.getParameter(req, "topic");
		String qosStr = HttpSessionStore.getParameter(req, "qos");
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

		String sessionId = HttpSessionStore.getClientJSessionId(req);

		HttpJsonpChannelEntity httpChannelEntity = HttpSessionStore.sessionMap
				.get(sessionId);

		if (httpChannelEntity.getSessionId() == null) {
			httpChannelEntity = new HttpJsonpChannelEntity(sessionId);
			HttpSessionStore.sessionMap.put(sessionId, httpChannelEntity);
		}

		MemPool.registerTopic(httpChannelEntity, topic);
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

		String sessionId = HttpSessionStore.getClientJSessionId(req);
		if (!HttpSessionStore.checkJSessionId(sessionId)) {
			sessionId = HttpSessionStore.genJSessionId();
			res.headers().add("Set-Cookie",
					ServerCookieEncoder.encode("JSESSIONID", sessionId));

			HttpSessionStore.sessionMap.put(sessionId, HttpChannelEntity.BLANK);
		}
		res.headers().set(CONTENT_TYPE, HEADER_CONTENT_TYPE);
		setContentLength(res, content.readableBytes());

		sendHttpResponse(ctx, req, res);
		// ctx.executor().schedule(command, delay, unit)
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
		String callbackparam = HttpSessionStore.getParameter(req,
				"jsoncallback");
		if (callbackparam == null) {
			callbackparam = "jsoncallback";
		}

		logger.debug("format json message : "
				+ String.format(TEMPLATE, callbackparam, jsonMessage));

		return String.format(TEMPLATE, callbackparam, jsonMessage);
	}

	private static final class SessionTimeoutTask implements Runnable {

		private String sessionId;

		public SessionTimeoutTask(String sessionId) {
			this.sessionId = sessionId;
		}

		@Override
		public void run() {
			HttpSessionStore.sessionMap.remove(sessionId);
			// TODO unregister topics
		}
	};
}