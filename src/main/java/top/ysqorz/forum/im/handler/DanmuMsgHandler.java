package top.ysqorz.forum.im.handler;

import io.netty.channel.Channel;
import org.springframework.util.ObjectUtils;
import org.springframework.web.util.HtmlUtils;
import top.ysqorz.forum.dao.DanmuMsgMapper;
import top.ysqorz.forum.im.entity.AsyncInsertTask;
import top.ysqorz.forum.im.entity.ChannelMap;
import top.ysqorz.forum.im.entity.MsgModel;
import top.ysqorz.forum.im.entity.MsgType;
import top.ysqorz.forum.po.DanmuMsg;
import top.ysqorz.forum.po.User;
import top.ysqorz.forum.utils.JsonUtils;
import top.ysqorz.forum.utils.RandomUtils;
import top.ysqorz.forum.utils.SpringUtils;

import java.time.LocalDateTime;

/**
 * @author passerbyYSQ
 * @create 2022-01-15 23:02
 */
public class DanmuMsgHandler extends MsgHandler {
    public DanmuMsgHandler() {
        super(MsgType.DANMU);
    }

    @Override
    protected boolean doHandle0(MsgModel msg, Channel channel, User loginUser) {
        DanmuMsg danmu = JsonUtils.nodeToObj(msg.getDataNode(), DanmuMsg.class);
        if (danmu == null || ObjectUtils.isEmpty(danmu.getContent()) || danmu.getVideoId() == null) {
            return true;
        }
        // TODO check video exists
        int endIndex = Math.min(255, danmu.getContent().length());
        String text = danmu.getContent().substring(0, endIndex); // 如果过长只截取前500个字符
        text = HtmlUtils.htmlEscape(text, "UTF-8"); // 转义，防止XSS攻击
        danmu.setId(RandomUtils.generateUUID())
                .setContent(text)
                .setCreatorId(loginUser.getId())
                .setCreateTime(LocalDateTime.now())
                .setStartMs(Math.max(danmu.getStartMs(), 0)); // 负数时做纠正
        // 推送弹幕
        ChannelMap.ChannelMatcher matcher = (extra, channel1) ->
                extra instanceof Integer && extra.equals(danmu.getVideoId());
        this.channelMap.pushExceptCurr(danmu, channel, matcher);
        // 异步将弹幕插入数据库
        DanmuMsgMapper mapper = SpringUtils.getBean(DanmuMsgMapper.class);
        AsyncInsertTask<DanmuMsg> insertTask = new AsyncInsertTask<>(mapper, danmu);
        this.dbExecutor.execute(insertTask);
        return true;
    }

}
