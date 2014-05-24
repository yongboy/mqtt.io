package io.mqtt.processer;

import io.mqtt.handler.entity.ChannelEntity;
import io.mqtt.handler.entity.TcpChannelEntity;
import io.mqtt.tool.MemPool;
import io.netty.channel.ChannelHandlerContext;

import org.meqantt.message.DisconnectMessage;
import org.meqantt.message.Message;
import org.meqantt.message.QoS;
import org.meqantt.message.SubAckMessage;
import org.meqantt.message.SubscribeMessage;

public class SubscribeProcesser implements Processer {

	private static DisconnectMessage DISCONNECT = new DisconnectMessage();

	public Message proc(Message msg, ChannelHandlerContext ctx) {
		String clientId = MemPool.getClientId(ctx.channel());
		if (clientId == null) {
			return DISCONNECT;
		}

		SubscribeMessage sm = (SubscribeMessage) msg;
		SubAckMessage sam = new SubAckMessage();
		sam.setMessageId(sm.getMessageId());
		if (sm.getTopics() != null) {
			for (String topic : sm.getTopics()) {
				sam.addQoS(QoS.AT_MOST_ONCE);
				ChannelEntity channelEntity = new TcpChannelEntity(
						ctx.channel());
				MemPool.registerTopic(channelEntity, topic);
			}
		}

		return sam;
	}
}