package org.aaron.kotlin.http2

import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.channel.ChannelInitializer
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.http.HttpMessage
import io.netty.handler.codec.http.HttpObjectAggregator
import io.netty.handler.codec.http.HttpServerCodec
import io.netty.handler.codec.http.HttpServerUpgradeHandler
import io.netty.handler.codec.http.HttpServerUpgradeHandler.UpgradeCodecFactory
import io.netty.handler.codec.http2.CleartextHttp2ServerUpgradeHandler
import io.netty.handler.codec.http2.Http2CodecUtil
import io.netty.handler.codec.http2.Http2ServerUpgradeCodec
import io.netty.handler.ssl.SslContext
import io.netty.util.AsciiString
import io.netty.util.ReferenceCountUtil


class Http2ServerInitializer(
        private val sslCtx: SslContext?,
        private val maxHttpContentLength: Int = 16 * 1024) : ChannelInitializer<SocketChannel>() {

    private val upgradeCodecFactory = UpgradeCodecFactory { protocol ->
        if (AsciiString.contentEquals(Http2CodecUtil.HTTP_UPGRADE_PROTOCOL_NAME, protocol)) {
            Http2ServerUpgradeCodec(HelloWorldHttp2HandlerBuilder().build())
        } else {
            null
        }
    }

    public override fun initChannel(ch: SocketChannel) {
        if (sslCtx != null) {
            configureSsl(ch)
        } else {
            configureClearText(ch)
        }
    }

    /**
     * Configure the pipeline for TLS NPN negotiation to HTTP/2.
     */
    private fun configureSsl(ch: SocketChannel) {
        ch.pipeline().addLast(sslCtx!!.newHandler(ch.alloc()), Http2OrHttpHandler())
    }

    /**
     * Configure the pipeline for a cleartext upgrade from HTTP to HTTP/2.0
     */
    private fun configureClearText(ch: SocketChannel) {
        val p = ch.pipeline()
        val sourceCodec = HttpServerCodec()
        val upgradeHandler = HttpServerUpgradeHandler(sourceCodec, upgradeCodecFactory)
        val cleartextHttp2ServerUpgradeHandler = CleartextHttp2ServerUpgradeHandler(sourceCodec, upgradeHandler,
                HelloWorldHttp2HandlerBuilder().build())

        p.addLast(cleartextHttp2ServerUpgradeHandler)
        p.addLast(object : SimpleChannelInboundHandler<HttpMessage>() {
            @Throws(Exception::class)
            override fun channelRead0(ctx: ChannelHandlerContext, msg: HttpMessage) {
                // If this handler is hit then no upgrade has been attempted and the client is just talking HTTP.
                System.err.println("Directly talking: " + msg.protocolVersion() + " (no upgrade was attempted)")
                val pipeline = ctx.pipeline()
                val thisCtx = pipeline.context(this)
                pipeline.addAfter(thisCtx.name(), null, HelloWorldHttp1Handler("Direct. No Upgrade Attempted."))
                pipeline.replace(this, null, HttpObjectAggregator(maxHttpContentLength))
                ctx.fireChannelRead(ReferenceCountUtil.retain(msg))
            }
        })

        p.addLast(UserEventLogger())
    }

    /**
     * Class that logs any User Events triggered on this channel.
     */
    private class UserEventLogger : ChannelInboundHandlerAdapter() {
        override fun userEventTriggered(ctx: ChannelHandlerContext, evt: Any) {
            println("User Event Triggered: " + evt)
            ctx.fireUserEventTriggered(evt)
        }
    }
}