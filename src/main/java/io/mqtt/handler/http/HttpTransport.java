package io.mqtt.handler.http;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;

public abstract class HttpTransport {

	public void handleTimeout(ChannelHandlerContext ctx) {
		// do nothing ...
	}

	public abstract void handleRequest(ChannelHandlerContext ctx,
			FullHttpRequest req) throws Exception;
	
	
	public abstract String getPrefixUri();
}
