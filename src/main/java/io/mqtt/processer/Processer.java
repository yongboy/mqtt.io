package io.mqtt.processer;

import io.netty.channel.ChannelHandlerContext;

import org.meqantt.message.Message;

public interface Processer {

	/**
	 * Process message and reply it.<br>
	 * retrun <code>null</code> if no need for reply.
	 * 
	 * @param msg
	 * @param ctx
	 * @return
	 */
	public Message proc(Message msg, ChannelHandlerContext ctx);
}