package top.ysqorz.forum.dao;

import top.ysqorz.forum.common.BaseMapper;
import top.ysqorz.forum.dto.req.QueryUserCondition;
import top.ysqorz.forum.dto.resp.SimpleUserDTO;
import top.ysqorz.forum.dto.resp.UserDTO;
import top.ysqorz.forum.dto.resp.chat.ChatUserCardDTO;
import top.ysqorz.forum.po.User;

import java.util.List;
import java.util.Map;

public interface UserMapper extends BaseMapper<User> {

    List<UserDTO> selectAllUser(QueryUserCondition conditions);

    int updateRewardPoints(Map<String, Object> param);

    int updateActivated(Integer userId);

    SimpleUserDTO selectSimpleUserById(Integer userId);

    SimpleUserDTO selectHomeInformationById(Integer visitId);

    /**
     * 增加user表中数据的粉丝数
     */
    void updateAndAddFansCount(Integer userId);

    /**
     * 减少user表中数据粉丝数
     */
    void updateAndReduceFansCount(Integer userId);

    /**
     * 根据用户名模糊匹配用户
     */
    List<ChatUserCardDTO> selectUserCardsLikeUsername(Integer myId, String username);
}
