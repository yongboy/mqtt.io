package com.mqtt.io.processer;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;

import java.util.Set;

import org.meqantt.message.DisconnectMessage;
import org.meqantt.message.Message;
import org.meqantt.message.PublishMessage;

import com.mqtt.io.tool.MemPool;

public class PublishProcesser implements Processer {

	private static DisconnectMessage DISCONNECT = new DisconnectMessage();

	public static ChannelFutureListener CLOSE_ON_FAILURE = new ChannelFutureListener() {
		public void operationComplete(ChannelFuture future) {
			if (!future.isSuccess()) {
				future.channel().close();
			}
		}
	};

	public Message proc(Message msg, ChannelHandlerContext ctx) {
		String clientId = MemPool.getClientId(ctx.channel());
		if (clientId == null) {
			return DISCONNECT;
		}

		PublishMessage pm = (PublishMessage) msg;
		Set<Channel> channels = MemPool.getChannelByTopics(pm.getTopic());
		if (channels != null) {
			for (Channel chn : channels) {
				chn.writeAndFlush(pm).addListener(CLOSE_ON_FAILURE);
			}
		}

		return null;
	}
}