package io.mqtt.processer;

import io.netty.channel.ChannelHandlerContext;

import org.meqantt.message.Message;

public class DisConnectProcesser implements Processer {

	public Message proc(Message msg, ChannelHandlerContext ctx) {
		ctx.channel().close();
		
		return null;
	}
}