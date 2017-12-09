package org.aaron.kotlin.http2

import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.HttpObjectAggregator
import io.netty.handler.codec.http.HttpServerCodec
import io.netty.handler.ssl.ApplicationProtocolNames
import io.netty.handler.ssl.ApplicationProtocolNegotiationHandler

class Http2OrHttpHandler : ApplicationProtocolNegotiationHandler(ApplicationProtocolNames.HTTP_1_1) {

    companion object {
        private val MAX_CONTENT_LENGTH = 1024 * 100
    }

    override fun configurePipeline(ctx: ChannelHandlerContext, protocol: String?) {
        when {
            ApplicationProtocolNames.HTTP_2.equals(protocol) -> ctx.pipeline().addLast(HelloWorldHttp2HandlerBuilder().build())
            ApplicationProtocolNames.HTTP_1_1.equals(protocol) -> ctx.pipeline().addLast(HttpServerCodec(),
                    HttpObjectAggregator(MAX_CONTENT_LENGTH),
                    HelloWorldHttp1Handler("ALPN Negotiation"))
            else -> throw IllegalStateException("unknown protocol: " + protocol)
        }
    }

}