package com.mqtt.io.server;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;

import com.mqtt.io.coder.MqttMessageNewDecoder;
import com.mqtt.io.coder.MqttMessageNewEncoder;

public class TcpChannelInitializer extends ChannelInitializer<SocketChannel> {
	
	@Override
	public void initChannel(SocketChannel ch) throws Exception {
		ChannelPipeline pipeline = ch.pipeline();
		pipeline.addLast("encoder", new MqttMessageNewEncoder());
		pipeline.addLast("decoder", new MqttMessageNewDecoder());
		pipeline.addLast("handler", new MessageHandler());
	}
}