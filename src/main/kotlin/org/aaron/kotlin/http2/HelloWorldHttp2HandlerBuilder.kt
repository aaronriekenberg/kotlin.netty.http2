package org.aaron.kotlin.http2

import io.netty.handler.codec.http2.*
import io.netty.handler.logging.LogLevel.INFO

class HelloWorldHttp2HandlerBuilder : AbstractHttp2ConnectionHandlerBuilder<HelloWorldHttp2Handler, HelloWorldHttp2HandlerBuilder>() {
    init {
        frameLogger(logger)
    }

    public override fun build(): HelloWorldHttp2Handler {
        return super.build()
    }

    override fun build(decoder: Http2ConnectionDecoder, encoder: Http2ConnectionEncoder,
                       initialSettings: Http2Settings): HelloWorldHttp2Handler {
        val handler = HelloWorldHttp2Handler(decoder, encoder, initialSettings)
        frameListener(handler)
        return handler
    }

    companion object {
        private val logger = Http2FrameLogger(INFO, HelloWorldHttp2Handler::class.java)
    }
}