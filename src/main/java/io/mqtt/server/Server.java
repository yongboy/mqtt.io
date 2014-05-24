package io.mqtt.server;

import io.mqtt.tool.ConfigService;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.Log4JLoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class Server {
	private static final InternalLogger logger = InternalLoggerFactory
			.getInstance(Server.class);

	private final int port = ConfigService.getIntProperty("tcp.port", 1883);
	private final int httpPort = ConfigService
			.getIntProperty("http.port", 8080);

	private List<Channel> channels = new ArrayList<Channel>();
	private EventLoopGroup bossGroup = new NioEventLoopGroup(1);
	private EventLoopGroup workerGroup = new NioEventLoopGroup();

	public Server() {
	}

	private ServerBootstrap getDefaultServerBootstrap() {
		ServerBootstrap server = new ServerBootstrap();
		server.group(bossGroup, workerGroup)
				.option(ChannelOption.SO_BACKLOG, 1000)
				.option(ChannelOption.TCP_NODELAY, true)
				.channel(NioServerSocketChannel.class)
				.childOption(ChannelOption.SO_KEEPALIVE, true);
		return server;
	}

	public ChannelFuture run() throws Exception {
		InternalLoggerFactory.setDefaultFactory(new Log4JLoggerFactory());

		Channel channle = getDefaultServerBootstrap()
				.childHandler(new TcpChannelInitializer()).bind(port).sync()
				.channel();
		channels.add(channle);

		logger.info("mqtt.io tcp server started at port " + port + '.');

		ChannelFuture future = getDefaultServerBootstrap().childHandler(
				new HttpChannelInitializer()).bind(httpPort);

		Channel httpChannel = future.sync().channel();
		channels.add(httpChannel);

		logger.info("mqtt.io websocket server started at port " + httpPort
				+ '.');

		return future;
	}

	public void destroy() {
		logger.info("destroy mqtt.io server ...");
		for (Channel channel : channels) {
			channel.close();
		}
		bossGroup.shutdownGracefully();
		workerGroup.shutdownGracefully();
	}

	public static void main(String[] args) throws Exception {
		logger.info("start mqtt.io server ...");
		final Server server = new Server();
		ChannelFuture future = server.run();

		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				server.destroy();
			}
		});

		future.channel().closeFuture().syncUninterruptibly();
	}
}