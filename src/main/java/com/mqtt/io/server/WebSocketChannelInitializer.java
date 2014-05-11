package com.mqtt.io.server;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;

import com.mqtt.io.handler.CustomWebSocketServerHandler;
import com.mqtt.io.handler.coder.MqttMessageWebSocketFrameEncoder;

public class WebSocketChannelInitializer extends
		ChannelInitializer<SocketChannel> {
	@Override
	public void initChannel(final SocketChannel ch) throws Exception {
//		ch.pipeline().addLast(
//				new HttpResponseEncoder(),
//				new MqttMessageWebSocketFrameEncoder(),
//				new HttpRequestDecoder(), 
//				new HttpObjectAggregator(65536),
//				new WebSocketServerProtocolHandler("/websocket"),
//				new MqttMessageWebSocketFrameDecoder(), 
//				new MessageHandler());
		
		ch.pipeline().addLast(
//				new HttpServerCodec(),
				new HttpResponseEncoder(),
//				new MqttMessageWebSocketFrameEncoder(),
				new HttpRequestDecoder(), 
				new HttpObjectAggregator(65536),
//				new HttpHelloWorldServerHandler(),
//				new WebSocketServerProtocolHandler("/websocket"),
//				new MqttMessageWebSocketFrameDecoder(), 
				new CustomWebSocketServerHandler()
//				new MessageHandler()
				);
	}
}