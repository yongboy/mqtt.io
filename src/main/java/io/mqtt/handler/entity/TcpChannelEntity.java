package io.mqtt.handler.entity;

import io.netty.channel.Channel;

import org.meqantt.message.Message;

public class TcpChannelEntity extends ChannelEntity {

	protected Channel channel;

	public TcpChannelEntity(Channel channel) {
		this.channel = channel;
	}

	@Override
	public Channel getChannel() {
		return this.channel;
	}

	@Override
	public void write(Message message) {
		this.channel.writeAndFlush(message);
	}
}