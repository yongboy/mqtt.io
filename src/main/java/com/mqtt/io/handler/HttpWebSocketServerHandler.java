/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package com.mqtt.io.handler;

import static io.netty.handler.codec.http.HttpHeaders.Names.HOST;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import io.netty.handler.timeout.ReadTimeoutException;

import java.util.ArrayList;
import java.util.List;

import com.mqtt.io.handler.coder.MqttMessageNewDecoder;

/**
 * Handles handshakes and messages
 */
public class HttpWebSocketServerHandler extends
		SimpleChannelInboundHandler<Object> {
	protected static final String WEBSOCKET_PATH = "/websocket";

	private WebSocketServerHandshaker handshaker;
	private MqttMessageNewDecoder messageNewDecoder;
	private MessageHandler messageHandler;

	private HttpJSONPHandler httpJSONPHandler;

	public HttpWebSocketServerHandler() {
		this.messageNewDecoder = new MqttMessageNewDecoder();
		this.messageHandler = new MessageHandler();
		this.httpJSONPHandler = new HttpJSONPHandler(this);
	}

	@Override
	public void channelRead0(ChannelHandlerContext ctx, Object msg)
			throws Exception {
		if (msg instanceof FullHttpRequest) {
			this.httpJSONPHandler.handleHttpRequest(ctx, (FullHttpRequest) msg);
			return;
		} else if (msg instanceof WebSocketFrame) {
			handleWebSocketFrame(ctx, (WebSocketFrame) msg);
		}
	}

	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
		ctx.flush();
	}

	protected void handshakeRequest(ChannelHandlerContext ctx,
			FullHttpRequest req) throws Exception {
		if (req.getUri().contains(WEBSOCKET_PATH)) {
			// Handshake
			WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(
					getWebSocketLocation(req), null, false);
			handshaker = wsFactory.newHandshaker(req);
			if (handshaker == null) {
				WebSocketServerHandshakerFactory
						.sendUnsupportedVersionResponse(ctx.channel());
			} else {
				handshaker.handshake(ctx.channel(), req);
			}
		}
	}

	private void handleWebSocketFrame(ChannelHandlerContext ctx,
			WebSocketFrame frame) throws Exception {

		// Check for closing frame
		if (frame instanceof CloseWebSocketFrame) {
			handshaker.close(ctx.channel(),
					(CloseWebSocketFrame) frame.retain());
			return;
		}
		if (frame instanceof PingWebSocketFrame) {
			ctx.channel().write(
					new PongWebSocketFrame(frame.content().retain()));
			return;
		}

		ByteBuf buf = frame.content();
		List<Object> out = new ArrayList<Object>();
		this.messageNewDecoder.decode(ctx, buf, out);
		this.messageHandler.channelRead(ctx, out.get(0));

		if ((frame instanceof TextWebSocketFrame)) {
			throw new UnsupportedOperationException(String.format(
					"%s frame types not supported", frame.getClass().getName()));
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
			throws Exception {
		if (cause instanceof ReadTimeoutException) {
			this.httpJSONPHandler.handleTimeout(ctx);
		} else {
			cause.printStackTrace();
			ctx.close();
		}
	}

	private static String getWebSocketLocation(FullHttpRequest req) {
		return "ws://" + req.headers().get(HOST) + WEBSOCKET_PATH;
	}
}