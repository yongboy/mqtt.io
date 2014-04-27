package com.mqtt.io.coder;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

public class MqttMessageWebSocketFrameHandler extends
		SimpleChannelInboundHandler<BinaryWebSocketFrame> {
	@Override
	protected void channelRead0(ChannelHandlerContext ctx,
			BinaryWebSocketFrame frame) throws Exception {
//		String request = frame.text();
//		ctx.channel().writeAndFlush(
//				new TextWebSocketFrame(request.toUpperCase()));
	}
}