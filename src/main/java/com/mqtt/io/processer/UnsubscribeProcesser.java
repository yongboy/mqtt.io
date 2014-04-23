package com.mqtt.io.processer;

import io.netty.channel.ChannelHandlerContext;

import org.meqantt.message.Message;
import org.meqantt.message.UnsubAckMessage;
import org.meqantt.message.UnsubscribeMessage;

import com.mqtt.io.tool.ChannelPool;

public class UnsubscribeProcesser implements Processer {

	public Message proc(Message msg, ChannelHandlerContext ctx) {
		if (ChannelPool.getClientId(ctx.channel()) == null) {
			ctx.channel().close();
			return null;
		}

		UnsubscribeMessage usm = (UnsubscribeMessage) msg;
		ChannelPool.removeTopic(ctx.channel());
		UnsubAckMessage usam = new UnsubAckMessage();
		usam.setMessageId(usm.getMessageId());

		return usam;
	}
}