package top.ysqorz.forum.im.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.util.ObjectUtils;
import top.ysqorz.forum.utils.JsonUtils;

/**
 * @author passerbyYSQ
 * @create 2021-02-05 22:31
 */
@Data
@NoArgsConstructor
public class MsgModel {
    // 消息类型
    private String msgType;

    /**
     * 注意：这里channelType若只设置可读，则只能被序列化，发出去的话不会读取出来。
     * 这里需要能把字段反序列化出来，所以设置的读和写
     */
    // 通道类型
    @JsonProperty(access = JsonProperty.Access.READ_WRITE) // 只会反序列化(json -> obj)，不会序列化(obj -> json)
    private String channelType;

    // 数据
    private Object data;

    public MsgModel(MsgType msgType, Object data) {
        this(msgType, null, data);
    }

    public MsgModel(MsgType msgType, ChannelType channelType, Object data) {
        this.msgType = msgType.name();
        if (!ObjectUtils.isEmpty(channelType)) {
            this.channelType = channelType.name();
        }
        this.data = data;
    }

    // 不要以getXx开头，否则objectMapper在序列化为json的时候会认为是一个属性
    public JsonNode transformToDataNode() {
        return data instanceof JsonNode ? (JsonNode) data : JsonUtils.objToNode(data);
    }

    public boolean check() {
        return !(ObjectUtils.isEmpty(msgType) || ObjectUtils.isEmpty(channelType) || ObjectUtils.isEmpty(data));
    }
}
