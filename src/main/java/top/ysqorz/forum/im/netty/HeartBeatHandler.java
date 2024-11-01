package top.ysqorz.forum.im.netty;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;
import top.ysqorz.forum.im.IMUtils;

/**
 * 之所以不继承SimpleChannelInboundHandler方法，而实现它的父类ChannelInboundHandlerAdapter
 * 是因为我们的心跳保持不需要实现channelRead0()这个方法
 *
 * @author passerbyYSQ
 * @create 2021-02-08 22:11
 */
@Slf4j
public class HeartBeatHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ctx.channel().attr(IMUtils.ALL_IDLE_KEY).set(0); // 通道一旦建立，设置空闲次数为0，否则第一次get出来为null
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) { // 空闲状态
            IdleStateEvent event = (IdleStateEvent) evt;
            Channel channel = ctx.channel();
            String channelId = channel.id().asLongText();

            if (event.state() == IdleState.ALL_IDLE) {
                // 这里判断出 idle 状态了，没有执行 fireUserEventTriggered 就不会传递到后面的 handler
                log.info("channel进入[读写]空闲状态：{}", channelId);
                Integer allIdleCount = channel.attr(IMUtils.ALL_IDLE_KEY).get();
                if (Integer.valueOf(3).equals(allIdleCount)) { // >=3
                    log.info("channel[读写]空闲状态超过3次，已关闭：{}", channelId);
                    channel.close();
                } else { // <3
                    channel.attr(IMUtils.ALL_IDLE_KEY).compareAndSet(allIdleCount, allIdleCount + 1);
                }
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }
}
