package org.aaron.kotlin.http2

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.ChannelOption
import io.netty.channel.MultithreadEventLoopGroup
import io.netty.channel.epoll.Epoll
import io.netty.channel.epoll.EpollEventLoopGroup
import io.netty.channel.epoll.EpollServerSocketChannel
import io.netty.channel.kqueue.KQueue
import io.netty.channel.kqueue.KQueueEventLoopGroup
import io.netty.channel.kqueue.KQueueServerSocketChannel
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.ServerSocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.http2.Http2SecurityUtil
import io.netty.handler.logging.LogLevel
import io.netty.handler.logging.LoggingHandler
import io.netty.handler.ssl.*
import io.netty.handler.ssl.util.SelfSignedCertificate
import org.slf4j.LoggerFactory
import kotlin.reflect.KClass

object Http2Server {
    val SSL = true
    val PORT = 8443
    val LOGGER = LoggerFactory.getLogger(Http2Server::class.java)
}

fun createEventLoopGroup(threads: Int = 0): MultithreadEventLoopGroup =
        when {
            Epoll.isAvailable() -> EpollEventLoopGroup(threads)
            KQueue.isAvailable() -> KQueueEventLoopGroup(threads)
            else -> NioEventLoopGroup(threads)
        }

fun serverSocketChannelClass(): KClass<out ServerSocketChannel> =
        when {
            Epoll.isAvailable() -> EpollServerSocketChannel::class
            KQueue.isAvailable() -> KQueueServerSocketChannel::class
            else -> NioServerSocketChannel::class
        }

fun main(args: Array<String>) {
    val logger = Http2Server.LOGGER
    // Configure SSL.
    val sslCtx =
            if (Http2Server.SSL) {
                val provider = if (OpenSsl.isAlpnSupported()) SslProvider.OPENSSL else SslProvider.JDK;
                val ssc = SelfSignedCertificate()
                SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey())
                        .sslProvider(provider)
                        /* NOTE: the cipher filter may not include all ciphers required by the HTTP/2 specification.
                         * Please refer to the HTTP/2 specification for cipher requirements. */
                        .ciphers(Http2SecurityUtil.CIPHERS, SupportedCipherSuiteFilter.INSTANCE)
                        .applicationProtocolConfig(ApplicationProtocolConfig(
                                ApplicationProtocolConfig.Protocol.ALPN,
                                // NO_ADVERTISE is currently the only mode supported by both OpenSsl and JDK providers.
                                ApplicationProtocolConfig.SelectorFailureBehavior.NO_ADVERTISE,
                                // ACCEPT is currently the only mode supported by both OpenSsl and JDK providers.
                                ApplicationProtocolConfig.SelectedListenerFailureBehavior.ACCEPT,
                                ApplicationProtocolNames.HTTP_2,
                                ApplicationProtocolNames.HTTP_1_1))
                        .build()
            } else {
                null
            }

    // Configure the server.
    val group = createEventLoopGroup()
    try {
        val b = ServerBootstrap()
        b.option(ChannelOption.SO_BACKLOG, 1024);
        b.group(group)
                .channel(serverSocketChannelClass().java)
                .handler(LoggingHandler(LogLevel.INFO))
                .childHandler(Http2ServerInitializer(sslCtx))

        val ch = b.bind(Http2Server.PORT).sync().channel();

        logger.info("Open your HTTP/2-enabled web browser and navigate to " +
                (if (Http2Server.SSL) "https" else "http") + "://127.0.0.1:" + Http2Server.PORT + '/');

        ch.closeFuture().sync();
    } finally {
        group.shutdownGracefully();
    }
}