package io.mqtt.handler.entity;

import org.meqantt.message.Message;

import io.netty.channel.Channel;

public abstract class ChannelEntity {

	public Channel getChannel() {
		return null;
	}

	public abstract void write(Message message);

	@Override
	public int hashCode() {
		return getChannel().hashCode();
	}

	@Override
	public String toString() {
		return getChannel().toString();
	}
}