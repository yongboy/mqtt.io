package io.mqtt.handler.entity;

import org.meqantt.message.Message;

import io.netty.util.concurrent.ScheduledFuture;

public class HttpChannelEntity extends ChannelEntity {

	private String sessionId;
	private ScheduledFuture<?> scheduleTask = null;

	protected HttpChannelEntity() {
	}

	public HttpChannelEntity(String sessionId) {
		this.sessionId = sessionId;
	}

	public String getSessionId() {
		return sessionId;
	}

	public ScheduledFuture<?> getScheduleTask() {
		return scheduleTask;
	}

	public void setScheduleTask(ScheduledFuture<?> scheduleTask) {
		this.scheduleTask = scheduleTask;
	}

	@Override
	public void write(Message message) {
	}

	@Override
	public int hashCode() {
		return getSessionId().hashCode();
	}

	@Override
	public String toString() {
		return "JSESSIONID=" + getSessionId();
	}
	
	public boolean isBlank(){
		return true;
	}
	
	public void setBlank(boolean isInit) {
	}
}
