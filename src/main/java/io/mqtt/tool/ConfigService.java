package io.mqtt.tool;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.log4j.Logger;

public class ConfigService {

	private final static Logger logger = Logger.getLogger(ConfigService.class);

	private final static Properties props = new Properties();

	static {
		InputStream is = ClassLoader
				.getSystemResourceAsStream("mqtt.properties");
		try {
			props.load(is);
		} catch (IOException e) {
			logger.error("Config file load error!", e);
		}
	}

	public static String getProperty(String key) {
		return props.getProperty(key);
	}

	public static String getProperty(String key, String defaultVal) {
		return props.getProperty(key, defaultVal);
	}

	public static int getIntProperty(String key) {
		String val = props.getProperty(key);
		try {
			return Integer.parseInt(val);
		} catch (NumberFormatException e) {
			logger.info("Get config error! key : " + key, e);
			return 0;
		}

	}

	public static int getIntProperty(String key, int defaultVal) {
		String val = props.getProperty(key);
		if (val == null) {
			return defaultVal;
		}
		try {
			return Integer.parseInt(val);
		} catch (NumberFormatException e) {
			logger.info("Get config error! key : " + key, e);
			return defaultVal;
		}
	}
}