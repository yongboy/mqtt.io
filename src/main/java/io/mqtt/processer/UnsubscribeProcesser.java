package io.mqtt.processer;

import io.mqtt.tool.MemPool;
import io.netty.channel.ChannelHandlerContext;

import org.meqantt.message.Message;
import org.meqantt.message.UnsubAckMessage;
import org.meqantt.message.UnsubscribeMessage;

public class UnsubscribeProcesser implements Processer {

	public Message proc(Message msg, ChannelHandlerContext ctx) {
		if (MemPool.getClientId(ctx.channel()) == null) {
			ctx.channel().close();
			return null;
		}

		UnsubscribeMessage usm = (UnsubscribeMessage) msg;
		for (String topic : usm.getTopics()) {
			MemPool.unregisterTopic(ctx.channel(), topic);
		}
		UnsubAckMessage usam = new UnsubAckMessage();
		usam.setMessageId(usm.getMessageId());

		return usam;
	}
}