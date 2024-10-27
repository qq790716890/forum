package top.ysqorz.forum.dao;


import org.apache.ibatis.annotations.Mapper;
import top.ysqorz.forum.common.BaseMapper;
import top.ysqorz.forum.po.ChatgptMessage;


/**
 * @author ChatViewer
 */
@Mapper
public interface ChatgptMessageMapper extends BaseMapper<ChatgptMessage> {
}
