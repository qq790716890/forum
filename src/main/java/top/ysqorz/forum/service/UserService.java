package top.ysqorz.forum.service;

import top.ysqorz.forum.common.StatusCode;
import top.ysqorz.forum.common.enumeration.Activation;
import top.ysqorz.forum.dto.req.CheckUserDTO;
import top.ysqorz.forum.dto.req.QueryUserCondition;
import top.ysqorz.forum.dto.req.RegisterDTO;
import top.ysqorz.forum.dto.resp.BlackInfoDTO;
import top.ysqorz.forum.dto.resp.SimpleUserDTO;
import top.ysqorz.forum.dto.resp.UserDTO;
import top.ysqorz.forum.dto.resp.chat.ChatUserCardDTO;
import top.ysqorz.forum.po.Blacklist;
import top.ysqorz.forum.po.Role;
import top.ysqorz.forum.po.User;
import top.ysqorz.forum.shiro.JwtToken;

import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Set;

/**
 * @author 阿灿
 * @create 2021-05-10 16:09
 */
public interface UserService {

    /**
     * 加密登录密码
     * @param originPwd     原视密码
     * @param salt          加密的盐
     */
    String encryptLoginPwd(String originPwd, String salt);

    /**
     * 加减积分。注意自增和自减，只能自己写SQL语句，没办法使用tk
     */
    int updateRewardPoints(Integer userId, Integer num);

    /**
     * 更新最后一次登录的时间
     */
    void updateLastLoginTime(Integer userId);

    /**
     * 登录
     */
    String login(Integer userId, String loginSalt, HttpServletResponse response);

    /**
     * 退出登录
     */
    void logout();

    /**
     * 生成jwt字符串
     */
    JwtToken generateJwtToken(Integer userId, String jwtSalt);

    /**
     * 根据用户名查询用户
     */
    User getUserByEmail(String email);

    /**
     * 根据手机号查询用户
     */
    User getUserByPhone(String phone);

    /**
     * 根据用户名模糊匹配用户
     */
    List<User> getUsersLikeUsername(String username, boolean isExcludeMe);

    /**
     * 根据用户名模糊匹配用户
     */
    List<ChatUserCardDTO> getUserCardsLikeUsername(String username);

    /**
     * 后台用户管理
     */
    List<UserDTO> getMyUserList(QueryUserCondition conditions);

    /**
     * 重置密码
     */
    int updatePsw(Integer userId, String loginSalt);

    /**
     * 修改用户
     */
    int updateUserById(User user);

    /**
     * 根据id查询user信息
     */
    User getUserById(Integer userId);

    /**
     * 取消拉黑
     */
    int cancelBlock(Integer id);

    /**
     * 拉黑
     */
    int block(Blacklist blacklist);

    /**
     * 获取封禁信息
     */
    BlackInfoDTO getBlackInfo(Integer userId);

    /**
     * 获取所有角色信息
     */
    List<Role> getAllRole();

    /**
     * 为某一用户添加角色
     */
    void addRoleForUser(Integer[] roleIds, Integer userId);

    /**
     * 查询用户已有角色
     */
    List<Role> getRoleByUserId(Integer userId);

    /**
     * 删除用户已分配角色
     */
    void delRoleForUser(Integer[] roleIds, Integer userId);

    /**
     * 注册
     */
    void register(RegisterDTO vo);

    SimpleUserDTO getSimpleUser(Integer userId);

    SimpleUserDTO getLoginUser();

    /**
     * 检查用户账号密码是否正确
     */
    StatusCode checkUser(CheckUserDTO checkUser);

    /**
     * 检查所使用的第三方号码是否已绑定
     */
    Boolean checkBind(String bindNum, String property);

    /**
     * 获取主页信息
     */
    SimpleUserDTO getHomeInformationById(Integer visitId);

    /**
     * 判断是否关注
     */
    Boolean isFocusOn(Integer visitId);

    /**
     * 添加关注
     */
    void addFollow(Integer visitId);

    /**
     * 删除关注
     */
    void deleteFollow(Integer visitId);

    Activation activation(String username, String code);

    /**
     * 添加初始系统好友
     * @param userId
     */
    void addInitSystemFriend(Integer userId);

    /**
     * 获取所有系统账号的id
     */
    Set<Integer> getAllSystemUserId();
}
