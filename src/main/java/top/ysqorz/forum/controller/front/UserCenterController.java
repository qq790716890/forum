package top.ysqorz.forum.controller.front;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.ObjectUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import top.ysqorz.forum.common.Constant;
import top.ysqorz.forum.common.StatusCode;
import top.ysqorz.forum.dto.PageData;
import top.ysqorz.forum.dto.req.CheckUserDTO;
import top.ysqorz.forum.dto.req.RegisterDTO;
import top.ysqorz.forum.dto.resp.MessageListDTO;
import top.ysqorz.forum.dto.resp.SimpleUserDTO;
import top.ysqorz.forum.dto.resp.UploadResult;
import top.ysqorz.forum.po.User;
import top.ysqorz.forum.service.CollectService;
import top.ysqorz.forum.service.MessageService;
import top.ysqorz.forum.service.PostService;
import top.ysqorz.forum.service.UserService;
import top.ysqorz.forum.shiro.ShiroUtils;
import top.ysqorz.forum.upload.UploadRepository;
import top.ysqorz.forum.upload.uploader.ImageUploader;

import javax.annotation.Resource;
import javax.validation.constraints.NotBlank;
import java.io.IOException;

/**
 *
 * 用户个人信息修改
 *
 * @author ligouzi
 * @create 2021-06-30 13:12
 */
@Controller
@RequestMapping("/user/center")
@Validated
public class UserCenterController {

    @Resource
    private UserService userService;
    // 为了方便不同组员开发，使用阿里云OSS
//    @Resource
//    private UploadRepository aliyunOssRepository;
    @Resource
    private UploadRepository localRepository;
    @Resource
    private MessageService messageService;
    @Resource
    private CollectService collectService;

    @Resource
    private PostService postService;

    /**
     * 跳转到我的主页
     */
    @GetMapping("/home")
    public String personalHomePage() {
        return "forward:/user/home/" + ShiroUtils.getUserId();
    }

    /**
     * 跳转到用户中心
     */
    @GetMapping("/index")
    public String indexPage(Model model) {
        SimpleUserDTO information = userService.getHomeInformationById(ShiroUtils.getUserId());
        model.addAttribute("information", information);
        int myPostCnt = postService.countPostListByCreatorId(ShiroUtils.getUserId());
        int myCollectCnt = collectService.countCollectByUserId(ShiroUtils.getUserId());
        model.addAttribute("myPostCnt",myPostCnt);
        model.addAttribute("myCollectCnt",myCollectCnt);
        return "front/user/index";
    }

    /**
     * 跳转到第三方账号绑定页面，此页面必须登录才能进
     */
    @GetMapping("/set")
    public String bindOauthAccountPage(Model model) {
        User user = userService.getUserById(ShiroUtils.getUserId());
        user.setEmail(encryption(user.getEmail()));
        user.setPhone(encryption(user.getPhone()));
        model.addAttribute("user", user);
        return "front/user/set";
    }

    @PostMapping("/updatePassword")
    @ResponseBody
    public StatusCode updatePassword(@Validated(RegisterDTO.UpdatePassword.class)
                                                  RegisterDTO dto) {
        if (!dto.getNewPassword().equals(dto.getRePassword())) {
            return StatusCode.PASSWORD_NOT_EQUAL; // 两次密码不一致
        }
        User me = userService.getUserById(ShiroUtils.getUserId());

        String encryptPwd = userService.encryptLoginPwd(dto.getPassword(), me.getLoginSalt());
        if (!me.getEmail().equals(dto.getEmail()) ||
                !me.getPassword().equals(encryptPwd)) {
            return StatusCode.ACCOUNT_OR_PASSWORD_INCORRECT; // 邮箱或密码错误
        }

        String newEncryptPwd = userService.encryptLoginPwd(dto.getNewPassword(), me.getLoginSalt());
        User record = new User();
        record.setId(me.getId())
                .setPassword(newEncryptPwd);
        userService.updateUserById(record);
        return StatusCode.SUCCESS;
    }

    @PostMapping("/updateInfo")
    @ResponseBody
    public StatusCode updatePersonalInfo(User user) {
        user.setId(ShiroUtils.getUserId());
        int cnt = userService.updateUserById(user);
        if (cnt != 1) {
            return StatusCode.INFO_UPDATE_FAILED;
        }
        ShiroUtils.clearCurrentUserAuthenticationCache(); // 清除缓存
        return StatusCode.SUCCESS;
    }

    @PostMapping("/uploadFaceImage")
    @ResponseBody
    public UploadResult uploadFaceImage(@NotBlank String imageBase64) throws IOException {
        ImageUploader imageUploader = new ImageUploader(imageBase64, localRepository);
        UploadResult uploadResult = imageUploader.uploadBase64();
        User record = new User();
        record.setId(ShiroUtils.getUserId())
                .setPhoto(uploadResult.getUrl()[0]);
        userService.updateUserById(record);
        ShiroUtils.clearCurrentUserAuthenticationCache(); // 清除缓存
        return uploadResult;
    }

    /**
     * 邮箱、手机加密显示
     */
    private String encryption(String str) {
        int len;
        if (ObjectUtils.isEmpty(str) || (len = str.length()) < 6) {
            return str;
        }
        StringBuilder sbd = new StringBuilder(str);
        StringBuilder mid = new StringBuilder();
        for (int i = 0; i < len - 6; i++) {
            mid.append("*");
        }
        return sbd.replace(3, len - 3, mid.toString()).toString();
    }

    /**
     * 跳转到我的消息
     */
    @GetMapping("/message")
    public String messagePage() {
        return "front/user/message";
    }


    /**
     * 获取消息列表
     *
     * @param limit
     * @param page
     * @param conditions
     */
    @ResponseBody
    @GetMapping("/message/list")
    public PageData<MessageListDTO> megList(@RequestParam(defaultValue = "3") Integer limit,
                                            @RequestParam(defaultValue = "1") Integer page,
                                            Integer conditions) {
        return messageService.getMegList(page, Math.max(1, limit), conditions);
    }

    /**
     * 清空所有消息
     */
    @ResponseBody
    @GetMapping("/message/clearAll")
    public StatusCode clearAllMeg() {
        int cnt = messageService.clearAllMeg();
        return cnt >= 0 ? StatusCode.SUCCESS : StatusCode.SERVER_ERROR;
    }


    /**
     * 手机绑定检验
     */
    @ResponseBody
    @PostMapping("changeUserPhone")
    public StatusCode changeUserPhone(@Validated CheckUserDTO checkUser) {
        //检查用户账号密码是否正确
        StatusCode check = userService.checkUser(checkUser);
        if (!check.equals(StatusCode.SUCCESS)) {
            return check;
        }
        //手机格式不正确
        if (!checkUser.getNewPhone().matches(Constant.REGEX_PHONE)) {
            return StatusCode.PHONE_INCORRECT;
        }
        //检查手机是否已被绑定
        if (userService.checkBind(checkUser.getNewPhone(), "phone")) {
            return StatusCode.PHONE_IS_EXIST;
        }
        User record = new User();
        record.setId(ShiroUtils.getUserId())
            .setPhone(checkUser.getNewPhone());
        userService.updateUserById(record);
        ShiroUtils.clearCurrentUserAuthenticationCache(); // 清除缓存
        return StatusCode.SUCCESS;
    }

    /**
     * 邮箱绑定检验
     */
    @ResponseBody
    @PostMapping("changeUserEmail")
    public StatusCode changeUserEmail(@Validated CheckUserDTO checkUser) {
        int myId = ShiroUtils.getUserId();
        User me = userService.getUserById(myId);
        User record = new User();
        record.setId(myId); // ！！
        //判断用户是否已经有邮箱绑定
        if (ObjectUtils.isEmpty(me.getEmail())) { // 设置邮箱和密码
            //检查两次输入的密码是否一致
            if (!checkUser.getCheckPassword().equals(checkUser.getRePassword())) {
                return StatusCode.PASSWORD_NOT_EQUAL;
            }
            //检查邮箱是否已被绑定
            if (userService.checkBind(checkUser.getOldEmail(), "email")) {
                return StatusCode.EMAIL_IS_EXIST;
            }
            record.setEmail(checkUser.getOldEmail())
                    .setPassword(userService.encryptLoginPwd(
                            checkUser.getCheckPassword(), me.getLoginSalt()));

        } else { // 修改邮箱
            //检查用户账号密码是否正确
            StatusCode code = userService.checkUser(checkUser);
            if (!code.equals(StatusCode.SUCCESS)) {
                return code;
            }
            //检查邮箱是否已被绑定
            if (userService.checkBind(checkUser.getNewEmail(), "email")) {
                return StatusCode.EMAIL_IS_EXIST;
            }
            record.setEmail(checkUser.getNewEmail());
        }
        userService.updateUserById(record);
        ShiroUtils.clearCurrentUserAuthenticationCache(); // 清除缓存
        return StatusCode.SUCCESS;
    }

    /**
     * QQ、Gitee、百度解绑检验
     */
    @ResponseBody
    @PostMapping("Unbundling")
    public StatusCode unbind(@Validated CheckUserDTO checkUser) {
        //检查用户账号密码是否正确
        StatusCode check = userService.checkUser(checkUser);
        if (!check.equals(StatusCode.SUCCESS)) {
            return check;
        }
        User record = new User();
        record.setId(ShiroUtils.getUserId());
        if (checkUser.getOauth2App() != null) {
            switch (checkUser.getOauth2App()) {
                case QQ:      record.setQqId("");     break;
                case GITEE:   record.setGiteeId("");  break;
                case BAIDU:   record.setBaiduId("");  break;
            }
            userService.updateUserById(record);
        }
        return StatusCode.SUCCESS;
    }

}
