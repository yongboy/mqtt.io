package io.mqtt.handler.entity;

import io.mqtt.handler.http.HttpJsonpTransport;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.LinkedList;
import java.util.Queue;

import org.meqantt.message.Message;
import org.meqantt.message.PublishMessage;

public class HttpJsonpChannelEntity extends HttpChannelEntity {
	private static final InternalLogger logger = InternalLoggerFactory
			.getInstance(HttpJsonpChannelEntity.class);

	private Queue<Message> queue;
	private ChannelHandlerContext ctx = null;
	private boolean isInit;

	public HttpJsonpChannelEntity(String sessionId, boolean isInit) {
		super(sessionId);
		queue = new LinkedList<Message>();
		this.isInit = isInit;
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

	public Queue<Message> getQueue() {
		return queue;
	}

	public ChannelHandlerContext getCtx() {
		return ctx;
	}

	public void setCtx(ChannelHandlerContext ctx) {
		this.ctx = ctx;
	}
	
	public void setBlank(boolean isInit) {
		this.isInit = isInit;
	}

	@Override
	public boolean isBlank(){
		return this.isInit;
	}
}