package com.mqtt.io.server;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;

import com.mqtt.io.coder.MqttMessageWebSocketFrameDecoder;
import com.mqtt.io.coder.MqttMessageWebSocketFrameEncoder;

public class WebSocketChannelInitializer extends
		ChannelInitializer<SocketChannel> {
	@Override
	public void initChannel(final SocketChannel ch) throws Exception {
		ch.pipeline().addLast(
				new HttpResponseEncoder(),
				new MqttMessageWebSocketFrameEncoder(),
				new HttpRequestDecoder(), 
				new HttpObjectAggregator(65536),
				new WebSocketServerProtocolHandler("/websocket"),
				new MqttMessageWebSocketFrameDecoder(), 
				new MessageHandler());
	}
}