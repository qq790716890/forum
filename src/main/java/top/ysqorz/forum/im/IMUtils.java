package top.ysqorz.forum.im;

import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.timeout.IdleState;
import io.netty.util.AttributeKey;
import org.apache.zookeeper.server.auth.DigestAuthenticationProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import top.ysqorz.forum.common.Constant;
import top.ysqorz.forum.im.entity.MsgModel;
import top.ysqorz.forum.im.entity.MsgType;
import top.ysqorz.forum.utils.CommonUtils;
import top.ysqorz.forum.utils.JsonUtils;
import top.ysqorz.forum.utils.SpringUtils;

import java.net.*;
import java.security.NoSuchAlgorithmException;

/**
 * @author passerbyYSQ
 * @create 2022-01-25 19:12
 */
@Component
public class IMUtils {
    public static final AttributeKey<String> TOKEN_KEY = AttributeKey.valueOf("token");
    public static final AttributeKey<String> GROUP_ID_KEY = AttributeKey.valueOf("groupId");
    public static final AttributeKey<String> CHANNEL_TYPE_KEY = AttributeKey.valueOf("channelType");
    public static final AttributeKey<Integer> ALL_IDLE_KEY = AttributeKey.valueOf(IdleState.ALL_IDLE.name());

    public static String extractIpFromUrl(String urlString) {
        try {
            URL url = new URL(urlString);
            String host = url.getHost();
            InetAddress[] addresses = InetAddress.getAllByName(host);
            for (InetAddress address : addresses) {
                if (address instanceof Inet4Address) {
                    return address.getHostAddress();
                }
            }
        } catch (MalformedURLException e) {
            System.out.println("Malformed URL: " + e.getMessage());
        } catch (UnknownHostException e) {
            System.out.println("Unknown Host: " + e.getMessage());
        }
        return null;
    }

    public static String getWsServer() {
//        return String.format("ws://%s:%d/ws", CommonUtils.getLocalIp(), Constant.WS_SERVER_PORT);
        Environment env = SpringUtils.getBean(Environment.class);
        String urlString = env.getProperty("community.path.domain");
        String wsServerIp = extractIpFromUrl(urlString);
        return String.format("ws://%s:%d/ws", wsServerIp, Constant.WS_SERVER_PORT);
    }

    public static String getWebServer() {
        Environment env = SpringUtils.getBean(Environment.class);
        String urlString = env.getProperty("community.path.domain");
        String wsServerIp = extractIpFromUrl(urlString);
        return wsServerIp + ":" + env.getProperty("server.port");
    }

    public static String generateAuthDigest(String userPwd) {
        // user:passowrd  --sha1--> --base64--> digest
        try {
            return DigestAuthenticationProvider.generateDigest(userPwd);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static TextWebSocketFrame createTextFrame(MsgType type, Object data) {
        MsgModel respModel = new MsgModel(type, data);
        String respText = JsonUtils.objToJson(respModel);
        return new TextWebSocketFrame(respText);
    }

    public static TextWebSocketFrame createTextFrame(MsgType type) {
        return createTextFrame(type, null);
    }

    public static String getTokenFromChannel(Channel channel) {
        return channel.attr(TOKEN_KEY).get();
    }

    public static String getChannelTypeFromChannel(Channel channel) {
        return channel.attr(CHANNEL_TYPE_KEY).get();
    }

    public static String getGroupIdFromChannel(Channel channel) {
        return channel.attr(GROUP_ID_KEY).get();
    }
}
