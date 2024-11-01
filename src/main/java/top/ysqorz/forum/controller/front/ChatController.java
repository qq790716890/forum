package top.ysqorz.forum.controller.front;

import org.hibernate.validator.constraints.Length;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import top.ysqorz.forum.common.ResultModel;
import top.ysqorz.forum.common.StatusCode;
import top.ysqorz.forum.common.enumeration.ApplyStatus;
import top.ysqorz.forum.dto.PageData;
import top.ysqorz.forum.dto.resp.UploadResult;
import top.ysqorz.forum.dto.resp.chat.ChatFriendApplyDTO;
import top.ysqorz.forum.dto.resp.chat.ChatListDTO;
import top.ysqorz.forum.dto.resp.chat.ChatUserCardDTO;
import top.ysqorz.forum.dto.resp.chat.NotSignedChatFriendMsg;
import top.ysqorz.forum.po.ChatFriendGroup;
import top.ysqorz.forum.po.ChatFriendMsg;
import top.ysqorz.forum.service.ChatService;
import top.ysqorz.forum.upload.UploadRepository;
import top.ysqorz.forum.upload.uploader.ImageUploader;

import javax.annotation.Resource;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author passerbyYSQ
 * @create 2022-03-24 17:04
 */
@Validated
@Controller
@RequestMapping("/chat")
public class ChatController {
    @Resource
    private ChatService chatService;
//    // 暂时使用阿里云OSS
//    @Resource
//    private UploadRepository aliyunOssRepository;
    @Resource
    private UploadRepository localRepository;

    /**
     * 个人中心私聊页面
     */
    @GetMapping("")
    public String chatPage() {
        return "front/user/chat";
    }

    /**
     * 查找用户和群聊的页面
     */
    @GetMapping("/find")
    public String findPage() {
        return "front/chat/find";
    }

    /**
     * 消息盒子的页面，显示好友申请的通知
     */
    @GetMapping("/msgbox")
    public String msgBoxPage() {
        return "front/chat/msgbox";
    }

    /**
     * 好友聊天历史页面
     */
    @GetMapping("/chatlog")
    public String historyPage() {
        return "front/chat/chatlog";
    }

    /**
     * 查找用户，添加好友。
     *
     * @param keyword 关键词。手机号，邮箱，用户名
     * @param status  状态。all, online, offline
     */
    @ResponseBody
    @GetMapping("/search/user")
    public PageData<ChatUserCardDTO> searchUser(
            @RequestParam(defaultValue = "") @Length(max = 16) String keyword,
            @RequestParam(defaultValue = "all") String status, @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "8") Integer count) {
        return chatService.getChatUserCards(keyword, status, page, Math.max(1, count));
    }

    /**
     * 申请添加好友
     */
    @ResponseBody
    @PostMapping("/friend/apply")
    public StatusCode applyFiend(@NotNull Integer receiverId, Integer friendGroupId, // 允许为空，未分组就为空
                                 @RequestParam(defaultValue = "") String content) {
        return chatService.applyFriend(receiverId, friendGroupId, content);
    }

    /**
     * 好友申请的消息通知列表, 查数据库
     */
    @ResponseBody
    @GetMapping("/notification")
    public PageData<ChatFriendApplyDTO> notification(@RequestParam(defaultValue = "1") Integer page,
                                                     @RequestParam(defaultValue = "8") Integer count) {
        return chatService.getFriendApplyNotifications(page, Math.max(1, count));
    }

    /**
     * 处理好友申请
     */
    @ResponseBody
    @PostMapping("/apply/process")
    public String processFriendApply(@NotNull Integer friendApplyId, Integer friendGroupId, // agree时才需要
                                     @NotNull ApplyStatus status) {
        return chatService.processFriendApply(friendApplyId, friendGroupId, status);
    }

    /**
     * 签收好友申请的通知
     *
     * @param friendApplyIds 用逗号分隔。形如：1,2,3
     */
    @ResponseBody
    @PostMapping("/apply/sign")
    public StatusCode signNotification(@NotBlank String friendApplyIds) {
        chatService.signFriendApplyNotifications(friendApplyIds);
        return StatusCode.SUCCESS;
    }

    /**
     * 查询在线聊天相关的列表信息
     */
    @ResponseBody
    @GetMapping("/list")
    public ResultModel<ChatListDTO> chatList() {
        ResultModel<ChatListDTO> result = ResultModel.success(chatService.getChatList());
        result.setCode(0); // 前端LayIM的限制
        return result;
    }

    /**
     * 新建好友分组
     */
    @ResponseBody
    @PostMapping("/friend/group/create")
    public ChatFriendGroup createFriendGroup(@Length(max = 16) String friendGroupName) {
        return chatService.createFriendGroup(friendGroupName);
    }

    /**
     * 删除好友分组
     */
    @ResponseBody
    @PostMapping("/friend/group/delete")
    public StatusCode deleteFriendGroup(@NotNull Integer friendGroupId) {
        return chatService.deleteFriendGroup(friendGroupId);
    }

    /**
     * 移动好友至指定分组
     *
     * @param friendIds           好友的用户id，用逗号分隔。形如：1,2,3
     * @param targetFriendGroupId 如果缺省，则全部移动至"未分组"
     */
    @ResponseBody
    @PostMapping("/friend/move")
    public StatusCode moveFriend(@NotBlank String friendIds, Integer targetFriendGroupId) {
        return chatService.moveFriendToGroup(friendIds, targetFriendGroupId);
    }

    /**
     * 删除好友
     *
     * @param friendId 好友的用户id，而非好友关系id
     */
    @ResponseBody
    @PostMapping("/friend/delete")
    public StatusCode deleteFriend(@NotNull Integer friendId) {
        return chatService.deleteChatFriend(friendId);
    }

    /**
     * 发送好友私聊消息
     */
    @ResponseBody
    @PostMapping("/friend/msg/send")
    public StatusCode sendChatFriendMsg(@NotNull Integer friendId, @Length(max = 10000) String content,
                                        @NotBlank String channelId) {
        return chatService.sendChatFriendMsg(friendId, content, channelId);
    }

    /**
     * 签收多条聊天消息
     *
     * @param msgIds 需要签收的消息的id。用逗号分隔，形如：id1,id2
     */
    @ResponseBody
    @PostMapping("/friend/msg/sign")
    public StatusCode signChatFriendMsg(@NotBlank String msgIds) {
        chatService.signChatFriendMsg(msgIds);
        return StatusCode.SUCCESS;
    }

    /**
     * 获取未签收的好友私聊消息
     */
    @ResponseBody
    @GetMapping("/friend/msg/not_signed")
    public List<NotSignedChatFriendMsg> getNotSignedChatFriendMsg() {
        return chatService.getNotSignedChatFriendMsg();
    }

    /**
     * 与好友的聊天历史记录
     */
    @ResponseBody
    @GetMapping("/friend/msg/history")
    public PageData<ChatFriendMsg> getChatHistoryWithFriend(@NotNull Integer friendId,
                                                            @RequestParam(defaultValue = "1") Integer page,
                                                            @RequestParam(defaultValue = "8") Integer count) {
        return chatService.getChatHistoryWithFriend(friendId, page, Math.max(1, count));
    }

    /**
     * 聊天图片。由于前端LayIM的数据格式限制，需要专门写一个接口
     */
    @ResponseBody
    @PostMapping("/upload/image")
    public ResultModel<Map<String, Object>> uploadChatImage(@RequestParam(name = "file") @NotNull MultipartFile image)
            throws IOException {
        ImageUploader imageUploader = new ImageUploader(image, localRepository);
        UploadResult uploaded = imageUploader.upload();
        Map<String, Object> data = new HashMap<>();
        data.put("src", uploaded.getUrl()[0]);
        ResultModel<Map<String, Object>> result = ResultModel.success(data);
        result.setCode(0);
        return result;
    }
}
