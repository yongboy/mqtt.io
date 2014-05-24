package io.mqtt.handler.coder;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;

import java.util.List;

import org.meqantt.message.Message;

@Sharable
public class MqttMessageNewEncoder extends MessageToMessageEncoder<Object> {
	@Override
	protected void encode(ChannelHandlerContext ctx, Object msg,
			List<Object> out) throws Exception {
		if (!(msg instanceof Message)) {
			return;
		}
		
		byte[] data = ((Message) msg).toBytes();

		out.add(Unpooled.wrappedBuffer(data));
	}
}