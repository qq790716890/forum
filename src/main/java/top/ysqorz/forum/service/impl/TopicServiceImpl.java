package top.ysqorz.forum.service.impl;

import org.springframework.stereotype.Service;
import top.ysqorz.forum.dao.TopicMapper;
import top.ysqorz.forum.po.Topic;
import top.ysqorz.forum.service.TopicService;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author passerbyYSQ
 * @create 2021-05-24 23:23
 */
@Service
public class TopicServiceImpl implements TopicService {

    @Resource
    private TopicMapper topicMapper;

    @Override
    public List<Topic> getAllTopic() {
        return topicMapper.selectAll();
    }

    @Override
    public Topic getTopicById(Integer topicId) {
        return topicMapper.selectByPrimaryKey(topicId);
    }

    @Override
    public int updatePostCountById(Integer topicId, Integer cnt) {
        Map<String, Object> params = new HashMap<>();
        params.put("topicId", topicId);
        params.put("cnt", cnt);
        return topicMapper.updatePostCountById(params);
    }
}
