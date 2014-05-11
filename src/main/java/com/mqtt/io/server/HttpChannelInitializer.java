package com.mqtt.io.server;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;

import com.mqtt.io.handler.HttpWebSocketServerHandler;
import com.mqtt.io.handler.coder.MqttMessageWebSocketFrameEncoder;

public class HttpChannelInitializer extends
		ChannelInitializer<SocketChannel> {
	@Override
	public void initChannel(final SocketChannel ch) throws Exception {
		// ch.pipeline().addLast(
		// new HttpResponseEncoder(),
		// new MqttMessageWebSocketFrameEncoder(),
		// new HttpRequestDecoder(),
		// new HttpObjectAggregator(65536),
		// new WebSocketServerProtocolHandler("/websocket"),
		// new MqttMessageWebSocketFrameDecoder(),
		// new MessageHandler());

		ch.pipeline().addLast(
				new HttpServerCodec(),
				new MqttMessageWebSocketFrameEncoder(),
				new HttpObjectAggregator(65536),
				new HttpWebSocketServerHandler());
	}
}