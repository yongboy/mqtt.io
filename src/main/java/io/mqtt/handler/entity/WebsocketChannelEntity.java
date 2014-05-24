package io.mqtt.handler.entity;

import io.netty.channel.Channel;

public class WebsocketChannelEntity extends TcpChannelEntity {

	public WebsocketChannelEntity(Channel channel) {
		super(channel);
	}
}