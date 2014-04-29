package com.mqtt.io.server;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;

import org.meqantt.message.ConnAckMessage;
import org.meqantt.message.ConnAckMessage.ConnectionStatus;
import org.meqantt.message.DisconnectMessage;
import org.meqantt.message.Message;

import com.mqtt.io.processer.Processer;

public class WebSocketMessageHandler extends MessageHandler {
	
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object obj)
			throws Exception {
		Message msg = (Message) obj;
		Processer p = processers.get(msg.getType());
		if (p == null) {
			return;
		}
		Message rmsg = p.proc(msg, ctx);
		if (rmsg == null) {
			return;
		}

		if (rmsg instanceof ConnAckMessage
				&& ((ConnAckMessage) rmsg).getStatus() != ConnectionStatus.ACCEPTED) {
			ctx.write(encode(rmsg)).addListener(ChannelFutureListener.CLOSE);
		} else if (rmsg instanceof DisconnectMessage) {
			ctx.write(encode(rmsg)).addListener(ChannelFutureListener.CLOSE);
		} else {
			ctx.write(encode(rmsg)).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
		}
	}
	
	protected BinaryWebSocketFrame encode(Message msg){
		if (msg == null) {
			return null;
		}

		byte[] data = ((Message) msg).toBytes();

		return new BinaryWebSocketFrame(Unpooled.wrappedBuffer(data));
	}
}
