package io.mqtt.server;

import io.mqtt.handler.HttpJsonpRequestHandler;
import io.mqtt.handler.MessageHandler;
import io.mqtt.handler.coder.MqttMessageWebSocketFrameDecoder;
import io.mqtt.handler.coder.MqttMessageWebSocketFrameEncoder;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;

public class HttpChannelInitializer extends ChannelInitializer<SocketChannel> {
	private final static String websocketUri = "/websocket";
	private final String jsonpUriPrefix = "/jsonp/";

	@Override
	public void initChannel(final SocketChannel ch) throws Exception {
		ch.pipeline().addLast(new HttpServerCodec(),
				new MqttMessageWebSocketFrameEncoder(),
				new HttpObjectAggregator(65536),
				new HttpJsonpRequestHandler(websocketUri, jsonpUriPrefix),
				new WebSocketServerProtocolHandler(websocketUri),
				new MqttMessageWebSocketFrameDecoder(),
				new MessageHandler());

		// ch.pipeline().addLast(
		// new HttpServerCodec(),
		// new MqttMessageWebSocketFrameEncoder(),
		// new HttpObjectAggregator(65536),
		// new HttpWebSocketServerHandler());
	}
}