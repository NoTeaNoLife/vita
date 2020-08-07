package com.baldyoung.vita.merchant.serverEndpoint;

import com.alibaba.fastjson.JSON;
import com.baldyoung.vita.common.pojo.dto.diningRoom.MDiningRoomNewsDto;
import com.baldyoung.vita.common.pojo.exception.systemException.UtilityException;
import com.baldyoung.vita.common.service.impl.SystemMessageServiceImpl;
import com.baldyoung.vita.common.utility.RandomStringModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.baldyoung.vita.common.utility.CommonMethod.isEmpty;

@ServerEndpoint(value = "/mSystemMessage/{key}")
@Component
public class MerchantSystemMessageServerPoint {
    private static Logger logger = LoggerFactory.getLogger(MerchantSystemMessageServerPoint.class);
    // 统计当前在线人数
    private static Integer currentUserNumber = 0;
    // 当前在线用户的映射图
    private static ConcurrentHashMap<Integer, Session> sessionPools = new ConcurrentHashMap<>();
    // 建立websocket连接的匹配密钥存储图，key：密钥    value：商家用户ID
    private static ConcurrentHashMap<String, Integer> linkKeyMap = new ConcurrentHashMap();

    private static ApplicationContext applicationContext;

    private static SystemMessageServiceImpl systemMessageService;

    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    public static void setApplicationContext(ApplicationContext applicationContext) {
        MerchantSystemMessageServerPoint.applicationContext = applicationContext;
        systemMessageService = applicationContext.getBean(SystemMessageServiceImpl.class);
    }

    /**
     * 关闭指定用户的websocket连接
     * @param userId
     * @throws IOException
     */
    public static void closeSocket(Integer userId) throws IOException {
        Session session = sessionPools.get(userId);
        if (null != session) {
            session.close();
        }
    }

    @PostConstruct
    private void init() {
        // 测试
        // linkKeyMap.put("123", 1);
    }

    // 生成一个随机字符串，并将其映射到一个值上
    public static String createLinkKey(Integer merchantUserId) {
        Set<Map.Entry<String, Integer>> existsMap = linkKeyMap.entrySet();
        for (Map.Entry<String, Integer> entry : existsMap) {
            if (merchantUserId.equals(entry.getValue())) {
                return entry.getKey();
            }
        }
        String key = RandomStringModule.getRandomString(16);
        linkKeyMap.put(key, merchantUserId);
        return key;
    }

    // 移除指定key的映射，并返回其对应的值
    public static Integer removeLinkKey(String key) {
        Integer value = linkKeyMap.get(key);
        if (null != value) {
            linkKeyMap.remove(key);
        }
        return value;
    }

    // 添加在线人数
    private static void addUserNumber(int number) {
        synchronized (currentUserNumber) {
            currentUserNumber += number;
        }
    }

    // 获取当前在线人数
    private static int getUserNumber() {
        synchronized (currentUserNumber) {
            return currentUserNumber.intValue();
        }
    }

    // 获取最新数据，并发送给在线商家端
    public static void newsOption(List<MDiningRoomNewsDto> news) {
        Set<Map.Entry<Integer, Session>> entries = sessionPools.entrySet();
        if (null == entries || 0 == entries.size()) {
            return;
        }
        if (null == news) {
            return;
        }
        String newsString = JSON.toJSONString(news);
        for (Map.Entry<Integer, Session> entry : entries) {
            try {
                sendMessage(entry.getValue(), newsString);
            } catch (IOException e) {
                logger.error("userId:{} -> {}", entry.getKey(), e.getMessage());
            }
        }
    }

    // 给指定session发送消息
    public static void sendMessage(Session session, String message) throws IOException {
        // System.out.println("发送消息到客户端");
        if (session != null) {
            synchronized (session) {
                session.getBasicRemote().sendText(message);
            }
        }
    }

    // 给指定用户Id发送信息
    public static void sendMessage(Integer userId, String message) throws IOException {
        Session session = sessionPools.get(userId);
        sendMessage(session, message);
    }

    // 成功建立连接后的调用
    @OnOpen
    public void onOpen(Session websocketSession, @PathParam(value = "key")String key) throws UtilityException, IOException {
        if (isEmpty(key)) {
            websocketSession.close();
            return;
        }
        Integer value = removeLinkKey(key);
        if (null == value) {
            websocketSession.close();
            return;
        }
        websocketSession.getUserProperties().put("userId", value);
        sessionPools.put(value, websocketSession);
        addUserNumber(1);
        systemMessageService.pullMerchantUnreadMessage();
        logger.warn("{title:'新增用户连接', mUserId:"+value+", currentLinkNumber:"+getUserNumber()+"}");
    }

    // 连接关闭后的调用
    @OnClose
    public void onClose(Session websocketSession) throws UtilityException {
        Object object = websocketSession.getUserProperties().get("userId");
        if (null == object) {
            return;
        }
        Integer userId = Integer.valueOf(String.valueOf(object));
        sessionPools.remove(userId);
        addUserNumber(-1);
        logger.warn("{title:'清除用户连接', mUserId:"+userId+", currentLinkNumber:"+getUserNumber()+"}");
    }

    // 接收到客户端的消息后调用
    @OnMessage
    public void onMessage(Session websocketSession, String message) throws IOException {
        // System.out.println("接收到客户端消息："+message);
        /*if (message.contains("close")) {
            websocketSession.close();
        }*/
        // newsOption();
    }

    // 出现异常后被调用
    @OnError
    public void onError(Session session, Throwable throwable) {
        Object object = session.getUserProperties().get("userId");
        Integer userId = null;
        if (null != object) {
            userId = Integer.valueOf(String.valueOf(object));
        }
        logger.error("{title:'用户连接异常', mUserId:"+userId+", errorContent:"+throwable.getMessage()+"}");
    }

}

