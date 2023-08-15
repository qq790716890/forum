package top.ysqorz.forum.service;

import top.ysqorz.forum.dto.PageData;
import top.ysqorz.forum.dto.resp.PostDTO;
import top.ysqorz.forum.po.Collect;

/**
 * @author passerbyYSQ
 * @create 2021-06-19 13:53
 */
public interface CollectService {

    // 某用户是否收藏了某个帖子
    Collect getCollectByUserIdAndPostId(Integer userId, Integer postId);

    Collect addCollect(Integer postId);

    int cancelCollect(Integer collectId, Integer postId);

    int countCollectByUserId(Integer userId);

    PageData<PostDTO> getCollectPostListByUserId(Integer userId, Integer page, Integer count);



}
