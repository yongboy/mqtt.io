package io.mqtt.server;

import io.mqtt.handler.MqttMessageHandler;
import io.mqtt.handler.coder.MqttMessageNewDecoder;
import io.mqtt.handler.coder.MqttMessageNewEncoder;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;

public class TcpChannelInitializer extends ChannelInitializer<SocketChannel> {
	
	@Override
	public void initChannel(SocketChannel ch) throws Exception {
		ChannelPipeline pipeline = ch.pipeline();
		pipeline.addLast("encoder", new MqttMessageNewEncoder());
		pipeline.addLast("decoder", new MqttMessageNewDecoder());
		pipeline.addLast("handler", new MqttMessageHandler());
	}
}