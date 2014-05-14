package io.mqtt.handler;

import io.mqtt.processer.ConnectProcesser;
import io.mqtt.processer.DisConnectProcesser;
import io.mqtt.processer.PingReqProcesser;
import io.mqtt.processer.Processer;
import io.mqtt.processer.PublishProcesser;
import io.mqtt.processer.SubscribeProcesser;
import io.mqtt.processer.UnsubscribeProcesser;
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

public class MqttMessageHandler extends ChannelInboundHandlerAdapter {
	private static PingRespMessage PINGRESP = new PingRespMessage();

	public static Map<Message.Type, Processer> processers = new HashMap<Message.Type, Processer>(
			6);

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
				ctx.write(PINGRESP).addListener(
						ChannelFutureListener.CLOSE_ON_FAILURE);
			} else {
				ctx.channel().close();
			}
		} catch (Throwable t) {
			t.printStackTrace();
			ctx.channel().close();
		}

		e.printStackTrace();
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
			ctx.write(rmsg).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
		}
	}

	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
		ctx.flush();
	}
}