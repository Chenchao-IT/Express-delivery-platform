package com.campus.express.service;

import com.campus.express.entity.SystemMessage;
import com.campus.express.repository.SystemMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final SystemMessageRepository systemMessageRepository;

    private final Map<Long, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Long userId) {
        SseEmitter emitter = new SseEmitter(0L);
        emitters.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(emitter);
        emitter.onCompletion(() -> remove(userId, emitter));
        emitter.onTimeout(() -> remove(userId, emitter));
        emitter.onError((e) -> remove(userId, emitter));

        sendEvent(userId, emitter, "connected", Map.of(
            "ts", Instant.now().toEpochMilli(),
            "unreadCount", unreadCount(userId)
        ));
        return emitter;
    }

    @Transactional(readOnly = true)
    public List<SystemMessage> listUserMessages(Long userId) {
        return systemMessageRepository.findByReceiverIdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public long unreadCount(Long userId) {
        return systemMessageRepository.countByReceiverIdAndReadFalse(userId);
    }

    @Transactional
    public void markAllRead(Long userId) {
        List<SystemMessage> messages = systemMessageRepository.findByReceiverIdOrderByCreatedAtDesc(userId);
        boolean changed = false;
        for (SystemMessage message : messages) {
            if (!Boolean.TRUE.equals(message.getRead())) {
                message.setRead(true);
                changed = true;
            }
        }
        if (changed) {
            systemMessageRepository.saveAll(messages);
            broadcast(userId, "message_read_all", Map.of("unreadCount", 0));
        }
    }

    @Transactional
    public SystemMessage createMessage(Long userId, String type, String title, String content) {
        if (userId == null) {
            return null;
        }

        SystemMessage message = new SystemMessage();
        message.setReceiverId(userId);
        message.setType(type);
        message.setTitle(title);
        message.setContent(content);
        message.setRead(false);
        SystemMessage saved = systemMessageRepository.save(message);

        broadcast(userId, type, toPayload(saved, unreadCount(userId)));
        return saved;
    }

    public void notifyUser(Long userId, String type, Object payload) {
        if (userId == null) {
            return;
        }
        broadcast(userId, type, payload);
    }

    private Map<String, Object> toPayload(SystemMessage message, long unreadCount) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", message.getId());
        payload.put("type", message.getType());
        payload.put("title", message.getTitle());
        payload.put("content", message.getContent());
        payload.put("read", message.getRead());
        payload.put("createdAt", message.getCreatedAt());
        payload.put("unreadCount", unreadCount);
        return payload;
    }

    private void broadcast(Long userId, String eventName, Object payload) {
        List<SseEmitter> list = emitters.get(userId);
        if (list == null || list.isEmpty()) {
            return;
        }
        for (SseEmitter emitter : list) {
            sendEvent(userId, emitter, eventName, payload);
        }
    }

    private void sendEvent(Long userId, SseEmitter emitter, String eventName, Object payload) {
        try {
            emitter.send(SseEmitter.event().name(eventName).data(payload));
        } catch (IOException e) {
            remove(userId, emitter);
        }
    }

    private void remove(Long userId, SseEmitter emitter) {
        List<SseEmitter> list = emitters.get(userId);
        if (list != null) {
            list.remove(emitter);
        }
    }
}
