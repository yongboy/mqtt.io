package io.mqtt.handler;

import static io.netty.handler.codec.http.HttpHeaders.isKeepAlive;
import static io.netty.handler.codec.http.HttpHeaders.setContentLength;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import io.mqtt.handler.http.HttpSessionStore;
import io.mqtt.handler.http.HttpTransport;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.timeout.ReadTimeoutException;
import io.netty.util.CharsetUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.HashMap;
import java.util.Map;

@Sharable
public class HttpRequestHandler extends
		SimpleChannelInboundHandler<FullHttpRequest> {
	private static final InternalLogger logger = InternalLoggerFactory
			.getInstance(HttpRequestHandler.class);

	private String websocketUri;

	private static Map<String, HttpTransport> transportMap = new HashMap<String, HttpTransport>(
			1);

	public HttpRequestHandler(String websocketUri) {
		this.websocketUri = websocketUri;
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest req)
			throws Exception {
		if (!req.getDecoderResult().isSuccess()) {
			logger.debug("invalid http request");
			sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HTTP_1_1,
					BAD_REQUEST));
			return;
		}

		if (req.getUri().equalsIgnoreCase(this.websocketUri)) {
			logger.debug("it is websocket request");
			ctx.fireChannelRead(req.retain());
			return;
		}

		HttpTransport transport = getTransport(req);
		if (transport == null) {
			sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HTTP_1_1,
					BAD_REQUEST));
		} else {
			transport.handleRequest(ctx, req);
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
			throws Exception {
		if (cause instanceof ReadTimeoutException) {
			HttpTransport transport = getTransport(ctx);
			if (transport != null) {
				transport.handleTimeout(ctx);
			}
		} else {
			cause.printStackTrace();
			ctx.close();
		}
	}

	private HttpTransport getTransport(ChannelHandlerContext ctx) {
		HttpRequest req = ctx.attr(HttpSessionStore.key).get();
		return getTransport(req);
	}

	private HttpTransport getTransport(HttpRequest req) {
		if (req == null)
			return null;

		for (String prefix : transportMap.keySet()) {
			if (req.getUri().startsWith(prefix)) {
				return transportMap.get(prefix);
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

	public static void registerTransport(HttpTransport httpTransport) {
		if (httpTransport == null)
			return;

		transportMap.put(httpTransport.getPrefixUri(), httpTransport);
	}
}