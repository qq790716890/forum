package top.ysqorz.forum.service.impl;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.crypto.hash.Md5Hash;
import org.apache.shiro.subject.Subject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import tk.mybatis.mapper.entity.Example;
import top.ysqorz.forum.common.Constant;
import top.ysqorz.forum.common.StatusCode;
import top.ysqorz.forum.common.enumeration.Activation;
import top.ysqorz.forum.common.enumeration.Gender;
import top.ysqorz.forum.common.enumeration.SysUser;
import top.ysqorz.forum.common.exception.ParamInvalidException;
import top.ysqorz.forum.common.exception.ServiceFailedException;
import top.ysqorz.forum.dao.*;
import top.ysqorz.forum.dto.req.CheckUserDTO;
import top.ysqorz.forum.dto.req.QueryUserCondition;
import top.ysqorz.forum.dto.req.RegisterDTO;
import top.ysqorz.forum.dto.resp.BlackInfoDTO;
import top.ysqorz.forum.dto.resp.SimpleUserDTO;
import top.ysqorz.forum.dto.resp.UserDTO;
import top.ysqorz.forum.dto.resp.chat.ChatUserCardDTO;
import top.ysqorz.forum.middleware.redis.RedisUtils;
import top.ysqorz.forum.middleware.redis.SystemUserCache;
import top.ysqorz.forum.oauth.provider.BaiduProvider;
import top.ysqorz.forum.oauth.provider.GiteeProvider;
import top.ysqorz.forum.oauth.provider.QQProvider;
import top.ysqorz.forum.po.*;
import top.ysqorz.forum.service.RoleService;
import top.ysqorz.forum.service.UserService;
import top.ysqorz.forum.shiro.JwtToken;
import top.ysqorz.forum.shiro.ShiroUtils;
import top.ysqorz.forum.utils.DateTimeUtils;
import top.ysqorz.forum.utils.JwtUtils;
import top.ysqorz.forum.utils.MailClient;
import top.ysqorz.forum.utils.RandomUtils;

import javax.annotation.Resource;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;


/**
 * @author 阿灿
 * @create 2021-05-10 16:10
 */
@Service
public class UserServiceImpl implements UserService {
    @Resource
    private UserMapper userMapper;
    @Resource
    private BlacklistMapper blacklistMapper;
    @Resource
    private RoleMapper roleMapper;
    @Resource
    private RoleService roleService;
    @Resource
    private UserRoleMapper userRoleMapper;
    @Resource
    private GiteeProvider giteeProvider;
    @Resource
    private QQProvider qqProvider;
    @Resource
    private BaiduProvider baiduProvider;
    @Resource
    private FollowMapper followMapper;
    @Resource
    private PostMapper postMapper;
    @Resource
    private FirstCommentMapper firstCommentMapper;
    @Resource
    private CommentNotificationMapper commentNotificationMapper;

    @Resource
    private ChatFriendMapper chatFriendMapper;

    @Resource
    private SystemUserCache systemUserCache;

    @Resource
    private ChatgptServiceImpl chatgptService;

    // 发邮件所需 的 两件套
    @Autowired
    private MailClient mailClient;

    @Autowired
    private TemplateEngine templateEngine;

    @Value("${community.path.domain}")
    private String domain;


    @Override
    public User getUserByEmail(String email) {
        Example example = new Example(User.class);
        example.createCriteria().andEqualTo("email", email).andEqualTo("activationCode", "-1");
        return userMapper.selectOneByExample(example);
    }

    @Override
    public User getUserByPhone(String phone) {
        Example example = new Example(User.class);
        example.createCriteria().andEqualTo("phone", phone);
        return userMapper.selectOneByExample(example);
    }

    @Override
    public List<User> getUsersLikeUsername(String username, boolean isExcludeMe) {
        Example example = new Example(User.class);
        example.orderBy("lastLoginTime").desc();
        Example.Criteria criteria = example.createCriteria();
        if (isExcludeMe) {
            criteria.andNotEqualTo("id", ShiroUtils.getUserId());
        }
        criteria.andLike("username", "%" + username + "%");
        return userMapper.selectByExample(example);
    }

    @Override
    public List<ChatUserCardDTO> getUserCardsLikeUsername(String username) {
        return userMapper.selectUserCardsLikeUsername(ShiroUtils.getUserId(), username);
    }

    @Override
    public List<UserDTO> getMyUserList(QueryUserCondition conditions) {
        return userMapper.selectAllUser(conditions);
    }

    @Override
    public int updatePsw(Integer userId, String loginSalt) {
        User record = new User();
        record.setId(userId);
        record.setPassword(this.encryptLoginPwd("123456", loginSalt));
        return userMapper.updateByPrimaryKeySelective(record);
    }

    @Override
    public int updateUserById(User user) {
        return userMapper.updateByPrimaryKeySelective(user);
    }

    @Override
    public User getUserById(Integer userId) {
        return userMapper.selectByPrimaryKey(userId);
    }

    @Override
    public int cancelBlock(Integer id) {
        Example example = new Example(Blacklist.class);
        example.createCriteria().andEqualTo("userId", id)
                .andGreaterThan("endTime", LocalDateTime.now());

        Blacklist record = new Blacklist();
        record.setIsRead((byte) 0); // 重置为未读
        record.setEndTime(LocalDateTime.now().minusMinutes(1));  //这里给解封时间设置为当前时间-1分钟
        return blacklistMapper.updateByExampleSelective(record, example);
    }

    @Override
    public int block(Blacklist blacklist) {
        LocalDateTime start = LocalDateTime.now();
        if (start.isAfter(blacklist.getEndTime())) {
            throw new ParamInvalidException(StatusCode.END_TIME_BEFORE_START_TIME);
        }
        blacklist.setCreateTime(LocalDateTime.now());
        blacklist.setStartTime(start);
        blacklist.setIsRead((byte) 0);
        return blacklistMapper.insert(blacklist);
    }

    @Override
    public BlackInfoDTO getBlackInfo(Integer userId) {
        return blacklistMapper.getBlockInfo(userId, LocalDateTime.now());
    }

    @Override
    public List<Role> getAllRole() {
        return roleMapper.selectAll();
    }

    @Transactional // 开启事务操作
    @Override
    public void addRoleForUser(Integer[] roleIds, Integer userId) {
        for (Integer roleId : roleIds) {
            Role role = roleService.getRoleById(roleId);
            if (role == null) {
                throw new ParamInvalidException(StatusCode.ROLE_NOT_EXIST);
            }
            Example example = new Example(UserRole.class);
            example.createCriteria().andEqualTo("userId", userId)
                    .andEqualTo("roleId", roleId);
            if (userRoleMapper.selectCountByExample(example) == 0) {
                UserRole userRole = new UserRole();
                userRole.setRoleId(roleId);
                userRole.setUserId(userId);
                userRole.setCreateTime(LocalDateTime.now());
                userRoleMapper.insert(userRole);
            }
        }
        // 清除授权缓存
        ShiroUtils.clearUserAuthorizationCache(userId);
    }

    @Override
    public List<Role> getRoleByUserId(Integer userId) {
        return userRoleMapper.getRoleByUserId(userId);
    }

    @Override
    public void delRoleForUser(Integer[] roleIds, Integer userId) {
        for (Integer roleId : roleIds) {
            Example example = new Example(UserRole.class);
            example.createCriteria().andEqualTo("userId", userId)
                    .andEqualTo("roleId", roleId);
            userRoleMapper.deleteByExample(example);
        }
        // 清除授权缓存
        ShiroUtils.clearUserAuthorizationCache(userId);
    }

    @Override
    @Transactional
    public void register(RegisterDTO dto) {
        User user = new User();
        user.setEmail(dto.getEmail());
        user.setUsername(dto.getUsername().trim());
        user.setActivationCode(UUID.randomUUID().toString().substring(0, 12).replace("-", ""));

        // 8个字符的随机字符串，作为加密登录的随机盐。
        String loginSalt = RandomUtils.generateStr(8);
        // 保存到user表，以后每次密码登录的时候，需要使用
        user.setLoginSalt(loginSalt);

        // Md5Hash默认将随机盐拼接到源字符串的前面，然后使用md5加密，再经过x次的哈希散列
        // 第三个参数（hashIterations）：哈希散列的次数
        String encryptPwd = this.encryptLoginPwd(dto.getPassword(), loginSalt);
        // 保存加密后的密码
        user.setPassword(encryptPwd)
                .setRegisterTime(LocalDateTime.now())
                .setModifyTime(LocalDateTime.now())
                .setConsecutiveAttendDays(0)
                .setRewardPoints(0)
                .setFansCount(0)
                .setGender(Gender.SECRET) // 性别保密
                .setJwtSalt("")
                .setPhoto("/admin/assets/images/defaultUserPhoto.jpg");

        userMapper.insertUseGeneratedKeys(user);
        this.addInitSystemFriend(user.getId());
        // 激活邮件
        Context context = new Context();
        context.setVariable("email", user.getEmail());
        // http://localhost:8080/activation/101/code

        String encodedName = user.getUsername();
        String url = domain + "/user/activation?username=" + encodedName + "&code=" + user.getActivationCode(); //userId 会在添加入数据库后自动回写
        context.setVariable("url", url);
        // 将数据写入激活模板html，得到html内容
        String content = templateEngine.process("mail/activation", context);
        mailClient.sendMail(user.getEmail(), "激活邮箱", content);


    }

    @Override
    public SimpleUserDTO getSimpleUser(Integer userId) {
        SimpleUserDTO simpleUserDTO = userMapper.selectSimpleUserById(userId);
        if (simpleUserDTO == null) return null;
        simpleUserDTO.setLevel(6); // TODO 根据积分计算level
        Example example = new Example(CommentNotification.class);
        example.createCriteria().andEqualTo("receiverId", userId).andEqualTo("isRead", 0);
        simpleUserDTO.setNewMeg(commentNotificationMapper.selectCountByExample(example));
        return simpleUserDTO;
    }

    @Override
    public SimpleUserDTO getHomeInformationById(Integer visitId) {
        SimpleUserDTO information = userMapper.selectHomeInformationById(visitId);
        information.setId(visitId);
        information.setLevel(6); // TODO 根据积分计算level
        return information;
    }

    @Override
    public Boolean isFocusOn(Integer visitId) {
        // 如果未登录，直接返回false
        if (ObjectUtils.isEmpty(ShiroUtils.getUserId())) {
            return false;
        }
        Example example = new Example(Follow.class);
        example.createCriteria()
                .andEqualTo("fromUserId", ShiroUtils.getUserId())
                .andEqualTo("toUserId", visitId);
        Follow follow = followMapper.selectOneByExample(example);
        return follow != null;
    }

    @Transactional
    @Override
    public void addFollow(Integer visitId) {
        Integer myId = ShiroUtils.getUserId();
        //user表对应数据+1粉丝数
        userMapper.updateAndAddFansCount(visitId);
        //follow表添加数据
        Follow follow = new Follow();
        follow.setFromUserId(myId);
        follow.setToUserId(visitId);
        follow.setCreateTime(DateTimeUtils.getFormattedTime());
        followMapper.insert(follow);
    }

    @Transactional
    @Override
    public void deleteFollow(Integer visitId) {
        Integer myId = ShiroUtils.getUserId();
        //user表对应数据-1粉丝数
        userMapper.updateAndReduceFansCount(visitId);
        //follow表删除数据
        Example example = new Example(Follow.class);
        example.createCriteria()
                .andEqualTo("fromUserId", myId)
                .andEqualTo("toUserId", visitId);
        followMapper.deleteByExample(example);
    }

    @Override
    public Activation activation(String username, String code) {
        Example example = new Example(User.class);
        example.createCriteria().andEqualTo("username", username).andEqualTo("activationCode", "-1");
        User user = userMapper.selectOneByExample(example);
        if (user !=null){
            return Activation.REPEAT;
        }
        example.clear();
        example.createCriteria().andEqualTo("username", username).andEqualTo("activationCode", code);
        User userToActive = userMapper.selectOneByExample(example);
        if (userToActive == null) return Activation.NOT_PAIR;
        else if (userToActive.getActivationCode().equals(code)) {
            userMapper.updateActivated(userToActive.getId());
            return Activation.SUCCESS;
        } else {
            return Activation.FAIL;
        }
    }

    @Override
    public void addInitSystemFriend(Integer userId) {
        Example example = new Example(User.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andIn("email", SysUser.SYS_EMAIL_SET);
        List<User> sysFriends = userMapper.selectByExample(example);

        // TODO: 筛选出 没有的系统账号，添加初始好友
//        Example sysFriendExample = new Example(ChatFriend.class);
//        Example.Criteria sysFriendCriteria = example.createCriteria();
//        sysFriendCriteria.andEqualTo("myId",userId);
//        sysFriendCriteria.andIn("friendId", sysFriend.stream().map(User::getId).collect(Collectors.toList()));
//        List<ChatFriend> chatFriends = chatFriendMapper.selectByExample(sysFriendExample);

        List<ChatFriend> collect = sysFriends.stream().map(sysFriend -> {
            ChatFriend chatFriend = new ChatFriend();
            chatFriend.setMyId(userId).setFriendId(sysFriend.getId());
            chatFriend.setCreateTime(LocalDateTime.now());
            return chatFriend;
        }).collect(Collectors.toList());
        chatFriendMapper.insertList(collect);
    }

    @Override
    public Set<Integer> getAllSystemUserId() {
        return systemUserCache.getSysUserIds();

    }

    @Override
    public String login(Integer userId, String loginSalt, HttpServletResponse response) {
        this.updateLastLoginTime(userId);

        // shiro login
        JwtToken jwtToken = this.generateJwtToken(userId, loginSalt);
        Subject subject = SecurityUtils.getSubject();
        subject.login(jwtToken);

        // 将token放置到cookie中
        String token = (String) jwtToken.getCredentials();
        Cookie cookie = new Cookie("token", token);
        cookie.setMaxAge((int) Constant.DURATION_JWT.getSeconds());
        cookie.setPath("/"); // ！！！
        response.addCookie(cookie);
        response.setHeader("token", token);

        return token;
    }

    @Override
    public void logout() {
        this.updateLastLoginTime(ShiroUtils.getUserId());
        Subject subject = SecurityUtils.getSubject();
        subject.logout();
    }

    @Override
    public JwtToken generateJwtToken(Integer userId, String jwtSalt) {
        String jwt = JwtUtils.generateJwt("userId", userId.toString(), jwtSalt, Constant.DURATION_JWT.toMillis());
        return new JwtToken(jwt);
    }

    @Override
    public String encryptLoginPwd(String originPwd, String salt) {
        return new Md5Hash(originPwd, salt, 1024).toHex();
    }

    @Override
    public int updateRewardPoints(Integer userId, Integer num) {
        Map<String, Object> param = new HashMap<>();
        param.put("userId", userId);
        param.put("num", num);
        return userMapper.updateRewardPoints(param);
    }

    @Override
    public void updateLastLoginTime(Integer userId) {
        User record = new User();
        record.setId(userId).setLastLoginTime(LocalDateTime.now());
        userMapper.updateByPrimaryKeySelective(record);
    }

    @Override
    public SimpleUserDTO getLoginUser() {
        if (ShiroUtils.isAuthenticated()) { // 当前主体已登录
            Integer myId = ShiroUtils.getUserId(); // 当前登录用户的userId
            return this.getSimpleUser(myId);
        }
        return new SimpleUserDTO();
    }

    @Override
    public StatusCode checkUser(CheckUserDTO checkUser) {
        User user = this.getUserByEmail(checkUser.getOldEmail());
        // 验证登录身份
        if (!ObjectUtils.isEmpty(user) && user.getId().equals(ShiroUtils.getUserId())) {
            String encryptPwd = this.encryptLoginPwd(
                    checkUser.getCheckPassword(), user.getLoginSalt());
            if (user.getPassword().equals(encryptPwd)) {
                return StatusCode.SUCCESS;
            }
        }
        return StatusCode.ACCOUNT_OR_PASSWORD_INCORRECT;
    }

    /**
     * @param bindNum  指手机邮箱这类绑定号码
     * @param property 指明检查的第三方账号类型，举例：phone、email
     * @return true代表已有用户绑定，false代表没有用户绑定
     */
    @Override
    public Boolean checkBind(String bindNum, String property) {
        Example example = new Example(User.class);
        example.createCriteria().andEqualTo(property, bindNum);
        User user = userMapper.selectOneByExample(example);
        return user != null;
    }
}
