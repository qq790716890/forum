package top.ysqorz.forum.common.enumeration;

import cn.hutool.core.collection.ConcurrentHashSet;
import lombok.Getter;
import org.apache.commons.lang.StringUtils;
import top.ysqorz.forum.po.User;
import top.ysqorz.forum.service.UserService;
import top.ysqorz.forum.utils.SpringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @ClassName SysUser
 * @Description
 * @Author LZY
 * @Date 2024/10/27 12:50
 */
@Getter
public enum SysUser {


    GPT("GPT@SYSTEM")

    ;
    private static final Set<String> SYS_USERID_SET = new ConcurrentHashSet<>();
    public static final Set<String> SYS_EMAIL_SET = new HashSet<>();
    static{
        SYS_EMAIL_SET.addAll(Arrays.stream(SysUser.values()).map(SysUser::getEmail).collect(Collectors.toList()));
    }
    private final String email;

    SysUser(String email) {
        this.email = email;
    }

}
