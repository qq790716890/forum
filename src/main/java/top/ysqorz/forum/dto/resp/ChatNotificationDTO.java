package top.ysqorz.forum.dto.resp;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * @author passerbyYSQ
 * @create 2022-04-05 15:42
 */
@Data
@Accessors(chain = true)
public class ChatNotificationDTO {
    private Integer senderId; // 0：表示系统消息
    private Integer receiverId;
    private String action;
    private LocalDateTime time;
    private Map<String, Object> payload = new HashMap<>();

    public ChatNotificationDTO addPayload(String key, Object value) {
        payload.put(key, value);
        return this;
    }
}
