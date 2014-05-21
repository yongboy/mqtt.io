package io.mqtt.processer;

import io.mqtt.handler.entity.ChannelEntity;
import io.mqtt.tool.MemPool;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.Set;

import org.meqantt.message.DisconnectMessage;
import org.meqantt.message.Message;
import org.meqantt.message.PublishMessage;

public class PublishProcesser implements Processer {
	private static final InternalLogger logger = InternalLoggerFactory
			.getInstance(PublishProcesser.class);
	private static DisconnectMessage DISCONNECT = new DisconnectMessage();

	public Message proc(Message msg, ChannelHandlerContext ctx) {
		String clientId = MemPool.getClientId(ctx.channel());
		if (clientId == null) {
			return DISCONNECT;
		}

		PublishMessage pm = (PublishMessage) msg;
		Set<ChannelEntity> channelEntitys = MemPool.getChannelByTopics(pm
				.getTopic());
		if (channelEntitys == null) {
			return null;
		}

		for (ChannelEntity channelEntity : channelEntitys) {
			logger.debug("PUBLISH to ChannelEntity topic = " + pm.getTopic()
					+ " payload = " + pm.getDataAsString());
			channelEntity.write(pm);
		}

		return null;
	}
}