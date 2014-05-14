package io.mqtt.handler.entity;

import io.netty.channel.Channel;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.meqantt.message.Message;

public class HttpChannelEntity extends ChannelEntity {

	private String sessionId;
	private BlockingQueue<Message> queue;

	public HttpChannelEntity(String sessionId) {
		this.sessionId = sessionId;
		queue = new LinkedBlockingQueue<Message>();
	}

	@Override
	public Channel getChannel() {
		throw new UnsupportedOperationException(
				"The TcpChannelEntity.java does not supported getChannel() method !");
	}

	@Override
	public void write(Message message) {
		if (message == null)
			return;

		queue.add(message);
	}

	public String getSessionId() {
		return sessionId;
	}

	public BlockingQueue<Message> getQueue() {
		return queue;
	}

	@Override
	public int hashCode() {
		return getSessionId().hashCode();
	}

	@Override
	public String toString() {
		return "JSESSIONID=" + getSessionId();
	}
}