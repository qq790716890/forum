package top.ysqorz.forum.dao;

import top.ysqorz.forum.common.BaseMapper;
import top.ysqorz.forum.dto.resp.PostDTO;
import top.ysqorz.forum.po.Collect;

import java.util.List;

public interface CollectMapper extends BaseMapper<Collect> {

    List<PostDTO> selectCollectPostListByUserId(Integer userId);
}