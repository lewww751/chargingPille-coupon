package itheima.core;

import ch.qos.logback.classic.spi.EventArgUtil;
import javax.websocket.Session;
import com.mysql.cj.x.protobuf.MysqlxCursor;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnOpen;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket 服务器端
 */
@Component
@Slf4j
@ServerEndpoint("/{token}") //对应前端传过来的路径，比如/token
public class WebsocketServer {
    public static ConcurrentHashMap<String, Session> SESSION_MAP = new ConcurrentHashMap<>();
    @OnOpen
    public void onOpen(Session session , @PathParam("token") String token) {
        log.info("WebSocket connection opened{}", token);
        SESSION_MAP.put(token, session);
    }
    @OnClose
    public void onClose(Session session, @PathParam("token") String token) {
        log.info("WebSocket connection closed{}", token);
        SESSION_MAP.remove(token);
    }
    @OnError
    public void onError(Session session, @PathParam("token") String token, Throwable throwable) {
        log.info("WebSocket connection{} error{}", token, throwable.getMessage());
    }

}
