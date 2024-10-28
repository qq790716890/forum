package top.ysqorz.forum.im.handler;

import com.fasterxml.jackson.databind.JsonNode;
import top.ysqorz.forum.common.Constant;
import top.ysqorz.forum.dao.ChatFriendMsgMapper;
import top.ysqorz.forum.im.entity.*;
import top.ysqorz.forum.middleware.redis.SystemUserCache;
import top.ysqorz.forum.po.ChatFriendMsg;
import top.ysqorz.forum.service.RedisService;
import top.ysqorz.forum.utils.JsonUtils;
import top.ysqorz.forum.utils.SpringUtils;

import java.util.Set;

/**
 * @author passerbyYSQ
 * @create 2022-04-03 16:52
 */
public class ChatFriendMsgHandler extends NonFunctionalMsgHandler<ChatFriendMsg> {
    private final SystemUserCache systemUserCache;
    public ChatFriendMsgHandler() {
        super(MsgType.CHAT_FRIEND, ChannelType.CHAT);
        this.systemUserCache = SpringUtils.getBean(SystemUserCache.class);
    }

    @Override
    protected Set<String> queryServersChannelLocated(ChatFriendMsg msg) {
        RedisService redisService = SpringUtils.getBean(RedisService.class);
        return redisService.getWsServers(this.getChannelType(), msg.getReceiverId().toString());
    }

    protected boolean checkMsgType(MsgModel msg) {
        try {
            JsonNode jsonNode = JsonUtils.objToNode(msg.getData());
            Integer receiverId = jsonNode.get("receiverId").asInt();

            Integer gptId = systemUserCache.getSystemUserByEmail(Constant.GPT_EMAIL).getId();
            if (receiverId.equals(gptId)){
                msg.setMsgType(MsgType.CHAT_SYSTEM.name());
                msg.setChannelType(ChannelType.GPT.name());
                return false;
            }
            return super.checkMsgType(msg);
        }catch (Throwable e){
            // omit the error
        }
        return false;
    }

    @Override
    protected void doPush(ChatFriendMsg msg, String sourceChannelId) {
        this.channelMap.pushToGroup(this.getMsgType(), msg, sourceChannelId, msg.getReceiverId().toString());
    }

    @Override
    protected AsyncInsertTask createAsyncInsertTask(ChatFriendMsg msg) {
        ChatFriendMsgMapper mapper = SpringUtils.getBean(ChatFriendMsgMapper.class);
        return new AsyncInsertTask<>(mapper, msg);
    }
}
