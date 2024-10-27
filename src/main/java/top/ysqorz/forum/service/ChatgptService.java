package top.ysqorz.forum.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import top.ysqorz.forum.dto.PageData;
import top.ysqorz.forum.po.ChatFriendMsg;
import top.ysqorz.forum.po.ChatgptMessage;

import java.util.List;

/**
 * @author ChatViewer
 */
public interface ChatgptService {


    /**
     * 返回与got的分页会话
     * @param page
     * @param cnt
     * @return
     */
    List<ChatgptMessage> messagesOfConversation(int page, int cnt);

    /**
     * 提问函数
     * @param useToken 使用token / 使用ApiKey
     * @param conversationId 会话id，以得到上下文信息
     * @param prompt 本次提问问题
     * @return 以SSE的形式返回响应
     */
    SseEmitter sendMsgSse(Boolean useToken, Long conversationId, String prompt);

    String sendMsgString(Boolean useToken, String prompt);

}
