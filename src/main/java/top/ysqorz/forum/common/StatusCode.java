package top.ysqorz.forum.common;

/**
 * 2000 - 成功处理请求
 * 3*** - 重定向，需要进一步的操作已完成请求
 * 4*** - 客户端错误，请求参数错误，语法错误等等
 * 5*** - 服务器内部错误
 * ...
 *
 * @author passerbyYSQ
 * @create 2020-11-02 16:26
 */
// 不加上此注解，Jackson将对象序列化为json时，直接将枚举类转成它的名字
//@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum StatusCode {
    SUCCESS(2000, "成功"),
    SUCCESS_BIND(2001, "用户绑定成功"),

    // 授权相关
    AUTHORIZATION_FAILED(403, "无权限访问"), // ！！！

    // 服务器内部错误
    SERVER_ERROR(5000, "服务器异常"),
    NO_PERM(5001, "无权限操作"),

    // 参数相关
    PARAM_NOT_COMPLETED(6001, "参数缺失"),
    PARAM_INVALID(400, "参数无效"), // ！！！
    FILE_TYPE_INVALID(6003, "非法文件类型"),
    FILE_SIZE_EXCEEDED(6004, "文件大小超出限制"),
    CAPTCHA_INVALID(6005, "验证码错误"),
    CAPTCHA_EXPIRED(6005, "验证码已过期"),
    DO_NOT_REPEAT_OPERATE(6006, "请勿重复操作"),
    BASE64_INVALID(6007, "base64编码错误"),
    END_TIME_BEFORE_START_TIME(6008, "结束时间早于起始时间"),

    // 认证相关
    TOKEN_INVALID(6000, "无效token"),
    TOKEN_EXPIRED(6001, "token已过期，请重新登录"),
    ACCOUNT_INCORRECT(6002, "账号错误"),
    CREDENTIAL_INCORRECT(6003, "密码错误"), // 凭证错误
    AUTHENTICATION_FAILED(6004, "尚未登录或登录过期"),
    PASSWORD_NOT_EQUAL(6005, "两次输入密码不一致"),
    OAUTH_FAILED(6006, "oauth授权登录失败"),
    CSRF_ATTACK(6007, "正在受到CSRF攻击"),

    // 用户相关
    USERNAME_EXIST(6101, "用户名已存在"),
    USER_NOT_EXIST(6102, "用户不存在"), // 可能是userId错误
    USER_ID_INVALID(6103, "用户id非法"),
    USER_NOT_BLOCK(6103, "用户当前不处于封禁状态"),
    USER_NOT_LOGIN(6104, "用户未登录"),
    PHONE_IS_EXIST(6105, "该手机号码已被绑定"),
    EMAIL_IS_EXIST(6106, "该邮箱已被绑定"),
    ACCOUNT_OR_PASSWORD_INCORRECT(6107, "账号或密码错误"),
    PHONE_INCORRECT(6108, "请输入11位的正确手机号码"),
    INFO_UPDATE_FAILED(6109, "修改个人信息失败"),
    DECRYPT_FAILED(6110, "解密失败"),

    // 账户相关
    TOKEN_IS_MISSING(6200, "token缺失"),
    FORCED_OFFLINE(6201, "当前账号在异地登录，您已被挤下线"),
    TOKEN_IS_EXPIRED(6202, "token已过期，请重新登录"),
    TOKEN_IS_INVALID(6203, "无效token"),
    EMAIL_INCORRECT(6204, "邮箱错误或者邮箱尚未注册"),
    PASSWORD_INCORRECT(6205, "密码错误"),
    ACCOUNT_IS_BOUND(6206, "该社交账号已被其他账号绑定"),
    EMAIL_NOT_SET(6206, "请先绑定邮箱"),
    NOT_LOGIN_REPEATEDLY(6207, "请勿重复登录"),
    UNSUPPORTED_OAUTH_AUTHORIZATION(6208, "不支持的第三方授权"),
    NOT_ACTIVATED(6209, "邮箱还未激活"),
    //REFERER_ERROR(6207, "referer错误"),

    // 权限相关
    AUTHORITY_NAME_EXIST(6300, "权限名称已存在"),
    AUTHORITY_NOT_EXIST(6301, "权限不存在"),
    AUTHORITY_UPDATE_FAILED(6302, "您未做任何更新"),
    AUTHORITY_PID_NOT_VALID(6303, "新的父权限不能为当前权限及子孙权限"),
    AUTHORITY_DEL_FAILED(6301, "非叶子节点的权限无法直接删除"),

    // 角色相关的
    ROLE_NOT_EXIST(6400, "角色不存在"),

    // 话题相关
    TOPIC_NOT_EXIST(6500, "话题不存在"),
    TOPIC_ARCHIVED(6501, "话题已归档"),

    // 帖子相关
    POST_NOT_EXIST(6600, "帖子不存在"),
    POST_LOCKED(6601, "帖子被锁定，无法评论"),

    // 评论相关
    FIRST_COMMENT_NOT_EXIST(6700, "一级评论不存在"),
    SECOND_COMMENT_NOT_EXIST(6701, "二级评论不存在"),
    COMMENT_TYPE_INVALID(6702, "不合法的评论类型"),

    // im相关
    CHANNEL_NOT_EXIST(6800, "channel不存在"),
    NOT_SUPPORT_FUNC_TYPE(6801, "不支持功能类型的消息"),
    REMOTE_FORWARD_MSG_FAILED(6802, "远程调用API转发消息失败"),
    CHAT_ALREADY_FRIEND(6803, "已经是好友"),
    CHAT_FRIEND_APPLY_PROCESSED(6804, "好友申请已被处理，请重新发送申请"),
    CHAT_FRIEND_GROUP_INVALID(6805, "好友分组id非法"),
    CHAT_FRIEND_APPLY_INVALID(6806, "好友申请id非法"),
    CHAT_FRIEND_GROUP_EXIST(6807, "好友分组已存在"),
    CHAT_FRIEND_NOT_EXIST(6808, "好友不存在"),

    // 文件相关
    VIDEO_NOT_EXIST(6900, "视频不存在"),
    FILE_NOT_EXIST(6901, "文件不存在"),

    // gpt相关
    NO_Token_Count(7000, "用户余额不足"),
    GPT_FRIEND_ALREADY_EXIST(7001, "已经有GPT好友了"),


    ;
    // 状态码数值
    private Integer code;
    // 状态码描述信息
    private String msg;

    StatusCode(Integer code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    /**
     * 根据业务状态码获取对应的描述信息
     * @param code      业务状态码
     */
    public static String getMsgByCode(Integer code) {
        for (StatusCode status : StatusCode.values()) {
            if (status.code.equals(code)) {
                return status.msg;
            }
        }
        return null;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}
