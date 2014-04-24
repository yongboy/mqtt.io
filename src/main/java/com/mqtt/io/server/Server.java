package com.mqtt.io.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import com.mqtt.io.coder.MqttMessageNewDecoder;
import com.mqtt.io.coder.MqttMessageNewEncoder;
import com.mqtt.io.tool.ConfigService;

public class Server {
	private static int port = ConfigService.getIntProperty("mqtt.port", 1883);

	public Server() {
	}

	public void run() throws Exception {
		EventLoopGroup bossGroup = new NioEventLoopGroup(1);
		EventLoopGroup workerGroup = new NioEventLoopGroup();
		try {
			ServerBootstrap b = new ServerBootstrap();
			b.group(bossGroup, workerGroup)
					.channel(NioServerSocketChannel.class)
					.childHandler(new ChannelInitializer<SocketChannel>() {
						@Override
						public void initChannel(SocketChannel ch)
								throws Exception {
							ChannelPipeline pipeline = ch.pipeline();
							pipeline.addLast("encoder",
									new MqttMessageNewEncoder());
							pipeline.addLast("decoder",
									new MqttMessageNewDecoder());
							pipeline.addLast("handler", new MessageHandler());
						}
					});

			Channel ch = b.bind(port).sync().channel();
			System.out.println("mqtt.io server started at port " + port + '.');

			ch.closeFuture().sync();
		} finally {
			bossGroup.shutdownGracefully();
			workerGroup.shutdownGracefully();
		}
	}

	public static void main(String[] args) throws Exception {
		new Server().run();
	}
}