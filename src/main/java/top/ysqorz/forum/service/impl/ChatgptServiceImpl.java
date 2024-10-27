package top.ysqorz.forum.service.impl;

//import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.plexpt.chatgpt.ChatGPT;
import com.plexpt.chatgpt.ChatGPTStream;
import com.plexpt.chatgpt.entity.chat.ChatCompletion;
import com.plexpt.chatgpt.entity.chat.ChatCompletionResponse;
import com.plexpt.chatgpt.entity.chat.Message;
import com.plexpt.chatgpt.util.Proxys;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.AuthorizationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import top.ysqorz.forum.common.exception.ServiceFailedException;
import top.ysqorz.forum.dao.ChatFriendMapper;
import top.ysqorz.forum.dao.UserMapper;
import top.ysqorz.forum.event.GptEventSourceListener;
import top.ysqorz.forum.dao.ChatgptMessageMapper;
import top.ysqorz.forum.po.ChatFriend;
import top.ysqorz.forum.po.ChatgptMessage;
import top.ysqorz.forum.po.User;
import top.ysqorz.forum.service.ChatService;
import top.ysqorz.forum.service.ChatgptService;
import top.ysqorz.forum.service.UserService;
import top.ysqorz.forum.shiro.LoginUser;
import top.ysqorz.forum.shiro.ShiroUtils;

import javax.annotation.Resource;
import java.net.Proxy;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static top.ysqorz.forum.common.Constant.GPT_EMAIL;
import static top.ysqorz.forum.common.StatusCode.GPT_FRIEND_ALREADY_EXIST;
import static top.ysqorz.forum.common.StatusCode.NO_Token_Count;

/**
 * ChatGPT消息服务
 * @author ChatViewer
 */
@Slf4j
@Service
public class ChatgptServiceImpl implements ChatgptService {

    @Resource
    UserService userService;

    @Resource
    ChatService chatService;

    @Resource
    ChatFriendMapper chatFriendMapper;



    /**
     * ChatGPT的APIKey，获取方法：Clash科学上网 + VISA卡 + ChatGPT官网绑定卡号，得到API Key
     */
    @Value("${my-conf.gpt-key}")
    private String apiKey;

    /**
     * 代表消息发送方向
     */
    private static final int DIRECTION_QUESTION = 0;
    private static final int DIRECTION_ANSWER = 1;

    /**
     * 发送消息时加入的上下文信息数
     */
    private static final int CONTEXT_MESSAGE_NUM = 2;

    @Override
    public List<ChatgptMessage> messagesOfConversation(int page, int cnt) {
        // 1、校验会话是否属于当前登录用户
        Integer userId = ShiroUtils.getUserId();

        if (userId == null) {
            throw new AuthorizationException("用户未登录！不能查询会话！");
        }

        // 返回当前用户与 gpt 的聊天记录，
        // 2、查询conversation的最新showItem条会话
        List<ChatgptMessage> chatHistoryWithGPT = chatService.getChatHistoryWithGPT(page,cnt);

        return chatHistoryWithGPT;
    }

    /**
     * 将本项目的数据类型ChatgptMessage转为SDK需要的Message类格式，可以Ctrl+戳戳查看Message类型
     * @param src 数据库格式消息列表
     * @return SDK所需格式消息列表
     */
    private List<Message> transform(List<ChatgptMessage> src) {
        List<Message> tgt = new ArrayList<>();
        // 对src列表中的每条消息
        for (ChatgptMessage message: src) {
            // 根据消息类型调用对应的方法，向tgt中加入所需类型消息
            tgt.add(message.getMessageDirection() == DIRECTION_QUESTION ?
                    // 用户发送的消息
                    Message.of(message.getContent())
                    // GPT发送的消息
                    : Message.ofAssistant(message.getContent()));
        }
        return tgt;
    }


    @Override
    public SseEmitter sendMsgSse(Boolean useToken, Long conversationId, String prompt) {

        // 获取SecurityContextHolder中的用户id, 判断该会话是否属于当前用户
        Integer userId = ShiroUtils.getUserId();
        if (userId == null) {
            throw new AuthorizationException("用户未登录！不能向GPT发送消息！");
        }

        // 得到用户
        LoginUser user = ShiroUtils.getLoginUser();
        User userById = userService.getUserById(userId);

        // 1、设置api key
        String key;
        if (useToken) {
            // 如果选择了token支付方式，检查token数
            if (user.getGptTokenCount() > 0 ) {
                key = apiKey;
            }
            else {
                throw new ServiceFailedException(NO_Token_Count);
            }
        }
        else {
            // 使用用户账户设置的API Key
            key = user.getUserApiKey();
        }

        // 2、设置代理
        Proxy proxy = Proxys.http("127.0.0.1", 7890);

        // 3、借助SDK工具，实例化ChatGPTStream工具类对象
        ChatGPTStream chatgptStream = ChatGPTStream.builder()
                .timeout(50)
                .apiKey(key)
                .proxy(proxy)
                .apiHost("https://api.chatanywhere.tech")
                .build()
                .init();

        // 4、实例化流式输出类，设置监听，从而在所有消息输出完成后回调
        SseEmitter sseEmitter = new SseEmitter(-1L);
        GptEventSourceListener listener = new GptEventSourceListener(sseEmitter);

        // 5、加入历史消息记录，提供上下文信息
        // Message为SDK包中的数据结构
        List<Message> messages = conversationId != null ?
                // 第二个参数控制加入的上下文信息数，transform将ChatgptMessage转为SDK需要的格式
                transform(messagesOfConversation(1, CONTEXT_MESSAGE_NUM))
                : new ArrayList<>();

        // 6、加入本次提问问题
        Message message = Message.of(prompt);
        messages.add(message);

        // 7、设置完成时的回调函数
        listener.setOnComplete(msg -> {
            // 保存历史信息
            saveMessage(conversationId, prompt, DIRECTION_QUESTION);
            saveMessage(conversationId, msg, DIRECTION_ANSWER);
            // 如果使用token支付，扣除token，更新账户信息
            if (useToken) {
                user.setGptTokenCount(user.getGptTokenCount() - 1);
                userService.updateUserById(userById);
            }
        });

        // 8、提问
        chatgptStream.streamChatCompletion(messages, listener);

        return sseEmitter;
    }


    @Override
    public String sendMsgString(Boolean useToken, Long conversationId, String prompt) {
        // 获取SecurityContextHolder中的用户id, 判断该会话是否属于当前用户
        Integer userId = ShiroUtils.getUserId();
        if (userId == null) {
            throw new AuthorizationException("用户未登录！不能向GPT发送消息！");
        }

        // 得到用户
        LoginUser user = ShiroUtils.getLoginUser();
        User userById = userService.getUserById(userId);

        // 1、设置api key
        String key;
        if (useToken) {
            // 如果选择了token支付方式，检查token数
            if (user.getGptTokenCount() > 0 ) {
                key = apiKey;
            }
            else {
                throw new ServiceFailedException(NO_Token_Count);
            }
        }
        else {
            // 使用用户账户设置的API Key
            key = user.getUserApiKey();
        }

        ChatGPT chatGPT = ChatGPT.builder()
                .apiKey(key)
                .timeout(900)
                .apiHost("https://api.chatanywhere.tech")
                .build()
                .init();

//        Message system = Message.ofSystem("你现在是一个诗人，专门写七言绝句");
        Message message = Message.of(prompt);

        ChatCompletion chatCompletion = ChatCompletion.builder()
                .model(ChatCompletion.Model.GPT_3_5_TURBO)
                .messages(Arrays.asList(message))
                .maxTokens(3000)
                .temperature(0.9)
                .build();
        ChatCompletionResponse response = chatGPT.chatCompletion(chatCompletion);

//        // 保存历史信息
//        saveMessage(conversationId, prompt, DIRECTION_QUESTION);
//        saveMessage(conversationId, msg, DIRECTION_ANSWER);
        // 如果使用token支付，扣除token，更新账户信息
        if (useToken) {
            user.setGptTokenCount(user.getGptTokenCount() - 1);
            userService.updateUserById(userById);
        }

        return response.toPlainString();
    }


    /**
     * 将消息保存至数据库中
     * @param conversationId 会话id
     * @param msg 消息内容
     * @param messageDirection 消息方向
     */
    public void saveMessage(Long conversationId, String msg, Integer messageDirection) {
        ChatgptMessage message = new ChatgptMessage();
        message.setContent(msg);
        message.setMessageDirection(messageDirection);
        message.setConversationId(conversationId);
        message.setCreateTime(LocalDateTime.now());
//        save(message);
    }

    public static void main(String[] args) {
        //国内需要代理 国外不需要
//        Proxy proxy = Proxys.http("127.0.0.1", 1080);

        ChatGPT chatGPT = ChatGPT.builder()
                .apiKey("sk-tTlYqebmSKBKHgtA1SJnr6Y9eloIS4dVM5H1f0LFCnGlTCKC")
//                .proxy(proxy)
                .timeout(900)
                .apiHost("https://api.chatanywhere.tech")
                .build()
                .init();

        Message system = Message.ofSystem("你现在是一个诗人，专门写七言绝句");
        Message message = Message.of("写一段七言绝句诗，题目是：火锅！");

        ChatCompletion chatCompletion = ChatCompletion.builder()
                .model(ChatCompletion.Model.GPT_3_5_TURBO)
                .messages(Arrays.asList(system, message))
                .maxTokens(3000)
                .temperature(0.9)
                .build();
        ChatCompletionResponse response = chatGPT.chatCompletion(chatCompletion);
        System.out.println(response.toPlainString());

    }


}
