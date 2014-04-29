package com.mqtt.io.processer;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.ReadTimeoutHandler;

import java.util.concurrent.TimeUnit;

import org.meqantt.message.ConnAckMessage;
import org.meqantt.message.ConnAckMessage.ConnectionStatus;
import org.meqantt.message.ConnectMessage;
import org.meqantt.message.Message;

import com.mqtt.io.tool.ChannelPool;

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

		ctx.pipeline().addFirst("readTimeOutHandler",
				new ReadTimeoutHandler(timeout, TimeUnit.SECONDS));

		ChannelPool.put(ctx.channel(), cm.getClientId());

		return ACCEPTED;
	}
}