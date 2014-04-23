package com.mqtt.io.tool;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;

import java.util.concurrent.ConcurrentHashMap;

public class ChannelPool {

	private final static ConcurrentHashMap<String, Channel> pool = new ConcurrentHashMap<String, Channel>(
			2000000, 0.9f, 256);

	private final static ConcurrentHashMap<String, Channel> topics = new ConcurrentHashMap<String, Channel>(
			2000000, 0.9f, 256);

	private final static ConcurrentHashMap<Channel, String> channelClient = new ConcurrentHashMap<Channel, String>();

	private final static ConcurrentHashMap<Channel, String> channelTopic = new ConcurrentHashMap<Channel, String>();

	private final static ChannelFutureListener clientRemover = new ChannelFutureListener() {
		public void operationComplete(ChannelFuture future) throws Exception {
			remove(future.channel());
		}
	};

	public static void put(Channel channel, String clientId) {
		if (channel == null) {
			return;
		}
		if (clientId == null) {
			return;
		}
		channel.closeFuture().addListener(clientRemover);
		channelClient.put(channel, clientId);
		Channel oldChannel = pool.put(clientId, channel);
		if (oldChannel != null) {
			remove(oldChannel);
			oldChannel.close();
		}
	}

	public static void remove(Channel chn) {
		removeTopic(chn);
		String clientId = channelClient.remove(chn);
		if (clientId != null) {
			pool.remove(clientId, chn);
		}
		chn.closeFuture().removeListener(clientRemover);
	}

	public static void putTopic(Channel chn, String topic) {
		if (chn == null) {
			return;
		}
		if (topic == null) {
			return;
		}
			channelTopic.put(chn, topic);
			Channel old = topics.put(topic, chn);
			if (old != null) {
				old.close();
			}
	}

	public static void removeTopic(Channel chn) {
		String topic = channelTopic.remove(chn);
		if (topic != null) {
			topics.remove(topic, chn);
		}
	}

	public static String getClientId(Channel chn) {
		return channelClient.get(chn);
	}
	
	public static String getTopic(Channel chn) {
		return channelTopic.get(chn);
	}

	public static Channel getChannelByTopic(String topic) {
		if (topic == null || !isToken(topic)) {
			return null;
		}
		return topics.get(topic);
	}

	public static boolean isToken(String topic) {
		if (topic == null) {
			return false;
		}
		if (topic.length() <= 15) {
			return false;
		}
		return !topic.contains("/");
	}

	public static int poolSize() {
		return pool.size();
	}

	public static int topicSize() {
		return topics.size();
	}

	public static int clientLocalSize() {
		return channelClient.size();
	}

	public static int topicLocalSize() {
		return channelTopic.size();
	}

}
