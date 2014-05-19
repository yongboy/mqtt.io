package io.mqtt.processer;

import io.mqtt.tool.MemPool;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.ReadTimeoutHandler;

import java.util.concurrent.TimeUnit;

import org.meqantt.message.ConnAckMessage;
import org.meqantt.message.ConnAckMessage.ConnectionStatus;
import org.meqantt.message.ConnectMessage;
import org.meqantt.message.Message;

public class ConnectProcesser implements Processer {

	private static ConnAckMessage UNACCEPTABLE_PROTOCOL_VERSION = new ConnAckMessage(
			ConnectionStatus.UNACCEPTABLE_PROTOCOL_VERSION);

	private static ConnAckMessage ACCEPTED = new ConnAckMessage(
			ConnectionStatus.ACCEPTED);

	public Message proc(Message msg, ChannelHandlerContext ctx) {
		ConnectMessage cm = (ConnectMessage) msg;
		if (!"MQIsdp".equalsIgnoreCase(cm.getProtocolId())
				|| cm.getProtocolVersion() != 3) {
			return UNACCEPTABLE_PROTOCOL_VERSION;
		}

		int timeout = (int) Math.ceil(cm.getKeepAlive() * 1.5);
		System.out.println("timeout is " + timeout);

		ctx.pipeline().addFirst("readTimeOutHandler",
				new ReadTimeoutHandler(timeout, TimeUnit.SECONDS));

		MemPool.registerClienId(cm.getClientId(), ctx.channel());

		return ACCEPTED;
	}
}