package com.mqtt.io.server;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;

import com.mqtt.io.coder.FlashSecurityDecoder;
import com.mqtt.io.coder.FlashSecurityHandler;

public class FlashPolicyChannelInitializer extends
		ChannelInitializer<SocketChannel> {
	@Override
	public void initChannel(final SocketChannel ch) throws Exception {
		ch.pipeline().addLast(
				new FlashSecurityDecoder(), 
				new FlashSecurityHandler());
	}
}