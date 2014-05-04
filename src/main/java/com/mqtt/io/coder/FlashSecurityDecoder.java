package com.mqtt.io.coder;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;
import io.netty.util.CharsetUtil;

import java.util.List;

public class FlashSecurityDecoder extends ReplayingDecoder<Void> {
	private static final ByteBuf requestBuffer = Unpooled.copiedBuffer(
			"<policy-file-request/>", CharsetUtil.UTF_8);

	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf buffer,
			List<Object> out) {
		ByteBuf data = buffer.readBytes(requestBuffer.readableBytes());
		if (data.equals(requestBuffer)) {
			out.add(data);
			return;
		}
		ctx.close();
	}
}