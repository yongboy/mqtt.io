package com.mqtt.io.processer;

import io.netty.channel.ChannelHandlerContext;

import org.meqantt.message.DisconnectMessage;
import org.meqantt.message.Message;
import org.meqantt.message.PingRespMessage;

import com.mqtt.io.tool.MemPool;

public class PingReqProcesser implements Processer {

	private static PingRespMessage PING_RESP = new PingRespMessage();

	private static DisconnectMessage DISCONNECT = new DisconnectMessage();

	public Message proc(Message msg, ChannelHandlerContext ctx) {
		if (MemPool.getClientId(ctx.channel()) == null) {
			return DISCONNECT;
		}
		
		return PING_RESP;
	}
}