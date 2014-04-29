package com.mqtt.io.coder;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;

import java.util.List;

public class MqttMessageWebSocketFrameDecoder extends
		MessageToMessageDecoder<BinaryWebSocketFrame> {

	@Override
	protected void decode(ChannelHandlerContext ctx,
			BinaryWebSocketFrame wsFrame, List<Object> out) throws Exception {
		ByteBuf buf = wsFrame.content();

		new MqttMessageNewDecoder().decode(ctx, buf, out);
	}
}