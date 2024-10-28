package top.ysqorz.forum.im.handler;

import cn.hutool.core.lang.UUID;
import com.fasterxml.jackson.databind.JsonNode;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.springframework.stereotype.Service;
import top.ysqorz.forum.common.Constant;
import top.ysqorz.forum.dao.ChatFriendMsgMapper;
import top.ysqorz.forum.im.IMUtils;
import top.ysqorz.forum.im.entity.AsyncInsertTask;
import top.ysqorz.forum.im.entity.ChannelType;
import top.ysqorz.forum.im.entity.MsgModel;
import top.ysqorz.forum.im.entity.MsgType;
import top.ysqorz.forum.middleware.redis.SystemUserCache;
import top.ysqorz.forum.po.ChatFriendMsg;
import top.ysqorz.forum.po.User;
import top.ysqorz.forum.service.ChatgptService;
import top.ysqorz.forum.service.RedisService;
import top.ysqorz.forum.shiro.ShiroUtils;
import top.ysqorz.forum.utils.JsonUtils;
import top.ysqorz.forum.utils.SpringUtils;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * @author passerbyYSQ
 * @create 2022-04-03 16:52
 */

public class ChatGPTMsgHandler extends NonFunctionalMsgHandler<ChatFriendMsg> {

    private final ChatgptService chatgptService;

    private final SystemUserCache systemUserCache;

    public ChatGPTMsgHandler() {
        super(MsgType.CHAT_SYSTEM, ChannelType.GPT);
        this.chatgptService = SpringUtils.getBean(ChatgptService.class);
        this.systemUserCache = SpringUtils.getBean(SystemUserCache.class);
    }

    @Override
    protected Set<String> queryServersChannelLocated(ChatFriendMsg msg) {
        RedisService redisService = SpringUtils.getBean(RedisService.class);
        return redisService.getWsServers(ChannelType.CHAT, msg.getSenderId().toString());
    }

    @Override
    protected void doPush(ChatFriendMsg msg, String sourceChannelId) {
        // GPT 消息，直接处理后返回
        String prompt = msg.getContent();

        String gptAnswer = chatgptService.sendMsgString(true, prompt);

        Integer gptId = systemUserCache.getSystemUserByEmail(Constant.GPT_EMAIL).getId();

        ChatFriendMsg ansChatMsg = new ChatFriendMsg().
                setContent(gptAnswer).
                setSenderId(gptId).
                setReceiverId(msg.getSenderId()).
                setId(UUID.randomUUID().toString()).
                setSignFlag(msg.getSignFlag()).
                setCreateTime(LocalDateTime.now());
        MsgModel newMsg = new MsgModel(MsgType.CHAT_FRIEND, ChannelType.CHAT, ansChatMsg);
        MsgCenter.getInstance().remoteDispatch(newMsg, "-1", ShiroUtils.getToken());

    }

    @Override
    protected AsyncInsertTask createAsyncInsertTask(ChatFriendMsg msg) {
        ChatFriendMsgMapper mapper = SpringUtils.getBean(ChatFriendMsgMapper.class);
        return new AsyncInsertTask<>(mapper, msg);
    }
}
