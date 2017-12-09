package org.aaron.kotlin.http2

import io.netty.buffer.ByteBufUtil
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.http.DefaultFullHttpResponse
import io.netty.handler.codec.http.FullHttpRequest
import io.netty.handler.codec.http.HttpHeaderNames.*
import io.netty.handler.codec.http.HttpHeaderValues
import io.netty.handler.codec.http.HttpResponseStatus.CONTINUE
import io.netty.handler.codec.http.HttpResponseStatus.OK
import io.netty.handler.codec.http.HttpUtil
import io.netty.handler.codec.http.HttpVersion.HTTP_1_1

class HelloWorldHttp1Handler(
        private val establishApproach: String) : SimpleChannelInboundHandler<FullHttpRequest>() {

    @Throws(Exception::class)
    public override fun channelRead0(ctx: ChannelHandlerContext, req: FullHttpRequest) {
        if (HttpUtil.is100ContinueExpected(req)) {
            ctx.write(DefaultFullHttpResponse(HTTP_1_1, CONTINUE))
        }
        val keepAlive = HttpUtil.isKeepAlive(req)

        val content = ctx.alloc().buffer()
        content.writeBytes(HelloWorldHttp2Handler.RESPONSE_BYTES.duplicate())
        ByteBufUtil.writeAscii(content, " - via " + req.protocolVersion() + " (" + establishApproach + ")")

        val response = DefaultFullHttpResponse(HTTP_1_1, OK, content)
        response.headers().set(CONTENT_TYPE, "text/plain; charset=UTF-8")
        response.headers().setInt(CONTENT_LENGTH, response.content().readableBytes())

        if (!keepAlive) {
            ctx.write(response).addListener(ChannelFutureListener.CLOSE)
        } else {
            response.headers().set(CONNECTION, HttpHeaderValues.KEEP_ALIVE)
            ctx.write(response)
        }
    }

    @Throws(Exception::class)
    override fun channelReadComplete(ctx: ChannelHandlerContext) {
        ctx.flush()
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        cause.printStackTrace()
        ctx.close()
    }
}