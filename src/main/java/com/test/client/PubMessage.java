package com.test.client;
import java.io.UnsupportedEncodingException;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttTopic;

public class PubMessage {
	public static void main(String[] args) throws MqttException,
			UnsupportedEncodingException {

		String topicName = "sub/client1";
		String message = "Hello Mqtt Server !";
		String tcpUrl = "tcp://127.0.0.1:8083";
		String clientId = "pub-msg/client";
		pubMsg(tcpUrl, clientId, topicName, message);
	}

	public static void pubMsg(String tcpUrl, String clientId, String topicName,
			String message) throws MqttException, UnsupportedEncodingException {
		MqttClient client = new MqttClient(tcpUrl, clientId);
		MqttConnectOptions mqcConf = new MqttConnectOptions();
		mqcConf.setConnectionTimeout(300);
		mqcConf.setKeepAliveInterval(1200);
		client.connect(mqcConf);

		MqttTopic topic = client.getTopic(topicName);
		topic.publish(message.getBytes("utf8"), 0, false);
		
		client.close();
	}
}