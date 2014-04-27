package com.mqtt.io.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

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
					.option(ChannelOption.SO_BACKLOG, 1000)
					.option(ChannelOption.TCP_NODELAY, true)
					.channel(NioServerSocketChannel.class)
					.childOption(ChannelOption.SO_KEEPALIVE, true)
					.childHandler(new TcpChannelInitializer());

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