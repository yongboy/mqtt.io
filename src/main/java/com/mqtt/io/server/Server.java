package com.mqtt.io.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import com.mqtt.io.tool.ConfigService;

public class Server {
	private static int port = ConfigService.getIntProperty("tcp.port", 1883);
	private static int httpPort = ConfigService.getIntProperty("http.port",
			8080);
	private static int flashPolicyPort = ConfigService.getIntProperty(
			"flash.policy.port", 18443);

	public Server() {
	}

	public void run() throws Exception {
		EventLoopGroup bossGroup = new NioEventLoopGroup(1);
		EventLoopGroup workerGroup = new NioEventLoopGroup();

		try {
			ServerBootstrap tcp = new ServerBootstrap();
			tcp.group(bossGroup, workerGroup)
					.option(ChannelOption.SO_BACKLOG, 1000)
					.option(ChannelOption.TCP_NODELAY, true)
					.channel(NioServerSocketChannel.class)
					.childOption(ChannelOption.SO_KEEPALIVE, true)
					.childHandler(new TcpChannelInitializer());

			Channel tcpChannel = tcp.bind(port).sync().channel();

			System.out.println("mqtt.io tcp server started at port " + port
					+ '.');

			ServerBootstrap websocket = new ServerBootstrap();
			websocket.group(bossGroup, workerGroup)
					.option(ChannelOption.SO_BACKLOG, 1000)
					.option(ChannelOption.TCP_NODELAY, true)
					.channel(NioServerSocketChannel.class)
					.childOption(ChannelOption.SO_KEEPALIVE, true)
					.childHandler(new WebSocketChannelInitializer());

			websocket.bind(httpPort).sync().channel();

			System.out.println("mqtt.io websocket server started at port "
					+ httpPort + '.');

			ServerBootstrap flashpolicy = new ServerBootstrap();
			flashpolicy.group(bossGroup, workerGroup)
					.option(ChannelOption.TCP_NODELAY, true)
					.channel(NioServerSocketChannel.class)
					.childHandler(new FlashPolicyChannelInitializer());

			flashpolicy.bind(flashPolicyPort).sync().channel();

			tcpChannel.closeFuture().sync();
		} finally {
			bossGroup.shutdownGracefully();
			workerGroup.shutdownGracefully();
		}
	}

	public static void main(String[] args) throws Exception {
		new Server().run();
	}
}