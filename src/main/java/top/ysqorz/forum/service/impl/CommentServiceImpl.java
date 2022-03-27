package top.ysqorz.forum.service.impl;

import com.github.pagehelper.PageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;
import org.springframework.web.util.HtmlUtils;
import tk.mybatis.mapper.entity.Example;
import top.ysqorz.forum.common.StatusCode;
import top.ysqorz.forum.dao.CommentNotificationMapper;
import top.ysqorz.forum.dao.FirstCommentMapper;
import top.ysqorz.forum.dao.SecondCommentMapper;
import top.ysqorz.forum.dto.FirstCommentDTO;
import top.ysqorz.forum.dto.PageData;
import top.ysqorz.forum.dto.SecondCommentDTO;
import top.ysqorz.forum.dto.SimpleUserDTO;
import top.ysqorz.forum.po.CommentNotification;
import top.ysqorz.forum.po.FirstComment;
import top.ysqorz.forum.po.Post;
import top.ysqorz.forum.po.SecondComment;
import top.ysqorz.forum.service.CommentService;
import top.ysqorz.forum.service.PermManager;
import top.ysqorz.forum.service.PostService;
import top.ysqorz.forum.service.RewardPointsAction;
import top.ysqorz.forum.shiro.ShiroUtils;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author passerbyYSQ
 * @create 2021-06-20 16:42
 */
@Service
public class CommentServiceImpl implements CommentService {

    @Resource
    private FirstCommentMapper firstCommentMapper;
    @Resource
    private SecondCommentMapper secondCommentMapper;
    @Resource
    private CommentNotificationMapper commentNotificationMapper;
    @Resource
    private PostService postService;
    @Resource
    private RewardPointsAction rewardPointsAction;
    @Resource
    private PermManager permManager;

    @Override
    public int getFrontFirstCommentCount(Integer firstCommentId) {
        FirstComment firstComment = firstCommentMapper.selectByPrimaryKey(firstCommentId);
        if (ObjectUtils.isEmpty(firstCommentId)) {
            return -1;
        }
        Example example = new Example(FirstComment.class);
        example.createCriteria().andEqualTo("postId", firstComment.getPostId())
                .andLessThan("createTime", firstComment.getCreateTime());
        return firstCommentMapper.selectCountByExample(example);
    }

    @Override
    public int[] getFrontSecondCommentCount(Integer secondCommentId) {
        SecondComment secondComment = secondCommentMapper.selectByPrimaryKey(secondCommentId);
        if (ObjectUtils.isEmpty(secondComment)) {
            return null;
        }
        Integer firstCommentId = secondComment.getFirstCommentId();
        Example example = new Example(SecondComment.class);
        example.createCriteria().andEqualTo("firstCommentId", firstCommentId)
                .andLessThan("createTime", secondComment.getCreateTime());
        int secondCount = secondCommentMapper.selectCountByExample(example);
        int firstCount = this.getFrontFirstCommentCount(firstCommentId);
        return new int[] {firstCount, secondCount};
    }


    @Transactional
    @Override
    public void publishFirstComment(Post post, String content) {
        /*
        // 这种方式，先必须查出FloorNum，再插入记录。并发情况下，可能导致FloorNum不正确
        FirstComment comment = new FirstComment();
        comment.setPostId(postId)
                .setContent(HtmlUtils.htmlUnescape(content))
                .setUserId(creatorId)
                //.setFloorNum()
                .setCreateTime(LocalDateTime.now())
                .setSecondCommentCount(0);
         */
        // 当前用户就是一级评论的creator
        Integer creatorId = ShiroUtils.getUserId();
        // 插入一级评论
        FirstComment comment = new FirstComment();
        comment.setPostId(post.getId())
                .setContent(HtmlUtils.htmlUnescape(content))
                .setUserId(creatorId);
        // comment 会被设置自增长的id
        firstCommentMapper.addFirstCommentUseGeneratedKeys(comment);

        // 更新评论数量（包括一级、二级）
        postService.updateCommentCountAndLastTime(post.getId(), 1);

        if (!creatorId.equals(post.getCreatorId())) {
            // 插入评论通知
            CommentNotification notification = new CommentNotification();
            notification.setSenderId(creatorId)
                    .setReceiverId(post.getCreatorId())
                    // 通知类型。0：主题帖被回复，1：一级评论被回复，2：二级评论被回复
                    .setCommentType((byte) 0) // 一级评论是回复主题帖的
                    // 被回复的id（可能是主题帖、一级评论、二级评论，根据评论类型来判断）
                    .setRepliedId(post.getId())
                    // 通知来自于哪条评论（可能是一级评论、二级评论）
                    .setCommentId(comment.getId())
                    .setCreateTime(LocalDateTime.now())
                    .setIsRead((byte) 0);
            commentNotificationMapper.insertUseGeneratedKeys(notification);

            // 奖励积分
            rewardPointsAction.publishComment();
        }
    }

    @Transactional
    @Override
    public void publishSecondComment(FirstComment firstComment,
                                     SecondComment quoteComment,
                                     String content) {
        Integer myId = ShiroUtils.getUserId();
        Integer receiverId = firstComment.getUserId();
        byte commentType = 1;
        Integer repliedId = firstComment.getId();

        // 插入二级评论
        SecondComment comment = new SecondComment();
        comment.setContent(HtmlUtils.htmlUnescape(content))
                .setCreateTime(LocalDateTime.now())
                .setFirstCommentId(firstComment.getId())
                .setUserId(myId);
        if (!ObjectUtils.isEmpty(quoteComment)) {
            comment.setQuoteSecondCommentId(quoteComment.getId());
            receiverId = quoteComment.getUserId();
            commentType = 2;
            repliedId = quoteComment.getId();
        }
        secondCommentMapper.insertUseGeneratedKeys(comment);

        // 更新帖子的评论数量
        postService.updateCommentCountAndLastTime(firstComment.getPostId(), 1);

        // 更新一级评论下的二级评论的数量
        this.addSecondCommentCount(firstComment.getId(), 1);

        // 评论通知
        if (!myId.equals(receiverId)) {
            // 插入评论通知
            CommentNotification notification = new CommentNotification();
            notification.setSenderId(myId)
                    .setReceiverId(receiverId)
                    // 通知类型。0：主题帖被回复，1：一级评论被回复，2：二级评论被回复
                    .setCommentType(commentType) // 一级评论是回复主题帖的
                    // 被回复的id（可能是主题帖、一级评论、二级评论，根据评论类型来判断）
                    .setRepliedId(repliedId)
                    // 通知来自于哪条评论（可能是一级评论、二级评论）
                    .setCommentId(comment.getId())
                    .setCreateTime(LocalDateTime.now())
                    .setIsRead((byte) 0);
            commentNotificationMapper.insertUseGeneratedKeys(notification);

            // 奖励积分
            rewardPointsAction.publishComment();
        }
    }

    @Override
    public PageData<FirstCommentDTO> getFirstCommentList(Post post, Integer page, Integer count, Boolean isTimeAsc) {
        PageHelper.startPage(page, count); // 里面会做page的越界纠正

        Map<String, Object> params = new HashMap<>();
        params.put("postId", post.getId());
        params.put("orderByStr", "create_time " + (isTimeAsc ? "asc" : "desc"));
        List<FirstCommentDTO> firstComments = firstCommentMapper.selectFirstCommentList(params);

        for (FirstCommentDTO firstComment : firstComments) {
            boolean isPostCreator = post.getCreatorId().equals(firstComment.getCreator().getId());
            // TODO 计算level
            firstComment.getCreator().setLevel(6);
            firstComment.setIsPostCreator(isPostCreator);
        }

        return new PageData<>(firstComments);
    }


    @Override
    public FirstComment getFirstCommentById(Integer firstCommentId) {
        return firstCommentMapper.selectByPrimaryKey(firstCommentId);
    }

    @Override
    public PageData<SecondCommentDTO> getSecondCommentList(FirstComment firstComment,
                                                           Integer page, Integer count) {
        PageHelper.startPage(page, count); // 里面会做page的越界纠正
        List<SecondCommentDTO> secondCommentList = secondCommentMapper.selectSecondCommentList(firstComment.getId());
        Post post = postService.getPostById(firstComment.getPostId());
        for (SecondCommentDTO secondComment : secondCommentList) {
            // TODO 计算level，是否是楼主
            SimpleUserDTO secondCreator = secondComment.getCreator();
            secondCreator.setLevel(6);
            secondComment.setIsPostCreator(secondCreator.getId().equals(post.getCreatorId()));
        }
        return new PageData<>(secondCommentList);
    }

    @Override
    public SecondComment getSecondCommentById(Integer secondCommentId) {
        return secondCommentMapper.selectByPrimaryKey(secondCommentId);
    }

    @Override
    public int addSecondCommentCount(Integer firstCommentId, Integer dif) {
        Map<String, Object> params = new HashMap<>();
        params.put("firstCommentId", firstCommentId);
        params.put("dif", dif);
        return firstCommentMapper.addSecondCommentCount(params);
    }

    @Transactional
    @Override
    public StatusCode deleteCommentById(Integer commentId, String type) {
        StatusCode code = StatusCode.SUCCESS;
        if ("FIRST_COMMENT".equals(type)) {
            FirstComment firstComment = this.getFirstCommentById(commentId);
            if (ObjectUtils.isEmpty(firstComment)) {
                return StatusCode.FIRST_COMMENT_NOT_EXIST;
            }
            if (!permManager.allowDelComment(firstComment.getUserId())) {
                return StatusCode.NO_PERM;
            }
            firstCommentMapper.deleteByPrimaryKey(commentId);
            postService.updateCommentCountAndLastTime(firstComment.getPostId(), -1);
            // TODO 积分回退
        } else if ("SECOND_COMMENT".equals(type)) {
            SecondComment secondComment = this.getSecondCommentById(commentId);
            if (ObjectUtils.isEmpty(secondComment)) {
                return StatusCode.SECOND_COMMENT_NOT_EXIST;
            }
            if (!permManager.allowDelComment(secondComment.getUserId())) {
                return StatusCode.NO_PERM;
            }
            secondCommentMapper.deleteByPrimaryKey(commentId);
            FirstComment firstComment = this.getFirstCommentById(secondComment.getFirstCommentId());
            this.addSecondCommentCount(firstComment.getId(), -1);
            postService.updateCommentCountAndLastTime(firstComment.getPostId(), -1);
            // TODO 积分回退
        } else {
            code = StatusCode.COMMENT_TYPE_INVALID; // type错误
        }
        return code;
    }
}
