package io.mqtt.server;

import io.mqtt.handler.HttpWebSocketServerHandler;
import io.mqtt.handler.coder.MqttMessageWebSocketFrameEncoder;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;

public class HttpChannelInitializer extends
		ChannelInitializer<SocketChannel> {
	@Override
	public void initChannel(final SocketChannel ch) throws Exception {
		// just support websocket
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