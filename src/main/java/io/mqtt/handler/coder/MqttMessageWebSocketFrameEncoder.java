package io.mqtt.handler.coder;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;

import java.util.List;

import org.meqantt.message.Message;

@Sharable
public class MqttMessageWebSocketFrameEncoder extends
		MessageToMessageEncoder<Message> {
	@Override
	protected void encode(ChannelHandlerContext ctx, Message msg,
			List<Object> out) throws Exception {
		if (msg == null)
			return;

		byte[] data = ((Message) msg).toBytes();

		out.add(new BinaryWebSocketFrame(Unpooled.wrappedBuffer(data)));
	}
}