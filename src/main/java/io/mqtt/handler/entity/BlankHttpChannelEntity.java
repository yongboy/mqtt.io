package io.mqtt.handler.entity;

public class BlankHttpChannelEntity extends HttpChannelEntity {

	private BlankHttpChannelEntity(String sessionId) {
		super(sessionId);
	}

	public static final HttpChannelEntity BLANK = new HttpChannelEntity(null);
}
