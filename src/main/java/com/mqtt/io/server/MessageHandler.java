package com.mqtt.io.server;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.ReadTimeoutException;

import java.util.HashMap;
import java.util.Map;

import org.meqantt.message.ConnAckMessage;
import org.meqantt.message.ConnAckMessage.ConnectionStatus;
import org.meqantt.message.DisconnectMessage;
import org.meqantt.message.Message;
import org.meqantt.message.Message.Type;
import org.meqantt.message.PingRespMessage;

import com.mqtt.io.processer.ConnectProcesser;
import com.mqtt.io.processer.DisConnectProcesser;
import com.mqtt.io.processer.PingReqProcesser;
import com.mqtt.io.processer.Processer;
import com.mqtt.io.processer.PublishProcesser;
import com.mqtt.io.processer.SubscribeProcesser;
import com.mqtt.io.processer.UnsubscribeProcesser;

public class MessageHandler extends ChannelInboundHandlerAdapter {
	private static PingRespMessage PINGRESP = new PingRespMessage();

	public static Map<Message.Type, Processer> processers = new HashMap<Message.Type, Processer>();

	static {
		processers.put(Type.CONNECT, new ConnectProcesser());
		processers.put(Type.PUBLISH, new PublishProcesser());
		processers.put(Type.SUBSCRIBE, new SubscribeProcesser());
		processers.put(Type.UNSUBSCRIBE, new UnsubscribeProcesser());
		processers.put(Type.PINGREQ, new PingReqProcesser());
		processers.put(Type.DISCONNECT, new DisConnectProcesser());
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable e)
			throws Exception {
		try {
			if (e.getCause() instanceof ReadTimeoutException) {
				ctx.write(PINGRESP)
						.addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
			} else {
				ctx.channel().close();
			}
		} catch (Throwable t) {
			ctx.channel().close();
		}
	}

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
			ctx.write(rmsg).addListener(ChannelFutureListener.CLOSE);
		} else if (rmsg instanceof DisconnectMessage) {
			ctx.write(rmsg).addListener(ChannelFutureListener.CLOSE);
		} else {
			ctx.write(rmsg)
					.addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
		}
	}
}