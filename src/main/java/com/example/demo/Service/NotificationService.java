package com.example.demo.Service;

import com.example.demo.Model.Notification;
import com.example.demo.Repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notificationRepository;

    public void send(Long userId, String title, String message, String type, Long refId, String refType) {
        Notification notification = Notification.builder()
                .userId(userId)
                .title(title)
                .message(message)
                .type(type)
                .referenceId(refId)
                .referenceType(refType)
                .build();
        notificationRepository.save(notification);
    }

    public List<Notification> getForUser(Long userId, int page, int size) {
        return notificationRepository.findByUserId(userId, page, size);
    }

    public long countUnread(Long userId) {
        return notificationRepository.countUnread(userId);
    }

    public void markAllRead(Long userId) {
        notificationRepository.markAllRead(userId);
    }

    public void markRead(Long id, Long userId) {
        notificationRepository.markRead(id, userId);
    }
}