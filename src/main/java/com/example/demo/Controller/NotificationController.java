package com.example.demo.Controller;

import com.example.demo.DTOs.ApiResponse;
import com.example.demo.Model.Notification;
import com.example.demo.Service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class NotificationController {
    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Notification>>> getMyNotifications(
            Authentication auth,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = (Long) auth.getCredentials();
        return ResponseEntity.ok(ApiResponse.success(notificationService.getForUser(userId, page, size)));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount(Authentication auth) {
        Long userId = (Long) auth.getCredentials();
        return ResponseEntity.ok(ApiResponse.success(notificationService.countUnread(userId)));
    }

    @PatchMapping("/mark-all-read")
    public ResponseEntity<ApiResponse<Void>> markAllRead(Authentication auth) {
        Long userId = (Long) auth.getCredentials();
        notificationService.markAllRead(userId);
        return ResponseEntity.ok(ApiResponse.success("All notifications marked as read", null));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markRead(@PathVariable Long id, Authentication auth) {
        Long userId = (Long) auth.getCredentials();
        notificationService.markRead(id, userId);
        return ResponseEntity.ok(ApiResponse.success("Notification marked as read", null));
    }
}