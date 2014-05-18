package io.mqtt.handler.entity;

import io.mqtt.handler.http.HttpJsonpTransport;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.meqantt.message.Message;
import org.meqantt.message.PublishMessage;

public class HttpJsonpChannelEntity extends ChannelEntity {
	private static final InternalLogger logger = InternalLoggerFactory
			.getInstance(HttpJsonpChannelEntity.class);

	private String sessionId;
	private BlockingQueue<Message> queue;

	private ChannelHandlerContext ctx = null;

	public HttpJsonpChannelEntity(String sessionId) {
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

		if (ctx == null) {
			queue.add(message);
			return;
		}

		if (message instanceof PublishMessage) {
			PublishMessage publishMessage = (PublishMessage) message;
			HttpJsonpTransport.doWriteBody(ctx, publishMessage);
		} else {
			logger.debug("message type = " + message.getClass());
		}

		ctx = null;
	}

	public String getSessionId() {
		return sessionId;
	}

	public BlockingQueue<Message> getQueue() {
		return queue;
	}

	public ChannelHandlerContext getCtx() {
		return ctx;
	}

	public void setCtx(ChannelHandlerContext ctx) {
		this.ctx = ctx;
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