package com.mqtt.io.coder;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;

import java.util.ArrayList;
import java.util.List;

import com.mqtt.io.server.MessageHandler;

public class MqttMessageWebSocketFrameHandler extends
		SimpleChannelInboundHandler<BinaryWebSocketFrame> {
	@Override
	protected void channelRead0(ChannelHandlerContext ctx,
			BinaryWebSocketFrame frame) throws Exception {
		ByteBuf byteBuf = frame.content();
		
		MqttMessageNewDecoder messageNewDecoder = new MqttMessageNewDecoder();
		List<Object>out = new ArrayList<Object>(1);
		messageNewDecoder.decode(ctx, byteBuf, out);
		if(out.isEmpty()){
			return;
		}
		
		Object obj = out.get(0);
		MessageHandler messageHandler = new MessageHandler();
		messageHandler.channelRead(ctx, obj);
		// String request = frame.text();
		// ctx.channel().writeAndFlush(
		// new TextWebSocketFrame(request.toUpperCase()));
	}

	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
		ctx.flush();
	}
}