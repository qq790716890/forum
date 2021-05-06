package top.ysqorz.forum.po;

import java.time.LocalDateTime;
import javax.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
@EqualsAndHashCode
@Table(name = "post")
public class Post {
    /**
     * 帖子id
     */
    @Id
    private Integer id;

    /**
     * 标题
     */
    private String title;

    /**
     * 发布者id
     */
    @Column(name = "creator_id")
    private Integer creatorId;

    /**
     * 所属话题
     */
    @Column(name = "topic_id")
    private Integer topicId;

    /**
     * 发布时间
     */
    @Column(name = "create_time")
    private LocalDateTime createTime;

    /**
     * 上一次修改时间
     */
    @Column(name = "last_modify_time")
    private LocalDateTime lastModifyTime;

    /**
     * 访问数
     */
    @Column(name = "visit_count")
    private Integer visitCount;

    /**
     * 点赞数
     */
    @Column(name = "like_count")
    private Integer likeCount;

    /**
     * 收藏数
     */
    @Column(name = "collect_count")
    private Integer collectCount;

    /**
     * 总评论数
     */
    @Column(name = "comment_count")
    private Integer commentCount;

    /**
     * 是否为精品
     */
    @Column(name = "is_hight_quality")
    private Byte isHightQuality;

    /**
     * 置顶权重
     */
    @Column(name = "top_weight")
    private Integer topWeight;

    /**
     * 最后一次评论时间
     */
    @Column(name = "last_comment_time")
    private LocalDateTime lastCommentTime;

    /**
     * 可见策略（之后再考虑规划）
     */
    @Column(name = "visibility_type")
    private Byte visibilityType;

    /**
     * 内容
     */
    private String content;
}