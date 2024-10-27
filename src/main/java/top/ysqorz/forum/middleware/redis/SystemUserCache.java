package top.ysqorz.forum.middleware.redis;

import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import tk.mybatis.mapper.entity.Example;
import top.ysqorz.forum.common.enumeration.SysUser;
import top.ysqorz.forum.dao.UserMapper;
import top.ysqorz.forum.po.User;
import top.ysqorz.forum.service.UserService;
import top.ysqorz.forum.utils.SpringUtils;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static top.ysqorz.forum.common.Constant.REDIS_KEY_SYS_USER;

/**
 * @ClassName SystemUserCache
 * @Description
 * @Author LZY
 * @Date 2024/10/27 15:33
 */
@Service
public class SystemUserCache {

    @Resource
    private RedisUtils redisUtils;

    @Resource
    private UserMapper userMapper;

    public Set<Integer> getSysUserIds(){
        String key = REDIS_KEY_SYS_USER;
        Map<Object, Object> hmget = redisUtils.hmget(key);
        if (CollectionUtils.isEmpty(hmget)){
            Example example = new Example(User.class);
            Example.Criteria criteria = example.createCriteria();
            criteria.andIn("email", SysUser.SYS_EMAIL_SET);

            List<User> sysUsers = userMapper.selectByExample(example);
            Map<String, Object> collect = sysUsers.stream().collect(Collectors.toUnmodifiableMap(user-> String.valueOf(user.getId()), user -> user, (k1, k2) -> k2));
            boolean hmset = redisUtils.hmset(key, collect);
            return collect.keySet().stream().map(Integer::valueOf).collect(Collectors.toSet());
        }
        return hmget.keySet().stream().map(e -> Integer.valueOf((String) e)).collect(Collectors.toSet());
    }


}
