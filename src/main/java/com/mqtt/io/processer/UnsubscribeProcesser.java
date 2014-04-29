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
		for (String topic : usm.getTopics()) {
			ChannelPool.removeTopic(ctx.channel(), topic);
		}
		UnsubAckMessage usam = new UnsubAckMessage();
		usam.setMessageId(usm.getMessageId());

		return usam;
	}
}