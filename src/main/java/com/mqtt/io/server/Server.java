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
	private static int port = ConfigService.getIntProperty("mqtt.port", 8080);

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
			System.out.println("Web socket server started at port " + port
					+ '.');
			System.out
					.println("Open your browser and navigate to http://localhost:"
							+ port + '/');

			ch.closeFuture().sync();
		} finally {
			bossGroup.shutdownGracefully();
			workerGroup.shutdownGracefully();
		}
	}

	// public void run2() {
	// InternalLoggerFactory.setDefaultFactory(new Log4JLoggerFactory());
	//
	// ServerBootstrap bootstrap = new ServerBootstrap(
	// new NioServerSocketChannelFactory(
	// Executors.newCachedThreadPool(),
	// Executors.newCachedThreadPool(), WORKER_COUNT));
	//
	// bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
	//
	// public ChannelPipeline getPipeline() throws Exception {
	// return Channels.pipeline(new MqttMessageEncoder(),
	// new MqttMessageDecoder(), new MessageHandler());
	// }
	// });
	//
	// bootstrap.setOption("child.tcpNoDelay", true);
	// bootstrap.setOption("child.keepAlive", true);
	// bootstrap.bind(new InetSocketAddress(port));
	// }

	public static void main(String[] args) throws Exception {
		new Server().run();
	}
}