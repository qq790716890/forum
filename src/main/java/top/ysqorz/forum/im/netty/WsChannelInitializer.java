package top.ysqorz.forum.im.netty;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;

/**
 * @author passerbyYSQ
 * @create 2021-02-05 21:04
 */
public class WsChannelInitializer extends ChannelInitializer<SocketChannel> {
    @Override
    protected void initChannel(SocketChannel socketChannel) throws Exception {
        ChannelPipeline pipeline = socketChannel.pipeline();
        // websocket基于http协议所需要的编码解码器
        pipeline.addLast(new HttpServerCodec())
                .addLast(new ChunkedWriteHandler())
                // 对httpMessage进行聚合处理(因为http数据是分段的)，聚合成request或response
                .addLast(new HttpObjectAggregator(1024 * 64))
                // 处理握手和心跳

                /**
                 * 说明：
                 *  1. 对于 WebSocket，它的数据是以帧frame 的形式传递的；
                 *  2. 可以看到 WebSocketFrame 下面有6个子类
                 *  3. 浏览器发送请求时： ws://localhost:7000/hello 表示请求的uri
                 *  4. WebSocketServerProtocolHandler 核心功能是把 http协议升级为 ws 协议，保持长连接；
                 *      是通过一个状态码 101 来切换的
                 */
                .addLast(new WebSocketServerProtocolHandler("/ws"))

                // 注意。关于心跳的两个handler在pipeline中的顺序不能更改。且需要放到业务handler的前面
                // IdleStateHandler一旦检查到空闲状态发生，会触发IdleStateEvent事件并且交给下一个handler处理
                // 下一个handler必须实现userEventTriggered方法处理对应事件
                /**
                 * 将 IdleStateHandler 放在 HeartBeatHandler前，就可以在HeartBeatHandler中感知到idle状态
                 *
                 * ps: handler中调用  ChannelHandlerContext.fireUserEventTriggered(evt) 才会传递给下个handler
                 */
                .addLast(new IdleStateHandler(16, 16, 16))
                // 空闲状态检查的handler
                .addLast(new HeartBeatHandler())

                // 自定义的业务的handler
                .addLast(new TextMsgHandler());
    }
}
