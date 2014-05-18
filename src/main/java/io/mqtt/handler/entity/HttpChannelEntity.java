package io.mqtt.handler.entity;

public class HttpChannelEntity extends HttpJsonpChannelEntity {

	private HttpChannelEntity(String sessionId) {
		super(sessionId);
	}

	public static final HttpJsonpChannelEntity BLANK = new HttpJsonpChannelEntity(
			null);
}
